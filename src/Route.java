import java.util.ArrayList;

import static java.lang.Math.max;

public class Route {
    private double obj;
    private double cost;
    private double time;
    private double delay;
    public int truckID;
    public ArrayList<Node> path;
    public ArrayList<Node> newCustomerList;
    public boolean hasMeetingNode;

    public Route() {
        this.obj        =  0.0;
        this.time       =  0.0;
        this.delay      =  0.0;
        this.cost       = 0.0;
        this.path       =  new ArrayList<>();
        this.hasMeetingNode = false;
        this.newCustomerList = new ArrayList<>();
    }

    public double getObj() {
        return obj;
    }

    public void setObj(double cost) {
        this.obj = cost;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public double getDelay() {
        return delay;
    }

    public void setDelay(double delay) {
        this.delay = delay;
    }

//    // Not applicable: compute the obj of the current route
//    public void computeObj(Instance instance) {
//        this.time = instance.truckList.get(this.truckID).getInitTime();
//        this.delay = 0.0;
//        this.cost = 0.0;
//        for(int i = 0; i < this.path.size() - 1; i++) { // Iterate over the path arcs, excluding the final depot
//            int iID = this.path.get(i).getId();
//            int jID = this.path.get(i+1).getId();
//            this.delay = this.delay + max(0, this.time - instance.nodeList.get(iID).getDueTime()) * instance.unitDelayCost;
//            this.cost = this.cost + instance.manhattanDis[iID][jID] * instance.truckUnitCost;
//            this.time = this.time + instance.truckTimeMatrix[iID][jID];
//        }
//        this.obj = this.cost + this.delay;
//    }


}
