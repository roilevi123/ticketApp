package Infastructure;

import Domain.User.IUserRepository;
import Domain.User.User;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserRepositoryImpl implements IUserRepository {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    public UserRepositoryImpl() {}


    @Override
    public void Store(String username, String password) {
        if(users.containsKey(username)) {
            throw new RuntimeException("User already exists");
        }
        User u=new User(username, password);
        users.put(username,u);
    }
    public String getUserPassword(String username) {
        return users.get(username).getPassword();
    }

    @Override
    public User getUser(String username) {
        User user = users.get(username);
        if (user == null) {
            return null;
        }
        return new User(user);
    }

    @Override
    public boolean userExists(String username) {
        return users.containsKey(username);
    }

    @Override
    public void deleteAll() {
        users.clear();
    }

    @Override
    public void deleteUser(String username) {
        users.remove(username);
    }


}
