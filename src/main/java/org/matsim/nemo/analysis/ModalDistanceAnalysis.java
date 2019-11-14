package org.matsim.nemo.analysis;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacility;
import org.matsim.nemo.RuhrAgentsFilter;
import org.matsim.nemo.runners.NemoModeLocationChoiceMainModeIdentifier;
import org.matsim.nemo.util.ExpectedModalDistanceDistribution;
import playground.vsp.cadyts.marginals.DistanceDistribution;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ModalDistanceAnalysis {

	private static Logger logger = Logger.getLogger(ModalDistanceAnalysis.class);

	@Parameter(names = {"-eventFile", "-ef"}, required = true)
	private List<String> eventFiles = new ArrayList<>();

	@Parameter(names = {"-networkFile", "-nf"}, required = true)
	private String networkFile = "";

	@Parameter(names = {"-populationFile", "-pf"}, required = true)
	private String populationFile = "";

	@Parameter(names = {"-scalingFactor", "-sf"})
	private double scalingFactor = 100;

    @Parameter(names = {"-ruhrShape", "-rs"})
    private String ruhrShapeFile;

    @Parameter(names = {"-outputFile", "-of"})
    private String outputFile;

	private Scenario scenario;
	private Network network;

	public static void main(String[] args) throws IOException {

		ModalDistanceAnalysis analysis = new ModalDistanceAnalysis();
		JCommander.newBuilder().addObject(analysis).build().parse(args);
		analysis.run();
	}

	private void run() throws IOException {

		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(populationFile);
        RuhrAgentsFilter agentsFilter = new RuhrAgentsFilter(this.scenario, ShapeFileReader.getAllFeatures(this.ruhrShapeFile));

		network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);

		DistanceDistribution expectedDistanceDistribution = ExpectedModalDistanceDistribution.create();


		List<NamedDistanceDistribution> result = eventFiles.parallelStream()
                .map(eventFile -> parseEventFile(Paths.get(eventFile), expectedDistanceDistribution, agentsFilter))
				.collect(Collectors.toList());

		// write results to an excel file
        XSSFWorkbook wb = new XSSFWorkbook();

        Sheet sheet = wb.createSheet("modal-distance-split");
		Row titleRow = sheet.createRow(0);
		titleRow.createCell(0).setCellValue("mode");
		titleRow.createCell(1).setCellValue("lower limit");
		titleRow.createCell(2).setCellValue("upper limit");
		titleRow.createCell(3).setCellValue("expected count");

		int cellIndex = 4;
		for (NamedDistanceDistribution namedDistanceDistribution : result) {

			titleRow.createCell(cellIndex).setCellValue(namedDistanceDistribution.name);
			cellIndex++;
		}

		// now, write values of expected
		Collection<DistanceDistribution.DistanceBin> expectedBins = expectedDistanceDistribution.getDistanceBins().stream()
				.sorted(this::compareBinsByModeAndDistanceRange)
				.collect(Collectors.toList());

		int rowIndex = 1; // start with one, since we used 0 for header
		for (DistanceDistribution.DistanceBin expectedBin : expectedBins) {

			Row row = sheet.createRow(rowIndex);
			row.createCell(0).setCellValue(expectedBin.getMode());
			row.createCell(1).setCellValue(expectedBin.getDistanceRange().getLowerLimit());
			row.createCell(2).setCellValue(expectedBin.getDistanceRange().getUpperLimit());
			row.createCell(3).setCellValue(expectedBin.getValue());
			rowIndex++;
		}

		// now write values of simulated
		rowIndex = 1;
		cellIndex = 4;

		for (NamedDistanceDistribution namedDistanceDistribution : result) {

			List<DistanceDistribution.DistanceBin> bins = namedDistanceDistribution.distanceDistribution.getDistanceBins().stream()
					.sorted(this::compareBinsByModeAndDistanceRange)
					.collect(Collectors.toList());

			for (DistanceDistribution.DistanceBin bin : bins) {

				sheet.getRow(rowIndex).createCell(cellIndex).setCellValue(bin.getValue() * scalingFactor);
				rowIndex++;
			}
			rowIndex = 1;
			cellIndex++;
		}

        // create second sheet with general modal split
        Sheet modalSplitSheet = wb.createSheet("modal-split");
        Row modalTitleRow = modalSplitSheet.createRow(0);
        modalTitleRow.createCell(0).setCellValue("mode");
        modalTitleRow.createCell(1).setCellValue("expected count");

        cellIndex = 2;
        for (NamedDistanceDistribution namedDistanceDistribution : result) {
            modalTitleRow.createCell(cellIndex).setCellValue(namedDistanceDistribution.name);
            cellIndex++;
        }

        // now calculate expected modal split
        Map<String, Double> expectedShare = new HashMap<>();
        for (DistanceDistribution.DistanceBin distanceBin : expectedDistanceDistribution.getDistanceBins()) {
            expectedShare.merge(distanceBin.getMode(), distanceBin.getValue(), Double::sum);
        }

        Collection<ModeShare> expectedModeShares = expectedShare.entrySet().stream()
                .map(share -> new ModeShare(share.getKey(), share.getValue()))
                .sorted(Comparator.comparing(share -> share.name))
                .collect(Collectors.toList());


        rowIndex = 1;
        for (ModeShare share : expectedModeShares) {

            Row row = modalSplitSheet.createRow(rowIndex);
            row.createCell(0).setCellValue(share.name);
            row.createCell(1).setCellValue(share.value);
            rowIndex++;
        }

        rowIndex = 1;
        cellIndex = 2;

        // now caluculate modal share of event files

        for (NamedDistanceDistribution namedDistanceDistribution : result) {

            Map<String, Double> simulatedShare = new HashMap<>();
            for (DistanceDistribution.DistanceBin distanceBin : namedDistanceDistribution.distanceDistribution.getDistanceBins()) {
                simulatedShare.merge(distanceBin.getMode(), distanceBin.getValue(), Double::sum);
            }
            Collection<ModeShare> simulatedModeShares = simulatedShare.entrySet().stream()
                    .map(share -> new ModeShare(share.getKey(), share.getValue()))
                    .sorted(Comparator.comparing(share -> share.name))
                    .collect(Collectors.toList());
            for (ModeShare simulatedModeShare : simulatedModeShares) {
                modalSplitSheet.getRow(rowIndex).createCell(cellIndex).setCellValue(simulatedModeShare.value * scalingFactor);
                rowIndex++;
            }
            rowIndex = 1;
            cellIndex++;
        }

        try (OutputStream fileOut = new FileOutputStream(outputFile)) {
			wb.write(fileOut);
		}

        logger.info("Finished writing analysis to: " + outputFile);
	}

	private int compareBinsByModeAndDistanceRange(DistanceDistribution.DistanceBin bin1, DistanceDistribution.DistanceBin bin2) {
		int mode = bin1.getMode().compareTo(bin2.getMode());
		return (mode == 0) ? Double.compare(bin1.getDistanceRange().getLowerLimit(), bin2.getDistanceRange().getLowerLimit()) : mode;
	}

    private NamedDistanceDistribution parseEventFile(Path file, DistanceDistribution expectedDistribution, RuhrAgentsFilter agentsFilter) {

		EventsManager manager = EventsUtils.createEventsManager();

        TripEventHandler tripEventHandler = new TripEventHandler(new NemoModeLocationChoiceMainModeIdentifier(), agentsFilter::includeAgent);

		manager.addHandler(tripEventHandler);
		new MatsimEventsReader(manager).readFile(file.toString());

		DistanceDistribution simulatedDistribution = expectedDistribution.copyWithEmptyBins();

        tripEventHandler.getTrips().entrySet().parallelStream().flatMap(entry -> entry.getValue().stream())
				.forEach(trip -> {
					double distance = calculateBeelineDistance(trip);
                    simulatedDistribution.increaseCountByOne(trip.getMainMode(), distance);
				});

		String runId = file.getFileName().toString().split("[.]")[0];

        if (tripEventHandler.getStuckPersons().size() > 0) {
            logger.warn("Run: " + runId + " had " + tripEventHandler.getStuckPersons().size() + " stuck agents.");
        }

		return new NamedDistanceDistribution(runId, simulatedDistribution);
	}

	private double calculateBeelineDistance(TripEventHandler.Trip trip) {

        if (scenario.getActivityFacilities().getFacilities().containsKey(trip.getDepartureFacility()) ||
                scenario.getActivityFacilities().getFacilities().containsKey(trip.getArrivalFacility())
		) {
            ActivityFacility departureFacility = scenario.getActivityFacilities().getFacilities().get(trip.getDepartureFacility());
            ActivityFacility arrivalFacility = scenario.getActivityFacilities().getFacilities().get(trip.getArrivalFacility());
			return CoordUtils.calcEuclideanDistance(departureFacility.getCoord(), arrivalFacility.getCoord());
		} else {
            Link departureLink = network.getLinks().get(trip.getDepartureLink());
            Link arrivalLink = network.getLinks().get(trip.getArrivalLink());
			return CoordUtils.calcEuclideanDistance(departureLink.getToNode().getCoord(), arrivalLink.getToNode().getCoord());
		}
	}

	private static class NamedDistanceDistribution {
		private String name;
		private DistanceDistribution distanceDistribution;

		private NamedDistanceDistribution(String name, DistanceDistribution distanceDistribution) {
			this.name = name;
			this.distanceDistribution = distanceDistribution;
		}
	}

    private static class ModeShare {
        private String name;
        private double value;

        ModeShare(String name, double value) {
            this.name = name;
            this.value = value;
        }
    }
}
