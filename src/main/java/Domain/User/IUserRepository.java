package Domain.User;

public interface IUserRepository {
    public void Store(String username, String password);
    public String getUserPassword(String username);
    public boolean userExists(String username);
    public void deleteAll();
    public void deleteUser(String username);


}
