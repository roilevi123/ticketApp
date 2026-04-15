package Domain.User;

public interface IUserRepository {
    public void Store(String username, String password);
    public String getUserPassword(String username);


}
