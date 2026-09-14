import com.gurobi.gurobi.*;

import java.io.IOException;
import java.util.*;


public class LabelingAlgorithm {

    Instance instance;
    ArrayList<label> labels;
    ArrayList<Node> nodeList;
    int originNodeNumber;
    double[][] arcCostMatrix;
    double[][] arcTimeMatrix;

    // Compute the arc index
    public static int getArcIndex(int head, int tail, int length) {
        int row = 0;
        if (head < tail) {
            row = tail - 1;
        } else {
            row = tail;
        }
        return head * length + row;
    }

    class label {
        public int city; // The current node
        public double cost;
        public double time;
        public double penalty;
        public boolean dominated;
        public int[] vertexVisited;
        public int type; // 0: Forward label; 1: Backward label.

        public label(int city, double cost, double time, double penalty, boolean dominated, int[] vertexVisited, int type) {
            this.city = city;
            this.cost = cost;
            this.time = time;
            this.penalty = penalty;
            this.dominated = dominated;
            this.vertexVisited = vertexVisited;
            this.type = type;
        }
    }

    class ForwardLabelComparator implements Comparator<Integer> {
        // Caution! When the comparator returns 0, the two labels are treated as completely identical!
        // This comparator is not only used to sort the list! When adding to the list, value 0 => not added!
        public int compare(Integer a, Integer b) {
            label A = labels.get(a);
            label B = labels.get(b);

            if ((A.cost + A.penalty) - (B.cost + B.penalty) < -1e-5) {
                return -1;
            } else if ((A.cost + A.penalty) - (B.cost + B.penalty) > 1e-5) {
                return 1;
            } else {
                if (A.city == B.city) {
                    if (A.time - B.time < -1e-5) {
                        return -1;
                    } else if (A.time - B.time > 1e-5) {
                        return 1;
                    } else {
                        int i = 0;
                        while (i < nodeList.size()) {
                            if (A.vertexVisited[i] != 0 && B.vertexVisited[i] == 0){
                                return -1;
                            }
                            if (A.vertexVisited[i] == 0 && B.vertexVisited[i] != 0){
                                return 1;
                            }
                            i++;
                        }
                        return 0;
                    }
                } else if (A.city > B.city){
                    return 1;
                } else {
                    return -1;
                }
            }
        }
    }

    class BackwardLabelComparator implements Comparator<Integer> {
        public int compare(Integer a, Integer b) {
            label A = labels.get(a);
            label B = labels.get(b);

            if ((A.cost + A.penalty) - (B.cost + B.penalty) < -1e-5) {
                return -1;
            } else if ((A.cost + A.penalty) - (B.cost + B.penalty) > 1e-5) {
                return 1;
            } else {
                if (A.city == B.city) {
                    if (A.time - B.time > -1e-5) { //Unlike the forward case, the larger time has priority here
                        return -1;
                    } else if (A.time - B.time < 1e-5) {
                        return 1;
                    } else {
                        int i = 0;
                        while (i < nodeList.size()) {
                            if (A.vertexVisited[i] != 0 && B.vertexVisited[i] == 0){
                                return -1;
                            }
                            if (A.vertexVisited[i] == 0 && B.vertexVisited[i] != 0){
                                return 1;
                            }
                            i++;
                        }
                        return 0;
                    }
                } else if (A.city > B.city){
                    return 1;
                } else {
                    return -1;
                }
            }
        }
    }

