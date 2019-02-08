package org.matsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.scenarioCalibration.marginals.RuhrAgentsFilter;
import playground.vsp.cadyts.marginals.BeelineDistanceCollector;
import playground.vsp.cadyts.marginals.prep.DistanceBin;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;
import playground.vsp.cadyts.marginals.prep.DistanceDistributionUtils;
import playground.vsp.cadyts.marginals.prep.ModalDistanceBinIdentifier;

import java.io.BufferedWriter;
import java.util.Map;
import java.util.SortedMap;

public class CadytsModeAnalysisDebugging {
    private final String configFile = "/Users/ihab/Documents/workspace/runs-svn/nemo/marginals/run307/output/run307.output_config.xml";
    private final String eventsFile = "C:\\Users\\Janek\\runs-svn\\nemo\\marginals\\run307\\output\\run307.output_events.xml.gz";
    private final String networkFile = "C:\\Users\\Janek\\runs-svn\\nemo\\marginals\\run307\\output\\run307.output_network.xml.gz";
    private final String shapeFile = "C:/Users/Janek/shared-svn/projects/nemo_mercator/data/original_files/shapeFiles/shapeFile_Ruhrgebiet/ruhrgebiet_boundary.shp";

    public static void main(String[] args) {
        CadytsModeAnalysisDebugging anaMain = new CadytsModeAnalysisDebugging();
        anaMain.run();
    }

    private static double getBeelineDistanceFactor(
            Map<String, PlansCalcRouteConfigGroup.ModeRoutingParams> modeRoutingParams,
            String modeKey,
            double defaultValue) {
        return modeRoutingParams.containsKey(modeKey) ? modeRoutingParams.get(modeKey).getBeelineDistanceFactor() : defaultValue;
    }

    private static double getScalingFactor(Double scalingFactor) {
        return scalingFactor == null ? 1.0 : scalingFactor;
    }

    private void run() {

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(networkFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        EventsManager events = EventsUtils.createEventsManager();

        DistanceDistribution inputDistanceDistribution = new DistanceDistribution();


        RuhrAgentsFilter filter = new RuhrAgentsFilter(scenario, shapeFile);
        BeelineDistanceCollector handler1 = new BeelineDistanceCollector(scenario, inputDistanceDistribution, events, filter);
        PlansCalcRouteConfigGroup plansCalcRouteConfigGroup = config.plansCalcRoute();
        Map<String, PlansCalcRouteConfigGroup.ModeRoutingParams> modeRoutingParamsMap = plansCalcRouteConfigGroup.getModeRoutingParams();

        inputDistanceDistribution.setBeelineDistanceFactorForNetworkModes(
                TransportMode.car, getBeelineDistanceFactor(modeRoutingParamsMap, TransportMode.car, 1.3));
        inputDistanceDistribution.setBeelineDistanceFactorForNetworkModes(
                TransportMode.bike, getBeelineDistanceFactor(modeRoutingParamsMap, TransportMode.bike, 1.3));
        inputDistanceDistribution.setBeelineDistanceFactorForNetworkModes(
                TransportMode.walk, getBeelineDistanceFactor(modeRoutingParamsMap, TransportMode.walk, 1.3));
        inputDistanceDistribution.setBeelineDistanceFactorForNetworkModes(
                TransportMode.ride, getBeelineDistanceFactor(modeRoutingParamsMap, TransportMode.ride, 1.3));

        inputDistanceDistribution.addToDistribution(TransportMode.car, new DistanceBin.DistanceRange(0, 1000000.), 123456789);
        inputDistanceDistribution.addToDistribution(TransportMode.ride, new DistanceBin.DistanceRange(0, 1000000.), 123456789);
        inputDistanceDistribution.addToDistribution(TransportMode.bike, new DistanceBin.DistanceRange(0, 1000000.), 123456789);
        inputDistanceDistribution.addToDistribution(TransportMode.walk, new DistanceBin.DistanceRange(0, 1000000.), 123456789);

        events.addHandler(handler1);

        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(eventsFile);

        writeData(handler1.getOutputDistanceDistribution(), "C:\\Users\\Janek\\Desktop\\run307_amitsAnalyse.csv");

    }

    private void writeData(DistanceDistribution averages, String fileName) {

        try (BufferedWriter writer = IOUtils.getBufferedWriter(fileName)) {
            writer.write(DistanceDistributionUtils.DistanceDistributionFileLabels.mode + "\t" +
                    DistanceDistributionUtils.DistanceDistributionFileLabels.distanceLowerLimit + "\t" +
                    DistanceDistributionUtils.DistanceDistributionFileLabels.distanceUpperLimit + "\t" +
                    DistanceDistributionUtils.DistanceDistributionFileLabels.measuredCount + "\t" +
                    "simulationCount");
            writer.newLine();


            for (SortedMap.Entry<ModalDistanceBinIdentifier, DistanceBin> entry : DistanceDistributionUtils.getSortedMap(averages).entrySet()) {
                writer.write(
                        entry.getKey().getMode() + "\t"
                                + entry.getKey().getDistanceRange().getLowerLimit() + "\t"
                                + entry.getKey().getDistanceRange().getUpperLimit() + "\t" +
                                averages.getModalBinToDistanceBin()
                                        .get(entry.getKey().getId())
                                        .getCount() + "\t" +
                                entry.getValue().getCount() * getScalingFactor(averages.getModeToScalingFactor().get(entry.getKey().getMode())));
                writer.newLine();
            }
        } catch (Exception e) {
            throw new RuntimeException("Data is not written. Reason :" + e);
        }
    }

}
