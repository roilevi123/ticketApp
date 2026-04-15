package Appliction;

import Domain.Company.iCompanyRepository;
import Domain.OwnerManagerTree.*;

import Domain.User.IUserRepository;
import Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class CompanyService {
    private iCompanyRepository companyRepository;
    private IUserRepository userRepository;
    private iTreeOfRoleRepository treeOfRoleRepository;
    private TokenService tokenService;
    private static final Logger logger = LoggerFactory.getLogger(CompanyService.class);
    public CompanyService(iCompanyRepository companyRepository,IUserRepository userRepository,iTreeOfRoleRepository iTreeOfRoleRepository, TokenService tokenService) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.treeOfRoleRepository = iTreeOfRoleRepository;
    }
    public String CreateCompany(String company, String token) {
        try {

            if(!tokenService.validateToken(token)){
                throw new RuntimeException("Invalid token");
            }
            String username=tokenService.extractUsername(token);

            logger.info("trying create company " ,username,company );

            boolean user = userRepository.userExists(username);
            if(!user) {
                throw new RuntimeException("User not found");
            }

            companyRepository.store(company,username);
            treeOfRoleRepository.storeOwner(username,company,"Administrator");
            logger.info("successfully create company " ,username,company );
            return "success";
        }catch (Exception e) {
            logger.error(e.getMessage());
            return "failed";
        }
    }
    public String AppointAManager(String manager, String company, Set<Permission> permissions,String token) {
        try {
            logger.info("trying appointAManager " ,manager,company);
            if(!tokenService.validateToken(token)){
                throw new RuntimeException("Invalid token");
            }
            String username=tokenService.extractUsername(token);
            boolean o=treeOfRoleRepository.exitsOwner(username,company);
            if(!o) {
                throw new RuntimeException("User not found1");
            }
            boolean UserExists = userRepository.userExists(manager);
            if(!UserExists) {
                throw new RuntimeException("User not found2");
            }
            treeOfRoleRepository.storeManager(manager,company,permissions,username);
            logger.info("successfully appointAManager " ,manager,company);
            return "success";
        }catch (Exception e) {
            logger.error(e.getMessage());
            return "failed";
        }
    }
    public String ApproveAppointmentForManager(String token, String company) {
        try {
            String username=tokenService.extractUsername(token);
            logger.info("trying approveAppointmentForManager " ,username,company);
            if(!tokenService.validateToken(token)){
                throw new RuntimeException("Invalid token");
            }
            Manager m=treeOfRoleRepository.getManager(username,company);
            m.acceptAppointment();
            treeOfRoleRepository.save(m);
            logger.info("successfully approveAppointmentForManager " ,username,company);
            return "success";
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return "failed";
        }
    }
    public String RejectAppointmentForManager(String token, String company) {
        try {
            String username=tokenService.extractUsername(token);
            logger.info("trying rejectAppointmentForManager " ,username,company);
            if(!tokenService.validateToken(token)){
                throw new RuntimeException("Invalid token");
            }
            boolean m=treeOfRoleRepository.isManager(username,company);
            if(!m) {
                throw new RuntimeException("User not found2");
            }
            treeOfRoleRepository.deleteManager(username,company);
            logger.info("successfully rejectAppointmentForManager " ,username,company);
            return "success";
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return "failed";
        }
    }
    public String AppointOwner(String owner, String company,String token) {
        try {
            logger.info("trying appointAManager " ,owner,company);
            if(!tokenService.validateToken(token)){
                throw new RuntimeException("Invalid token");
            }
            String username=tokenService.extractUsername(token);
            boolean o=treeOfRoleRepository.exitsOwner(username,company);
            if(!o) {
                throw new RuntimeException("User not found1");
            }
            boolean UserExists = userRepository.userExists(owner);
            if(!UserExists) {
                throw new RuntimeException("User not found3");
            }
            treeOfRoleRepository.storeOwner(owner,company,username);
            logger.info("successfully appointAManager " ,owner,company);
            return "success";
        }catch (Exception e) {
            logger.error(e.getMessage());
            return "failed";
        }
    }
    public String  ApproveAppointmentForOwner(String token, String company) {
        try {
            String username=tokenService.extractUsername(token);
            logger.info("trying approveAppointmentForManager " ,username,company);
            if(!tokenService.validateToken(token)){
                throw new RuntimeException("Invalid token");
            }
            Owner m=treeOfRoleRepository.getOwner(username,company);
            m.acceptAppointment();
            treeOfRoleRepository.save(m);
            logger.info("successfully approveAppointmentForManager " ,username,company);
            return "success";
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return "failed";
        }
    }
    public String RejectAppointmentForOwner(String token, String company) {
        try {
            String username=tokenService.extractUsername(token);
            logger.info("trying rejectAppointmentForowner " ,username,company);
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
            logger.info("successfully rejectAppointmentForowner " ,username,company);
            return "success";
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return "failed";
        }
    }
    public String FireOwner(String token, String company,String owner) {
        try {
            String username=tokenService.extractUsername(token);
            logger.info("trying fire owner " ,owner,company);
            if(!tokenService.validateToken(token)){
                throw new RuntimeException("Invalid token");
            }
            boolean m=treeOfRoleRepository.isAppointerOwner(owner,company,username);

            if(!m) {
                throw new RuntimeException("you are not allowed to fire owner ");
            }

            treeOfRoleRepository.deleteOwner(owner,company);
            logger.info("successfully fire owner " ,owner,company);
            return "success";
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return "failed";
        }
    }
    public String FireManager(String token, String company,String manager) {
        try {
            String username=tokenService.extractUsername(token);
            logger.info("trying fire owner " ,manager,company);
            if(!tokenService.validateToken(token)){
                throw new RuntimeException("Invalid token");
            }
            boolean m=treeOfRoleRepository.isAppointerManager(manager,company,username);
            if(!m) {
                throw new RuntimeException("you are not allowed to fire owner ");
            }
            treeOfRoleRepository.deleteManager(manager,company);
            logger.info("successfully fire owner " ,manager,company);
            return "success";
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return "failed";
        }
    }
    public String ChangeManagerPermissions(String token, String company, String managerName, Set<Permission> newPermissions) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            String username = tokenService.extractUsername(token);

            logger.info("User {} is trying to change permissions for manager {} in company {}", username, managerName, company);

            Manager manager = treeOfRoleRepository.getManager(managerName, company);
            if (manager == null) {
                throw new RuntimeException("Manager not found");
            }
            boolean m=treeOfRoleRepository.isAppointerManager(managerName, company, username);
            if (!m) {
                throw new RuntimeException("You are not authorized to change permissions for this manager (you did not appoint them)");
            }

            manager.setPermissions(newPermissions);

            treeOfRoleRepository.save(manager);

            logger.info("Successfully changed permissions for manager {} by {}", managerName, username);
            return "success";
        } catch (Exception e) {
            logger.error("Failed to change permissions: " + e.getMessage());
            return "failed";
        }
    }



}