    public double shortestPath(Instance instance, Route currentRoute, ArrayList<Node> newNodes, Truck truck) {

        //The maximum cost of this path
        double subUpperBound = currentRoute.getObj() + newNodes.size() * instance.rejectionCost;
        boolean flag = false;

        // First build the node set
        this.nodeList = new ArrayList<>();
        this.nodeList.add(instance.nodeList.get(truck.getInitLocation()));
        for (int i = 0; i < truck.getOnboardNodeList().size(); i++) {
            this.nodeList.add(truck.getOnboardNodeList().get(i));
        }
        originNodeNumber = this.nodeList.size();
        for (int i = 0; i < truck.getOnboardNodeList().size(); i++) {
            Node node = new Node(truck.getOnboardNodeList().get(i).getId(), truck.getOnboardNodeList().get(i).getxCoordinate()
                    , truck.getOnboardNodeList().get(i).getyCoordinate(), truck.getOnboardNodeList().get(i).getDueTime(), truck.getOnboardNodeList().get(i).getStartTime());
            node.setNodeType("cloneCustomer");
            this.nodeList.add(node);
        }
        int cloneNodeNumber = nodeList.size();
        for (int i = 0; i < newNodes.size(); i++) {
            this.nodeList.add(newNodes.get(i));
        }
        this.nodeList.add(instance.nodeList.get(0));

        // arc matrix
        this.arcCostMatrix = new double[nodeList.size()][nodeList.size()];
        this.arcTimeMatrix = new double[nodeList.size()][nodeList.size()];
        for (int i = 0; i < nodeList.size(); i++) {
            for (int j = 0; j < nodeList.size(); j++) {
                if (i == j || nodeList.get(i).getId() == nodeList.get(j).getId()) {
                    arcCostMatrix[i][j] = instance.M;
                    arcTimeMatrix[i][j] = instance.M;
                } else if (i < originNodeNumber && j >= originNodeNumber && !(i == 0 && j == nodeList.size()-1)){ // It is a meeting arc, excluding the arc directly from the start to the end
                    arcCostMatrix[i][j] = instance.manhattanDis[nodeList.get(i).getId()][nodeList.get(j).getId()] * instance.truckUnitCost
                            + instance.distance[0][nodeList.get(j).getId()] * 2 * instance.droneUnitCost;
                    arcTimeMatrix[i][j] = instance.truckTimeMatrix[nodeList.get(i).getId()][nodeList.get(j).getId()];
                } else if ((i < originNodeNumber && j < originNodeNumber) || (i >= originNodeNumber && j >= originNodeNumber)){ // It is its own arc
                    arcCostMatrix[i][j] = instance.manhattanDis[nodeList.get(i).getId()][nodeList.get(j).getId()] * instance.truckUnitCost;
                    arcTimeMatrix[i][j] = instance.truckTimeMatrix[nodeList.get(i).getId()][nodeList.get(j).getId()];
                } else{
                    arcCostMatrix[i][j] = instance.M;
                    arcTimeMatrix[i][j] = instance.M;
                }
            }
        }


        this.instance = instance;

        // Unprocessed label list => ordered TreeSet list (best: is such sorting needed?)
        TreeSet<Integer> forwardU = new TreeSet<Integer>(new ForwardLabelComparator()); // Unprocessed label list

//            // Processed label list => ordered TreeSet list
//            TreeSet<Integer> P = new TreeSet<Integer>(new MyLabelComparator()); // Unprocessed label list


        // label array
        labels = new ArrayList<label>(4 * (this.nodeList.size() - 2));
        // Initialize the Forward array
        int[] cust = new int[this.nodeList.size()];
        Arrays.fill(cust, 0); // Initialize to 0
        cust[0] = 1;
        labels.add(new label(0,0,0.0,0,false, cust, 0));
        forwardU.add(0);


        // For each customer point, define two arrays with corresponding label indices (used for dominance)
        int[] forwardCheckDom = new int[this.nodeList.size()];
        ArrayList<Integer>[] cityForwardLabels = new ArrayList[this.nodeList.size()];
        for (int i = 0; i < this.nodeList.size(); i++) {
            cityForwardLabels[i] = new ArrayList<Integer>();
            forwardCheckDom[i] = 0;
        }
        cityForwardLabels[0].add(0);

        ArrayList<Integer>[] finalizedLabels = new ArrayList[this.nodeList.size()]; //Used to store the Forward labels that have already been extended
        for (int i = 0; i < this.nodeList.size(); i++) {
            finalizedLabels[i] = new ArrayList<Integer>();
        }


        while (forwardU.size() > 0) {
            // Process forward first
            Integer currentidx = forwardU.pollFirst();

            label current = labels.get(currentidx);
            boolean isFinalized = true;

//            boolean f1 = false;
//            if (newNodes.size() == 2) {
//                f1 = true;
//                for (int i = 0; i < newNodes.size(); i++) {
//                    if (newNodes.get(i).getId() == 29  || newNodes.get(i).getId() == 25){//
//                        continue;
//                    }else {
//                        f1 = false;
//                    }
//                }
//            }
//            if (f1){
//                System.out.println("check");
//            }

            // Check for dominance
            int l1, l2;
            boolean pathdom;
            label la1, la2;
            ArrayList<Integer> cleaning = new ArrayList<Integer>();
            for (int i = forwardCheckDom[current.city]; i < cityForwardLabels[current.city].size(); i++) {
                // Check the dominance between the labels added since the last visit to this customer and all other labels
                for (int j = 0; j < i; j++) {
                    l1 = cityForwardLabels[current.city].get(i);
                    l2 = cityForwardLabels[current.city].get(j);
                    la1 = labels.get(l1);
                    la2 = labels.get(l2);
                    if (!(la1.dominated || la2.dominated)) { // This may happen because "city2labels" is only cleaned up through "cleaning" after the double loop
                        pathdom = true;
                        for (int k = 1; pathdom && (k < this.nodeList.size()); k++) {
                            if (la1.vertexVisited[k] == 0 && la2.vertexVisited[k] != 0) {
                                pathdom = false;
                            }
                        }
                        if (pathdom && (la1.cost + la1.penalty <= la2.cost + la2.penalty) && (la1.time <= la2.time)) {
                            labels.get(l2).dominated = true;
                            forwardU.remove((Integer) l2);
                            cleaning.add(l2);
                            pathdom = false;
                        }
                        pathdom = true;
                        for (int k = 0; pathdom && (k < this.nodeList.size()); k++) {
                            if (la2.vertexVisited[k] == 0 && la1.vertexVisited[k] != 0) {
                                pathdom = false;
                            }
                        }
                        if (pathdom && (la2.cost + la2.penalty < la1.cost + la1.penalty) && (la2.time < la1.time)) {
                            labels.get(l1).dominated = true;
                            forwardU.remove((Integer) l1);
                            cleaning.add(l1);
                            j = cityForwardLabels[current.city].size();
                        }
                    }
                }
            }
            for (Integer c : cleaning) {
                cityForwardLabels[current.city].remove((Integer) c);
            }
            cleaning = null;
            forwardCheckDom[current.city] = cityForwardLabels[current.city].size(); // Update checkDom: all current dominances in city2labels have been checked.

            // If not dominated, extend REF
            if (!current.dominated) {
                //  Try to extend to other points
                for (int i = 0; i < this.nodeList.size() - 1; i++) {
                    if ((current.vertexVisited[i] == 0) && (arcTimeMatrix[current.city][i] < instance.M)) {

//                        if (f1) {
//                            int[] r1 = new int[current.vertexVisited.length]; // Generate the current route based on newcust
//                            int c1 = generateRouteList(current.vertexVisited, 0, r1); // Record how many nodes are in the route
//                            if (r1[1] == 15 && r1[2] == 14 && r1[3] == 11 && r1[4] == 9 && r1[5] == 8 && r1[6] == 10 && r1[7] == 16 && current.city == 13 && i == 12  ) { //
//                                System.out.println("check");
//                            }
//                        }


                        // time
                        double t = current.time + arcTimeMatrix[current.city][i];
                        if (current.city < originNodeNumber && i >= originNodeNumber){ //meeting arc
                            t = Math.max(t, instance.droneTimeMatrix[0][nodeList.get(i).getId()]);
                        }
                        // is feasible?
                        if (t <= this.nodeList.get(i).getDueTime() + instance.relaxedTWLength) {

                            t = Math.max(t,(this.nodeList.get(i).getStartTime() - instance.relaxedTWLength));

                            int[] newcust = new int[nodeList.size()];
                            System.arraycopy(current.vertexVisited, 0, newcust, 0, nodeList.size());
                            newcust[i] = current.vertexVisited[current.city] + 1;
                            int clonei = getCloneIndex(nodeList, originNodeNumber, i);
                            if (clonei > 0) {
                                newcust[clonei] = 100; //Mark clone i as visited as well
                            }

                            // Acceleration: check in advance that all unvisited customer points can still be visited - as mentioned in the Laporte 2004 paper
                            boolean isFeasible = true;
                            for (int j = originNodeNumber; j < nodeList.size() - 1; j++) {
                                if (newcust[j] == 0) {
                                    int iID = nodeList.get(i).getId();
                                    int jID = nodeList.get(j).getId();
                                    double t2 = t + instance.truckTimeMatrix[iID][jID];
                                    if (t2 > nodeList.get(j).getDueTime() + instance.relaxedTWLength) {
                                        isFeasible = false;  // There is a point that the truck must visit but cannot be visited
                                    }
                                }
                            }

                            int idx = labels.size();
                            if (isFeasible) {
                                // Compute the minimum penalty corresponding to the current label
                                double cost = current.cost + arcCostMatrix[current.city][i];
                                double pen = calculatePenalty( newcust, 0);

                                if (cost + pen + instance.manhattanDis[nodeList.get(i).getId()][0] * instance.truckUnitCost >= subUpperBound){
                                    continue; //The new label is cut by optimality.
                                }

                                labels.add(new label(i, cost, t, pen, false, newcust, 0));
                                if (!forwardU.add((Integer) idx)) {
                                    // This only happens when a label with the same cost and demand that has already visited the same customers already exists at this vertex
                                    // It can happen on paths with certain permutations of the customer order
                                    // => We can forget this label and keep the other one
                                    labels.get(idx).dominated = true;
                                } else {
                                    cityForwardLabels[i].add(idx);
                                    isFinalized = false;
                                }
                            }
                        }
                    }
                }

                // If it can no longer be extended, add it to the finalized list
                if (isFinalized){
                    double currentObj = current.cost + current.penalty + arcCostMatrix[current.city][nodeList.size() - 1];

                    if (currentObj < subUpperBound){
                        if (forwardAllVisited(current.vertexVisited)){// If all have been visited
                            // Determine whether this path is currently the shortest path and update the best solution - currentRoute

                            subUpperBound = currentObj; //Add the time to return from the last point to the depot

                            currentRoute.setObj(currentObj);
                            currentRoute.truckID = truck.getId();
                            currentRoute.newCustomerList = newNodes;
                            currentRoute.setCost(current.cost + arcCostMatrix[current.city][nodeList.size() - 1]);
                            currentRoute.setDelay(current.penalty);

                            currentRoute.path = new ArrayList<Node>();
                            int[] route = new int[current.vertexVisited.length];
                            int cnt = generateRouteList(current.vertexVisited, 0, route);
                            for (int i = 0; i < cnt; i++) {
                                int id = nodeList.get(route[i]).getId();
                                Node node = BranchAndBound.nodeClone(instance.nodeList.get(id));
                                node.setMeetingNode(false);
                                if (i >= 1 && route[i-1] < originNodeNumber && route[i] >= originNodeNumber){
                                    node.setMeetingNode(true);
                                }
                                currentRoute.path.add(node);
                            }
                            currentRoute.path.add(instance.nodeList.get(0)); //Add the depot point at the end
                            flag = true;
                        } else {
                            finalizedLabels[current.city].add(currentidx);
                        }
                    }
                }

            }
        }

        if (flag) {
            return subUpperBound;
        } else{
            return -1;
        }

    }

