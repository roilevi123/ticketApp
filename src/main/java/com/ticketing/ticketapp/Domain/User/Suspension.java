package com.ticketing.ticketapp.Domain.User;

import java.time.LocalDateTime;

public class Suspension {
    String userID;
    LocalDateTime startTime;
    LocalDateTime endTime;

    public Suspension(String userID, LocalDateTime startTime, LocalDateTime endTime){
        this.userID=userID;
        this.startTime=startTime;
        this.endTime=endTime;
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
}
