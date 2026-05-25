package com.ticketing.ticketapp.Domain.User;

import java.time.LocalDateTime;

public class Suspension {
    int userID;
    LocalDateTime startTime;
    LocalDateTime endTime;

    Suspension(int userID, LocalDateTime startTime, LocalDateTime endTime){
        this.userID=userID;
        this.startTime=startTime;
        this.endTime=endTime;
    }

    int getUserID(){
        return userID;
    }

    LocalDateTime getStartTime(){
        return startTime;
    }

    LocalDateTime getEndTime(){
        return endTime;
    }
}
