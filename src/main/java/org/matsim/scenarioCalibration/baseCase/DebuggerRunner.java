package org.matsim.scenarioCalibration.baseCase;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.scenarioCalibration.marginals.RuhrAgentsFilter;
import org.matsim.util.NEMOUtils;
import playground.vsp.cadyts.marginals.AgentFilter;
import playground.vsp.cadyts.marginals.ModalDistanceAnalysisModule;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;

import java.util.Arrays;

public class DebuggerRunner {

    private static final String ruhrShapeFile = "/ruhrgebiet_boundary.shp";

    private final String configPath;
    private final String runId;
    private final String outputDir;
    private final String inputDir;
    private Config config;
    private Scenario scenario;
    private Controler controler;

    public DebuggerRunner(String configPath, String runId, String outputDir, String inputDir) {

        this.configPath = configPath;
        this.runId = runId;
        this.outputDir = outputDir;
        this.inputDir = inputDir;
    }

    public static void main(String[] args) {

        DebuggerRunner.InputArguments arguments = new InputArguments();
        JCommander.newBuilder().addObject(arguments).build().parse(args);
    }

    public void run() {
        if (controler == null) prepareControler();
        controler.run();
        System.exit(0);
    }

    private Controler prepareControler(AbstractQSimModule... overridingQSimModule) {

        if (scenario == null) prepareScenario();

        controler = new Controler(scenario);

        // use the (congested) car travel time for the teleported ride mode
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
                addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());
            }
        });

        // use special analysis for modalShare
        controler.addOverridingModule(NEMOUtils.createModalShareAnalysis());

        // add Modal Distance analysis
        DistanceDistribution distanceDistribution = NEMOUtils.getDistanceDistribution(
                config.counts().getCountsScaleFactor(),
                scenario.getConfig().plansCalcRoute(),
                false, false
        );
        controler.addOverridingModule(new ModalDistanceAnalysisModule(distanceDistribution));
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(AgentFilter.class).to(RuhrAgentsFilter.class);
                bind(Key.get(String.class, Names.named(RuhrAgentsFilter.ruhr_boundary_shape))).toInstance(inputDir + ruhrShapeFile);
            }
        });

        // add overridingQSimModules from method parameters
        Arrays.stream(overridingQSimModule).forEach(controler::addOverridingQSimModule);

        return controler;
    }

    private Scenario prepareScenario() {

        if (config == null) prepareConfig();

        scenario = ScenarioUtils.loadScenario(config);
        return scenario;
    }

    private Config prepareConfig(ConfigGroup... customModules) {

        OutputDirectoryLogging.catchLogEntries();
        config = ConfigUtils.loadConfig(configPath, customModules);
        config.controler().setRunId(runId);
        config.controler().setOutputDirectory(outputDir);
        config.facilities().setAddEmptyActivityOption(false);
        config.facilities().setRemovingLinksAndCoordinates(false); //keeping coordinates
        config.facilities().setAssigningOpeningTime(false);
        config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.onePerActivityLocationInPlansFile);
        config.plansCalcRoute().setInsertingAccessEgressWalk(true);
        config.qsim().setUsingTravelTimeCheckInTeleportation(true);
        config.subtourModeChoice().setProbaForRandomSingleTripMode(0.5);

        final long minDuration = 600;
        final long maxDuration = 3600 * 27;
        final long difference = 600;
        addTypicalDurations("home", minDuration, maxDuration, difference);
        addTypicalDurations("work", minDuration, maxDuration, difference);
        addTypicalDurations("education", minDuration, maxDuration, difference);
        addTypicalDurations("leisure", minDuration, maxDuration, difference);
        addTypicalDurations("shopping", minDuration, maxDuration, difference);
        addTypicalDurations("other", minDuration, maxDuration, difference);

        return config;
    }

    private void addTypicalDurations(String type, long minDurationInSeconds, long maxDurationInSeconds, long durationDifferenceInSeconds) {

        for (long duration = minDurationInSeconds; duration <= maxDurationInSeconds; duration += durationDifferenceInSeconds) {
            final PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams(type + "_" + duration + ".0");
            params.setTypicalDuration(duration);
            config.planCalcScore().addActivityParams(params);
        }
    }


    /**
     * Arguments for invocation from the command line
     */
    private static class InputArguments {

        @Parameter(names = "-configPath", required = true)
        private String configPath;

        @Parameter(names = "-runId", required = true)
        private String runId;

        @Parameter(names = "-outputDir", required = true)
        private String outputDir;

        @Parameter(names = "-inputDir", required = true)
        private String inputDir;
    }
}
