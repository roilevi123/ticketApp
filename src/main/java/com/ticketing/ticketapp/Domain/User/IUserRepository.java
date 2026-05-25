package com.ticketing.ticketapp.Domain.User;

import java.util.Map;

public interface IUserRepository {
    public User Store(String username, String password, int age, String email);
    public String getUserPassword(String username);
    public User getUserByUsername(String username);
    public User getUserByID(String ID);
    public String getUsernameByID(String ID);
    public boolean usernameExists(String username);
    public void save(User userToUpdate);
    public void deleteAll();
    public void deleteUser(String ID);
    public Map<Integer, Suspension> currentSuspensions();
}
