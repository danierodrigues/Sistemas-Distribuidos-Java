package utils;

import models.ProcessorHeartbeat;
import models.Script;

import java.util.ArrayList;

public class HeartbeatsUtils {

    public static ProcessorHeartbeat setProcessorAvailable(ProcessorHeartbeat heartbeat, ProcessorHeartbeat oldHeartbeat ){
        ProcessorHeartbeat information = oldHeartbeat;
        ArrayList<Script> scriptList = information.getNewsTasks();
        heartbeat.getDoneTasks().forEach(uuid -> {
            for(Script script : information.getNewsTasks()){
                if(script.getUuid() == uuid){
                    scriptList.remove(script);
                }
            }
        });

        heartbeat.getNewsTasks().forEach(script -> {
            scriptList.add(script);
        });

        information.setNewsTasks(scriptList);
        return information;
    }

}