    public boolean connectLabels(label f, label b, int currentcity, int[] route){
        for (int i = 0; i < nodeList.size(); i++) {
            if (i != currentcity && i != getCloneIndex(nodeList, originNodeNumber, currentcity)) {
                if (f.vertexVisited[i] > 0 && b.vertexVisited[i] > 0){ //Visited the same point
                    return false;
                }
                if (f.vertexVisited[i] == 0 && b.vertexVisited[i] == 0){ //Neither visited the same point
                    return false;
                }
            }
        }

        // Generate the current route based on newcust
        int cnt = generateRouteList(f.vertexVisited, 0, route);
        int[] tmpRoute = new int[nodeList.size()];
        int cnt1 = generateRouteList(b.vertexVisited, 1, tmpRoute);
        for (int i = 0; i < cnt1; i++) {
            route[cnt + i - 1] = tmpRoute[i];
        }
        return true;
    }

    public int generateRouteList(int[] newcust, int type, int[] route){
        int cnt = 0;
        Arrays.fill(route, -1);
        for (int i = 0; i < newcust.length; i++) {
            if (newcust[i] > 0 && newcust[i] < 100){
                route[newcust[i] - 1] = i;
                cnt++;
            }
        }

        if(type == 1) { // If backward, reverse the route
            int middle;
            for (int i = 0; i < cnt / 2; i++) {
                middle = route[i];
                route[i] = route[cnt - i - 1];
                route[cnt - i - 1] = middle;
            }
        }
        return cnt;
    }

//    public double calculatePenalty(GRBEnv env, int[] newcust, int type) throws GRBException {
//        int[] route = new int[newcust.length]; // Generate the current route based on newcust
//        int cnt = generateRouteList(newcust, type, route); // Record how many nodes are in the route
//
//        // Build a small linear programming model
//
//        GRBModel model = new GRBModel(env);
////        model.set(GRB.IntParam.LogToConsole, 0);
////        model.set(GRB.IntParam.OutputFlag, 0);
//
//        //Define decision variables
//        GRBVar[] w = new GRBVar[cnt];
//        GRBVar[] a = new GRBVar[cnt];
//        GRBVar[] b = new GRBVar[cnt];
//        for (int i = 0; i < cnt; i++) {
//            w[i] = model.addVar(Math.max(nodeList.get(route[i]).getStartTime() - instance.relaxedTWLength,0)
//                    , nodeList.get(route[i]).getDueTime() + instance.relaxedTWLength, 0, GRB.CONTINUOUS, "w_" + i);
//            a[i] = model.addVar(0, Double.MAX_VALUE, instance.unitDelayCost, GRB.CONTINUOUS, "a_" + i);
//            b[i] = model.addVar(0, Double.MAX_VALUE, instance.unitDelayCost, GRB.CONTINUOUS, "b_" + i);
//        }
//
//        //Constraints
//        for (int i = 0; i < cnt; i++) {
//            if (i < cnt-1) {
//                GRBLinExpr expr1 = new GRBLinExpr();
//                expr1.addTerm(1, w[i + 1]);
//                expr1.addTerm(-1, w[i]);
//                model.addConstr(expr1, GRB.GREATER_EQUAL, arcTimeMatrix[route[i]][route[i+1]], "cons1");
//            }
//
//            GRBLinExpr expr2 = new GRBLinExpr();
//            expr2.addTerm(1, a[i]);
//            expr2.addTerm(1, w[i]);
//            model.addConstr(expr2, GRB.GREATER_EQUAL, nodeList.get(route[i]).getStartTime(), "cons2");
//
//            GRBLinExpr expr3 = new GRBLinExpr();
//            expr3.addTerm(1, w[i]);
//            expr3.addTerm(-1, b[i]);
//            model.addConstr(expr3, GRB.LESS_EQUAL, nodeList.get(route[i]).getDueTime(), "cons3");
//
//            if (route[i] < originNodeNumber && route[i+1] >= originNodeNumber && route[i+1] < nodeList.size() - 1){
//                model.addConstr(w[i+1], GRB.GREATER_EQUAL, instance.droneTimeMatrix[0][nodeList.get(route[i+1]).getId()], "cons4");
//            }
//        }
//
//        model.optimize();
//        model.write("subproblem.lp");
//
//        double obj = model.get(GRB.DoubleAttr.ObjVal);
//
//        model.dispose();
//
//        return obj;
//    }

