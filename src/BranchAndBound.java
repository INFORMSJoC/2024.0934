import com.gurobi.gurobi.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class BranchAndBound {
    double lowerBound;
    double upperBound;
    treeBB incumbent_node;
    public double terminateTime = 1e10;
    public double initLB;
    public int branchNum = 0;
    public ArrayList<Route> rootOptimalSolution = new ArrayList<>();
    public ArrayList<Integer[][]> currentArc = new ArrayList<>();
    private final ExecutorService executorService;
    private final BlockingDeque<treeBB> nodeQueue;
    private final AtomicReference<Double> globalLowerBound;
    private final AtomicReference<Double> globalUpperBound;
    private final AtomicReference<treeBB> globalIncumbent;
    private final AtomicBoolean isTerminated;
    private final AtomicInteger activeTasks = new AtomicInteger(0); // Number of active tasks

    public BranchAndBound() {
        this.lowerBound = -1e10;
        this.upperBound = 1e10;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.nodeQueue = new LinkedBlockingDeque<>();
        this.globalLowerBound = new AtomicReference<>(lowerBound);
        this.globalUpperBound = new AtomicReference<>(upperBound);
        this.globalIncumbent = new AtomicReference<>(incumbent_node);
        this.isTerminated = new AtomicBoolean(false);
    }

    class treeBB {
        treeBB father; // Node before branching
        int depth; // The depth of this node
        int assignedTruck; // The selected truck
        double localLowerBound;
        double localUpperBound;
        ArrayList<Route>  routeList;
        ArrayList<Node>  remainNodeList;
        ArrayList<Node>  rejectNodeList;

        // Cut the rejection branch
        boolean isCut = false;
        treeBB rejBranch; // The sibling of this node, i.e., the node that performs the reject operation
        boolean topLevel; // Compute the global lower bound; it needs to know whether all of the above has been considered
        HashMap<Integer, ArrayList> P;
    }

    public static ArrayList arrayListClone(ArrayList list){
        ArrayList newList = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            newList.add(list.get(i));
        }
        return newList;
    }

    public static ArrayList<Route> routeClone(ArrayList<Route> routeList){
        ArrayList<Route> newRouteList = new ArrayList<>();
        for (int i = 0; i < routeList.size(); i++) {
            Route route = new Route();
            route.setObj(routeList.get(i).getObj());
            route.setCost(routeList.get(i).getCost());
            route.setTime(routeList.get(i).getTime());
            route.setDelay(routeList.get(i).getDelay());
            route.truckID = routeList.get(i).truckID;
            route.path = arrayListClone(routeList.get(i).path);
            route.newCustomerList = arrayListClone(routeList.get(i).newCustomerList);
            route.hasMeetingNode = routeList.get(i).hasMeetingNode;
            newRouteList.add(route);
        }
        return newRouteList;
    }

    public static Node nodeClone(Node node){
        Node newNode = new Node(node.getId(), node.getxCoordinate(), node.getyCoordinate(), node.getDueTime(), node.getStartTime());
        newNode.setMeetingNode(node.isMeetingNode());
        newNode.setNodeType(node.getNodeType());
        newNode.setTruckID(node.getTruckID());
        newNode.setDemand(node.getDemand());
        return newNode;
    }

    public double calculateLowerBound(ArrayList<Route> routeList){
        double lb = 0;
        for (int i = 0; i < routeList.size(); i++) {
            lb += routeList.get(i).getObj();
        }
        return lb;
    }

    public int[] selectCustomer(ArrayList<Node> remainNodeList, Instance instance, ArrayList<Route> routeList){
        int[] min_index = new int[1 + instance.truckNumber];
        Arrays.fill(min_index, -1);

        double min_dis = Double.MAX_VALUE; //Minimum insertion cost
        int min_dis_index = -1; // The customer corresponding to the minimum insertion cost
        int min_truck_index = -1; // The truck corresponding to the minimum insertion cost
        ArrayList<Integer> min_sort_truck = new ArrayList<>(); //Trucks sorted by the distance from the selected point to the truck in descending order
        for(int i = 0; i < remainNodeList.size(); i++){
            ArrayList<Double> sort_dis = new ArrayList<>(); //Distances from the selected point to the trucks sorted in descending order
            ArrayList<Integer> sort_truck = new ArrayList<>(); //Corresponding truck indices sorted in descending order
            for (int k = 0; k < routeList.size(); k++) {
                Route route = routeList.get(k);
                double min_truck_dis = Double.MAX_VALUE;
                for (int j = 0; j < route.path.size() - 1; j++){ // Iterate over the path arcs, excluding the final depot
                    double before_current_dis = instance.manhattanDis[route.path.get(j).getId()][remainNodeList.get(i).getId()];
                    double current_after_dis = instance.manhattanDis[remainNodeList.get(i).getId()][route.path.get(j + 1).getId()];
                    double before_after_dis = instance.manhattanDis[route.path.get(j).getId()][route.path.get(j + 1).getId()];
                    double add_dis =  before_current_dis + current_after_dis - before_after_dis;
                    if (add_dis < min_truck_dis){ // Find the nearest insertion distance of customer i for truck k
                        min_truck_dis = add_dis;
                    }
                }

                // Insert truck k into the list in descending order of distance
                if (min_truck_dis < Double.MAX_VALUE) {
                    if (sort_dis.isEmpty()) {
                        sort_dis.add(min_truck_dis);
                        sort_truck.add(k);
                    } else {
                        boolean flag = true;
                        for (int j = 0; j < sort_dis.size(); j++) {
                            if (sort_dis.get(j) < min_truck_dis) {
                                sort_dis.add(j, min_truck_dis);
                                sort_truck.add(j, k);
                                flag = false;
                                break;
                            }
                        }
                        if (flag) {
                            sort_truck.add(k);
                        }
                    }
                }

                if (min_truck_dis < min_dis){ //If the nearest insertion distance is globally minimal
                    min_dis = min_truck_dis;
                    min_dis_index = i;
                    min_truck_index = k;
                    min_sort_truck = sort_truck;
                }
            }
        }


        min_index[0] = min_dis_index;
        for (int i = 0; i < min_sort_truck.size(); i++) {
            min_index[i + 1] = min_sort_truck.get(i);
        }

        return min_index;
    }

    public synchronized boolean updateGlobalUpperBound(double expected, double newValue) {
        if (globalUpperBound.get().doubleValue() == expected) {
            globalUpperBound.set(newValue);
            return true;
        }
        return false;
    }

    class NodeProcessor implements Runnable {
        private final Instance instance;
        private final CountDownLatch completionLatch;

        public NodeProcessor(Instance instance, CountDownLatch completionLatch) {
            this.instance = instance;
            this.completionLatch = completionLatch;
        }

        @Override
        public void run() {
            try {
                while (!isTerminated.get()) {
                    treeBB currentNode = nodeQueue.poll(100, TimeUnit.MILLISECONDS); // Try to obtain a node from the blocking queue nodeQueue, waiting at most 100 milliseconds
//                    System.out.println(Thread.currentThread().getName() + " Trying to get a node, result: " + (currentNode != null));
                    if (currentNode == null) {
                        if (nodeQueue.isEmpty() && completionLatch.getCount() == 0) {
                            break;
                        }
                        continue;
                    }

                    processNode(currentNode, instance);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                completionLatch.countDown();
            }
        }

        private void processNode(treeBB currentNode, Instance instance) {
            if (currentNode.isCut) {
                return;
            }

            try {
                if (currentNode.father != null) {
                    LabelingAlgorithm labelingAlgo = new LabelingAlgorithm();
                    int truck = currentNode.assignedTruck;
                    double rejLB = 0, obj = 0;
                    if (truck > -1) {
                        rejLB = calculateLowerBound(currentNode.routeList) + (currentNode.rejectNodeList.size() + 1) * instance.rejectionCost;
                        obj = labelingAlgo.shortestPath(instance, currentNode.routeList.get(truck), (ArrayList) currentNode.P.get(truck), instance.truckList.get(truck));
                    }
                    if (obj == -1) {
//                        System.out.println("INFEASE | BB Depth: " + currentNode.depth + " | Local CG cost: --");
                        return;
                    } else {
                        currentNode.localLowerBound = calculateLowerBound(currentNode.routeList) + currentNode.rejectNodeList.size() * instance.rejectionCost;
                        currentNode.localUpperBound = currentNode.localLowerBound + currentNode.remainNodeList.size() * instance.rejectionCost;

                        if (currentNode.localLowerBound <= rejLB && currentNode.rejBranch != null) {
                            currentNode.rejBranch.isCut = true;
                        }

                        // Update global bounds atomically
                        while (true) {
                            double currentUB = globalUpperBound.get();
                            if (currentNode.localUpperBound < currentUB - 1e-4) {
//                                System.out.println(globalUpperBound.compareAndSet(currentUB, currentNode.localUpperBound));
//                                System.out.println(globalUpperBound.get() == currentUB);
                                if (updateGlobalUpperBound(currentUB, currentNode.localUpperBound)) {
                                System.out.println("OPT | Lower bound: " + globalLowerBound.get()
                                        + " | Upper bound: " + globalUpperBound.get() + " | Gap: "
                                        + ((globalUpperBound.get() - globalLowerBound.get()) / globalUpperBound.get())+
                                                "| BB Depth: " + currentNode.depth);
                                globalIncumbent.set(currentNode);
                                break;
                                }
                            } else {
                                break;
                            }
                        }

                        if (currentNode.localLowerBound > globalUpperBound.get()) { // Prune
//                            System.out.println("CUT | BB Depth: " + currentNode.depth + " | Local CG cost: " + currentNode.localLowerBound);
                            return;
                        }
                    }
                }

                if (currentNode.localLowerBound != currentNode.localUpperBound) {
                    int[] assignedPair = selectCustomer(currentNode.remainNodeList, instance, currentNode.routeList);

                    if (assignedPair[0] > -1) {
                        // Create rejection node
                        treeBB rejNode = createRejectionNode(currentNode, assignedPair, instance);
                        nodeQueue.offerFirst(rejNode);
                        activeTasks.incrementAndGet();

                        // Create assignment nodes
                        for (int i = 0; i < instance.truckList.size(); i++) {
                            int k = assignedPair[i + 1];
                            if (k == -1) {
                                break;
                            }

                            treeBB newNode = createAssignmentNode(currentNode, assignedPair, k, instance);
                            if (newNode == null) {
                                continue; // Skip if the new node is infeasible
                            }
                            nodeQueue.offerFirst(newNode);
                            activeTasks.incrementAndGet();

                        }
                    }
                }
            } finally {
                activeTasks.decrementAndGet();
            }
        }

        private treeBB createRejectionNode(treeBB currentNode, int[] assignedPair, Instance instance) {
            treeBB rejNode = new treeBB();
            rejNode.father = currentNode;
            rejNode.depth = currentNode.depth + 1;
            rejNode.assignedTruck = -1;
            rejNode.rejBranch = null;
            rejNode.routeList = routeClone(currentNode.routeList);
            rejNode.remainNodeList = arrayListClone(currentNode.remainNodeList);
            rejNode.remainNodeList.remove(assignedPair[0]);
            rejNode.rejectNodeList = arrayListClone(currentNode.rejectNodeList);
            rejNode.rejectNodeList.add(currentNode.remainNodeList.get(assignedPair[0]));
            rejNode.P = new HashMap();
            for (int j = 0; j < instance.truckList.size(); j++) {
                ArrayList<Node> newNodeList = arrayListClone((ArrayList) currentNode.P.get(j));
                rejNode.P.put(j, newNodeList);
            }
            return rejNode;
        }

        private treeBB createAssignmentNode(treeBB currentNode, int[] assignedPair, int truckIndex, Instance instance) {
            treeBB newNode = new treeBB();
            newNode.father = currentNode;
            newNode.depth = currentNode.depth + 1;
            newNode.assignedTruck = truckIndex;
            newNode.rejBranch = currentNode.rejBranch;
            newNode.routeList = routeClone(currentNode.routeList);
            newNode.remainNodeList = arrayListClone(currentNode.remainNodeList);
            newNode.remainNodeList.remove(assignedPair[0]);
            newNode.rejectNodeList = arrayListClone(currentNode.rejectNodeList);
            newNode.P = new HashMap();
            int droneNumber = 0;
            for (int j = 0; j < instance.truckList.size(); j++) {
                ArrayList<Node> newNodeList = arrayListClone((ArrayList) currentNode.P.get(j));
                if (truckIndex == j) {
                    newNodeList.add(currentNode.remainNodeList.get(assignedPair[0]));
                }
                newNode.P.put(j, newNodeList);
                if (newNodeList.size() > instance.droneCapacity) {
                    return null;
                }
                if (!newNodeList.isEmpty()) {
                    droneNumber ++;
                }
            }
            // Check whether newNode is feasible
            if (droneNumber > instance.droneNumber) {
                return null; // Exceeds the number of trucks, return null
            }

            return newNode;
        }
    }

    public void BBnode(Instance instance, ArrayList<Route> initRoutes) throws IOException, GRBException {
        double eps = 1e-3;
        long startTime = System.currentTimeMillis();

        // Initialize root node
        treeBB initialNode = new treeBB();
        initialNode.depth = 0;
        initialNode.rejBranch = null;
        initialNode.father = null;
        initialNode.routeList = routeClone(initRoutes);
        initialNode.remainNodeList = arrayListClone(instance.newNodeList);
        initialNode.rejectNodeList = new ArrayList<>();
        initialNode.localLowerBound = calculateLowerBound(initialNode.routeList) + initialNode.rejectNodeList.size() * instance.rejectionCost;
        initialNode.localUpperBound = initialNode.localLowerBound + initialNode.remainNodeList.size() * instance.rejectionCost;
        initialNode.P = new HashMap<Integer, ArrayList>();
        for (int i = 0; i < instance.truckList.size(); i++) {
            ArrayList<Node> newNodeList = new ArrayList<>();
            initialNode.P.put(i, newNodeList);
        }

        // Initialize global bounds
        globalLowerBound.set(initialNode.localLowerBound);
        globalUpperBound.set(initialNode.localUpperBound);
        globalIncumbent.set(initialNode);

        // Add root node to queue
        nodeQueue.offer(initialNode);
        activeTasks.incrementAndGet();

        System.out.println("OPT | Lower bound: " + globalLowerBound.get()
                + " | Upper bound: " + globalUpperBound.get() + " | Gap: "
                + ((globalUpperBound.get() - globalLowerBound.get()) / globalUpperBound.get()) + " | Active tasks: " + activeTasks.get());

        // Create and start worker threads
        int numWorkers = Runtime.getRuntime().availableProcessors();
        CountDownLatch completionLatch = new CountDownLatch(numWorkers);
        for (int i = 0; i < numWorkers; i++) {
            executorService.submit(new NodeProcessor(instance, completionLatch));
        }

        // Monitor progress and termination
        int cnt = 0;
        try {
            while (!(activeTasks.get() == 0 && nodeQueue.size() == 0) &&  // Main termination condition: no active threads and the queue is empty
                   System.currentTimeMillis() - startTime < terminateTime * 1000 && 
                   globalUpperBound.get() - globalLowerBound.get() > eps) {
                Thread.sleep(100); // Check conditions every 100ms
                cnt++;
                //Output the active task status every 100 seconds
                if (cnt % 1000 == 0) {
                    System.out.println("ACTIVE TASKS: " + activeTasks.get() + "queue" + nodeQueue.size());
                }
            }
            isTerminated.set(true);
            completionLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executorService.shutdown();
            try {
                executorService.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Update final results
        lowerBound = globalLowerBound.get();
        upperBound = globalUpperBound.get();
        incumbent_node = globalIncumbent.get();
    }

}
