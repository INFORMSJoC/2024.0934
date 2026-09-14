import com.gurobi.gurobi.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.max;

public class NearestAdditionAlgorithm {
    // List of candidate customers
    public ArrayList<Node> customers;
    public boolean isSelect[];
    public boolean isFull[]; // Whether the truck is already full
    public ArrayList<Route> initialRoute;

    public NearestAdditionAlgorithm() {
        this.customers = new ArrayList<>();
        this.initialRoute = new ArrayList<>();
    }

    public static boolean checkTimeWindow(ArrayList<Node> path, int insertIndex, Node newNode, Instance instance){
        double time = path.get(0).getDueTime(); //The due time of the init location is the truck's departure time
        for(int i = 0; i < path.size() - 1; i++){
            if (insertIndex == i) {
                time = time + instance.truckTimeMatrix[path.get(i).getId()][newNode.getId()];
                //Check whether it satisfies the new node's time window; here only the due time is checked; if the start time is not satisfied, move the time to the start time
                if (time > newNode.getDueTime() + instance.relaxedTWLength){
                    return false;
                } else{
                    time = max(newNode.getStartTime() - instance.relaxedTWLength, time) + instance.truckTimeMatrix[newNode.getId()][path.get(i+1).getId()];
                }
            } else {
                time = time + instance.truckTimeMatrix[path.get(i).getId()][path.get(i + 1).getId()];
            }
            //Check whether the time window of customer i+1 is still feasible
            if (i+1 > insertIndex){
                if (time > path.get(i+1).getDueTime() + instance.relaxedTWLength){
                    return false;
                }
            }
            time = max(path.get(i+1).getStartTime() - instance.relaxedTWLength, time);
        }
        return true;
    }

    public int[] selectCustomer(ArrayList<Node> nodeList, Instance instance, ArrayList<Route> routeList){
        int[] min_index = new int[3];

        double min_dis = Double.MAX_VALUE; //Minimum insertion cost
        int min_cus_index = -1; // The customer corresponding to the minimum insertion cost
        int min_loc = -1;
        int min_truck_index = -1; // The truck corresponding to the minimum insertion cost
        for(int i = 0; i < nodeList.size(); i++){
            if (isSelect[i]){
                continue;
            }
            for (int k = 0; k < routeList.size(); k++) {
                if (isFull[k]){
                    continue;
                }
                Route route = routeList.get(k);
                double min_truck_dis = Double.MAX_VALUE;
                int min_truck_loc = -1;

                for(int j = 0; j < route.path.size() - 1; j++){ // Iterate over the path arcs, excluding the final depot
                    //Check whether inserting here satisfies the time windows of all points
                    if (!checkTimeWindow(route.path, j, nodeList.get(i), instance)){
                        continue;
                    }
                    double before_current_dis = instance.manhattanDis[route.path.get(j).getId()][nodeList.get(i).getId()];
                    double current_after_dis = instance.manhattanDis[nodeList.get(i).getId()][route.path.get(j + 1).getId()];
                    double before_after_dis = instance.manhattanDis[route.path.get(j).getId()][route.path.get(j + 1).getId()];
                    double add_dis =  before_current_dis + current_after_dis - before_after_dis;
                    if (add_dis < min_truck_dis){ // Find the nearest insertion distance of customer i for truck k
                        min_truck_dis = add_dis;
                        min_truck_loc = j;
                    }
                }

                if (min_truck_dis < min_dis){ //If the nearest insertion distance is globally minimal
                    min_dis = min_truck_dis;
                    min_cus_index = i;
                    min_truck_index = k;
                    min_loc = min_truck_loc;
                }
            }
        }

        min_index[0] = min_cus_index;
        min_index[1] = min_loc;
        min_index[2] = min_truck_index;

        return min_index;
    }

