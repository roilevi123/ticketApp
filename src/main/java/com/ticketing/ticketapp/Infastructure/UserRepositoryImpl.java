package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.User.Suspension;
import com.ticketing.ticketapp.Domain.User.User;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class UserRepositoryImpl implements IUserRepository {
    private final Map<String, String> usernameToId = new ConcurrentHashMap<>();
    private final Map<String, User> usersByID = new ConcurrentHashMap<>();
    private final Map<String, Suspension> currentSuspensions = new ConcurrentHashMap<>();
    private final List<Suspension> suspensionHistory = new LinkedList<>();
    public UserRepositoryImpl() {}


    @Override
    public User Store(String username, String password, int age, String email) {
        if (usernameExists(username)) {
            throw new RuntimeException("User already exists");
        }
        User u = new User(username, password, age, email);
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
    public String getUsernameByID(String ID) {
        User user = usersByID.get(ID);
        if (user == null) {
            throw new RuntimeException("User not found for ID: " + ID);
        }
        return user.getName();
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

    @Override
    public void addCurrentSuspension(String userID, Suspension suspension){
        currentSuspensions.put(userID, suspension);
    }

    @Override
    public void addHistorySuspension(Suspension suspension){
        suspensionHistory.add(suspension);
    }

    @Override
    public boolean isUserSuspendedNow(String userID){
        if(currentSuspensions.containsKey(userID)){
            Suspension suspension = currentSuspensions.get(userID);
            if(suspension.isPermanent()==true)
                return true;
            if(!suspension.getEndTime().isBefore(LocalDateTime.now())){
                return true;
            }
            currentSuspensions.remove(userID);
            addHistorySuspension(suspension);
        }
        return false;
    }

}