    class MoveSegment {
        ArrayList<Double> moveTime = new ArrayList<>();
        ArrayList<Integer> delayNum = new ArrayList<>();
        ArrayList<Integer> earlyNum = new ArrayList<>();
        double gapTime = 0;
        double previousDelayTime = 0;
        double move = 0;
        int endID = -1;

        public MoveSegment(double initTime) {
            this.moveTime.add(initTime);
            this.delayNum.add(0);
            this.earlyNum.add(0);
        }

        public MoveSegment deepClone(){
            MoveSegment newSeg = new MoveSegment(0);
            newSeg.moveTime = new ArrayList<>(this.moveTime);
            newSeg.delayNum = new ArrayList<>(this.delayNum);
            newSeg.earlyNum = new ArrayList<>(this.earlyNum);
            newSeg.gapTime = this.gapTime;
            newSeg.previousDelayTime = this.previousDelayTime;
            newSeg.move = this.move;
            newSeg.endID = this.endID;
            return newSeg;
        }
    }
    public double calculatePenalty(int[] newcust, int type) {
        int[] route = new int[newcust.length]; // Generate the current route based on newcust
        int cnt = generateRouteList(newcust, type, route); // Record how many nodes are in the route

        double[] timeList = new double[cnt]; // Record the arrival time at each point
        double[] delayList = new double[cnt]; // Record the arrival time at each point

        double time, delay = 0;
        if (type == 0) {
            time = nodeList.get(route[0]).getDueTime();
        } else{
            time = nodeList.get(route[0]).getStartTime();
        }

        // Construct a feasible initial solution
        for (int i = 0; i < cnt; i++) {
            timeList[i] = time;

            if (timeList[i] > nodeList.get(route[i]).getDueTime() + instance.relaxedTWLength){ // If infeasible, need to roll back
                time = nodeList.get(route[i]).getDueTime() + instance.relaxedTWLength;
                double backTime = timeList[i] - (nodeList.get(route[i]).getDueTime() + instance.relaxedTWLength);

                //Roll back the previous times
                double gapTime = 0;
                for (int j = i; j >= 0; j--) {
                    backTime = backTime - gapTime;
                    if (backTime > 0) {
                        if (j >= 1) {
                            gapTime = (timeList[j] - timeList[j - 1]) - instance.truckTimeMatrix[nodeList.get(route[j]).getId()][nodeList.get(route[j - 1]).getId()];
                        }
                        timeList[j] -= backTime;
                        if (timeList[j] < 0 && timeList[j] > -1e-4){ // Exclude some numerical error; negative numbers close to 0 are treated as 0
                            timeList[j] = 0;
                        }
                        if (timeList[j] < Math.max(0, nodeList.get(route[j]).getStartTime() - instance.relaxedTWLength) - 1e-4) {
                            System.out.println("error"); // Should never happen unless there is a bug
                        }
                    } else {
                        break;
                    }
                }
            }

            if (i < cnt - 1) {
                int iID = nodeList.get(route[i]).getId();
                int jID = nodeList.get(route[i + 1]).getId();
                // Compute the arrival time at the next point
                time += instance.truckTimeMatrix[iID][jID];
                time = Math.max(time, (nodeList.get(route[i + 1]).getStartTime()));

                if (route[i] < originNodeNumber && route[i + 1] >= originNodeNumber && route[i + 1] < nodeList.size() - 1) {
                    time = Math.max(time, instance.droneTimeMatrix[0][jID]);
                }
            }
        }

        int lastDelayNode = 0;
        for (int i = 0; i < cnt; i++) {
            if (timeList[i] < nodeList.get(route[i]).getStartTime()){
                delayList[i] = instance.unitDelayCost * (timeList[i] - nodeList.get(route[i]).getStartTime());
            } else if (timeList[i] > nodeList.get(route[i]).getDueTime()){
                delayList[i] = instance.unitDelayCost * (timeList[i] - nodeList.get(route[i]).getDueTime());
                lastDelayNode = i;
            } else{
                delayList[i] = 0;
            }
            delay += Math.abs(delayList[i]);
        }

        if (delay > 0) {
            //Starting from the last point, look for a segment
            MoveSegment currentSegment = new MoveSegment(timeList[cnt-1]);
            ArrayList<MoveSegment> previousMove = new ArrayList<>();

            for (int i = lastDelayNode; i >= 0; i--) {

                Node node = nodeList.get(route[i]);

                double move2Due = timeList[i] - node.getDueTime();
                if (move2Due > 0) { // For delayed points, first consider the nodes that make it not delayed
                    move2Due += currentSegment.gapTime; // The move needed for this point to become not delayed
                    setMove2Due(move2Due, currentSegment);
                }

                double move2Start = Math.max(0, timeList[i] - node.getStartTime()) + currentSegment.gapTime;
                if (move2Start <= currentSegment.previousDelayTime) {
                    setMove2Start(move2Start, currentSegment);
                }

                double infeaseTime = Math.max(0, node.getStartTime() - instance.relaxedTWLength);
                if (i == 0){
                    infeaseTime = node.getDueTime();
                }
                double move2Infeas = timeList[i] - infeaseTime + currentSegment.gapTime;
                if (i >= 1 && route[i - 1] < originNodeNumber && route[i] >= originNodeNumber && route[i] < nodeList.size() - 1) {
                    move2Infeas = Math.min(move2Infeas, timeList[i] - instance.droneTimeMatrix[0][node.getId()] + currentSegment.gapTime);
                }
                if (move2Infeas <= currentSegment.previousDelayTime) {
                    setMove2Infease(move2Infeas, currentSegment);
                }

                currentSegment.previousDelayTime = Math.max(currentSegment.previousDelayTime, timeList[i] - node.getDueTime()); // The largest past delay

                if (i >= 1) {

                    currentSegment.gapTime += (timeList[i] - timeList[i - 1]) - instance.truckTimeMatrix[nodeList.get(route[i]).getId()][nodeList.get(route[i - 1]).getId()];

                    if (delayList[i] <= 0 && delayList[i - 1] > 0){ //For the end of a segment
                        // Check the current maxMove
                        if (currentSegment.moveTime.size() > 1) {
                            double maxMove = currentSegment.moveTime.get(currentSegment.moveTime.size() - 1);
                            for (int j = 0; j < currentSegment.moveTime.size(); j++) {
                                if (currentSegment.delayNum.get(j) == currentSegment.earlyNum.get(j)
                                        || (currentSegment.delayNum.get(j) < 0 && currentSegment.earlyNum.get(j) < 0)) {
                                    if (j == 0){
                                        maxMove = 0;
                                    } else {
                                        maxMove = currentSegment.moveTime.get(j - 1);
                                    }
                                    break;
                                }
                            }
                            if (previousMove.isEmpty()
                                    || maxMove > previousMove.get(previousMove.size() - 1).move - previousMove.get(previousMove.size() - 1).gapTime) { // If the current maxMove is greater than the move of the previous segment
                                currentSegment.move = maxMove;
                                currentSegment.endID = i;
                                previousMove.add(currentSegment.deepClone());
                            } else {
                                //Take out previousMove one by one and recompute
                                for (int j = previousMove.size() - 1; j >= 0; j--) {
                                    // First merge the two nearest segments
                                    MoveSegment combinedSegment = combineSegment(previousMove.get(j), currentSegment);

                                    maxMove = combinedSegment.moveTime.get(combinedSegment.moveTime.size() - 1);
                                    for (int k = 0; k < combinedSegment.moveTime.size(); k++) {
                                        if (combinedSegment.delayNum.get(k) == combinedSegment.earlyNum.get(k)
                                                || (combinedSegment.delayNum.get(k) < 0 && combinedSegment.earlyNum.get(k) < 0)) {
                                            if (k == 0){
                                                maxMove = 0;
                                            } else {
                                                maxMove = combinedSegment.moveTime.get(k - 1);
                                            }
                                            break;
                                        }
                                    }

                                    previousMove.remove(j);

                                    if (previousMove.isEmpty()
                                            || maxMove > previousMove.get(previousMove.size() - 1).move - previousMove.get(previousMove.size() - 1).gapTime){
                                        combinedSegment.move = maxMove;
                                        combinedSegment.endID = i;
                                        previousMove.add(combinedSegment);
                                        break;
                                    } else {
                                        currentSegment = combinedSegment.deepClone();
                                    }
                                }
                            }
                        }
                        currentSegment = new MoveSegment(timeList[i-1]);
                    }

                }
            }

            if (currentSegment.moveTime.size() > 1) {
                double maxMove = currentSegment.moveTime.get(currentSegment.moveTime.size() - 1);
                for (int j = 0; j < currentSegment.moveTime.size(); j++) {
                    if (currentSegment.delayNum.get(j) == currentSegment.earlyNum.get(j)
                            || (currentSegment.delayNum.get(j) < 0 && currentSegment.earlyNum.get(j) < 0)) {
                        if (j == 0){
                            maxMove = 0;
                        } else {
                            maxMove = currentSegment.moveTime.get(j - 1);
                        }
                        break;
                    }
                }
                if (previousMove.isEmpty()
                        || maxMove > previousMove.get(previousMove.size() - 1).move - previousMove.get(previousMove.size() - 1).gapTime) { // If the current maxMove is greater than the move of the previous segment
                    currentSegment.move = maxMove;
                    currentSegment.endID = 0;
                    previousMove.add(currentSegment.deepClone());
                } else {
                    //Take out previousMove one by one and recompute
                    for (int j = previousMove.size() - 1; j >= 0; j--) {
                        // First merge the two nearest segments
                        MoveSegment combinedSegment = combineSegment(previousMove.get(j), currentSegment);

                        maxMove = combinedSegment.moveTime.get(combinedSegment.moveTime.size() - 1);
                        for (int k = 0; k < combinedSegment.moveTime.size(); k++) {
                            if (combinedSegment.delayNum.get(k) == combinedSegment.earlyNum.get(k)
                                    || (combinedSegment.delayNum.get(k) < 0 && combinedSegment.earlyNum.get(k) < 0)) {
                                if (k == 0){
                                    maxMove = 0;
                                } else {
                                    maxMove = combinedSegment.moveTime.get(k - 1);
                                }
                                break;
                            }
                        }

                        previousMove.remove(j);

                        if (previousMove.isEmpty()
                                || maxMove > previousMove.get(previousMove.size() - 1).move - previousMove.get(previousMove.size() - 1).gapTime){
                            combinedSegment.move = maxMove;
                            combinedSegment.endID = 0;
                            previousMove.add(combinedSegment);
                            break;
                        } else {
                            currentSegment = combinedSegment.deepClone();
                        }
                    }
                }
            }


            double reducedDelayTime = 0;
            int start = lastDelayNode;
            for(MoveSegment seg: previousMove) {
                int end = seg.endID;
                double maxMove = seg.move;
                double gapTime = 0;
                double previousDelayTime = 0;
                for (int i = start; i >= end; i--) {
                    maxMove = maxMove - gapTime;
                    Node node = nodeList.get(route[i]);
                    previousDelayTime = Math.max(previousDelayTime, timeList[i] - node.getDueTime()); // The largest past delay

                    if (maxMove > 0) {
                        double move = maxMove;
                        if (maxMove > previousDelayTime) {
                            move = previousDelayTime;
                        }
                        double currentTime = timeList[i] - move;
                        double currentDelayTime = Math.max(0, currentTime - node.getDueTime());
                        reducedDelayTime += Math.max(0, timeList[i] - node.getDueTime()) - currentDelayTime;
                        double currentEarlyTime = Math.max(0, node.getStartTime() - currentTime);
                        reducedDelayTime += Math.max(0, node.getStartTime() - timeList[i]) - currentEarlyTime;

                        if (i >= 1) {
                            gapTime = (timeList[i] - timeList[i - 1]) - instance.truckTimeMatrix[nodeList.get(route[i]).getId()][nodeList.get(route[i - 1]).getId()];
                        }
                        timeList[i] = currentTime;

                    } else {
                        break;
                    }
                }
                start = end - 1;
            }
            delay -= reducedDelayTime * instance.unitDelayCost;

        }

        return delay;
    }

