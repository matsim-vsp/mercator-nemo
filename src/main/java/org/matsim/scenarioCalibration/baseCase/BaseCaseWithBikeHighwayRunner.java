package org.matsim.scenarioCalibration.baseCase;

import com.beust.jcommander.JCommander;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.scenarioCreation.network.BikeLinkSpeedCalculator;

public class BaseCaseWithBikeHighwayRunner {

    public static void main(String[] args) {

        RunnerInputArguments arguments = new RunnerInputArguments();
        JCommander.newBuilder().addObject(arguments).build().parse(args);

        BaseCaseCalibrationRunner runner = new BaseCaseCalibrationRunner(
                arguments.getConfigPath(), arguments.getRunId(), arguments.getOutputDir(), arguments.getInputDir());

        Controler controler = runner.prepareControler(new AbstractQSimModule() {

            @Override
            protected void configureQSim() {
                bind(QNetworkFactory.class).toProvider(new Provider<QNetworkFactory>() {
                    @Inject
                    private EventsManager events;

                    @Override
                    public QNetworkFactory get() {
                        final ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(events, runner.getScenario());
                        factory.setLinkSpeedCalculator(new BikeLinkSpeedCalculator());
                        return factory;
                    }
                });
            }
        });

        controler.run();
        System.exit(0);
    }
}
