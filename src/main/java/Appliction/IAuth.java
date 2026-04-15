package Appliction;

public interface IAuth {

    String register(String username, String password);

    String login(String username, String password);
}