    public MoveSegment combineSegment(MoveSegment previousSegment, MoveSegment currentSegment){
        MoveSegment combinedSegment = previousSegment.deepClone();

        double gap = previousSegment.gapTime;
        for (int i = 0; i < currentSegment.moveTime.size() - 1; i++) {
            double move = currentSegment.moveTime.get(i) + gap;
            if (currentSegment.delayNum.get(i) > currentSegment.delayNum.get(i+1)){ // delay - 1 operation
                setMove2Due(move, combinedSegment);
            } else if (currentSegment.earlyNum.get(i) < currentSegment.earlyNum.get(i+1)){ // early + 1 operation
                setMove2Start(move, combinedSegment);
            }
        }

        combinedSegment.previousDelayTime = Math.max(previousSegment.gapTime, currentSegment.gapTime);
        combinedSegment.gapTime = previousSegment.gapTime + currentSegment.gapTime;

        return combinedSegment;
    }

    public void setMove2Due(double move2Due, MoveSegment seg) {
        for (int j = 0; j < seg.moveTime.size(); j++) {
            if (move2Due <= seg.moveTime.get(j)) {
                seg.moveTime.add(j, move2Due);
                seg.delayNum.add(j, seg.delayNum.get(j)+1);
                seg.earlyNum.add(j, seg.earlyNum.get(j));
                break;
            }
            seg.delayNum.set(j, seg.delayNum.get(j) + 1);
        }
    }
    public void setMove2Start(double move2Start, MoveSegment seg) {
        boolean flag = false;
        for (int j = 0; j < seg.moveTime.size(); j++) {
            if (!flag) {
                if (move2Start <= seg.moveTime.get(j)) {
                    seg.moveTime.add(j, move2Start);
                    seg.delayNum.add(j, seg.delayNum.get(j));
                    seg.earlyNum.add(j, seg.earlyNum.get(j));
                    flag = true;
                }
            } else {
                seg.earlyNum.set(j, seg.earlyNum.get(j) + 1);
            }
        }
    }
    public void setMove2Infease(double move2Infeas, MoveSegment seg) {
        boolean flag = false;
        for (int j = 0; j < seg.moveTime.size(); j++) {
            if (!flag) {
                if (move2Infeas <= seg.moveTime.get(j)){
                    seg.moveTime.add(j, move2Infeas);
                    seg.delayNum.add(j, seg.delayNum.get(j));
                    seg.earlyNum.add(j, seg.earlyNum.get(j));
                    flag = true;
                }
            } else {
                seg.delayNum.set(j, -1);
                seg.earlyNum.set(j, -1);
            }
        }
    }


