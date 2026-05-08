package Domain.AdminAggregate;

public interface iAdminRepository {
    public boolean isAdmin(String adminID);
    public void addAdmin(String adminID);
    public void deleteAll();
}
