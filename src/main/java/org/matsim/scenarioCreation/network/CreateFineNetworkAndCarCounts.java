package org.matsim.scenarioCreation.network;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import lombok.val;

public class CreateFineNetworkAndCarCounts {

    public static void main(String[] args) {

        // parse input variables
        val arguments = new CreateFineNetworkAndCarCounts.InputArguments();
        JCommander.newBuilder().addObject(arguments).build().parse(args);


    }

    private static class InputArguments {

        @Parameter(names = "-svnDir", required = true,
                description = "Path to the checked out https://svn.vsp.tu-berlin.de/repos/shared-svn root folder")
        private String svnDir;
    }
}
