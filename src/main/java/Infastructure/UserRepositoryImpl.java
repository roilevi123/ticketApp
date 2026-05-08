package Infastructure;

import Domain.User.IUserRepository;
import Domain.User.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserRepositoryImpl implements IUserRepository {
    private final Map<String, String> usernameToId = new ConcurrentHashMap<>();
    private final Map<String, User> usersByID = new ConcurrentHashMap<>();
    public UserRepositoryImpl() {}


    @Override
    public User Store(String username, String password) {
        if(usernameExists(username)) {
            throw new RuntimeException("User already exists");
        }
        User u=new User(username, password);
        usersByID.put(u.getID(), u);
        usernameToId.put(username, u.getID());
        return u;
    }

    @Override
    public String getUserPassword(String username) {
        User user = getUserByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }
        return user.getPassword();
    }

    @Override
    public User getUserByUsername(String username) {
        String userId = usernameToId.get(username);
        if (userId == null) {
            return null;
        }
        return getUserByID(userId);
    }

    @Override
    public User getUserByID(String ID) {
        User user = usersByID.get(ID);
        if (user == null) {
            return null;
        }        
        return new User(user);
    }

    @Override
    public boolean usernameExists(String username) {
        return usernameToId.containsKey(username);
    }

    @Override
    public void save(User userToUpdate) {
        String id = userToUpdate.getID();
        User currentInDb = usersByID.get(id);
        if (currentInDb == null) {
            throw new RuntimeException("User not found for update: ID " + id);
        }
        String oldUsername = currentInDb.getName();
        String newUsername = userToUpdate.getName();
        if (!oldUsername.equals(newUsername) && usernameExists(newUsername)) {
            throw new RuntimeException("Username '" + newUsername + "' is already taken.");
        }
        User updatedUser = new User(userToUpdate);
        updatedUser.setVersion(userToUpdate.getVersion() + 1);
        boolean success = usersByID.replace(id, currentInDb, updatedUser);
        if (!success) {
            throw new RuntimeException("Optimistic Lock Failure: User '" + oldUsername +
                    "' was updated by another thread/user.");
        }
        if (!oldUsername.equals(newUsername)) {
            usernameToId.remove(oldUsername); 
            usernameToId.put(newUsername, id);
        }
    }

    @Override
    public void deleteAll() {
        usersByID.clear();
        usernameToId.clear();
    }

    @Override
    public void deleteUser(String ID) {
        User user = usersByID.get(ID);
        if (user != null) {
            usersByID.remove(ID);
            usernameToId.remove(user.getName());
        }
    }


}