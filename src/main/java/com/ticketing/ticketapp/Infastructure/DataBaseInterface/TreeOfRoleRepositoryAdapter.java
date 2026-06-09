package com.ticketing.ticketapp.Infastructure.DataBaseInterface;

import com.ticketing.ticketapp.Domain.OwnerManagerTree.Manager;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Owner;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.OwnerManagerException;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Permission;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.RoleKey;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Infastructure.JpaManagerRepository;
import com.ticketing.ticketapp.Infastructure.JpaOwnerRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@Primary
public class TreeOfRoleRepositoryAdapter implements iTreeOfRoleRepository {

    private final JpaOwnerRepository ownerRepo;
    private final JpaManagerRepository managerRepo;

    public TreeOfRoleRepositoryAdapter(JpaOwnerRepository ownerRepo, JpaManagerRepository managerRepo) {
        this.ownerRepo = ownerRepo;
        this.managerRepo = managerRepo;
    }

    @Override
    public void storeOwner(String ownerID, String company, String appointerID) {
        managerRepo.deleteByUserIDAndCompanyName(ownerID, company);
        Owner o = new Owner(ownerID, company, appointerID);
        if (FOUNDER_APPOINTER.equals(appointerID)) {
            o.acceptAppointment();
        }
        ownerRepo.saveAndFlush(o);
    }

    @Override
    public boolean exitsOwner(String ownerID, String company) {
        return ownerRepo.findById(new RoleKey(ownerID, company))
                .map(Owner::isAccepted)
                .orElse(false);
    }

    @Override
    public void storeManager(String managerID, String company, Set<Permission> permissions, String appointerID) {
        ownerRepo.findById(new RoleKey(managerID, company)).ifPresent(existingOwner -> {
            if (FOUNDER_APPOINTER.equals(existingOwner.getAppointerID())) {
                throw new OwnerManagerException("Cannot assign a manager role to the company founder");
            }
            ownerRepo.deleteByUserIDAndCompanyName(managerID, company);
        });
        Manager m = new Manager(managerID, company, permissions, appointerID);
        managerRepo.saveAndFlush(m);
    }

    @Override
    public Manager getManager(String managerID, String company) {
        return managerRepo.findById(new RoleKey(managerID, company)).orElse(null);
    }

    @Override
    public void save(Owner owner) {
        ownerRepo.saveAndFlush(owner);
    }

    @Override
    public void save(Manager manager) {
        managerRepo.saveAndFlush(manager);
    }

    @Override
    public void deleteManager(String managerID, String company) {
        managerRepo.findById(new RoleKey(managerID, company)).ifPresent(managerRepo::delete);
    }

    @Override
    public void deleteOwner(String ownerID, String company) {
        ownerRepo.deleteByUserIDAndCompanyName(ownerID, company);
    }

    @Override
    public boolean isManager(String managerID, String company) {
        return managerRepo.existsById(new RoleKey(managerID, company));
    }

    @Override
    public boolean isOwner(String ownerID, String company) {
        return ownerRepo.existsById(new RoleKey(ownerID, company));
    }

    @Override
    public Owner getOwner(String ownerID, String company) {
        return ownerRepo.findById(new RoleKey(ownerID, company)).orElse(null);
    }

    @Override
    public boolean isAppointerManager(String managerID, String company, String appointerID) {
        return managerRepo.findById(new RoleKey(managerID, company))
                .map(m -> m.isAccepted() && appointerID.equals(m.getAppointerID()))
                .orElse(false);
    }

    @Override
    public boolean isAppointerOwner(String ownerID, String company, String appointer) {
        return ownerRepo.findById(new RoleKey(ownerID, company))
                .map(o -> o.isAccepted() && appointer.equals(o.getAppointerID()))
                .orElse(false);
    }

    @Override
    public Set<Permission> getManagerPermissions(String managerID, String company) {
        return managerRepo.findById(new RoleKey(managerID, company))
                .map(Manager::getPermissions)
                .orElse(null);
    }

    @Override
    public List<Owner> getAllOwnersByCompany(String company) {
        return ownerRepo.findByCompanyName(company);
    }

    @Override
    public List<Manager> getAllManagersByCompany(String company) {
        return managerRepo.findByCompanyName(company);
    }

    @Override
    public boolean ManagerPermitedToCreateUpdateDelete(String managerID, String company) {
        return managerRepo.findById(new RoleKey(managerID, company))
                .map(m -> m.isAccepted() && m.getPermissions().contains(Permission.MANAGE_INVENTORY))
                .orElse(false);
    }

    @Override
    public void deleteAllRoles() {
        managerRepo.deleteAll();
        ownerRepo.deleteAll();
    }

    @Override
    public boolean ManagerPermitToSeeTransactions(String managerID, String company) {
        return managerRepo.findById(new RoleKey(managerID, company))
                .map(m -> m.getPermissions().contains(Permission.GENERATE_SALES_REPORTS))
                .orElse(false);
    }

    @Override
    public void deleteCompanyMangersAndOwners(String company) {
        managerRepo.deleteAll(managerRepo.findByCompanyName(company));
        ownerRepo.deleteByCompanyName(company);
    }

    @Override
    public void deleteUserRoles(String userID) {
        managerRepo.deleteAll(managerRepo.findByUserID(userID));
        ownerRepo.deleteByUserID(userID);
    }

    @Override
    public List<String> getUserCompanies(String userId) {
        Set<String> companies = new HashSet<>();
        ownerRepo.findByUserID(userId).stream()
                .filter(Owner::isAccepted)
                .forEach(o -> companies.add(o.getCompanyName()));
        managerRepo.findByUserID(userId).stream()
                .filter(Manager::isAccepted)
                .forEach(m -> companies.add(m.getCompanyName()));
        return new ArrayList<>(companies);
    }

    @Override
    public String getRoleInCompany(String userId, String companyName) {
        Owner o = ownerRepo.findById(new RoleKey(userId, companyName)).orElse(null);
        if (o != null && o.isAccepted()) {
            return FOUNDER_APPOINTER.equals(o.getAppointerID()) ? "FOUNDER" : "OWNER";
        }
        Manager m = managerRepo.findById(new RoleKey(userId, companyName)).orElse(null);
        if (m != null && m.isAccepted()) return "MANAGER";
        return "MEMBER";
    }

    @Override
    public String getUserHighestRole(String userId) {
        boolean isOwner = ownerRepo.findByUserID(userId).stream().anyMatch(Owner::isAccepted);
        if (isOwner) return "OWNER";
        boolean isManager = managerRepo.findByUserID(userId).stream().anyMatch(Manager::isAccepted);
        if (isManager) return "MANAGER";
        return "MEMBER";
    }
}
