package org.matsim.nemo;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.*;

public class NetworkBaseCaseContinue2 {

    private static final String inputNetwork = "D:/Arbeit/mercator-nemo/ruhrgebiet-v1.0-network-with-RSV.xml.gz";
    private static final String outputNetwork = "D:/Arbeit/mercator-nemo/testNetwork.xml";

    private static Network network = readNetwork();

    private static List<LinkAttributes> linksAttributes = new ArrayList<>();
    private static List<Link> links = new ArrayList<>();
    private static List<Node> nodes = new ArrayList<>();

    public static void main(String[] args) {

        addLinks_MuensterM_LotteOsnabrueck();
        addLinks_KoelnMuelheim_Leverkusen();
        addLinks_KoelnNiehl_Leverkusen();
        addLinks_Hagen_Westhofen();
        addLinks_Westring_Sonnborn();
        addLinks_DuisburgSerm_DuisburgRahm();
        addLinks_Herne_RecklinghausenHerten();

        addNodes_Loehne_Rehme();
        addNodes_BielefeldBrackwede_Borgholzhausen();
        addNodes_BielefeldBrackwede_Borgholzhausen_Zubringer();
        addNodes_DuesseldorfRatingen_Velbert();

        generateModifiedNetwork();

        new NetworkWriter(network).write(outputNetwork);
    }

    private static void addNodes_DuesseldorfRatingen_Velbert() {
        List<NodeAttributes> nodes = new ArrayList<>();
        final String preFix = "DuesseldorfRatingen_Velbert";

        nodes.add(new NodeAttributes(preFix + "1A", 353372.486, 5685090.744));
        nodes.add(new NodeAttributes(preFix + "2", 353916.842, 5685339.902));
        nodes.add(new NodeAttributes(preFix + "3", 354428.699, 5685819.261));
        nodes.add(new NodeAttributes(preFix + "4", 355154.508, 5686073.835));
        nodes.add(new NodeAttributes(preFix + "5", 355614.908, 5686396.116));
        nodes.add(new NodeAttributes(preFix + "6", 356421.964, 5686550.485));
        nodes.add(new NodeAttributes(preFix + "7A", 357323.807, 5686642.566));
        nodes.add(new NodeAttributes(preFix + "8", 358377.312, 5687067.759));
        nodes.add(new NodeAttributes(preFix + "9", 359956.216, 5687330.458));
        nodes.add(new NodeAttributes(preFix + "10", 361437.623, 5687915.438));
        nodes.add(new NodeAttributes(preFix + "11A", 361608.242, 5688500.418));

        addNodes(nodes);
        buildingLinks(nodes, 2, 4000, 33.33333333333333);
    }

    private static void addNodes_BielefeldBrackwede_Borgholzhausen_Zubringer() {
        List<NodeAttributes> nodes = new ArrayList<>();
        final String preFix = "BielefeldBrackwede_Borgholzhausen_Zubringer";

        nodes.add(new NodeAttributes(preFix + "1A", 464296.553, 5758168.792));
        nodes.add(new NodeAttributes(preFix + "2", 464040.896, 5757289.156));
        nodes.add(new NodeAttributes(preFix + "3", 463364.919, 5756565.514));
        nodes.add(new NodeAttributes(preFix + "4", 462515.615, 5755724.876));
        nodes.add(new NodeAttributes(preFix + "5A", 461341.323, 5755655.546));

        addNodes(nodes);
        buildingLinks(nodes, 1, 1800, 22.22222222222222);
    }

    private static void addNodes_BielefeldBrackwede_Borgholzhausen() {
        List<NodeAttributes> nodes = new ArrayList<>();
        final String preFix = "BielefeldBrackwede_Borgholzhausen";

        nodes.add(new NodeAttributes(preFix + "1A", 448273.526, 5769994.048));
        nodes.add(new NodeAttributes(preFix + "2", 449436.986, 5768938.918));
        nodes.add(new NodeAttributes(preFix + "3", 450478.033, 5768834.922));
        nodes.add(new NodeAttributes(preFix + "4", 451500.664, 5768947.584));
        nodes.add(new NodeAttributes(preFix + "5", 453012.945, 5768444.935));
        nodes.add(new NodeAttributes(preFix + "6", 454464.561, 5767664.962));
        nodes.add(new NodeAttributes(preFix + "7", 454785.217, 5767014.985));
        nodes.add(new NodeAttributes(preFix + "8A", 454689.887, 5766109.349));
        nodes.add(new NodeAttributes(preFix + "9", 455014.875, 5765372.708));
        nodes.add(new NodeAttributes(preFix + "10", 456362.495, 5764358.744));
        nodes.add(new NodeAttributes(preFix + "11A", 457723.115, 5763700.1));

        addNodes(nodes);
        buildingLinks(nodes, 2, 4000, 33.33333333333333);
    }

    private static void addNodes_Loehne_Rehme() {
        List<NodeAttributes> nodes = new ArrayList<>();
        final String preFix = "Loehne_Rehme_";

        nodes.add(new NodeAttributes(preFix + "1A", 482634.933, 5783669.391));
        nodes.add(new NodeAttributes(preFix + "2", 483133.249, 5783979.213));
        nodes.add(new NodeAttributes(preFix + "3", 483341.242, 5784449.364));
        nodes.add(new NodeAttributes(preFix + "4", 483302.243, 5785279.168));
        nodes.add(new NodeAttributes(preFix + "5", 483770.227, 5785844.649));
        nodes.add(new NodeAttributes(preFix + "6", 484890.354, 5786522.792));
        nodes.add(new NodeAttributes(preFix + "7A", 485873.987, 5786923.611));
        nodes.add(new NodeAttributes(preFix + "8", 486911.784, 5787142.437));
        nodes.add(new NodeAttributes(preFix + "9A", 487466.432, 5786901.945));
        nodes.add(new NodeAttributes(preFix + "10", 487763.255, 5786078.641));
        nodes.add(new NodeAttributes(preFix + "11A", 488125.076, 5785281.335));

        addNodes(nodes);
        buildingLinks(nodes, 2, 4000, 27.77777777777778);
    }

