package org.matsim.scenarioCreation.counts;

import lombok.val;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import java.util.List;

public class CombinedCountsWriter<T> {

    private List<Counts<T>> countsList;

    public void addCounts(Counts<T> counts) {
        this.countsList.add(counts);
    }

    public void write(String filename) {

        val combinedCounts = new Counts<T>();
        countsList.forEach(counts -> counts.getCounts().forEach((id, count) -> {
            combinedCounts.getCounts().put(id, count);
        }));
        val writer = new CountsWriter(combinedCounts);
        writer.write(filename);
    }
}