    // Assignment of new customers + the initial route of each truck
    public void generateInitialSolution(Instance instance) throws GRBException {
        //Filter the list of unprocessed customers
        for (int i = 0; i < instance.nodeList.size(); i++) {
            if (instance.nodeList.get(i).getNodeType().equals("onboardCustomer")){
                boolean flag = true; //If this point is the initial location of some truck, flag becomes false
                for (int j = 0; j < instance.truckNumber; j++) {
                    if(i == instance.truckList.get(j).getInitLocation()) {
                        flag = false;
                        break;
                    }
                }
                if (flag) {
                    customers.add(instance.nodeList.get(i));
                }
            }
        }
        this.isSelect = new boolean[customers.size()]; //Initially all false
        this.isFull = new boolean[instance.truckNumber]; //Initially all false

        //Limit the number of onboard customers per truck so it is not too large
        int avgCustomerNumber = (int) Math.ceil( (double) customers.size() / instance.truckNumber) + 1;

        for (int i = 0; i < instance.truckNumber; i++) { // Create a new route for each truck
            Route route = new Route();
            route.truckID = i;
            Node node = instance.nodeList.get(instance.truckList.get(i).getInitLocation());
            route.path.add(node); // The beginning is the initial location
            node.setTruckID(i);
            route.path.add(instance.nodeList.get(0)); // Add the depot at the end
            initialRoute.add(route);
        }
        do {
            //Find the nearest customer
            int[] insert = selectCustomer(customers, instance, initialRoute);

            if (insert[0] == -1){
                System.out.println("error");
            }
            Node customer = customers.get(insert[0]);
            Truck truck = instance.truckList.get(insert[2]);
            isSelect[insert[0]] = true;
            initialRoute.get(insert[2]).path.add(insert[1] + 1, customer);
            truck.getOnboardNodeList().add(customer);
            customer.setTruckID(insert[2]);
            if (truck.getOnboardNodeList().size() >= avgCustomerNumber) {
                isFull[insert[2]] = true;
            }
        } while (!isInitialComplete());
        //Compute the final obj of each route
        for (int i = 0; i < instance.truckNumber; i++) {
            double obj = simpleTSP(instance,instance.truckList.get(i),initialRoute.get(i));
            initialRoute.get(i).setObj(obj);
            initialRoute.get(i).setCost(obj);
        }
    }

    boolean isInitialComplete(){
        boolean isInitialComplete = true;
        for(int i = 0; i < customers.size(); i++){
            if(!isSelect[i]){ //Check whether there are still new customers
                isInitialComplete = false;
                break;
            }
        }
        return isInitialComplete;
    }

    // Assign routes here for the trucks that return to the depot each time afterwards
    public void generateTruckPaths(Instance instance, ArrayList<Integer> truckID) throws GRBException {
        this.isSelect = new boolean[customers.size()]; //Initially all false
        this.isFull = new boolean[instance.truckNumber]; //Initially all false
        Arrays.fill(isFull, true); //For trucks that did not return to the depot
        for (int i = 0; i < truckID.size(); i++) {
            isFull[truckID.get(i)] = false;
        }

        //Distribute the customers evenly among the trucks
        int avgCustomerNumber = (int) Math.ceil(customers.size() / truckID.size());

        for (int i = 0; i < instance.truckNumber; i++) { // Create a new route for each truck
            Route route = new Route();
            route.truckID = i;
            Node node = instance.nodeList.get(instance.truckList.get(i).getInitLocation());
            route.path.add(node); // The beginning is the initial location
            node.setTruckID(i);
            route.path.add(instance.nodeList.get(0)); // Add the depot at the end
            initialRoute.add(route);
        }
        do {
            //Find the nearest customer
            int[] insert = selectCustomer(customers, instance, initialRoute);

            if (insert[0] == -1){
                System.out.println("error");
            }
            Node customer = customers.get(insert[0]);
            Truck truck = instance.truckList.get(insert[2]);
            isSelect[insert[0]] = true;
            initialRoute.get(insert[2]).path.add(insert[1] + 1, customer);
            truck.getOnboardNodeList().add(customer);
            customer.setTruckID(insert[2]);
            if (truck.getOnboardNodeList().size() >= avgCustomerNumber) {
                isFull[insert[2]] = true;
            }
        } while (!isInitialComplete());
        //Compute the final obj of each route
        for (int i = 0; i < instance.truckNumber; i++) {
            double obj = simpleTSP(instance,instance.truckList.get(i),initialRoute.get(i));
            initialRoute.get(i).setObj(obj);
        }
    }

