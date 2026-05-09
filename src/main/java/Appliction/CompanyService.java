package Appliction;

import Domain.Company.Company;
import Domain.Company.iCompanyRepository;
import Domain.OwnerManagerTree.*;

import Domain.User.IUserRepository;
import Domain.User.User;
import Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class CompanyService {
    private iCompanyRepository companyRepository;
    private IUserRepository userRepository;
    private iTreeOfRoleRepository treeOfRoleRepository;
    private INotifer notifier;
    private TokenService tokenService;
    private static final Logger logger = LoggerFactory.getLogger(CompanyService.class);
    public CompanyService(iCompanyRepository companyRepository,IUserRepository userRepository,iTreeOfRoleRepository iTreeOfRoleRepository, TokenService tokenService, INotifer notifier) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.treeOfRoleRepository = iTreeOfRoleRepository;
        this.notifier = notifier;
    }
    public String CreateCompany(String company, String token) {
        try {
            if(!tokenService.validateToken(token)){
                throw new RuntimeException("Invalid token");
            }
            String userID = tokenService.extractUserId(token);
            User userObj = userRepository.getUserByID(userID);
            if (userObj == null) {
                throw new RuntimeException("User not found");
            }
            String username = userObj.getName();

            logger.info("trying create company", username, company);

            companyRepository.store(company, username);
            treeOfRoleRepository.storeOwner(username, company, iTreeOfRoleRepository.FOUNDER_APPOINTER);
            logger.info("successfully create company", username, company);
            return "success";
        }catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }
    public String AppointAManager(String managerID, String company, Set<Permission> permissions,String token) {
        try {
            logger.info("trying appointAManager", managerID, company);
            if(!tokenService.validateToken(token)){
                throw new RuntimeException("Invalid token");
            }
            String username = tokenService.extractUsername(token);
            boolean o=treeOfRoleRepository.exitsOwner(username,company);
            if(!o) {
                throw new RuntimeException("User not found1");
            }
            boolean UserExists = userRepository.usernameExists(managerID);
            if(!UserExists) {
                throw new RuntimeException("User not found2");
            }
            treeOfRoleRepository.storeManager(managerID,company,permissions,username);
            logger.info("successfully appointAManager", managerID, company);
            return "success";
        }catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }
    public String ApproveAppointmentForManager(String token, String company) {
        try {
            String username = tokenService.extractUsername(token);
            logger.info("trying approveAppointmentForManager", username, company);
            if(!tokenService.validateToken(token)){
                throw new RuntimeException("Invalid token");
            }
            Manager m=treeOfRoleRepository.getManager(username,company);
            m.acceptAppointment();
            treeOfRoleRepository.save(m);
            logger.info("successfully approveAppointmentForManager", username, company);
            return "success";
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }
    public String RejectAppointmentForManager(String token, String company) {
        try {
            String username = tokenService.extractUsername(token);
            logger.info("trying rejectAppointmentForManager", username, company);
            if(!tokenService.validateToken(token)){
                throw new RuntimeException("Invalid token");
            }
            boolean m=treeOfRoleRepository.isManager(username,company);
            if(!m) {
                throw new RuntimeException("User not found2");
            }
            treeOfRoleRepository.deleteManager(username,company);
            logger.info("successfully rejectAppointmentForManager", username, company);
            return "success";
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }
    public String AppointOwner(String ownerID, String company,String token) {
        try {
            logger.info("trying appointOwner", ownerID, company);
            if(!tokenService.validateToken(token)){
                throw new RuntimeException("Invalid token");
            }
            String username = tokenService.extractUsername(token);
            boolean o=treeOfRoleRepository.exitsOwner(username,company);
            if(!o) {
                throw new RuntimeException("User not found1");
            }
            boolean UserExists = userRepository.usernameExists(ownerID);
            if(!UserExists) {
                throw new RuntimeException("User not found3");
            }
            treeOfRoleRepository.storeOwner(ownerID,company,username);
            logger.info("successfully appointOwner", ownerID, company);
            return "success";
        }catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }
    public String  ApproveAppointmentForOwner(String token, String company) {
        try {
            String username = tokenService.extractUsername(token);
            logger.info("trying approveAppointmentForOwner", username, company);
            if(!tokenService.validateToken(token)){
                throw new RuntimeException("Invalid token");
            }
            Owner m=treeOfRoleRepository.getOwner(username,company);
            m.acceptAppointment();
            treeOfRoleRepository.save(m);
            logger.info("successfully approveAppointmentForOwner", username, company);
            return "success";
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }
    public String RejectAppointmentForOwner(String token, String company) {
        try {
            String username = tokenService.extractUsername(token);
            logger.info("trying rejectAppointmentForOwner", username, company);
            if(!tokenService.validateToken(token)){
                throw new RuntimeException("Invalid token");
            }
            boolean m=treeOfRoleRepository.isOwner(username,company);
            if(!m) {
                throw new RuntimeException("User not found2");
            }
            String c=companyRepository.getCompanyFounder(company);
            if (username.equals(c)) {
                throw new RuntimeException("founder can not give up the appointment");
            }
            treeOfRoleRepository.deleteOwner(username,company);
            logger.info("successfully rejectAppointmentForOwner", username, company);
            return "success";
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }
    public String FireOwner(String token, String company,String ownerID) {
        try {
            String username = tokenService.extractUsername(token);
            logger.info("trying fire owner", ownerID, company);
            if(!tokenService.validateToken(token)){
                throw new RuntimeException("Invalid token");
            }
            boolean m=treeOfRoleRepository.isAppointerOwner(ownerID,company,username);

            if(!m) {
                throw new RuntimeException("you are not allowed to fire owner ");
            }

            treeOfRoleRepository.deleteOwner(ownerID,company);
            boolean isSend=notifier.notifyUser(ownerID,"you are not owner anymore for company: "+company);
            if(!isSend) {
                notifier.saveMessage(ownerID,"you are not owner anymore for company: "+company);
            }
            logger.info("successfully fire owner", ownerID, company);
            return "success";
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }
    public String FireManager(String token, String company,String managerID) {
        try {
            String username = tokenService.extractUsername(token);
            logger.info("trying fire manager", managerID, company);
            if(!tokenService.validateToken(token)){
                throw new RuntimeException("Invalid token");
            }
            boolean m=treeOfRoleRepository.isAppointerManager(managerID,company,username);
            if(!m) {
                throw new RuntimeException("you are not allowed to fire owner ");
            }
            treeOfRoleRepository.deleteManager(managerID,company);
            logger.info("successfully fire manager", managerID, company);
            boolean isSend=notifier.notifyUser(managerID,"you are not manager anymore for company: "+company);
            if(!isSend) {
                notifier.saveMessage(managerID,"you are not manager anymore for company: "+company);
            }
            return "success";
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }
    public String ChangeManagerPermissions(String token, String company, String managerID, Set<Permission> newPermissions) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            String username = tokenService.extractUsername(token);

            logger.info("User {} is trying to change permissions for manager {} in company {}", username, managerID, company);

            Manager manager = treeOfRoleRepository.getManager(managerID, company);
            if (manager == null) {
                throw new RuntimeException("Manager not found");
            }
            boolean m=treeOfRoleRepository.isAppointerManager(managerID, company, username);
            if (!m) {
                throw new RuntimeException("You are not authorized to change permissions for this manager (you did not appoint them)");
            }

            manager.setPermissions(newPermissions);

            treeOfRoleRepository.save(manager);

            logger.info("Successfully changed permissions for manager {} by {}", managerID, username);
            boolean isSend=notifier.notifyUser(managerID,"your manager permission change for company"+company);
            if(!isSend){
                notifier.saveMessage(managerID,"your manager permission change for company"+company);
            }

            return "success";
        } catch (Exception e) {
            logger.error("Failed to change permissions: " + e.getMessage());
            return e.getMessage();
        }
    }
    public String freezeCompany(String company, String token) {
        try {
            if(!tokenService.validateToken(token)){
                throw new RuntimeException("Invalid token");
            }
            String username = tokenService.extractUsername(token);
            logger.info("trying freeze company", username, company);
            Company companyObj = companyRepository.getCompany(company);
            companyObj.freezeCompany(username);
            companyRepository.save(companyObj);
            logger.info("successfully freeze company", username, company);
            String message = "successfully freeze company : "+company;
            List<Owner> owners = treeOfRoleRepository.getAllOwnersByCompany(company);
            for (Owner owner : owners) {
                boolean isSend=notifier.notifyUser(owner.getUserID(), message);
                if(!isSend){
                    notifier.saveMessage(owner.getUserID(), message);
                }
            }
            List<Manager> managers = treeOfRoleRepository.getAllManagersByCompany(company);
            for (Manager manager : managers) {
                boolean isSend=notifier.notifyUser(manager.getUserID(), message);
                if(!isSend){
                    notifier.saveMessage(manager.getUserID(), message);
                }
            }
            return "success";
        }catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }
    public String unfreezeCompany(String company, String token) {
        try {
            if(!tokenService.validateToken(token)){
                throw new RuntimeException("Invalid token");
            }
            String username = tokenService.extractUsername(token);
            logger.info("trying unfreeze company", username, company);
            Company companyObj = companyRepository.getCompany(company);
            companyObj.unfreezeCompany(username);
            companyRepository.save(companyObj);
            logger.info("successfully unfreeze company", username, company);
            return "success";
        }catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }
    public Set<Permission> GetManagerPermissions(String token, String company, String managerID) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            String username = tokenService.extractUsername(token);
            logger.info("User {} is trying to view permissions of manager {} in company {}", username, managerID, company);
            boolean owner = treeOfRoleRepository.exitsOwner(username, company);
            if (!owner) {
                throw new RuntimeException("Access denied: Only an owner can view manager permissions");
            }
            Set<Permission> manager = treeOfRoleRepository.getManagerPermissions(managerID, company);
            logger.info("Successfully retrieved permissions for manager {}", managerID);
            return manager;

        } catch (Exception e) {
            logger.error("Error retrieving manager permissions: " + e.getMessage());
            return null;
        }
    }
    public String GetRoleTreeString(String token, String companyName) {
        try {
            if (!tokenService.validateToken(token)) throw new RuntimeException("Invalid token");
            String username = tokenService.extractUsername(token);

            if (treeOfRoleRepository.getOwner(username, companyName) == null) {
                throw new RuntimeException("Only owners can view the role tree");
            }

            List<Owner> allOwners = treeOfRoleRepository.getAllOwnersByCompany(companyName);
            List<Manager> allManagers = treeOfRoleRepository.getAllManagersByCompany(companyName);
            Company company = companyRepository.getCompany(companyName);

            StringBuilder treeString = new StringBuilder();
            buildTreeString(company.getFounderID(), allOwners, allManagers, treeString, 0);

            return treeString.toString();
        }catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }

    }

    private void buildTreeString(String currentUsername, List<Owner> allOwners, List<Manager> allManagers, StringBuilder sb, int depth) {
        String indent = "  ".repeat(depth);
        String role = allOwners.stream().anyMatch(o -> o.getUserID().equals(currentUsername)) ? "Owner" : "Manager";

        sb.append(indent).append("|-- ").append(currentUsername).append(" (").append(role).append(")\n");

        allOwners.stream()
                .filter(o -> currentUsername.equals(o.getAppointerID()))
                .forEach(o -> buildTreeString(o.getUserID(), allOwners, allManagers, sb, depth + 1));

        allManagers.stream()
                .filter(m -> currentUsername.equals(m.getAppointerID()))
                .forEach(m -> {
                    String mIndent = "  ".repeat(depth + 1);
                    sb.append(mIndent).append("|-- ").append(m.getUserID()).append(" (Manager)\n");
                });
    }



}
