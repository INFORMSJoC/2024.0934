import java.util.ArrayList;

public class Truck {
    private int id;
    private int initLocation;
    private double initTime;
    private ArrayList<Node> onboardNodeList;

    public Truck(int id, int initLocation, double initTime) {
        this.id = id;
        this.initLocation = initLocation;
        this.initTime = initTime;
        this.onboardNodeList = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInitLocation() {
        return initLocation;
    }

    public void setInitLocation(int initLocation) {
        this.initLocation = initLocation;
    }

    public double getInitTime() {
        return initTime;
    }

    public void setInitTime(double initTime) {
        this.initTime = initTime;
    }

    public ArrayList<Node> getOnboardNodeList() {
        return onboardNodeList;
    }

    public void setOnboardNodeList(ArrayList<Node> onboardNodeList) {
        this.onboardNodeList = onboardNodeList;
    }

}
