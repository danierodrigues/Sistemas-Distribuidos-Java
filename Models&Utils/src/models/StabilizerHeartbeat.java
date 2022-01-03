package models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

public class StabilizerHeartbeat extends Heartbeat implements Serializable {
    private ArrayList<UUID> confirmationDoneList;
    private ArrayList<UUID> confirmationNewsList;

    public StabilizerHeartbeat(String address, String type, ArrayList<UUID> confirmationDoneList, ArrayList<UUID> confirmationNewsList){
        super(address, type);
        this.confirmationDoneList = confirmationDoneList;
        this.confirmationNewsList = confirmationNewsList;
    }


    public ArrayList<UUID> getConfirmationDoneList() {return confirmationDoneList;}

    public void setConfirmationDoneList(ArrayList<UUID> confirmationDoneList) {this.confirmationDoneList = confirmationDoneList;}

    public ArrayList<UUID> getConfirmationNewsList() {return confirmationNewsList;}

    public void setConfirmationNewsList(ArrayList<UUID> confirmationNewsList) {this.confirmationNewsList = confirmationNewsList;}
}
