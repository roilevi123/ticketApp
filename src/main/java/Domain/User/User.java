package Domain.User;

public class User {
    private String name;
    private String password;
    private String ID;
    private int version;
    public User(String name, String password) {
        this.name = name;
        this.password = password;
        this.ID = java.util.UUID.randomUUID().toString();
        version = 0;
    }
    public User(User other) {
        this.name = other.getName();
        this.password = other.getPassword();
        this.ID = other.getID();
        this.version = other.getVersion();
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getID() {
        return ID;
    }
    public int getVersion() {
        return version;
    }
    public void setVersion(int version) {
        this.version = version;
    }

    public String getUserInfo() {
        return "name=" + name;
    }
}

