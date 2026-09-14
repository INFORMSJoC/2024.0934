import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Instance {
    public double M = 10e6;
    public int nodeNumber;
    public int truckNumber;
    public int droneNumber = 4; // Currently assume there are 4 drones
    public ArrayList<Node>  nodeList;
    public ArrayList<Node>  newNodeList;
    public ArrayList<Truck>  truckList;
    public ArrayList<Node>[]  priorList;  // The set of nodes unreachable from node i (pi_i)
    public ArrayList<Node>[]  successorList;  // The set of nodes that cannot reach node i (sigma_i)


    //truck
    public double truckUnitCost = 0.1245;
    public double truckFixedCost = 20;
    public double truckSpeed = 4;
    public double truckCapacity = 100;

    //time window;
    public double timeWindowLength = 5;
    public double relaxedTWLength = 1.5 * timeWindowLength; //The true hard time window is the original time window TW +/- timeWindowLength * relaxedTWratio.


//    public double T = 0; // The latest decision-cycle time
//    public double lamda; // The travel time of the longest arc

    //drone
    public double droneUnitCost = 0.0315;
    public double droneSpeed = 4;
    public double droneCapacity = 4;
    public double droneTotalEnergy = 0.4236;

    //other cost
    public double unitDelayCost = 0.125;
    public double rejectionCost = 2.5;

    public double[][] manhattanDis;
    public double[][] distance;
    public double[][] truckTimeMatrix;
    public double[][] droneTimeMatrix;

    public Instance() {
        this.truckList     =  new ArrayList<>();
        this.nodeList      =  new ArrayList<>();
        this.newNodeList   =  new ArrayList<>();
    }

    public void ReadData(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));

        String line = new String();
        line = br.readLine();
        String[] tokens = line.split("\\s+");
        this.truckNumber = Integer.parseInt(tokens[0]);
        this.nodeNumber = Integer.parseInt(tokens[1]);

        line = br.readLine();
        for (int i = 0; i < this.truckNumber; i++) {
            line = br.readLine();
            tokens = line.split("\\s+");
            Truck truck = new Truck(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]),Double.parseDouble(tokens[2]));
            this.truckList.add(truck);
        }

        line = br.readLine();
        for (int i = 0; i < this.nodeNumber; i++) {
//            System.out.println(Arrays.binarySearch(dockingIndex, i));
            line = br.readLine();
            tokens = line.split("\\s+");

            Node node = new Node(Integer.parseInt(tokens[0]), Double.parseDouble(tokens[1])
                    , Double.parseDouble(tokens[2]), Double.parseDouble(tokens[3]), Math.max(0, Double.parseDouble(tokens[3]) - this.timeWindowLength));
            int type = Integer.parseInt(tokens[4]);
            if (type == 0) {
                node.setNodeType("depot");
                node.setStartTime(0);
            } else if (type == 1) {
                node.setNodeType("onboardCustomer");
                int truckID = Integer.parseInt(tokens[5]);
                if (truckID != -1) {
                    node.setTruckID(truckID);
                    if (!(node.getId() == this.truckList.get(truckID).getInitLocation())) { //If it is not the truck's initial location, add it to the onboard list
                        this.truckList.get(truckID).getOnboardNodeList().add(node);
                    } else {
                        node.setStartTime(node.getDueTime());
                    }
                }
            } else {
                node.setNodeType("newCustomer");
                newNodeList.add(node);
            }
            nodeList.add(node);
        }
        calculateDistances();

//        for (int i = 0; i < nodeList.size(); i++) {
//            System.out.println(Arrays.toString(distance[i]));
//        }
//        for (int i = 0; i < nodeList.size(); i++)
//            System.out.println(nodeList.get(i).getNodeType());


//        //The data structure for the order relation between nodes
//        this.successorList = new ArrayList[this.nodeList.size()];
//        this.priorList = new ArrayList[this.nodeList.size()];
//        for (int i = 0; i < this.nodeList.size(); i++) {
//            successorList[i] = new ArrayList<>();
//            priorList[i] = new ArrayList<>();
//        }
    }

    public void calculateDistances(){
        manhattanDis = new double[nodeList.size()][nodeList.size()];
        distance = new double[nodeList.size()][nodeList.size()];
        for (int i = 0; i < nodeList.size(); i++) {
            for (int j = i; j < nodeList.size(); j++) {
                if (i == j) {
                    distance[i][j] = M;
                    manhattanDis[i][j] = M;
                } else {
                    distance[i][j] = Math.sqrt(Math.pow(nodeList.get(i).getxCoordinate() - nodeList.get(j).getxCoordinate(), 2)
                            + Math.pow(nodeList.get(i).getyCoordinate() - nodeList.get(j).getyCoordinate(), 2));
                    manhattanDis[i][j] = Math.abs(nodeList.get(i).getxCoordinate() - nodeList.get(j).getxCoordinate())
                            + Math.abs(nodeList.get(i).getyCoordinate() - nodeList.get(j).getyCoordinate());
                }
                distance[i][j] = Math.floor(distance[i][j] * 100) / 100; //Rounded to two decimal places
                distance[j][i] = Math.floor(distance[i][j] * 100) / 100;
                manhattanDis[j][i] = manhattanDis[i][j];
            }
        }

        truckTimeMatrix = new double[nodeList.size()][nodeList.size()];
        droneTimeMatrix = new double[nodeList.size()][nodeList.size()];
        for (int i = 0; i < nodeList.size(); i++) {
            for (int j = 0; j < nodeList.size(); j++) {
                truckTimeMatrix[i][j] = manhattanDis[i][j] / truckSpeed;
                droneTimeMatrix[i][j] = distance[i][j] / droneSpeed;
            }
        }
        for (int i = 0; i < nodeList.size(); i++) {
            System.out.println(Arrays.toString(manhattanDis[i]));
        }

        for (int i = 0; i < nodeList.size(); i++) {
            System.out.println(Arrays.toString(truckTimeMatrix[i]));
        }

    }


}
