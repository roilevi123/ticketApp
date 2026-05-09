package Domain.User;

public class User {
    private String name;
    private String password;
    private String ID;
    private int version;
    private int age;
    public User(String name, String password,int age) {
        this.name = name;
        this.password = password;
        this.ID = java.util.UUID.randomUUID().toString();
        this.age = age;
        version = 0;
    }
    public User(User other) {
        this.name = other.getName();
        this.password = other.getPassword();
        this.ID = other.getID();
        this.version = other.getVersion();
        this.age = other.getAge();
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
    public int getAge() {
        return age;
    }

    public String getUserInfo() {
        return "name=" + name;
    }
}

