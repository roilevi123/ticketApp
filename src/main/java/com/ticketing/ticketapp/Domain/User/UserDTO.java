package com.ticketing.ticketapp.Domain.User;

public class UserDTO {
    private String name;
    private String ID;
    private int age;

    public UserDTO(String name, String ID, int age) {
        this.name = name;
        this.ID = ID;
        this.age = age;
    }

    public static UserDTO fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserDTO(user.getName(), user.getID(), user.getAge());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