    private static void addNodes(List<NodeAttributes> nodesAttributes) {
        for (NodeAttributes node : nodesAttributes) {
            nodes.add(NetworkUtils.createNode(Id.createNodeId(node.NodeId), new Coord(node.xCoord, node.yCoord)));
        }
    }

    private static void buildingLinks(List<NodeAttributes> nodesAttributes, double lanes, double capacity, double freeSpeed) {
        Iterator<NodeAttributes> iterator = nodesAttributes.listIterator();
        NodeAttributes nodeAttributes1 = iterator.next();
        while (iterator.hasNext()) {
            NodeAttributes nodeAttributes2 = iterator.next();
            conectTwoNodes(nodeAttributes1, nodeAttributes2, lanes, capacity, freeSpeed);
            nodeAttributes1 = nodeAttributes2;
        }
    }

    private static void conectTwoNodes(NodeAttributes n1, NodeAttributes n2, double lanes, double capacity, double freeSpeed) {
        NetworkFactory nf = network.getFactory();

        Set<String> modes = new HashSet<>();
        modes.add(TransportMode.car);
        modes.add(TransportMode.ride);

        Node node1 = NetworkUtils.createNode(Id.createNodeId(n1.NodeId), new Coord(n1.xCoord, n1.yCoord));
        Node node2 = NetworkUtils.createNode(Id.createNodeId(n2.NodeId), new Coord(n2.xCoord, n2.yCoord));
        Link link1 = nf.createLink(Id.createLinkId(n1.NodeId + "_" + n2.NodeId), node1, node2);
        Link link2 = nf.createLink(Id.createLinkId(n2.NodeId + "_" + n1.NodeId), node2, node1);

        link1.setFreespeed(freeSpeed);
        link2.setFreespeed(freeSpeed);
        link1.setCapacity(capacity);
        link2.setCapacity(capacity);
        link1.setNumberOfLanes(lanes);
        link2.setNumberOfLanes(lanes);
        link1.setAllowedModes(modes);
        link2.setAllowedModes(modes);

        links.add(link1);
        links.add(link2);
    }

    private static void addLinks_Herne_RecklinghausenHerten() {
        new LinkAttributes("425917", 3, 6000);
        new LinkAttributes("425895", 3, 6000);
        new LinkAttributes("483602", 3, 6000);
        new LinkAttributes("440369", 3, 6000);
        new LinkAttributes("425901", 3, 6000);
        new LinkAttributes("421174", 3, 6000);
        new LinkAttributes("78817", 3, 6000);
        new LinkAttributes("171289", 3, 6000);
        new LinkAttributes("171299", 3, 6000);
        new LinkAttributes("409591", 3, 6000);
        new LinkAttributes("78801", 3, 6000);
        new LinkAttributes("79408", 3, 6000);
        new LinkAttributes("272294", 3, 6000);
        new LinkAttributes("272296", 3, 6000);
        new LinkAttributes("504111", 3, 6000);
        new LinkAttributes("231675", 3, 6000);
        new LinkAttributes("231690", 3, 6000);
        new LinkAttributes("231669", 3, 6000);
        new LinkAttributes("231680", 3, 6000);
        new LinkAttributes("231633", 3, 6000);
        new LinkAttributes("234410", 3, 6000);
        new LinkAttributes("29522", 3, 6000);
        new LinkAttributes("29539", 3, 6000);
        new LinkAttributes("79420", 3, 6000);
        new LinkAttributes("79425", 3, 6000);
        new LinkAttributes("79105", 3, 6000);
        new LinkAttributes("79103", 3, 6000);
        new LinkAttributes("371600", 3, 6000);
        new LinkAttributes("371599", 3, 6000);
        new LinkAttributes("397339", 3, 6000);
        new LinkAttributes("39566", 3, 6000);
        new LinkAttributes("440404", 3, 6000);
        new LinkAttributes("203319", 3, 6000);
        new LinkAttributes("12062", 3, 6000);
        new LinkAttributes("12063", 3, 6000);
        new LinkAttributes("512139", 3, 6000);
        new LinkAttributes("512129", 3, 6000);
        new LinkAttributes("320942", 3, 6000);
        new LinkAttributes("320937", 3, 6000);
        new LinkAttributes("352671", 3, 6000);
        new LinkAttributes("85690", 3, 6000);
        new LinkAttributes("85333", 3, 6000);
        new LinkAttributes("157001", 3, 6000);
        new LinkAttributes("157013", 3, 6000);

        new LinkAttributes("85508", 3, 6000);
        new LinkAttributes("157002", 3, 6000);
        new LinkAttributes("157010", 3, 6000);
        new LinkAttributes("422682", 3, 6000);
        new LinkAttributes("85689", 3, 6000);
        new LinkAttributes("85332", 3, 6000);
        new LinkAttributes("352670", 3, 6000);
        new LinkAttributes("320934", 3, 6000);
        new LinkAttributes("320945", 3, 6000);
        new LinkAttributes("512126", 3, 6000);
        new LinkAttributes("512132", 3, 6000);
        new LinkAttributes("12070", 3, 6000);
        new LinkAttributes("12064", 3, 6000);
        new LinkAttributes("20312", 3, 6000);
        new LinkAttributes("440386", 3, 6000);
        new LinkAttributes("171290", 3, 6000);
        new LinkAttributes("371602", 3, 6000);
        new LinkAttributes("371601", 3, 6000);
        new LinkAttributes("425893", 3, 6000);
        new LinkAttributes("425899", 3, 6000);
        new LinkAttributes("440372", 3, 6000);
        new LinkAttributes("440403", 3, 6000);
        new LinkAttributes("421171", 3, 6000);
        new LinkAttributes("421172", 3, 6000);
        new LinkAttributes("78224", 3, 6000);
        new LinkAttributes("396502", 3, 6000);
        new LinkAttributes("510095", 3, 6000);
        new LinkAttributes("396463", 3, 6000);
        new LinkAttributes("78226", 3, 6000);
        new LinkAttributes("78221", 3, 6000);
        new LinkAttributes("272295", 3, 6000);
        new LinkAttributes("272293", 3, 6000);
        new LinkAttributes("178447", 3, 6000);
        new LinkAttributes("231662", 3, 6000);
        new LinkAttributes("231622", 3, 6000);
        new LinkAttributes("231687", 3, 6000);
        new LinkAttributes("231674", 3, 6000);
        new LinkAttributes("234409", 3, 6000);
        new LinkAttributes("29487", 3, 6000);
        new LinkAttributes("186105", 3, 6000);
        new LinkAttributes("105659", 3, 6000);
        new LinkAttributes("105660", 3, 6000);
        new LinkAttributes("104611", 3, 6000);
    }

