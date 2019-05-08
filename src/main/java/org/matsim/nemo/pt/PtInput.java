package org.matsim.nemo.pt;



import java.nio.file.Path;
import java.nio.file.Paths;

public class PtInput {

    private static final String GTFS_FILE = "projects/nemo_mercator/data/pt/google_transit_vrr_2018_05_11_corrected-v3.zip";
    private static final String OSM_SCHEDULE_FILE = "projects/nemo_mercator/data/pt/ptNetworkScheduleFileFromOSM.xml";

    private String svnDir;

    public PtInput(String svnDir) {
        this.svnDir = svnDir;
    }

    public Path getGtfsFile() {
        return Paths.get(svnDir, GTFS_FILE);
    }

    public Path getOsmScheduleFile() {
        return Paths.get(svnDir, OSM_SCHEDULE_FILE);
    }
}
