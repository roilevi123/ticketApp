package Domain.OwnerManagerTree;

import java.util.List;
import java.util.Set;

public interface iTreeOfRoleRepository {
    public void storeOwner(String owner,String company,String appointer);
    public boolean exitsOwner(String owner,String company);
    public void storeManager(String manager, String company, Set<Permission> permissions,String appointer);
    public Manager getManager(String manager,String company);
    public void save(Owner owner);
    public void save(Manager manager);
    public void deleteManager(String manager,String company);
    public void deleteOwner(String owner,String company);
    public boolean isManager(String manager,String company);
    public boolean isOwner(String owner,String company);
    public Owner getOwner( String owner ,String company);
    public boolean isAppointerManager(String manager,String company,String appointer);
    public boolean isAppointerOwner(String owner,String company,String appointer);
    public Set<Permission> getManagerPermissions(String manager,String company);

}
