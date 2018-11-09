package org.matsim.scenarioCreation.counts;

import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import java.util.ArrayList;
import java.util.List;

public class CombinedCountsWriter<T> {

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
}
