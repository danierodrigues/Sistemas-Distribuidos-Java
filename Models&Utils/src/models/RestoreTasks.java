package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

public class RestoreTasks {
    private ArrayList<Script> tasks;
    private ArrayList<UUID> scriptID;
    private LocalDateTime dateTime;

    public RestoreTasks(ArrayList<Script> tasks, ArrayList<UUID> scriptID){
        this.tasks = tasks;
        this.scriptID = scriptID;
        this.dateTime = LocalDateTime.now();
    }


    public ArrayList<Script> getTasks() {
        return tasks;
    }

    public void setTasks(ArrayList<Script> tasks) {
        this.tasks = tasks;
    }


    public ArrayList<UUID> getScriptID() {
        return scriptID;
    }

    public void setScriptID(ArrayList<UUID> scriptID) {
        this.scriptID = scriptID;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }
}
