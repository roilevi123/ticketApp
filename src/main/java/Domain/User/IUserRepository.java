package Domain.User;

public interface IUserRepository {
    public User Store(String username, String password);
    public String getUserPassword(String username);
    public User getUserByUsername(String username);
    public User getUserByID(String ID);
    public boolean usernameExists(String username);
    public void save(User userToUpdate);
    public void deleteAll();
    public void deleteUser(String ID);


}
