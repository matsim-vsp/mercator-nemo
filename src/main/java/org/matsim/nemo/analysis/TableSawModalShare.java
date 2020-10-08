package org.matsim.nemo.analysis;


import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.nemo.runners.NemoModeLocationChoiceMainModeIdentifier;
import tech.tablesaw.aggregate.AggregateFunctions;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

public class TableSawModalShare {

    public static void main(String[] args) throws Exception {


        EventsManager manager = EventsUtils.createEventsManager();
        TripEventHandler handler = new TripEventHandler(new NemoModeLocationChoiceMainModeIdentifier(), id -> true);
        manager.addHandler(handler);
        new MatsimEventsReader(manager).readFile("C:\\Users\\Janekdererste\\Desktop\\deurb\\output-base\\baseCase.output_events.xml.gz");

        var persons = handler.getTrips();
        Table tripTable = Table.create("Trips")
                .addColumns(
                        StringColumn.create("personId"),
                        IntColumn.create("tripNo"),
                        StringColumn.create("mode")
                );

        for (var trips : persons.entrySet()) {

            for (var i = 0; i < trips.getValue().size(); i++) {

                var trip = trips.getValue().get(i);

                tripTable.stringColumn("personId").append(trips.getKey().toString());
                tripTable.intColumn("tripNo").append(i);
                tripTable.stringColumn("mode").append(trip.getMainMode());
            }
        }

        tripTable.print();

        var sumModal = tripTable.summarize("mode", AggregateFunctions.count).by("mode");

        sumModal.print();

     /*   Plot.show(
                VerticalBarPlot.create(
                        "test", // plot title
                        sumModal, // table
                        "mode", // grouping column name
                        "number of trips")); // numeric column name

      */
    }
}
