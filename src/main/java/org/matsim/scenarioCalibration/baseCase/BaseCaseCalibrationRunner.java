package org.matsim.scenarioCalibration.baseCase;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;

public class BaseCaseCalibrationRunner {

    public static void main(String[] args) {

        InputArguments arguments = new InputArguments();
        JCommander.newBuilder().addObject(arguments).build().parse(args);

        Config config = ConfigUtils.loadConfig(arguments.configPath);
        config.controler().setRunId(arguments.runId);
        config.controler().setOutputDirectory(arguments.outputDir);
        config.plansCalcRoute().setInsertingAccessEgressWalk(true);

        //vspDefaults
        config.qsim().setUsingTravelTimeCheckInTeleportation(true);
        config.subtourModeChoice().setProbaForRandomSingleTripMode(0.5);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);

        // ride gets network routes but teleported.
        // TODO: move to config file
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addTravelTimeBinding("ride").to(networkTravelTime());
                this.addTravelDisutilityFactoryBinding("ride").to(carTravelDisutilityFactoryKey());
            }
        });

        // use fast pt router
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                install(new SwissRailRaptorModule());
            }
        });

        //analysis
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addControlerListenerBinding().to(ModalShareControlerListener.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);
            }
        });

        controler.run();
    }

    private static class InputArguments {

        @Parameter(names = "-configPath", required = true)
        private String configPath;

        @Parameter(names = "-runId", required = true)
        private String runId;

        @Parameter(names = "-outputDir", required = true)
        private String outputDir;
    }
}
