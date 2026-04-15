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



}
