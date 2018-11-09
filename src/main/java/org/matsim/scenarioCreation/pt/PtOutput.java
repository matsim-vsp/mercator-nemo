package org.matsim.scenarioCreation.pt;


import java.nio.file.Path;
import java.nio.file.Paths;

public class PtOutput {

    private static final String transitSchedule = "projects/nemo_mercator/data/matsim_input/pt/transit_schedule.xml.gz";
    private static final String transitVehicles = "projects/nemo_mercator/data/matsim_input/pt/transit_vehicles.xml.gz";

    private final String svnDir;

    public PtOutput(String svnDir) {
        this.svnDir = svnDir;
    }

    public Path getTransitScheduleFile() {
        return Paths.get(svnDir, transitSchedule);
    }

    public Path getTransitVehiclesFile() {
        return Paths.get(svnDir, transitVehicles);
    }
}