    /**
     * von Gregor
     */
    private static void addLinks_MuensterM_LotteOsnabrueck() {
        linksAttributes.add(new LinkAttributes("454963", 3, 6000));
        linksAttributes.add(new LinkAttributes("300431", 3, 6000));
        linksAttributes.add(new LinkAttributes("300476", 3, 6000));
        linksAttributes.add(new LinkAttributes("136062", 3, 6000));
        linksAttributes.add(new LinkAttributes("136087", 3, 6000));
        linksAttributes.add(new LinkAttributes("136089", 3, 6000));
        linksAttributes.add(new LinkAttributes("136088", 3, 6000));
        linksAttributes.add(new LinkAttributes("300508", 3, 6000));
        linksAttributes.add(new LinkAttributes("299881", 3, 6000));
        linksAttributes.add(new LinkAttributes("47936", 3, 6000));
        linksAttributes.add(new LinkAttributes("47937", 3, 6000));
        linksAttributes.add(new LinkAttributes("302738", 3, 6000));
        linksAttributes.add(new LinkAttributes("302741", 3, 6000));
        linksAttributes.add(new LinkAttributes("507566", 3, 6000));
        linksAttributes.add(new LinkAttributes("83648", 3, 6000));
        linksAttributes.add(new LinkAttributes("83665", 3, 6000));
        linksAttributes.add(new LinkAttributes("168163", 3, 6000));
        linksAttributes.add(new LinkAttributes("168153", 3, 6000));
        linksAttributes.add(new LinkAttributes("169855", 3, 6000));
        linksAttributes.add(new LinkAttributes("169926", 3, 6000));
        linksAttributes.add(new LinkAttributes("47673", 3, 6000));
        linksAttributes.add(new LinkAttributes("168146", 3, 6000));
        linksAttributes.add(new LinkAttributes("361452", 3, 6000));
        linksAttributes.add(new LinkAttributes("168172", 3, 6000));
        linksAttributes.add(new LinkAttributes("302034", 3, 6000));
        linksAttributes.add(new LinkAttributes("226024", 3, 6000));
        linksAttributes.add(new LinkAttributes("130125", 3, 6000));
        linksAttributes.add(new LinkAttributes("226031", 3, 6000));
        linksAttributes.add(new LinkAttributes("180768", 3, 6000));
        linksAttributes.add(new LinkAttributes("170024", 3, 6000));
        linksAttributes.add(new LinkAttributes("169884", 3, 6000));
        linksAttributes.add(new LinkAttributes("226028", 3, 6000));
        linksAttributes.add(new LinkAttributes("274102", 3, 6000));
        linksAttributes.add(new LinkAttributes("170022", 3, 6000));
        linksAttributes.add(new LinkAttributes("262263", 3, 6000));
        linksAttributes.add(new LinkAttributes("274101", 3, 6000));
        linksAttributes.add(new LinkAttributes("262245", 3, 6000));
        linksAttributes.add(new LinkAttributes("262241", 3, 6000));
        linksAttributes.add(new LinkAttributes("169907", 3, 6000));
        linksAttributes.add(new LinkAttributes("169975", 3, 6000));
        linksAttributes.add(new LinkAttributes("215753", 3, 6000));
        linksAttributes.add(new LinkAttributes("215749", 3, 6000));
        linksAttributes.add(new LinkAttributes("169876", 3, 6000));
        linksAttributes.add(new LinkAttributes("169929", 3, 6000));
        linksAttributes.add(new LinkAttributes("169892", 3, 6000));
        linksAttributes.add(new LinkAttributes("169892", 3, 6000));
        linksAttributes.add(new LinkAttributes("302034", 3, 6000));
        linksAttributes.add(new LinkAttributes("264349", 3, 6000));
        linksAttributes.add(new LinkAttributes("169976", 3, 6000));
        linksAttributes.add(new LinkAttributes("226040", 3, 6000));
        linksAttributes.add(new LinkAttributes("59687", 3, 6000));
        linksAttributes.add(new LinkAttributes("265676", 3, 6000));
        linksAttributes.add(new LinkAttributes("170253", 3, 6000));
        linksAttributes.add(new LinkAttributes("169847", 3, 6000));
        linksAttributes.add(new LinkAttributes("289346", 3, 6000));
        linksAttributes.add(new LinkAttributes("169914", 3, 6000));
        linksAttributes.add(new LinkAttributes("183111", 3, 6000));
        linksAttributes.add(new LinkAttributes("183106", 3, 6000));
        linksAttributes.add(new LinkAttributes("498054", 3, 6000));
        linksAttributes.add(new LinkAttributes("498058", 3, 6000));
        linksAttributes.add(new LinkAttributes("262260", 3, 6000));
        linksAttributes.add(new LinkAttributes("262235", 3, 6000));
        linksAttributes.add(new LinkAttributes("262242", 3, 6000));
        linksAttributes.add(new LinkAttributes("350297", 3, 6000));
        linksAttributes.add(new LinkAttributes("130124", 3, 6000));
        linksAttributes.add(new LinkAttributes("226062", 3, 6000));
        linksAttributes.add(new LinkAttributes("226062", 3, 6000));
        linksAttributes.add(new LinkAttributes("350299", 3, 6000));
        linksAttributes.add(new LinkAttributes("226016", 3, 6000));
        linksAttributes.add(new LinkAttributes("466127", 3, 6000));
        linksAttributes.add(new LinkAttributes("253655", 3, 6000));
        linksAttributes.add(new LinkAttributes("253652", 3, 6000));
        linksAttributes.add(new LinkAttributes("467280", 3, 6000));
        linksAttributes.add(new LinkAttributes("226053", 3, 6000));
        linksAttributes.add(new LinkAttributes("169967", 3, 6000));
        linksAttributes.add(new LinkAttributes("226065", 3, 6000));
        linksAttributes.add(new LinkAttributes("226056", 3, 6000));
        linksAttributes.add(new LinkAttributes("169968", 3, 6000));
        linksAttributes.add(new LinkAttributes("226021", 3, 6000));
        linksAttributes.add(new LinkAttributes("183107", 3, 6000));
        linksAttributes.add(new LinkAttributes("170019", 3, 6000));
        linksAttributes.add(new LinkAttributes("188724", 3, 6000));
        linksAttributes.add(new LinkAttributes("188718", 3, 6000));
        linksAttributes.add(new LinkAttributes("460074", 3, 6000));
        linksAttributes.add(new LinkAttributes("460069", 3, 6000));
        linksAttributes.add(new LinkAttributes("460073", 3, 6000));
        linksAttributes.add(new LinkAttributes("460076", 3, 6000));
        linksAttributes.add(new LinkAttributes("86897", 3, 6000));
        linksAttributes.add(new LinkAttributes("86902", 3, 6000));
        linksAttributes.add(new LinkAttributes("86863", 3, 6000));
        linksAttributes.add(new LinkAttributes("86872", 3, 6000));
        linksAttributes.add(new LinkAttributes("226017", 3, 6000));
        linksAttributes.add(new LinkAttributes("170023", 3, 6000));
        linksAttributes.add(new LinkAttributes("170021", 3, 6000));
        linksAttributes.add(new LinkAttributes("169948", 3, 6000));
        linksAttributes.add(new LinkAttributes("169966", 3, 6000));
        linksAttributes.add(new LinkAttributes("462015", 3, 6000));
        linksAttributes.add(new LinkAttributes("462031", 3, 6000));
        linksAttributes.add(new LinkAttributes("462026", 3, 6000));
        linksAttributes.add(new LinkAttributes("462022", 3, 6000));
        linksAttributes.add(new LinkAttributes("226041", 3, 6000));
        linksAttributes.add(new LinkAttributes("134825", 3, 6000));

        linksAttributes.add(new LinkAttributes("225966", 3, 6000));
        linksAttributes.add(new LinkAttributes("462030", 3, 6000));
        linksAttributes.add(new LinkAttributes("462018", 3, 6000));
        linksAttributes.add(new LinkAttributes("462029", 3, 6000));
        linksAttributes.add(new LinkAttributes("462021", 3, 6000));
        linksAttributes.add(new LinkAttributes("186856", 3, 6000));
        linksAttributes.add(new LinkAttributes("186860", 3, 6000));
        linksAttributes.add(new LinkAttributes("186855", 3, 6000));
        linksAttributes.add(new LinkAttributes("186857", 3, 6000));
        linksAttributes.add(new LinkAttributes("86928", 3, 6000));
        linksAttributes.add(new LinkAttributes("86929", 3, 6000));
        linksAttributes.add(new LinkAttributes("86875", 3, 6000));
        linksAttributes.add(new LinkAttributes("86888", 3, 6000));
        linksAttributes.add(new LinkAttributes("460066", 3, 6000));
        linksAttributes.add(new LinkAttributes("460072", 3, 6000));
        linksAttributes.add(new LinkAttributes("460075", 3, 6000));
        linksAttributes.add(new LinkAttributes("460067", 3, 6000));
        linksAttributes.add(new LinkAttributes("169874", 3, 6000));
        linksAttributes.add(new LinkAttributes("188729", 3, 6000));
        linksAttributes.add(new LinkAttributes("188719", 3, 6000));
        linksAttributes.add(new LinkAttributes("170006", 3, 6000));
        linksAttributes.add(new LinkAttributes("169986", 3, 6000));
        linksAttributes.add(new LinkAttributes("169993", 3, 6000));
        linksAttributes.add(new LinkAttributes("183105", 3, 6000));
        linksAttributes.add(new LinkAttributes("183110", 3, 6000));
        linksAttributes.add(new LinkAttributes("467283", 3, 6000));
        linksAttributes.add(new LinkAttributes("467286", 3, 6000));
        linksAttributes.add(new LinkAttributes("253653", 3, 6000));
        linksAttributes.add(new LinkAttributes("253654", 3, 6000));
        linksAttributes.add(new LinkAttributes("226052", 3, 6000));
        linksAttributes.add(new LinkAttributes("226055", 3, 6000));
        linksAttributes.add(new LinkAttributes("226059", 3, 6000));
        linksAttributes.add(new LinkAttributes("226018", 3, 6000));
        linksAttributes.add(new LinkAttributes("226061", 3, 6000));
        linksAttributes.add(new LinkAttributes("226038", 3, 6000));
        linksAttributes.add(new LinkAttributes("350289", 3, 6000));
        linksAttributes.add(new LinkAttributes("350292", 3, 6000));
        linksAttributes.add(new LinkAttributes("350298", 3, 6000));
        linksAttributes.add(new LinkAttributes("85776", 3, 6000));
        linksAttributes.add(new LinkAttributes("85777", 3, 6000));
        linksAttributes.add(new LinkAttributes("183108", 3, 6000));
        linksAttributes.add(new LinkAttributes("183219", 3, 6000));
        linksAttributes.add(new LinkAttributes("169845", 3, 6000));
        linksAttributes.add(new LinkAttributes("289354", 3, 6000));
        linksAttributes.add(new LinkAttributes("169904", 3, 6000));
        linksAttributes.add(new LinkAttributes("169904", 3, 6000));
        linksAttributes.add(new LinkAttributes("170005", 3, 6000));
        linksAttributes.add(new LinkAttributes("301472", 3, 6000));
        linksAttributes.add(new LinkAttributes("301462", 3, 6000));
        linksAttributes.add(new LinkAttributes("59691", 3, 6000));
        linksAttributes.add(new LinkAttributes("59693", 3, 6000));
        linksAttributes.add(new LinkAttributes("264363", 3, 6000));
        linksAttributes.add(new LinkAttributes("264358", 3, 6000));
        linksAttributes.add(new LinkAttributes("169934", 3, 6000));
        linksAttributes.add(new LinkAttributes("169915", 3, 6000));
        linksAttributes.add(new LinkAttributes("169920", 3, 6000));
        linksAttributes.add(new LinkAttributes("169945", 3, 6000));
        linksAttributes.add(new LinkAttributes("169945", 3, 6000));
        linksAttributes.add(new LinkAttributes("215747", 3, 6000));
        linksAttributes.add(new LinkAttributes("215750", 3, 6000));
        linksAttributes.add(new LinkAttributes("169869", 3, 6000));
        linksAttributes.add(new LinkAttributes("170001", 3, 6000));
        linksAttributes.add(new LinkAttributes("169946", 3, 6000));
        linksAttributes.add(new LinkAttributes("169977", 3, 6000));
        linksAttributes.add(new LinkAttributes("262238", 3, 6000));
        linksAttributes.add(new LinkAttributes("262267", 3, 6000));
        linksAttributes.add(new LinkAttributes("262270", 3, 6000));
        linksAttributes.add(new LinkAttributes("262259", 3, 6000));
        linksAttributes.add(new LinkAttributes("274097", 3, 6000));
        linksAttributes.add(new LinkAttributes("169950", 3, 6000));
        linksAttributes.add(new LinkAttributes("170010", 3, 6000));
        linksAttributes.add(new LinkAttributes("169881", 3, 6000));
        linksAttributes.add(new LinkAttributes("180759", 3, 6000));
        linksAttributes.add(new LinkAttributes("180767", 3, 6000));
        linksAttributes.add(new LinkAttributes("130110", 3, 6000));
        linksAttributes.add(new LinkAttributes("53713", 3, 6000));
        linksAttributes.add(new LinkAttributes("301913", 3, 6000));
        linksAttributes.add(new LinkAttributes("301912", 3, 6000));
        linksAttributes.add(new LinkAttributes("53702", 3, 6000));
        linksAttributes.add(new LinkAttributes("168170", 3, 6000));
        linksAttributes.add(new LinkAttributes("168169", 3, 6000));
        linksAttributes.add(new LinkAttributes("168148", 3, 6000));
        linksAttributes.add(new LinkAttributes("168145", 3, 6000));
        linksAttributes.add(new LinkAttributes("169922", 3, 6000));
        linksAttributes.add(new LinkAttributes("169875", 3, 6000));
        linksAttributes.add(new LinkAttributes("60537", 3, 6000));
        linksAttributes.add(new LinkAttributes("168156", 3, 6000));
        linksAttributes.add(new LinkAttributes("168168", 3, 6000));
        linksAttributes.add(new LinkAttributes("83657", 3, 6000));
        linksAttributes.add(new LinkAttributes("83662", 3, 6000));
        linksAttributes.add(new LinkAttributes("302737", 3, 6000));
        linksAttributes.add(new LinkAttributes("302734", 3, 6000));
        linksAttributes.add(new LinkAttributes("501808", 3, 6000));
        linksAttributes.add(new LinkAttributes("48647", 3, 6000));
        linksAttributes.add(new LinkAttributes("463679", 3, 6000));
        linksAttributes.add(new LinkAttributes("48654", 3, 6000));
        linksAttributes.add(new LinkAttributes("300520", 3, 6000));
        linksAttributes.add(new LinkAttributes("300513", 3, 6000));
        linksAttributes.add(new LinkAttributes("136090", 3, 6000));
        linksAttributes.add(new LinkAttributes("136084", 3, 6000));
        linksAttributes.add(new LinkAttributes("136091", 3, 6000));
        linksAttributes.add(new LinkAttributes("136083", 3, 6000));
        linksAttributes.add(new LinkAttributes("300494", 3, 6000));
        linksAttributes.add(new LinkAttributes("300493", 3, 6000));
        linksAttributes.add(new LinkAttributes("300437", 3, 6000));
        linksAttributes.add(new LinkAttributes("300436", 3, 6000));
        linksAttributes.add(new LinkAttributes("454982", 3, 6000));
        linksAttributes.add(new LinkAttributes("501901", 3, 6000));
        linksAttributes.add(new LinkAttributes("21295", 3, 6000));
    }

