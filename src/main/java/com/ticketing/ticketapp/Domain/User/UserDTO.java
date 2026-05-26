package com.ticketing.ticketapp.Domain.User;

public class UserDTO {
    private String  name;
    private String  ID;
    private Integer age;
    private String  email;

    public UserDTO() {}

    public UserDTO(String name, String ID, Integer age) {
        this.name = name;
        this.ID   = ID;
        this.age  = age;
    }

    public static UserDTO fromEntity(User user) {
        if (user == null) {
            return null;
        }
        UserDTO dto = new UserDTO(user.getName(), user.getID(), user.getAge());
        dto.setEmail(user.getEmail());
        return dto;
    }

    public String getName()          { return name; }
    public void   setName(String v)  { name = v; }

    public String getID()            { return ID; }
    public void   setID(String v)    { ID = v; }

    public Integer getAge()           { return age; }
    public void    setAge(Integer v) { age = v; }

    public String getEmail()         { return email; }
    public void   setEmail(String v) { email = v; }
}