    public double calculatePenalty_heur(GRBEnv env, int[] newcust, int type) throws GRBException {
        int[] route = new int[newcust.length]; // Generate the current route based on newcust
        int cnt = generateRouteList(newcust, type, route); // Record how many nodes are in the route

        double[] timeList = new double[cnt]; // Record the arrival time at each point
        double[] delayList = new double[cnt]; // Record the arrival time at each point

        double time, delay = 0;
        if (type == 0) {
            time = nodeList.get(route[0]).getDueTime();
        } else{
            time = nodeList.get(route[0]).getStartTime();
        }

        for (int i = 0; i < cnt - 1; i++) {
            int iID = nodeList.get(route[i]).getId();
            int jID = nodeList.get(route[i+1]).getId();

            if (time > nodeList.get(route[i]).getDueTime() + instance.relaxedTWLength) {
                return instance.M; // Infeasible
            }

            timeList[i] = time;
            delayList[i] = instance.unitDelayCost * Math.max(0, time - nodeList.get(route[i]).getDueTime());
            delay += delayList[i];

            // Compute the arrival time at the next point
            time += instance.truckTimeMatrix[iID][jID];
            time = Math.max(time,(nodeList.get(route[i+1]).getStartTime()));

            if (route[i] < originNodeNumber && route[i+1] >= originNodeNumber && route[i+1] < nodeList.size() - 1){
                time = Math.max(time,instance.droneTimeMatrix[0][jID]);
            }
        }

        return delay;
    }



    public int getCloneIndex(ArrayList<Node> nodeList, int originNodeNumber, int index) {
        if (index >= originNodeNumber){
            for (int i = 0; i < originNodeNumber; i++) {
                if (nodeList.get(i).getId() == nodeList.get(index).getId()) {
                    return i;
                }
            }
        } else if (index < originNodeNumber) {
            for (int i = originNodeNumber; i < nodeList.size(); i++) {
                if (nodeList.get(i).getId() == nodeList.get(index).getId()) {
                    return i;
                }
            }
        }
        return -1;
    }

    public boolean forwardAllVisited(int[] newcust){
        for (int i = 0; i < newcust.length  - 1; i++) {
            if (newcust[i] == 0){
                return false;
            }
        }
        return  true;
    }



}
