package org.matsim.nemo.analysis;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Collectors;

public class MoversToCsv {

    private static final String MURMO_SHAPE_FILE = "projects\\nemo_mercator\\data\\original_files\\murmo\\Ruhr_Grid_1km\\Ruhr_Grid_1km_EW.shp";
    private static final Comparator<SimpleFeature> featureComparator = Comparator.comparingLong(feature -> (Long) (feature.getAttribute("ID_Gitter_")));
    private static final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:25832", TransformationFactory.WGS84);

    @Parameter(names = {"-input"})
    private String inputFile = "";

    @Parameter(names = {"-output"})
    private String outputFile = "";

    @Parameter(names = {"-shared-svn"})
    private String sharedSvn = "";

    public static void main(String[] args) throws IOException {

        var analysis = new MoversToCsv();
        JCommander.newBuilder().addObject(analysis).build().parse(args);
        analysis.run();
    }

    private void run() throws IOException {

        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(inputFile);

        var sharedSvnPath = Paths.get(sharedSvn);
        // read in murmo transition raster
        var murmoFeatures = ShapeFileReader.getAllFeatures(sharedSvnPath.resolve(MURMO_SHAPE_FILE).toString()).stream()
                .sorted(featureComparator)
                .map(feature -> (Geometry) feature.getDefaultGeometry())
                .collect(Collectors.toList());


        try (Writer writer = Files.newBufferedWriter(Paths.get(outputFile))) {
            try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

                //print header
                printer.printRecord("id", "fromId", "fromLat", "fromLon", "toId", "toLat", "toLon", "moved", "moved_all_acts");

                //print moving relations of all persons
                scenario.getPopulation().getPersons().values().stream()
                        .filter(person -> person.getAttributes().getAttribute("was_moved") != null)
                        .forEach(person -> {

                            long sourceFeatureIndex = (long) person.getAttributes().getAttribute("source-feature");
                            long destinationFeatureIndex = (long) person.getAttributes().getAttribute("destination-feature");
                            boolean moved = person.getAttributes().getAttribute("was_moved") != null;
                            boolean movedAllActivities = person.getAttributes().getAttribute("moved_all_activities") != null;
                            var sourceFeature = murmoFeatures.get((int) sourceFeatureIndex);
                            var destinationFeature = murmoFeatures.get((int) destinationFeatureIndex);

                            var fromCoord = transformation.transform(new Coord(sourceFeature.getCentroid().getX(), sourceFeature.getCentroid().getY()));
                            var toCoord = transformation.transform(new Coord(destinationFeature.getCentroid().getX(), destinationFeature.getCentroid().getY()));

                            // iih, this is ugly
                            try {
                                printer.printRecord(person.getId(), sourceFeatureIndex, fromCoord.getY(), fromCoord.getX(),
                                        destinationFeatureIndex, toCoord.getY(), toCoord.getX(), moved, movedAllActivities);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            }
        }
    }
}
