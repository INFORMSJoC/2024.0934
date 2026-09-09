//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class GenerateInstance {
    private static int frange = 20;

    public GenerateInstance() {
    }

    public ArrayList<Node> generateNewCus(int customerNumber) {
        ArrayList<Node> newOrders = new ArrayList();
        Random random = new Random();

        for(int i = 0; i < customerNumber; ++i) {
            double xCoordinate = (double)random.nextInt(frange * 10);
            xCoordinate /= 10;
            double yCoordinate = (double)random.nextInt(frange * 10);
            yCoordinate /= 10;
            double dueTime = (double)(6 + random.nextInt(30));
            dueTime /= 2.0;
            Instance instance = new Instance();
            Node node = new Node(i, xCoordinate, yCoordinate, dueTime, Math.max(0.0, dueTime - instance.timeWindowLength));
            node.setNodeType("newCustomer");
            node.setTruckID(-1);
            newOrders.add(node);
        }

        return newOrders;
    }

    public static void main(String[] args) {
        int totalx = 0;
        int totaly = 0;
            Data data = new Data(72, 17, 5);

        try {
            FileWriter writer = new FileWriter("data/A_" + data.truckNum + "_" + data.nodeNum + "_3.txt");
            writer.write(data.truckNum + "\t" + data.nodeNum + "\r\n");
            writer.write("TruckID\tinitLoc\tinitTime\r\n");
            Random random = new Random();

            int i;
            for(i = 0; i < data.truckNum; ++i) {
                data.truckID[i] = i;
                writer.write(i + "\t\t");
                data.initLocation[i] = 1 + random.nextInt(data.onboardCustomerNum);
                writer.write(data.initLocation[i] + "\t\t");
                data.initTime[i] = (double)random.nextInt(4);
                data.initTime[i] /= 2.0;
                writer.write(data.initTime[i] + "\r\n");
            }

            writer.write("NodeID\tCorX\t\tCorY\t\tDueTime\t\tType\tTruckID\r\n");

            for(i = 1; i < data.nodeNum; ++i) {
                data.nodeID[i] = i;
                data.corX[i] = (double)random.nextInt(frange * 10);
                data.corX[i] /= 10;
                data.corY[i] = (double)random.nextInt(frange * 10);
                data.corY[i] /= 10;
                totalx = (int)((double)totalx + data.corX[i]);
                totaly = (int)((double)totaly + data.corY[i]);
                if (i <= data.onboardCustomerNum) {
                    data.type[i] = 1;
                    data.dueTime[i] = (double)(6 + random.nextInt(data.nodeNum * 3));
                    data.dueTime[i] /= 2.0;

                    for(int j = 0; j < data.truckNum; ++j) {
                        if (i == data.initLocation[j]) {
                            data.dueTime[i] = data.initTime[j];
                            break;
                        }
                    }
                } else {
                    data.type[i] = 2;
                    data.dueTime[i] = (double)(6 + random.nextInt(data.nodeNum * 3));
                    data.dueTime[i] /= 2.0;
                }

                data.assignedTruck[i] = -1;
            }

            data.nodeID[0] = 0;
            data.corX[0] = (double)(totalx / (data.nodeNum - 1) * 10);
            data.corX[0] /= 10;
            data.corY[0] = (double)(totaly / (data.nodeNum - 1) * 10);
            data.corY[0] /= 10;
            data.dueTime[0] = (double)(data.nodeNum * 3);
            data.type[0] = 0;
            data.assignedTruck[0] = -1;

            for(i = 0; i < data.nodeNum; ++i) {
                writer.write(i + "\t\t" + data.corX[i] + " \t\t" + data.corY[i] + " \t\t" + data.dueTime[i] + " \t\t" + data.type[i] + "\t\t" + data.assignedTruck[i] + "\r\n");
            }

            writer.close();
            JFrame f = new JFrame();
            f.getContentPane().add(new DrawTextUsingGUI(data));
            f.setSize(1050, 1050);
            f.setVisible(true);
        } catch (Exception var8) {
            System.out.println("An error occurred.");
            var8.printStackTrace();
        }

    }

    public static class DrawTextUsingGUI extends JPanel {
        Data data;

        public DrawTextUsingGUI(Data data) {
            this.data = data;
        }

        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Font font = new Font("Serif", 0, 12);
            g2.setFont(font);

            for(int i = 0; i < this.data.nodeNum; ++i) {
                int x = (int) (this.data.corX[i] * 50) + 20;
                int y = (int) (this.data.corY[i] * 50) + 20;
                g2.fillOval(x, y, 5, 5);
                g2.drawString(String.valueOf(i), x, y);
            }

        }
    }

    static class Data {
        public int onboardCustomerNum;
        public int newCustomerNum;
        public int nodeNum;
        public int[] nodeID;
        public double[] corX;
        public double[] corY;
        public double[] dueTime;
        public int[] type;
        public int[] assignedTruck;
        public int truckNum;
        public int[] truckID;
        public int[] initLocation;
        public double[] initTime;

        public Data(int onboardNum, int newNum, int truckNum) {
            this.onboardCustomerNum = onboardNum;
            this.newCustomerNum = newNum;
            this.nodeNum = 1 + onboardNum + newNum;
            this.truckNum = truckNum;
            this.nodeID = new int[this.nodeNum];
            this.corX = new double[this.nodeNum];
            this.corY = new double[this.nodeNum];
            this.dueTime = new double[this.nodeNum];
            this.type = new int[this.nodeNum];
            this.assignedTruck = new int[this.nodeNum];
            this.truckID = new int[this.truckNum];
            this.initLocation = new int[this.truckNum];
            this.initTime = new double[this.truckNum];
        }
    }
}