    /**
     * von Gregor
     */
    private static void addLinks_KoelnMuelheim_Leverkusen() {
        linksAttributes.add(new LinkAttributes("61355", 4, 8000));
        linksAttributes.add(new LinkAttributes("478238", 4, 8000));
        linksAttributes.add(new LinkAttributes("478241", 4, 8000));
        linksAttributes.add(new LinkAttributes("148327", 4, 8000));
        linksAttributes.add(new LinkAttributes("151277", 4, 8000));
        linksAttributes.add(new LinkAttributes("394559", 4, 8000));
        linksAttributes.add(new LinkAttributes("332372", 4, 8000));
        linksAttributes.add(new LinkAttributes("148090", 4, 8000));
        linksAttributes.add(new LinkAttributes("192406", 4, 8000));
        linksAttributes.add(new LinkAttributes("148328", 4, 8000));
        linksAttributes.add(new LinkAttributes("219204", 4, 8000));
        linksAttributes.add(new LinkAttributes("148089", 4, 8000));
        linksAttributes.add(new LinkAttributes("148330", 4, 8000));
        linksAttributes.add(new LinkAttributes("255701", 4, 8000));
        linksAttributes.add(new LinkAttributes("476668", 4, 8000));
        linksAttributes.add(new LinkAttributes("370863", 4, 8000));
        linksAttributes.add(new LinkAttributes("255706", 4, 8000));
        linksAttributes.add(new LinkAttributes("230110", 4, 8000));
        linksAttributes.add(new LinkAttributes("422336", 4, 8000));
        //
        linksAttributes.add(new LinkAttributes("230093", 4, 8000));
        linksAttributes.add(new LinkAttributes("230105", 4, 8000));
        linksAttributes.add(new LinkAttributes("65320", 4, 8000));
        linksAttributes.add(new LinkAttributes("65320", 4, 8000));
        linksAttributes.add(new LinkAttributes("255703", 4, 8000));
        linksAttributes.add(new LinkAttributes("476656", 4, 8000));
        linksAttributes.add(new LinkAttributes("184840", 4, 8000));
        linksAttributes.add(new LinkAttributes("52502", 4, 8000));
        linksAttributes.add(new LinkAttributes("65323", 4, 8000));
        linksAttributes.add(new LinkAttributes("431516", 4, 8000));
        linksAttributes.add(new LinkAttributes("152764", 4, 8000));
        linksAttributes.add(new LinkAttributes("65343", 4, 8000));
        linksAttributes.add(new LinkAttributes("332373", 4, 8000));
        linksAttributes.add(new LinkAttributes("332376", 4, 8000));
        linksAttributes.add(new LinkAttributes("152643", 4, 8000));
        linksAttributes.add(new LinkAttributes("151289", 4, 8000));
        linksAttributes.add(new LinkAttributes("61359", 4, 8000));
        linksAttributes.add(new LinkAttributes("61358", 4, 8000));
        linksAttributes.add(new LinkAttributes("61333", 4, 8000));
    }

