import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.gurobi.gurobi.*;

import static java.lang.Math.max;

public class TestMain {
    public static void main(String[] args) throws IOException, GRBException {

        String dataFile = "data/A_4_50_3.txt";
        Instance instance = new Instance();
        instance.ReadData(dataFile);
        ArrayList<Route> bestRoutes = new ArrayList<>();
        // Generate the initial route
        NearestAdditionAlgorithm na = new NearestAdditionAlgorithm();
        na.generateInitialSolution(instance);  //For instances where the truck of an onboardCustomer has not yet been determined
        ArrayList<Route> initRoutes = na.initialRoute;

        // Main algorithm body
        long startTime = System.currentTimeMillis();
//        BranchAndBound_CBFS bb = new BranchAndBound_CBFS();
//        BranchAndBound_BFS bb = new BranchAndBound_BFS();
        BranchAndBound bb = new BranchAndBound();
//        bb.terminateTime = 100;
        bb.BBnode(instance, initRoutes);
        long endTime = System.currentTimeMillis();
        System.out.print("Initial LB：" + bb.lowerBound);
        System.out.print(" | Best Obj：" + bb.upperBound);
        if (endTime - startTime > bb.terminateTime * 1000) {
            System.out.print(" | Gap：" + (bb.incumbent_node.localUpperBound - bb.lowerBound) / bb.upperBound * 100 + "%");
        } else {
            System.out.print(" | Gap：0");
        }
        System.out.print(" | CPU Time：" + (endTime - startTime) + "ms");
        System.out.println(" | Branch Num：" + bb.branchNum);

        for (int i = 0; i < bb.incumbent_node.routeList.size(); i++) {
            System.out.print("truck"+ i + " | path:");
            ArrayList<Node> path = bb.incumbent_node.routeList.get(i).path;
            for (int j = 0; j < path.size()-1; j++) {
                int iID = path.get(j).getId();
                int jID = path.get(j+1).getId();
                System.out.print(path.get(j).getId() + "-");
                if (path.get(j+1).isMeetingNode()){
                    System.out.print("*");
                }
            }
            System.out.println(path.get(path.size()-1).getId() + " | cost:" + bb.incumbent_node.routeList.get(i).getCost()
                    + " | delay" + bb.incumbent_node.routeList.get(i).getDelay());
//            totalObj += cost + delay;
        }

        // Write the results to a file in the result folder, named after the instance
        try {
            File resultDir = new File("results");
            if (!resultDir.exists()) {
                resultDir.mkdirs();
            }
            String instanceName = new File(dataFile).getName().replaceFirst("[.][^.]+$", "");
            FileWriter writer = new FileWriter(new File(resultDir, instanceName + ".txt"));

            writer.write("Initial LB：" + bb.lowerBound);
            writer.write(" | Best Obj：" + bb.upperBound);
            if (endTime - startTime > bb.terminateTime * 1000) {
                writer.write(" | Gap：" + (bb.incumbent_node.localUpperBound - bb.lowerBound) / bb.upperBound * 100 + "%");
            } else {
                writer.write(" | Gap：0");
            }
            writer.write(" | CPU Time：" + (endTime - startTime) + "ms");
            writer.write(" | Branch Num：" + bb.branchNum);
            writer.write(System.lineSeparator());

            for (int i = 0; i < bb.incumbent_node.routeList.size(); i++) {
                writer.write("truck" + i + " | path:");
                ArrayList<Node> path = bb.incumbent_node.routeList.get(i).path;
                for (int j = 0; j < path.size() - 1; j++) {
                    writer.write(path.get(j).getId() + "-");
                    if (path.get(j + 1).isMeetingNode()) {
                        writer.write("*");
                    }
                }
                writer.write(path.get(path.size() - 1).getId() + " | cost:" + bb.incumbent_node.routeList.get(i).getCost()
                        + " | delay" + bb.incumbent_node.routeList.get(i).getDelay());
                writer.write(System.lineSeparator());
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
