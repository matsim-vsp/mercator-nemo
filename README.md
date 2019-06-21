# mercator-nemo
Neue Emscher Mobilität

[![Build Status](https://travis-ci.org/matsim-vsp/mercator-nemo.svg?branch=master)](https://travis-ci.org/matsim-vsp/mercator-nemo)

## Project Set Up
This project uses the [Lombok](https://projectlombok.org/features/all) project. It generates Setters, Getters, Constructors and other things via annotations. This helps to reduce boilerplate code. To help your IDE understand Lombok it must know about it. The following steps are necessary:

* IntelliJ: File -> Settings -> Plugins: Click 'Browse repositories...' -> Search for 'lombok plugin': Click 'install'
* Eclipse: [Download](https://projectlombok.org/download) lombok. Execute downloaded jar via double-click. Select the Eclipese instances you want to install the plugin for.

Lombok is also configured as a maven dependency. The build process doesn't require any further steps.

## Network and counts creation
The project requires several network versions. Each version of the network can be created with a separate scripts located under ```org.matsim.nemo.scenarioCreation.network ```. All files consumed and genarated by the scripts in this package can be found in the VSP's shared repository. All paths to input and output files are encapsulated in input- and output-Classes. E.g. ```org.matsim.nemo.scenarioCreation.network.NeworkInput```

Since counts are usually mapped to links in the matsim network, they should be generated in one step. The required input files for counts can be found at ```org.matsim.nemo.counts.CountsInput```. 

As an example for network and counts creation use ```org.matsim.nemo.scenarioCreation.network.CreateCoarseNetworkAndCarCounts```. This script reads in the input files from ```NetworkInput``` and ```CountsInput```. After processing, a network file and a counts file with similar names are written into a subfolder of the network input folder withing the VSP's SVN.