    /**
     * von Gregor
     */
    private static void addLinks_KoelnNiehl_Leverkusen() {
        linksAttributes.add(new LinkAttributes("161255", 4, 8000));
        linksAttributes.add(new LinkAttributes("203991", 4, 8000));
        linksAttributes.add(new LinkAttributes("161224", 4, 8000));
        linksAttributes.add(new LinkAttributes("150239", 4, 8000));
        linksAttributes.add(new LinkAttributes("188379", 4, 8000));
        linksAttributes.add(new LinkAttributes("150242", 4, 8000));
        linksAttributes.add(new LinkAttributes("161252", 4, 8000));
        linksAttributes.add(new LinkAttributes("161214", 4, 8000));
        linksAttributes.add(new LinkAttributes("161228", 4, 8000));
        linksAttributes.add(new LinkAttributes("66148", 4, 8000));
        linksAttributes.add(new LinkAttributes("66140", 4, 8000));
        linksAttributes.add(new LinkAttributes("34919", 4, 8000));
        linksAttributes.add(new LinkAttributes("67855", 4, 8000));
        linksAttributes.add(new LinkAttributes("202837", 4, 8000));
        linksAttributes.add(new LinkAttributes("67851", 4, 8000));
        linksAttributes.add(new LinkAttributes("34922", 4, 8000));
        linksAttributes.add(new LinkAttributes("67867", 4, 8000));
        linksAttributes.add(new LinkAttributes("494291", 4, 8000));
        linksAttributes.add(new LinkAttributes("34918", 4, 8000));
        linksAttributes.add(new LinkAttributes("223459", 4, 8000));
        linksAttributes.add(new LinkAttributes("161477", 4, 8000));
        linksAttributes.add(new LinkAttributes("67868", 4, 8000));
        linksAttributes.add(new LinkAttributes("177877", 4, 8000));
        linksAttributes.add(new LinkAttributes("248088", 4, 8000));
        linksAttributes.add(new LinkAttributes("248087", 4, 8000));
        linksAttributes.add(new LinkAttributes("98901", 4, 8000));
        linksAttributes.add(new LinkAttributes("153757", 4, 8000));
        linksAttributes.add(new LinkAttributes("12115", 4, 8000));
        linksAttributes.add(new LinkAttributes("80177", 4, 8000));
        linksAttributes.add(new LinkAttributes("253341", 4, 8000));
        linksAttributes.add(new LinkAttributes("248086", 4, 8000));
        linksAttributes.add(new LinkAttributes("391240", 4, 8000));
        linksAttributes.add(new LinkAttributes("67870", 4, 8000));
        linksAttributes.add(new LinkAttributes("139704", 4, 8000));
        linksAttributes.add(new LinkAttributes("223461", 4, 8000));
        linksAttributes.add(new LinkAttributes("223460", 4, 8000));
        linksAttributes.add(new LinkAttributes("67865", 4, 8000));
        linksAttributes.add(new LinkAttributes("150195", 4, 8000));
        linksAttributes.add(new LinkAttributes("67858", 4, 8000));
        linksAttributes.add(new LinkAttributes("67823", 4, 8000));
        linksAttributes.add(new LinkAttributes("139695", 4, 8000));
        linksAttributes.add(new LinkAttributes("34914", 4, 8000));
        linksAttributes.add(new LinkAttributes("66116", 4, 8000));
        linksAttributes.add(new LinkAttributes("351956", 4, 8000));
        linksAttributes.add(new LinkAttributes("66144", 4, 8000));
        linksAttributes.add(new LinkAttributes("149575", 4, 8000));
        linksAttributes.add(new LinkAttributes("341111", 4, 8000));
        linksAttributes.add(new LinkAttributes("161229", 4, 8000));
        linksAttributes.add(new LinkAttributes("341112", 4, 8000));
        linksAttributes.add(new LinkAttributes("341113", 4, 8000));
        linksAttributes.add(new LinkAttributes("161223", 4, 8000));
        linksAttributes.add(new LinkAttributes("150698", 4, 8000));
        linksAttributes.add(new LinkAttributes("66172", 4, 8000));
        linksAttributes.add(new LinkAttributes("505869", 4, 8000));
        linksAttributes.add(new LinkAttributes("449211", 4, 8000));
    }

