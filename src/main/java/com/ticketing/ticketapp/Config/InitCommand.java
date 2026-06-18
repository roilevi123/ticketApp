package com.ticketing.ticketapp.Config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Maps one entry of init-state.json. Every action uses only the subset of
 * fields relevant to it; unused fields stay null/default.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InitCommand {

    private String action;

    // register_user
    private String username;
    private String password;
    private Integer age;
    private String email;

    // grant_admin / create_company / create_event / configure_lottery / seed_purchase
    private String founderUsername;
    private String creatorUsername;
    private String buyerUsername;
    private String company;

    // create_event
    private String name;
    private String artist;
    private String eventType;
    private Double price;
    private Integer eventDateDaysFromNow;
    private String location;
    private Integer mapRows;
    private Integer mapCols;

    // configure_lottery / seed_purchase (references an existing event by name)
    private String event;

    // configure_lottery
    private Integer lotteryEndMinutesFromNow;
    private Integer maxWinners;

    // seed_purchase
    private Integer count;

    // seed_notification / mark_last_notification_read
    private String recipient;
    private String title;
    private String message;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFounderUsername() { return founderUsername; }
    public void setFounderUsername(String founderUsername) { this.founderUsername = founderUsername; }

    public String getCreatorUsername() { return creatorUsername; }
    public void setCreatorUsername(String creatorUsername) { this.creatorUsername = creatorUsername; }

    public String getBuyerUsername() { return buyerUsername; }
    public void setBuyerUsername(String buyerUsername) { this.buyerUsername = buyerUsername; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getEventDateDaysFromNow() { return eventDateDaysFromNow; }
    public void setEventDateDaysFromNow(Integer eventDateDaysFromNow) { this.eventDateDaysFromNow = eventDateDaysFromNow; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Integer getMapRows() { return mapRows; }
    public void setMapRows(Integer mapRows) { this.mapRows = mapRows; }

    public Integer getMapCols() { return mapCols; }
    public void setMapCols(Integer mapCols) { this.mapCols = mapCols; }

    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    public Integer getLotteryEndMinutesFromNow() { return lotteryEndMinutesFromNow; }
    public void setLotteryEndMinutesFromNow(Integer lotteryEndMinutesFromNow) { this.lotteryEndMinutesFromNow = lotteryEndMinutesFromNow; }

    public Integer getMaxWinners() { return maxWinners; }
    public void setMaxWinners(Integer maxWinners) { this.maxWinners = maxWinners; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