    public double simpleTSP(Instance instance, Truck truck, Route route) throws GRBException {
        ArrayList<Node> nodeList = BranchAndBound.arrayListClone(route.path);
        double M = 1e3;

        GRBEnv env = new GRBEnv();
        GRBModel model = new GRBModel(env);
        model.set(GRB.IntParam.LogToConsole, 0);
        model.set(GRB.IntParam.OutputFlag, 0);

        //Define decision variables
        GRBVar[][] x = new GRBVar[nodeList.size()][nodeList.size()];
        for (int i = 0; i < nodeList.size(); i++) {
            for (int j = 0; j < nodeList.size(); j++) {
                if (i != j) {
                    int iID = nodeList.get(i).getId();
                    int jID = nodeList.get(j).getId();
                    double obj = instance.manhattanDis[iID][jID] * instance.truckUnitCost;
                    x[i][j] = model.addVar(0, 1, obj, GRB.BINARY, "x_" + i + "_" + j);
                }
            }
        }
        GRBVar[] tau = new GRBVar[nodeList.size()];
        GRBVar[] etau = new GRBVar[nodeList.size()];
        GRBVar[] ptau = new GRBVar[nodeList.size()];
        for (int i = 0; i < nodeList.size(); i++) {
            tau[i] = model.addVar(-Double.MAX_VALUE, Double.MAX_VALUE, 0.0, GRB.CONTINUOUS, "tau_" + i);
            etau[i] = model.addVar(0, Double.MAX_VALUE, instance.unitDelayCost, GRB.CONTINUOUS, "etau_" + i);
            ptau[i] = model.addVar(0, Double.MAX_VALUE, instance.unitDelayCost, GRB.CONTINUOUS, "ptau_" + i);
        }
        // Constraints
        // The truck departs from the start point and returns to the depot
        GRBLinExpr expr = new GRBLinExpr();
        GRBLinExpr expr1 = new GRBLinExpr();
        for (int i = 1; i < nodeList.size() - 1; i++) {
            expr.addTerm(1, x[0][i]);
            expr1.addTerm(1, x[0][i]);
            expr1.addTerm(-1, x[i][nodeList.size() - 1]);
        }
        model.addConstr(expr, GRB.EQUAL, 1, "cons");
        model.addConstr(expr1, GRB.EQUAL, 0, "cons1");

        // Truck flow balance
        for (int i = 1; i < nodeList.size() - 1; i++) {
            GRBLinExpr expr2 = new GRBLinExpr();
            for (int j = 0; j < nodeList.size(); j++) {
                if (i != j) {
                    expr2.addTerm(1, x[i][j]);
                    expr2.addTerm(-1, x[j][i]);
                }
            }
            model.addConstr(expr2, GRB.EQUAL, 0, "cons2");
        }

        // All customers are served
        for (int i = 1; i < nodeList.size() - 1; i++) {
            GRBLinExpr expr3 = new GRBLinExpr();
            for (int j = 1; j < nodeList.size(); j++) {
                if (i != j) {
                    expr3.addTerm(1, x[i][j]);
                }
            }
            model.addConstr(expr3, GRB.EQUAL, 1, "cons3");
        }

        // Truck time window
        for (int i = 0; i < nodeList.size(); i++) {
            for (int j = 0; j < nodeList.size(); j++) {
                if (i != j ) {
                    int iID = nodeList.get(i).getId();
                    int jID = nodeList.get(j).getId();
                    GRBLinExpr expr5 = new GRBLinExpr();
                    expr5.addTerm(M, x[i][j]);
                    expr5.addTerm(1, tau[i]);
                    expr5.addTerm(-1, tau[j]);
                    model.addConstr(expr5, GRB.LESS_EQUAL, M - instance.truckTimeMatrix[iID][jID], "cons5");
                }
            }
        }

        //Truck initial time
        GRBLinExpr expr6 = new GRBLinExpr();
        expr6.addTerm(1, tau[0]);
        model.addConstr(expr6, GRB.GREATER_EQUAL, truck.getInitTime(), "cons6");

        // Delivery hard time window
        for (int i = 0; i < nodeList.size() - 1; i++) {
            GRBLinExpr expr60 = new GRBLinExpr();
            expr60.addTerm(1, tau[i]);
            model.addConstr(expr60, GRB.GREATER_EQUAL, nodeList.get(i).getStartTime() - instance.relaxedTWLength, "cons60");
            model.addConstr(expr60, GRB.LESS_EQUAL, nodeList.get(i).getDueTime() + instance.relaxedTWLength, "cons60");
        }

        // Compute the delay time
        for (int i = 0; i < nodeList.size(); i++) {
            if (!nodeList.get(i).getNodeType().equals("depot")) {
                GRBLinExpr expr8 = new GRBLinExpr();
                expr8.addTerm(1, ptau[i]);
                expr8.addTerm(-1, tau[i]);
                model.addConstr(expr8, GRB.GREATER_EQUAL, -nodeList.get(i).getDueTime(), "cons8");

                GRBLinExpr expr9 = new GRBLinExpr();
                expr9.addTerm(1, etau[i]);
                expr9.addTerm(1, tau[i]);
                model.addConstr(expr9, GRB.GREATER_EQUAL, nodeList.get(i).getStartTime(), "cons9");
            }
        }

        model.write("modal.lp");
        model.optimize();

        route.path.clear();

        int currentNode = 0;
        route.path.add(nodeList.get(currentNode));
        System.out.print("[" + nodeList.get(currentNode).getId());
        while (currentNode != nodeList.size() - 1) {
            for (int j = 0; j < nodeList.size(); j++) {
                if (currentNode != j) {
                    if (x[currentNode][j].get(GRB.DoubleAttr.X) > 0.5) {
                        System.out.print("-" + nodeList.get(j).getId());
                        currentNode = j;
                        route.path.add(nodeList.get(currentNode));
                    }
                }
            }
        }
        System.out.println("]");

        return model.get(GRB.DoubleAttr.ObjVal);

    }


