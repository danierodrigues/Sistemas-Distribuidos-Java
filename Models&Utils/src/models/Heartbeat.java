package models;

import java.io.Serializable;
import java.time.LocalDateTime;

public abstract class Heartbeat implements Serializable {
    private String address;
    private String type;
    private LocalDateTime time;

    public Heartbeat(String address,String type) {
        this.address = address;
        this.type = type;
        this.time = LocalDateTime.now();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public String getAddress() {return address;}

    public void setAddress(String address) {this.address = address;}


    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}