    /**
     * von Gregor
     */
    private static void addLinks_Hagen_Westhofen() {
        linksAttributes.add(new LinkAttributes("494734", 3, 6000));
        linksAttributes.add(new LinkAttributes("245830", 3, 6000));
        linksAttributes.add(new LinkAttributes("245831", 3, 6000));
        linksAttributes.add(new LinkAttributes("356898", 3, 6000));
        linksAttributes.add(new LinkAttributes("74360", 3, 6000));
        linksAttributes.add(new LinkAttributes("45244", 3, 6000));
        linksAttributes.add(new LinkAttributes("435518", 3, 6000));
        linksAttributes.add(new LinkAttributes("435524", 3, 6000));
        linksAttributes.add(new LinkAttributes("48134", 3, 6000));
        linksAttributes.add(new LinkAttributes("266683", 3, 6000));
        linksAttributes.add(new LinkAttributes("145835", 3, 6000));
        linksAttributes.add(new LinkAttributes("353300", 3, 6000));
        linksAttributes.add(new LinkAttributes("353311", 3, 6000));
        linksAttributes.add(new LinkAttributes("353297", 3, 6000));
        linksAttributes.add(new LinkAttributes("256269", 3, 6000));
        linksAttributes.add(new LinkAttributes("353310", 3, 6000));
        linksAttributes.add(new LinkAttributes("349977", 3, 6000));
        linksAttributes.add(new LinkAttributes("349982", 3, 6000));
        linksAttributes.add(new LinkAttributes("73172", 3, 6000));
        linksAttributes.add(new LinkAttributes("73171", 3, 6000));
        linksAttributes.add(new LinkAttributes("73175", 3, 6000));
        linksAttributes.add(new LinkAttributes("201441", 3, 6000));
        linksAttributes.add(new LinkAttributes("86044", 3, 6000));
        linksAttributes.add(new LinkAttributes("86054", 3, 6000));
        linksAttributes.add(new LinkAttributes("85883", 3, 6000));
        linksAttributes.add(new LinkAttributes("86056", 3, 6000));
        linksAttributes.add(new LinkAttributes("35175", 3, 6000));
        linksAttributes.add(new LinkAttributes("63830", 3, 6000));
        linksAttributes.add(new LinkAttributes("63829", 3, 6000));
        //
        linksAttributes.add(new LinkAttributes("356581", 3, 6000));
        linksAttributes.add(new LinkAttributes("356574", 3, 6000));
        linksAttributes.add(new LinkAttributes("86039", 3, 6000));
        linksAttributes.add(new LinkAttributes("86046", 3, 6000));
        linksAttributes.add(new LinkAttributes("86047", 3, 6000));
        linksAttributes.add(new LinkAttributes("129462", 3, 6000));
        linksAttributes.add(new LinkAttributes("129474", 3, 6000));
        linksAttributes.add(new LinkAttributes("129465", 3, 6000));
        linksAttributes.add(new LinkAttributes("161769", 3, 6000));
        linksAttributes.add(new LinkAttributes("161784", 3, 6000));
        linksAttributes.add(new LinkAttributes("460598", 3, 6000));
        linksAttributes.add(new LinkAttributes("460599", 3, 6000));
        linksAttributes.add(new LinkAttributes("349989", 3, 6000));
        linksAttributes.add(new LinkAttributes("353298", 3, 6000));
        linksAttributes.add(new LinkAttributes("353296", 3, 6000));
        linksAttributes.add(new LinkAttributes("256230", 3, 6000));
        linksAttributes.add(new LinkAttributes("353299", 3, 6000));
        linksAttributes.add(new LinkAttributes("145830", 3, 6000));
        linksAttributes.add(new LinkAttributes("145834", 3, 6000));
        linksAttributes.add(new LinkAttributes("145840", 3, 6000));
        linksAttributes.add(new LinkAttributes("435521", 3, 6000));
        linksAttributes.add(new LinkAttributes("189992", 3, 6000));
        linksAttributes.add(new LinkAttributes("435525", 3, 6000));
        linksAttributes.add(new LinkAttributes("396642", 3, 6000));
        linksAttributes.add(new LinkAttributes("412071", 3, 6000));
        linksAttributes.add(new LinkAttributes("356913", 3, 6000));
        linksAttributes.add(new LinkAttributes("245918", 3, 6000));
        linksAttributes.add(new LinkAttributes("245919", 3, 6000));
        linksAttributes.add(new LinkAttributes("190768", 3, 6000));
        linksAttributes.add(new LinkAttributes("387646", 3, 6000));
        linksAttributes.add(new LinkAttributes("387651", 3, 6000));
    }

