package models;

import java.io.Serializable;
import java.util.ArrayList;

public class ProcessorHeartbeat extends Heartbeat implements Serializable {
    private double cpuUsage;
    private ArrayList<Script> tasks;


    public ProcessorHeartbeat(String address, String type, double cpuUsage, ArrayList<Script> tasks) {
        super(address, type);
        this.cpuUsage = cpuUsage;
        this.tasks = tasks;
    }

    public double getCpuUsage() {return cpuUsage;}

    public void setCpuUsage(double cpuUsage) {this.cpuUsage = cpuUsage;}

    public ArrayList<Script> getTasks() {
        return tasks;
    }

    public void setTasks(ArrayList<Script> tasks) {
        this.tasks = tasks;
    }
}
