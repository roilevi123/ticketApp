package com.ticketing.ticketapp.Domain.User;

import java.time.LocalDateTime;

public class Suspension {
    String userID;
    LocalDateTime startTime;
    LocalDateTime endTime;
    boolean isPermanent;

    public Suspension(String userID, LocalDateTime startTime, LocalDateTime endTime){
        this.userID=userID;
        this.startTime=startTime;
        this.endTime=endTime;
        this.isPermanent=false;
    }
    public Suspension(String userID, LocalDateTime startTime){
        this.userID=userID;
        this.startTime=startTime;
        this.endTime=null;
        this.isPermanent=true;
    }

    public String getUserID(){
        return userID;
    }

    public LocalDateTime getStartTime(){
        return startTime;
    }

    public LocalDateTime getEndTime(){
        return endTime;
    }

    public boolean isPermanent(){
        return isPermanent;
    }
}
