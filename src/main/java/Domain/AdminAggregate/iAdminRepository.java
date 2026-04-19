package Domain.AdminAggregate;

public interface iAdminRepository {
    public boolean isAdmin(String adminName);
    public void deleteAll();
}
