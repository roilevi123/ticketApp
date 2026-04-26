package Appliction;

import Domain.Domains.UserDomain;
import Domain.User.User;

public class UserService {
    private UserDomain domain;
    public UserService(UserDomain domain) {
        this.domain = domain;
    }
    public String register(String username, String password) {
        return domain.register(username, password);

    }

    public String login(String username, String password) {
        return domain.login(username, password);
    }

    public String logout(String token) {
        return domain.logout(token);
    }

    public String getUserInfo(String token) {
        return domain.getUserInfo(token);
    }

    public String updateUserPassword(String token, String newPassword) {
        return domain.updateUserPassword(token, newPassword);
    }

}
