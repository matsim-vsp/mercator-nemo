package org.matsim.nemo;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class InvestigateMurmoData {

    @Parameter(names = "-inputFile")
    private String inputFile = "";

    @Parameter(names = "-outputFile")
    private String outputFile = "";

    private int unequalTransitions = 0;
    private int equalTransitions;

    public static void main(String[] args) throws IOException {

        InvestigateMurmoData converter = new InvestigateMurmoData();
        JCommander.newBuilder().addObject(converter).build().parse(args);
        converter.convert();
    }

    private void convert() throws IOException {

        int relationsCounter = 0;
        int recordCounter = 0;

        Map<String, Double> transitions = new HashMap<>();


        try (CSVPrinter printer = CSVFormat.DEFAULT.print(Paths.get(outputFile), Charset.defaultCharset())) {
            try (FileReader reader = new FileReader(Paths.get(inputFile).toString())) {

                for (CSVRecord record : CSVFormat.newFormat(',').parse(reader)) {
                    if (record.getRecordNumber() != 1) {
                        final long cellIndex = record.getRecordNumber() - 2;
                        if (recordCounter != cellIndex)
                            throw new RuntimeException(recordCounter + " vs. " + cellIndex);
                        for (int i = 0; i < record.size(); i++) {

                            if (record.size() != 4653) {
                                throw new RuntimeException("Should always have 4653 entries");
                            }

                            double value = Double.parseDouble(record.get(i));
                            String transitionKey = cellIndex + "_" + i;
                            String reverseTransitionKey = i + "_" + cellIndex;

                            if (transitions.containsKey(transitionKey)) {
                                compareTransitionValues(transitions, transitionKey, value);
                            } else if (transitions.containsKey(reverseTransitionKey)) {
                                compareTransitionValues(transitions, reverseTransitionKey, value);
                            } else {
                                transitions.put(transitionKey, value);
                            }
                            relationsCounter++;
                        }
                        recordCounter++;
                    }
                }
            }
        }

        System.out.println(transitions.size());
        System.out.println(equalTransitions);
        System.out.println(unequalTransitions);
        System.out.println(relationsCounter);
        System.out.println(recordCounter);
    }

    private void compareTransitionValues(Map<String, Double> transitions, String key, double currentValue) {
        double transitionValue = transitions.get(key);
        if (currentValue * -1 != transitionValue) {
            unequalTransitions++;
        } else {
            equalTransitions++;
        }
    }
}
