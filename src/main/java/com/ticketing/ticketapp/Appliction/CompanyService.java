package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.Company.Company;
import com.ticketing.ticketapp.Domain.Company.CompanyDTO;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.*;

import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.User.User;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class CompanyService {
    private iCompanyRepository companyRepository;
    private IUserRepository userRepository;
    private iTreeOfRoleRepository treeOfRoleRepository;
    private IPendingNotificationRepository notificationRepository;
    private TokenService tokenService;
    private INotifier notifier;
    private static final Logger logger = LoggerFactory.getLogger(CompanyService.class);

    public CompanyService(iCompanyRepository companyRepository, IUserRepository userRepository,
            iTreeOfRoleRepository iTreeOfRoleRepository, TokenService tokenService, INotifier notifier,
            IPendingNotificationRepository notificationRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.treeOfRoleRepository = iTreeOfRoleRepository;
        this.notifier = notifier;
        this.notificationRepository = notificationRepository;
    }

    public Response<String> CreateCompany(String company, String token) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            String userID = tokenService.extractUserId(token);
            User userObj = userRepository.getUserByID(userID);
            if (userObj == null) {
                throw new RuntimeException("User not found");
            }
            String username = userObj.getName();
            if(userRepository.isUserSuspendedNow(userID))
                throw new RuntimeException("User is suspended");
            logger.info("trying create company", username, company);

            companyRepository.store(company, username);
            treeOfRoleRepository.storeOwner(username, company, iTreeOfRoleRepository.FOUNDER_APPOINTER);
            logger.info("successfully create company", username, company);
            return Response.success("success");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> AppointAManager(String managerID, String company, Set<Permission> permissions,
            String token) {
        try {
            logger.info("trying appointAManager", managerID, company);
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            String username = tokenService.extractUsername(token);
            String userID=tokenService.extractUserId(token);
            User userObj=userRepository.getUserByID(userID);
            if(userRepository.isUserSuspendedNow(managerID))
                throw new RuntimeException("User is suspended");
            boolean o = treeOfRoleRepository.exitsOwner(username, company);
            if (!o) {
                throw new RuntimeException("User not found1");
            }
            boolean UserExists = userRepository.usernameExists(managerID);
            if (!UserExists) {
                throw new RuntimeException("User not found2");
            }

            treeOfRoleRepository.storeManager(managerID, company, permissions, username);
            logger.info("successfully appointAManager", managerID, company);
            notifyMember(managerID, "Manager Appointment",
                    "You have been appointed as a manager of '" + company
                            + "'. Please approve or reject the appointment.");
            return Response.success("success");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> ApproveAppointmentForManager(String token, String company) {
        try {
            String username = tokenService.extractUsername(token);
            logger.info("trying approveAppointmentForManager", username, company);
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            String userID=tokenService.extractUserId(token);
            if(userRepository.isUserSuspendedNow(userID))
                throw new RuntimeException("User is suspended");
            Manager m = treeOfRoleRepository.getManager(username, company);
            m.acceptAppointment();
            treeOfRoleRepository.save(m);
            logger.info("successfully approveAppointmentForManager", username, company);
            return Response.success("success");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> RejectAppointmentForManager(String token, String company) {
        try {
            String username = tokenService.extractUsername(token);
            logger.info("trying rejectAppointmentForManager", username, company);
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }

            String userId= tokenService.extractUserId(token);
            if(userRepository.isUserSuspendedNow(userId))
                throw new RuntimeException("User is suspended");

            boolean m = treeOfRoleRepository.isManager(username, company);
            if (!m) {
                throw new RuntimeException("User not found2");
            }
            treeOfRoleRepository.deleteManager(username, company);
            logger.info("successfully rejectAppointmentForManager", username, company);
            return Response.success("success");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> AppointOwner(String ownerID, String company, String token) {
        try {
            logger.info("trying appointOwner", ownerID, company);
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            String username = tokenService.extractUsername(token);
            if(userRepository.isUserSuspendedNow(ownerID))
                throw new RuntimeException("User is suspended");

            boolean o = treeOfRoleRepository.exitsOwner(username, company);
            if (!o) {
                throw new RuntimeException("User not found1");
            }
            boolean UserExists = userRepository.usernameExists(ownerID);
            if (!UserExists) {
                throw new RuntimeException("User not found3");
            }
            treeOfRoleRepository.storeOwner(ownerID, company, username);
            logger.info("successfully appointOwner", ownerID, company);
            notifyMember(ownerID, "Owner Appointment",
                    "You have been appointed as an owner of '" + company
                            + "'. Please approve or reject the appointment.");
            return Response.success("success");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> ApproveAppointmentForOwner(String token, String company) {
        try {
            String username = tokenService.extractUsername(token);
            logger.info("trying approveAppointmentForOwner", username, company);
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }

            Owner m = treeOfRoleRepository.getOwner(username, company);
            m.acceptAppointment();
            treeOfRoleRepository.save(m);
            logger.info("successfully approveAppointmentForOwner", username, company);
            return Response.success("success");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> RejectAppointmentForOwner(String token, String company) {
        try {
            String username = tokenService.extractUsername(token);
            logger.info("trying rejectAppointmentForOwner", username, company);
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }

            boolean m = treeOfRoleRepository.isOwner(username, company);
            if (!m) {
                throw new RuntimeException("User not found2");
            }
            String c = companyRepository.getCompanyFounder(company);
            if (username.equals(c)) {
                throw new RuntimeException("founder can not give up the appointment");
            }
            treeOfRoleRepository.deleteOwner(username, company);
            logger.info("successfully rejectAppointmentForOwner", username, company);
            return Response.success("success");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> FireOwner(String token, String company, String ownerID) {
        try {
            String username = tokenService.extractUsername(token);
            logger.info("trying fire owner", ownerID, company);
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }

            boolean m = treeOfRoleRepository.isAppointerOwner(ownerID, company, username);
            if (!m) {
                throw new RuntimeException("you are not allowed to fire owner ");
            }
            treeOfRoleRepository.deleteOwner(ownerID, company);
            logger.info("successfully fire owner", ownerID, company);
            return Response.success("success");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> FireManager(String token, String company, String managerID) {
        try {
            String username = tokenService.extractUsername(token);
            logger.info("trying fire manager", managerID, company);
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }

            boolean m = treeOfRoleRepository.isAppointerManager(managerID, company, username);
            if (!m) {
                throw new RuntimeException("you are not allowed to fire owner ");
            }
            treeOfRoleRepository.deleteManager(managerID, company);
            logger.info("successfully fire manager", managerID, company);
            return Response.success("success");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> ChangeManagerPermissions(String token, String company, String managerID,
            Set<Permission> newPermissions) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            String username = tokenService.extractUsername(token);
            logger.info("User {} is trying to change permissions for manager {} in company {}", username, managerID,company);
            Manager manager = treeOfRoleRepository.getManager(managerID, company);
            if (manager == null) {
                throw new RuntimeException("Manager not found");
            }
            boolean m = treeOfRoleRepository.isAppointerManager(managerID, company, username);
            if (!m) {
                throw new RuntimeException(
                        "You are not authorized to change permissions for this manager (you did not appoint them)");
            }
            manager.setPermissions(newPermissions);
            treeOfRoleRepository.save(manager);
            logger.info("Successfully changed permissions for manager {} by {}", managerID, username);
            return Response.success("success");
        } catch (Exception e) {
            logger.error("Failed to change permissions: " + e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> freezeCompany(String company, String token) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            String username = tokenService.extractUsername(token);
            logger.info("trying freeze company", username, company);

            Company companyObj = companyRepository.getCompany(company);
            companyObj.freezeCompany(username);
            companyRepository.save(companyObj);
            logger.info("successfully freeze company", username, company);
            String title = "Company Suspended";
            String message = "Company '" + company + "' has been suspended by its founder.";
            treeOfRoleRepository.getAllOwnersByCompany(company)
                    .forEach(o -> notifyMember(o.getUserID(), title, message));
            treeOfRoleRepository.getAllManagersByCompany(company)
                    .forEach(m -> notifyMember(m.getUserID(), title, message));
            return Response.success("success");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> unfreezeCompany(String company, String token) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            String username = tokenService.extractUsername(token);
            logger.info("trying unfreeze company", username, company);

            Company companyObj = companyRepository.getCompany(company);
            companyObj.unfreezeCompany(username);
            companyRepository.save(companyObj);
            logger.info("successfully unfreeze company", username, company);
            return Response.success("success");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> replyToBuyer(String token, String companyName, String buyerId, String message) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }

            String username = tokenService.extractUsername(token);
            logger.info("User {} is attempting to reply to buyer {} for company {}", username, buyerId, companyName);
            boolean isOwner = treeOfRoleRepository.exitsOwner(username, companyName);
            boolean isManager = treeOfRoleRepository.isManager(username, companyName);

            if (!isOwner && !isManager) {
                throw new RuntimeException("Unauthorized: Only owners or managers can reply to buyers");
            }

            String formattedMessage = String.format("Message from %s: %s", companyName, message);
            notificationRepository.save(buyerId, formattedMessage);
            logger.info("Successfully replied to buyer {}", buyerId);
            return Response.success("success");

        } catch (Exception e) {
            logger.error("Failed to reply to buyer: {}", e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<List<CompanyDTO>> getActiveCompanies(String token) {
        try {
            boolean isGuest = token == null || token.trim().isEmpty() || token.contains("guest-temporary-token");
            if (!isGuest && !tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            List<CompanyDTO> companies = companyRepository.getActiveCompanies()
                    .stream()
                    .map(CompanyDTO::fromEntity)
                    .toList();
            logger.info("Retrieved {} active companies", companies.size());
            return Response.success(companies);
        } catch (Exception e) {
            logger.error("Failed to retrieve active companies: {}", e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<Set<Permission>> GetManagerPermissions(String token, String company, String managerID) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            String username = tokenService.extractUsername(token);
            logger.info("User {} is trying to view permissions of manager {} in company {}", username, managerID,
                    company);
            boolean owner = treeOfRoleRepository.exitsOwner(username, company);
            if (!owner) {
                throw new RuntimeException("Access denied: Only an owner can view manager permissions");
            }
            Set<Permission> permissions = treeOfRoleRepository.getManagerPermissions(managerID, company);
            logger.info("Successfully retrieved permissions for manager {}", managerID);
            return Response.success(permissions);
        } catch (Exception e) {
            logger.error("Error retrieving manager permissions: " + e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> GetRoleTreeString(String token, String companyName) {
        try {
            if (!tokenService.validateToken(token))
                throw new RuntimeException("Invalid token");
            String username = tokenService.extractUsername(token);

            if (treeOfRoleRepository.getOwner(username, companyName) == null) {
                throw new RuntimeException("Only owners can view the role tree");
            }

            List<Owner> allOwners = treeOfRoleRepository.getAllOwnersByCompany(companyName);
            List<Manager> allManagers = treeOfRoleRepository.getAllManagersByCompany(companyName);
            Company company = companyRepository.getCompany(companyName);

            StringBuilder treeString = new StringBuilder();
            buildTreeString(company.getFounderID(), allOwners, allManagers, treeString, 0);

            return Response.success(treeString.toString());
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    private void buildTreeString(String currentUsername, List<Owner> allOwners, List<Manager> allManagers,
            StringBuilder sb, int depth) {
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

    public Response<String> sendMessageToUser(String token, String companyName, String targetUserId, String message) {
        try {
            if (!tokenService.validateToken(token))
                throw new RuntimeException("Invalid token");
            String username = tokenService.extractUsername(token);
            boolean isOwner = treeOfRoleRepository.exitsOwner(username, companyName);
            boolean isManager = treeOfRoleRepository.isManager(username, companyName);
            if (!isOwner && !isManager)
                throw new RuntimeException("Not authorized to send messages for this company");
            notifier.notifyUser(targetUserId, "Message from " + companyName, message);
            return Response.success("success");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    private void notifyMember(String username, String title, String message) {
        try {
            User u = userRepository.getUserByUsername(username);
            if (u != null)
                notifier.notifyUser(u.getID(), title, message);
        } catch (Exception e) {
            logger.warn("Failed to notify user {}: {}", username, e.getMessage());
        }
    }

}
