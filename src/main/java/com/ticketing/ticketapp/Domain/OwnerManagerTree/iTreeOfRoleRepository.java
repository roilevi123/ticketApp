package com.ticketing.ticketapp.Domain.OwnerManagerTree;

import java.util.List;
import java.util.Set;

public interface iTreeOfRoleRepository {
    String FOUNDER_APPOINTER = "SYSTEM_FOUNDER";
    public void storeOwner(String ownerID,String company,String appointerID);
    public boolean exitsOwner(String ownerID,String company);
    public void storeManager(String managerID, String company, Set<Permission> permissions,String appointerID);
    public Manager getManager(String managerID,String company);
    public void save(Owner owner);
    public void save(Manager manager);
    public void deleteManager(String managerID,String company);
    public void deleteOwner(String ownerID,String company);
    public boolean isManager(String managerID,String company);
    public boolean isOwner(String ownerID,String company);
    public Owner getOwner( String ownerID ,String company);
    public boolean isAppointerManager(String managerID,String company,String appointerID);
    public boolean isAppointerOwner(String ownerID,String company,String appointerID);
    public Set<Permission> getManagerPermissions(String managerID,String company);
    List<Owner> getAllOwnersByCompany(String company);
    List<Manager> getAllManagersByCompany(String company);
    public boolean ManagerPermitedToCreateUpdateDelete(String managerID,String company);
    public void deleteAllRoles();
    public boolean ManagerPermitToSeeTransactions(String managerID, String company);
    public void deleteCompanyMangersAndOwners(String company);
    public void deleteUserRoles(String UserID);
    String getUserHighestRole(String userID);
    List<String> getUserCompanies(String userID);
    String getRoleInCompany(String userID, String companyName);
}