    /**
     * von Gregor
     */
    private static void addLinks_Westring_Sonnborn() {
        linksAttributes.add(new LinkAttributes("45323", 3, 6000));
        linksAttributes.add(new LinkAttributes("45341", 3, 6000));
        linksAttributes.add(new LinkAttributes("92013", 3, 6000));
        linksAttributes.add(new LinkAttributes("293605", 3, 6000));
        linksAttributes.add(new LinkAttributes("478154", 3, 6000));
        linksAttributes.add(new LinkAttributes("194808", 3, 6000));
        linksAttributes.add(new LinkAttributes("293602", 3, 6000));
        linksAttributes.add(new LinkAttributes("270096", 3, 6000));
        //
        linksAttributes.add(new LinkAttributes("241734", 3, 6000));
        linksAttributes.add(new LinkAttributes("266985", 3, 6000));
        linksAttributes.add(new LinkAttributes("252862", 3, 6000));
        linksAttributes.add(new LinkAttributes("478147", 3, 6000));
        linksAttributes.add(new LinkAttributes("293616", 3, 6000));
        linksAttributes.add(new LinkAttributes("247872", 3, 6000));
        linksAttributes.add(new LinkAttributes("328728", 3, 6000));
        linksAttributes.add(new LinkAttributes("45325", 3, 6000));
    }

