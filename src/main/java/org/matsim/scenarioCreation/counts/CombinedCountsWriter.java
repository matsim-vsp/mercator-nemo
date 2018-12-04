package org.matsim.scenarioCreation.counts;

import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

public class CombinedCountsWriter<T> {

    private static final Logger logger = LoggerFactory.getLogger(CombinedCountsWriter.class);

    private List<Counts<T>> countsList = new ArrayList<>();

    public void addCounts(Counts<T> counts) {
        this.countsList.add(counts);
    }

    public void write(String filename) {

        Counts<T> combinedCounts = new Counts<>();
        countsList.forEach(counts -> counts.getCounts().forEach((id, count) -> {
            // can't use map and flat map since 'getcounts' returns a treemap which doesn't implement streaming
            combinedCounts.getCounts().put(id, count);
        }));
        CountsWriter writer = new CountsWriter(combinedCounts);
        writer.write(filename);
    }

    @SafeVarargs
    public static void writeCounts(Path directory, String filenamePrefix, Set<String> columnCombinations, Map<String, Counts<Link>>... countsMaps) {

        // create a separate counts file for each column combination
        // each counts file contains all counts long term and short term count stations
        columnCombinations.forEach(combination -> {
            CombinedCountsWriter<Link> writer = new CombinedCountsWriter<>();
            Arrays.stream(countsMaps).forEach(map -> writer.addCounts(map.get(combination)));
            logger.info("writing counts to folder: " + directory.toString());
            writer.write(directory.resolve(filenamePrefix + "_counts_" + combination + ".xml").toString());
        });
    }
}