    // Compute the time-window connection relation between each pair of nodes
    public void calculateSequentialOrder(Instance instance){
        for (int i = 0; i < instance.nodeList.size(); i++) {
            for (int j = 0; j < instance.nodeList.size(); j++) {
                Node nodei = instance.nodeList.get(i);
                Node nodej = instance.nodeList.get(j);
                if (nodei.getNodeType().equals("depot") || nodej.getNodeType().equals("depot")){
                    continue;
                }
                if (i != j && (nodej.getTruckID() == nodei.getTruckID()
                                || nodei.getNodeType().equals("newCustomer")
                                || nodej.getNodeType().equals("newCustomer"))){
                    double ej = nodej.getStartTime() - instance.relaxedTWLength;
                    double li = nodei.getDueTime() + instance.relaxedTWLength;
                    if (ej + instance.truckTimeMatrix[j][i] > li){ //In this case i << j, i.e., node i cannot be reached from node j
                        instance.priorList[j].add(nodei);  // The set of nodes unreachable from node j
                        instance.successorList[i].add(nodej);  // The set of nodes that cannot reach node i
                    }
                }
            }
        }
    }

//    //The trucks have been assigned; generate an original route for each truck
//    public void generateInitialPath(Instance instance, Truck truck) {
//        //List of customers to be inserted
//        for (int i = 0; i < truck.getOnboardNodeList().size(); i++) {
//            customers.add(truck.getOnboardNodeList().get(i));
//        }
//        this.isSelect = new boolean[customers.size()]; //Initially all false
//
//        Route route = new Route(); // Create a new route
//        route.truckID = truck.getId();
//        route.path.add(instance.nodeList.get(truck.getInitLocation())); // The beginning is the initial location
//        route.path.add(instance.nodeList.get(0)); // Add the depot at the end
//        //Find the nearest customer
//        int insertCustomer = getNearestNode(instance, truck.getInitLocation(), 0);
//        Node customer = customers.get(insertCustomer);
//        isSelect[insertCustomer] = true;
//        route.path.add(1, customer);
//        while (true) {
//            int[] insert = getNearestTourCustomer(instance, route);
//            if(insert[0] == -1)
//                break;
//            customer = customers.get(insert[0]);
//            isSelect[insert[0]] = true;
//            route.path.add(insert[1] + 1, customer);
//        }
//        route.computeObj(instance);
//        initialRoute.add(route);
//    }
//
//
//    int getNearestNode(Instance instance, int head, int tail){
//        int nearestIndex = 0; //Index of the nearest customer point in the list
//        double nearestDistance = Double.MAX_VALUE; //Distance from the nearest customer point to the depot, initially infinity
//        for(int i = 0; i < customers.size(); i++){
//            if(isSelect[i]) // Check the selection state of this customer point
//                continue;
//            int nodeID = customers.get(i).getId();
//            if(instance.manhattanDis[head][nodeID] + instance.manhattanDis[nodeID][tail] < nearestDistance){
//                nearestIndex = i;
//                nearestDistance = instance.manhattanDis[head][nodeID] + instance.manhattanDis[nodeID][tail];
//            }
//        }
//        return nearestIndex;
//    }
//
//    int[] getNearestTourCustomer(Instance instance, Route route){
//        int[] min_index = new int[2];
//        double min_dis = -Double.NEGATIVE_INFINITY; //Minimum insertion cost
//        double before_current_dis = 0; // Distance from the previous point to the current point
//        double current_after_dis = 0; // Distance from the current point to the next point
//        double before_after_dis = 0; // Distance from the previous point to the next point
//        double add_dis = 0; // The insertion cost produced by each combination of three points during iteration: add_dis = before_current_dis + current_after_dis - before_after_dis
//        int min_cus_index = 0; // The index of the previous point corresponding to the minimum insertion cost
//        int min_loc = 0; // The index of the current point corresponding to the minimum insertion cost
//        boolean ifAllSelect = true;
//        for(int i = 0; i < customers.size(); i++){
//            if(isSelect[i])
//                continue;
//            for(int j = 0; j < route.path.size() - 1; j++){ // Iterate over the path arcs, excluding the final depot
//                ifAllSelect = false;
//                before_current_dis = instance.manhattanDis[route.path.get(j).getId()][customers.get(i).getId()];
//                current_after_dis = instance.manhattanDis[customers.get(i).getId()][route.path.get(j + 1).getId()];
//                before_after_dis = instance.manhattanDis[route.path.get(j).getId()][route.path.get(j + 1).getId()];
//                add_dis =  before_current_dis + current_after_dis - before_after_dis;
//                if(add_dis < min_dis){
//                    min_dis = add_dis;
//                    min_cus_index = i;
//                    min_loc = j;
//                }
//            }
//        }
//        if(ifAllSelect){
//            min_index[0] = -1;
//            min_index[1] = -1;
//        }
//        else {
//            min_index[0] = min_cus_index;
//            min_index[1] = min_loc;
//        }
//
//        return min_index;
//    }
//

}
