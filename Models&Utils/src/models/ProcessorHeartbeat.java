package models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

public class ProcessorHeartbeat extends Heartbeat implements Serializable {
    private double cpuUsage;
    private ArrayList<Script> tasks;
    private ArrayList<UUID> doneTasks;
    private ArrayList<Script> newsTasks;


    public ProcessorHeartbeat(String address, String type, double cpuUsage, ArrayList<UUID> doneTasks, ArrayList<Script> newsTasks) {
        super(address, type);
        this.cpuUsage = cpuUsage;
        this.tasks = tasks;
        this.doneTasks = doneTasks;
        this.newsTasks = newsTasks;
    }

    public double getCpuUsage() {return cpuUsage;}

    public void setCpuUsage(double cpuUsage) {this.cpuUsage = cpuUsage;}


    public ArrayList<UUID> getDoneTasks() { return doneTasks; }

    public void setDoneTasks(ArrayList<UUID> doneTasks) { this.doneTasks = doneTasks; }

    public ArrayList<Script> getNewsTasks() { return newsTasks; }

    public void setNewsTasks(ArrayList<Script> newsTasks) { this.newsTasks = newsTasks; }
}
