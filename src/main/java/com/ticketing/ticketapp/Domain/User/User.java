package com.ticketing.ticketapp.Domain.User;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "user_id")
    private String ID;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email")
    private String email;

    @Version
    @Column(name = "version")
    private int version;

    @Column(name = "age")
    private int age;

    protected User() {}

    public User(String name, String password, int age) {
        this(name, password, age, "");
    }

    public User(String name, String password, int age, String email) {
        this.name = name;
        this.password = password;
        this.ID = java.util.UUID.randomUUID().toString();
        this.age = age;
        this.email = email != null ? email : "";
        this.version = 0;
    }

    public User(User other) {
        this.name = other.getName();
        this.password = other.getPassword();
        this.ID = other.getID();
        this.version = other.getVersion();
        this.age = other.getAge();
        this.email = other.getEmail();
    }

    public String getName()              { return name; }
    public void   setName(String name)   { this.name = name; }

    public String getPassword()              { return password; }
    public void   setPassword(String password) { this.password = password; }

    public String getID()                { return ID; }

    public String getEmail()             { return email; }
    public void   setEmail(String email) { this.email = email != null ? email : ""; }

    public int  getVersion()             { return version; }
    public void setVersion(int version)  { this.version = version; }

    public int  getAge()                 { return age; }

    public String getUserInfo() {
        return "name=" + name;
    }
}
