package org.matsim.nemo.analysis;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;

public class PopulationToCsv {

    private static final String MURMO_SHAPE_FILE = "projects\\nemo_mercator\\data\\original_files\\murmo\\Ruhr_Grid_1km\\Ruhr_Grid_1km_EW.shp";
    private static final Comparator<SimpleFeature> featureComparator = Comparator.comparingLong(feature -> (Long) (feature.getAttribute("ID_Gitter_")));

    @Parameter(names = {"-input"})
    private String inputFile = "";

    @Parameter(names = {"-output"})
    private String outputFile = "";

    public static void main(String[] args) throws IOException {

        var analysis = new PopulationToCsv();
        JCommander.newBuilder().addObject(analysis).build().parse(args);
        analysis.run();
    }

    private void run() throws IOException {

        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(inputFile);


        try (Writer writer = Files.newBufferedWriter(Paths.get(outputFile))) {
            try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

                //print header
                printer.printRecord("id", "x", "y", "moved", "moved_all_acts");

                //print moving relations of all persons
                scenario.getPopulation().getPersons().values()
                        //.filter(person -> person.getAttributes().getAttribute("was_moved") != null)
                        .forEach(person -> {


                            var id = person.getId();
                            var homeCoord = person.getSelectedPlan().getPlanElements().stream()
                                    .filter(el -> el instanceof Activity)
                                    .map(el -> (Activity) el)
                                    .filter(act -> act.getType().startsWith("home"))
                                    .findAny().get().getCoord();
                            boolean moved = person.getAttributes().getAttribute("was_moved") != null;
                            boolean movedAllActivities = person.getAttributes().getAttribute("moved_all_activities") != null;


                            // iih, this is ugly
                            try {
                                printer.printRecord(person.getId(), homeCoord.getX(), homeCoord.getY(), moved, movedAllActivities);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            }
        }
    }
}

