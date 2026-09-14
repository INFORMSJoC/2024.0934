public class Node {
    // Basic attributes
    private int id;
    private double xCoordinate;
    private double yCoordinate;
    private double startTime;
    private double dueTime;
    private boolean isMeetingNode = false;

    private String nodeType;
    private int truckID;
    private double demand;

    public boolean isMeetingNode() {
        return isMeetingNode;
    }

    public void setMeetingNode(boolean meetingNode) {
        isMeetingNode = meetingNode;
    }


    public Node(int id, double xCoordinate, double yCoordinate, double dueTime, double startTime) {
        this.id = id;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
        this.dueTime =  dueTime;
        this.startTime = startTime;
    }


    private int index;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getxCoordinate() {
        return xCoordinate;
    }

    public void setxCoordinate(double xCoordinate) {
        this.xCoordinate = xCoordinate;
    }

    public double getyCoordinate() {
        return yCoordinate;
    }

    public void setyCoordinate(double yCoordinate) {
        this.yCoordinate = yCoordinate;
    }

    public double getDueTime() {
        return dueTime;
    }

    public void setDueTime(double dueTime) {
        this.dueTime = dueTime;
    }

    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }


    public double getDemand() {
        return demand;
    }

    public void setDemand(double demand) {
        this.demand = demand;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public int getTruckID() {
        return truckID;
    }

    public void setTruckID(int truckID) {
        this.truckID = truckID;
    }
}