    /**
     * von Gregor
     */
    private static void addLinks_DuisburgSerm_DuisburgRahm() {
        linksAttributes.add(new LinkAttributes("241734", 2, 4000));
        linksAttributes.add(new LinkAttributes("233393", 2, 4000));
        linksAttributes.add(new LinkAttributes("305378", 2, 4000));
        linksAttributes.add(new LinkAttributes("331781", 2, 4000));
        linksAttributes.add(new LinkAttributes("79278", 2, 4000));
        linksAttributes.add(new LinkAttributes("85068", 2, 4000));
        linksAttributes.add(new LinkAttributes("85067", 2, 4000));
        linksAttributes.add(new LinkAttributes("239049", 2, 4000));
        //
        linksAttributes.add(new LinkAttributes("331776", 2, 4000));
        linksAttributes.add(new LinkAttributes("331777", 2, 4000));
        linksAttributes.add(new LinkAttributes("331778", 2, 4000));
        linksAttributes.add(new LinkAttributes("79280", 2, 4000));
        linksAttributes.add(new LinkAttributes("233336", 2, 4000));
    }

    private static Network readNetwork() {
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile(inputNetwork);
        //        Network network = NetworkUtils.createNetwork();
        return scenario.getNetwork();
    }

    private static void generateModifiedNetwork() {
        for (Node node : nodes) {
            network.addNode(node);
        }
        for (Link link : links) {
            network.addLink(link);
        }
        for (LinkAttributes linkAttributes : linksAttributes) {
            Link link = network.getLinks().get(linkAttributes.linkId);
            link.setNumberOfLanes(linkAttributes.numberOfLanes);
            link.setCapacity(linkAttributes.capacity);
        }
    }

    private static class LinkAttributes {
        private Id<Link> linkId;
        private int numberOfLanes;
        private int capacity;

        LinkAttributes(String linkId, int numberOfLanes, int capacity) {
            this.linkId = Id.createLinkId(linkId);
            this.numberOfLanes = numberOfLanes;
            this.capacity = capacity;
        }
    }

    private static class NodeAttributes {
        private String NodeId;
        private double xCoord;
        private double yCoord;

        NodeAttributes(String NodeId, double xCoord, double yCoord) {
            this.NodeId = NodeId;
            this.xCoord = xCoord;
            this.yCoord = yCoord;
        }
    }

}
