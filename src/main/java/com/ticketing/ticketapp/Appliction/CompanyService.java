package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.Company.Company;
import com.ticketing.ticketapp.Domain.Company.CompanyDTO;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Manager;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Owner;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.OwnerManagerException;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Permission;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.User.User;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class CompanyService {
    private iCompanyRepository companyRepository;
    private IUserRepository userRepository;
    private iTreeOfRoleRepository treeOfRoleRepository;
    private TokenService tokenService;
    private INotifier notifier;
    @Autowired private iEventRepository eventRepository;
    private static final Logger logger = LoggerFactory.getLogger(CompanyService.class);

    public CompanyService(iCompanyRepository companyRepository, IUserRepository userRepository,
            iTreeOfRoleRepository iTreeOfRoleRepository, TokenService tokenService, INotifier notifier) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.treeOfRoleRepository = iTreeOfRoleRepository;
        this.notifier = notifier;
    }

    @Transactional
    public Response<String> CreateCompany(String company, String token) {
        try {
            logger.info("User of token {}, is attempting to create a company: ", token, company);
            if (!tokenService.validateToken(token)) throw new OwnerManagerException("Invalid token");
            String userID = tokenService.extractUserId(token);
            if (userRepository.isUserSuspendedNow(userID))
                throw new OwnerManagerException("User is suspended");
            companyRepository.store(company, userID);
            treeOfRoleRepository.storeOwner(userID, company, iTreeOfRoleRepository.FOUNDER_APPOINTER);
            logger.info("User {} successfully created company {}",userID, company);
            User userObj = userRepository.getUserByID(userID);
            String founderToken = tokenService.generateCompanyToken(userID, userObj.getName(), "FOUNDER", company);
            return Response.success(founderToken);
        } catch (OwnerManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional
    public Response<String> AppointAManager(String managerUsername, String company, Set<Permission> permissions, String token) {
        try {
            logger.info("User of token {} is attempting to assign {} as manager of company: ", token, managerUsername, company);
            if (!tokenService.validateToken(token)) throw new OwnerManagerException("Invalid token");
            String appointerID = tokenService.extractUserId(token);
            if (userRepository.isUserSuspendedNow(appointerID))
                throw new OwnerManagerException("User is suspended");
            if (!treeOfRoleRepository.exitsOwner(appointerID, company))
                throw new OwnerManagerException("Only an owner can appoint a manager");
            User targetUser = userRepository.getUserByUsername(managerUsername);
            if (targetUser == null) throw new OwnerManagerException("Target user not found");
            String targetUserID = targetUser.getID();
            if (appointerID.equals(targetUserID)) throw new OwnerManagerException("You cannot appoint yourself");
            treeOfRoleRepository.storeManager(targetUserID, company, permissions, appointerID);
            notifyMember(targetUserID, "Manager Appointment",
                    "You have been appointed as a manager of '" + company + "'. Please approve or reject the appointment.");
            logger.info("User {} assigned {} as manager for {} successfully", appointerID, managerUsername, company);
            return Response.success("success");
        } catch (OwnerManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional
    public Response<String> ApproveAppointmentForManager(String token, String company) {
        try {
            logger.info("User of token {} is attempting to approve appointment for management of the company:", token,company);
            if (!tokenService.validateToken(token)) throw new OwnerManagerException("Invalid token");
            String userID = tokenService.extractUserId(token);
            if (userRepository.isUserSuspendedNow(userID))
                throw new OwnerManagerException("User is suspended");
            Manager m = treeOfRoleRepository.getManager(userID, company);
            m.acceptAppointment();
            treeOfRoleRepository.save(m);
            logger.info("User {} approved appointment for management of th eocmpany: ", userID, company);
            return Response.success("success");
        } catch (OwnerManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional
    public Response<String> RejectAppointmentForManager(String token, String company) {
        try {
            logger.info("User of token {} is attempting to reject appointment for managment for the company: ", token, company);
            if (!tokenService.validateToken(token)) throw new OwnerManagerException("Invalid token");
            String userID = tokenService.extractUserId(token);
            if (userRepository.isUserSuspendedNow(userID))
                throw new OwnerManagerException("User is suspended");
            if (!treeOfRoleRepository.isManager(userID, company))
                throw new OwnerManagerException("User is not a manager");
            treeOfRoleRepository.deleteManager(userID, company);
            logger.info("User {} rejected appointment for management for company {} successfully",userID, company );
            return Response.success("success");
        } catch (OwnerManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional
    public Response<String> AppointOwner(String ownerUsername, String company, String token) {
        try {
            logger.info("User of token {} is attempting to appoint owner {} for comapny: ", token, ownerUsername, company);
            if (!tokenService.validateToken(token)) throw new OwnerManagerException("Invalid token");
            String appointerID = tokenService.extractUserId(token);
            if (userRepository.isUserSuspendedNow(appointerID))
                throw new OwnerManagerException("User is suspended");
            if (!treeOfRoleRepository.exitsOwner(appointerID, company))
                throw new OwnerManagerException("Only an owner can appoint another owner");
            User targetUser = userRepository.getUserByUsername(ownerUsername);
            if (targetUser == null) throw new OwnerManagerException("Target user not found");
            String targetUserID = targetUser.getID();
            if (appointerID.equals(targetUserID)) throw new OwnerManagerException("You cannot appoint yourself");
            treeOfRoleRepository.storeOwner(targetUserID, company, appointerID);
            notifyMember(targetUserID, "Owner Appointment",
                    "You have been appointed as an owner of '" + company + "'. Please approve or reject the appointment.");
            logger.info("User {} appointed {} for owner for the company {} successfully", appointerID, targetUserID, company);
            return Response.success("success");
        } catch (OwnerManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional
    public Response<String> ApproveAppointmentForOwner(String token, String company) {
        try {
            logger.info("User of token {} is attempting to approve appointment for ownership for the company: ", token,company);
            if (!tokenService.validateToken(token)) throw new OwnerManagerException("Invalid token");
            String userID = tokenService.extractUserId(token);
            if (userRepository.isUserSuspendedNow(userID))
                throw new OwnerManagerException("User is suspended");
            Owner o = treeOfRoleRepository.getOwner(userID, company);
            o.acceptAppointment();
            treeOfRoleRepository.save(o);
            logger.info("User {} approved ownership for the company {} successfully", userID, company);
            return Response.success("success");
        } catch (OwnerManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional
    public Response<String> RejectAppointmentForOwner(String token, String company) {
        try {
            logger.info("User of token {} is attempting to reject appointment for ownership for the company: ", token, company);
            if (!tokenService.validateToken(token)) throw new OwnerManagerException("Invalid token");
            String userID = tokenService.extractUserId(token);
            if (userRepository.isUserSuspendedNow(userID))
                throw new OwnerManagerException("User is suspended");
            if (!treeOfRoleRepository.isOwner(userID, company))
                throw new OwnerManagerException("User is not an owner");
            String founderID = companyRepository.getCompanyFounder(company);
            if (userID.equals(founderID))
                throw new OwnerManagerException("Founder cannot give up the appointment");
            treeOfRoleRepository.deleteOwner(userID, company);
            logger.info("User {} rejected ownership for the company {} successfully", userID, company);
            return Response.success("success");
        } catch (OwnerManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional
    public Response<String> FireOwner(String token, String company, String ownerUsername) {
        try {
            logger.info("User of token {} is attempting to fire the owner {} of the company: ", token,ownerUsername, company);
            if (!tokenService.validateToken(token)) throw new OwnerManagerException("Invalid token");
            String appointerID = tokenService.extractUserId(token);
            if (userRepository.isUserSuspendedNow(appointerID))
                throw new OwnerManagerException("User is suspended");
            User targetUser = userRepository.getUserByUsername(ownerUsername);
            if (targetUser == null) throw new OwnerManagerException("Target user not found");
            String targetUserID = targetUser.getID();
            if (!treeOfRoleRepository.isAppointerOwner(targetUserID, company, appointerID))
                throw new OwnerManagerException("You are not allowed to fire this owner");
            treeOfRoleRepository.deleteOwner(targetUserID, company);
            logger.info("User {} fired {} from ownership for the company {} successfully", appointerID, targetUserID, company);
            return Response.success("success");
        } catch (OwnerManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional
    public Response<String> FireManager(String token, String company, String managerUsername) {
        try {
            logger.info("User of token {} is attempting to fire manager {} of the company: ", token, managerUsername, company);
            if (!tokenService.validateToken(token)) throw new OwnerManagerException("Invalid token");
            String appointerID = tokenService.extractUserId(token);
            if (userRepository.isUserSuspendedNow(appointerID))
                throw new OwnerManagerException("User is suspended");
            User targetUser = userRepository.getUserByUsername(managerUsername);
            if (targetUser == null) throw new OwnerManagerException("Target user not found");
            String targetUserID = targetUser.getID();
            if (!treeOfRoleRepository.isAppointerManager(targetUserID, company, appointerID))
                throw new OwnerManagerException("You are not allowed to fire this manager");
            treeOfRoleRepository.deleteManager(targetUserID, company);
            logger.info("User {} fired {} for management for the company {} successfully", appointerID, targetUserID, company);
            return Response.success("success");
        } catch (OwnerManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional
    public Response<String> FireMember(String token, String company, String memberUsername) {
        try {
            logger.info("User of token {} is attempting to fire the member {} of the company: ", token, memberUsername, company);
            if (!tokenService.validateToken(token)) throw new OwnerManagerException("Invalid token");
            String appointerID = tokenService.extractUserId(token);
            if (userRepository.isUserSuspendedNow(appointerID))
                throw new OwnerManagerException("User is suspended");
            User targetUser = userRepository.getUserByUsername(memberUsername);
            if (targetUser == null) throw new OwnerManagerException("User not found");
            String targetUserID = targetUser.getID();

            Manager m = treeOfRoleRepository.getManager(targetUserID, company);
            if (m != null) {
                if (!appointerID.equals(m.getAppointerID()))
                    throw new OwnerManagerException("You are not allowed to fire this manager");
                treeOfRoleRepository.deleteManager(targetUserID, company);
                notifyMember(targetUserID, "Role Removed",
                        "You have been removed from your manager role in '" + company + "'.");
                return Response.success("success");
            }

            Owner o = treeOfRoleRepository.getOwner(targetUserID, company);
            if (o != null) {
                if (iTreeOfRoleRepository.FOUNDER_APPOINTER.equals(o.getAppointerID()))
                    throw new OwnerManagerException("Founders cannot be fired");
                if (!appointerID.equals(o.getAppointerID()))
                    throw new OwnerManagerException("You are not allowed to fire this owner");
                List<String> cascaded = new ArrayList<>();
                cascadeDeleteAppointees(targetUserID, company, cascaded);
                treeOfRoleRepository.deleteOwner(targetUserID, company);
                notifyMember(targetUserID, "Role Removed",
                        "You have been removed from your owner role in '" + company + "'.");
                for (String removedId : cascaded) {
                    notifyMember(removedId, "Role Removed",
                            "You have been removed from your role in '" + company + "' because your appointer was removed.");
                }
                logger.info("User {} fired {} from membership for the company {} successfully", appointerID, targetUserID, company);
                return Response.success("success");
            }

            throw new OwnerManagerException("User has no role in this company");
        } catch (OwnerManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    private void cascadeDeleteAppointees(String userId, String company, List<String> removed) {
        treeOfRoleRepository.getAllOwnersByCompany(company).stream()
                .filter(o -> userId.equals(o.getAppointerID()))
                .forEach(o -> {
                    cascadeDeleteAppointees(o.getUserID(), company, removed);
                    treeOfRoleRepository.deleteOwner(o.getUserID(), company);
                    removed.add(o.getUserID());
                });
        treeOfRoleRepository.getAllManagersByCompany(company).stream()
                .filter(m -> userId.equals(m.getAppointerID()))
                .forEach(m -> {
                    treeOfRoleRepository.deleteManager(m.getUserID(), company);
                    removed.add(m.getUserID());
                });
    }

    @Transactional
    public Response<String> ChangeManagerPermissions(String token, String company, String managerUsername, Set<Permission> newPermissions) {
        try {
            logger.info("User of token {} is attempting to change the premmisions of the manager {} of the company: ", token, managerUsername, company);
            if (!tokenService.validateToken(token)) throw new OwnerManagerException("Invalid token");
            String appointerID = tokenService.extractUserId(token);
            if (userRepository.isUserSuspendedNow(appointerID))
                throw new OwnerManagerException("User is suspended");
            User targetUser = userRepository.getUserByUsername(managerUsername);
            if (targetUser == null) throw new OwnerManagerException("Target user not found");
            String targetUserID = targetUser.getID();
            Manager manager = treeOfRoleRepository.getManager(targetUserID, company);
            if (manager == null) throw new OwnerManagerException("Manager not found");
            if (!treeOfRoleRepository.isAppointerManager(targetUserID, company, appointerID))
                throw new OwnerManagerException("You are not authorized to change permissions for this manager");
            manager.setPermissions(newPermissions);
            treeOfRoleRepository.save(manager);
            logger.info("User {} changed premmisions of the manager {} of the company {} successfully", appointerID, targetUserID, company);
            return Response.success("success");
        } catch (OwnerManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to change permissions: " + e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional
    public Response<String> freezeCompany(String company, String token) {
        try {
            logger.info("User of token {} is attempting to freeze the company: ", token, company);
            if (!tokenService.validateToken(token)) throw new OwnerManagerException("Invalid token");
            String userID = tokenService.extractUserId(token);
            if (userRepository.isUserSuspendedNow(userID))
                throw new OwnerManagerException("User is suspended");
            Company companyObj = companyRepository.getCompany(company);
            companyObj.freezeCompany(userID);
            companyRepository.save(companyObj);
            String title = "Company Suspended";
            String msg = "Company '" + company + "' has been suspended by its founder.";
            treeOfRoleRepository.getAllOwnersByCompany(company).forEach(o -> notifyMember(o.getUserID(), title, msg));
            treeOfRoleRepository.getAllManagersByCompany(company).forEach(m -> notifyMember(m.getUserID(), title, msg));
            logger.info("User {} freezed the company {} successfully ", userID, company);
            return Response.success("success");
        } catch (OwnerManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional
    public Response<String> unfreezeCompany(String company, String token) {
        try {
            logger.info("User of token {} is attempting to unfreeze the company: ", token, company);
            if (!tokenService.validateToken(token)) throw new OwnerManagerException("Invalid token");
            String userID = tokenService.extractUserId(token);
            if (userRepository.isUserSuspendedNow(userID))
                throw new OwnerManagerException("User is suspended");
            Company companyObj = companyRepository.getCompany(company);
            companyObj.unfreezeCompany(userID);
            companyRepository.save(companyObj);
            logger.info("User {} unfreezed the company {} successfully ", userID, company);
            return Response.success("success");
        } catch (OwnerManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional
    public Response<String> closeCompany(String company, String token) {
        try {
            if (!tokenService.validateToken(token)) throw new OwnerManagerException("Invalid token");
            String userId = tokenService.extractUserId(token);
            if (userRepository.isUserSuspendedNow(userId))
                throw new OwnerManagerException("User is suspended");
            Owner o = treeOfRoleRepository.getOwner(userId, company);
            if (o == null || !iTreeOfRoleRepository.FOUNDER_APPOINTER.equals(o.getAppointerID()))
                throw new OwnerManagerException("Only the founder can close the company");
            List<Owner> owners = treeOfRoleRepository.getAllOwnersByCompany(company);
            List<Manager> managers = treeOfRoleRepository.getAllManagersByCompany(company);
            companyRepository.deleteCompany(company);
            eventRepository.deleteCompanyEvent(company);
            treeOfRoleRepository.deleteCompanyMangersAndOwners(company);
            String title = "Company Closed";
            String closeMsg = "Company '" + company + "' has been permanently closed by its founder.";
            owners.forEach(owner -> notifyMember(owner.getUserID(), title, closeMsg));
            managers.forEach(mgr -> notifyMember(mgr.getUserID(), title, closeMsg));
            logger.info("Company '{}' closed by founder '{}'", company, userId);
            return Response.success("success");
        } catch (OwnerManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to close company '{}': {}", company, e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> replyToBuyer(String token, String companyName, String buyerId, String message) {
        try {
            if (!tokenService.validateToken(token)) throw new OwnerManagerException("Invalid token");
            String userID = tokenService.extractUserId(token);
            if (userRepository.isUserSuspendedNow(userID))
                throw new OwnerManagerException("User is suspended");
            boolean isOwner = treeOfRoleRepository.exitsOwner(userID, companyName);
            boolean isManager = treeOfRoleRepository.isManager(userID, companyName);
            if (!isOwner && !isManager)
                throw new OwnerManagerException("Unauthorized: Only owners or managers can reply to buyers");
            notifier.notifyUser(buyerId, "Message from " + companyName, message);
            logger.info("Successfully replied to buyer {}", buyerId);
            return Response.success("success");
        } catch (OwnerManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to reply to buyer: {}", e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Response<List<CompanyDTO>> getActiveCompanies(String token) {
        try {
            boolean isGuest = token == null || token.trim().isEmpty() || token.contains("guest-temporary-token");
            if (!isGuest && !tokenService.validateToken(token))
                throw new OwnerManagerException("Invalid token");
            List<CompanyDTO> companies = companyRepository.getActiveCompanies()
                    .stream()
                    .map(CompanyDTO::fromEntity)
                    .toList();
            return Response.success(companies);
        } catch (OwnerManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to retrieve active companies: {}", e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Response<Set<Permission>> GetManagerPermissions(String token, String company, String managerUsername) {
        try {
            if (!tokenService.validateToken(token)) throw new OwnerManagerException("Invalid token");
            String userID = tokenService.extractUserId(token);
            if (!treeOfRoleRepository.exitsOwner(userID, company))
                throw new OwnerManagerException("Access denied: Only an owner can view manager permissions");
            User targetUser = userRepository.getUserByUsername(managerUsername);
            if (targetUser == null) throw new OwnerManagerException("Target user not found");
            String targetUserID = targetUser.getID();
            Set<Permission> permissions = treeOfRoleRepository.getManagerPermissions(targetUserID, company);
            return Response.success(permissions);
        } catch (OwnerManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error retrieving manager permissions: " + e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Response<String> GetRoleTreeString(String token, String companyName) {
        try {
            if (!tokenService.validateToken(token)) throw new OwnerManagerException("Invalid token");
            String userID = tokenService.extractUserId(token);
            if (treeOfRoleRepository.getOwner(userID, companyName) == null)
                throw new OwnerManagerException("Only owners can view the role tree");
            List<Owner> allOwners = treeOfRoleRepository.getAllOwnersByCompany(companyName);
            List<Manager> allManagers = treeOfRoleRepository.getAllManagersByCompany(companyName);
            Company company = companyRepository.getCompany(companyName);
            StringBuilder treeString = new StringBuilder();
            buildTreeString(company.getFounderID(), allOwners, allManagers, treeString, 0);
            return Response.success(treeString.toString());
        } catch (OwnerManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    private void buildTreeString(String currentUserID, List<Owner> allOwners, List<Manager> allManagers, StringBuilder sb, int depth) {
        String indent = "  ".repeat(depth);
        String role = allOwners.stream().anyMatch(o -> o.getUserID().equals(currentUserID)) ? "Owner" : "Manager";
        User u = userRepository.getUserByID(currentUserID);
        String displayName = (u != null) ? u.getName() : currentUserID;
        sb.append(indent).append("|-- ").append(displayName).append(" (").append(role).append(")\n");
        allOwners.stream()
                .filter(o -> currentUserID.equals(o.getAppointerID()) && o.isAccepted())
                .forEach(o -> buildTreeString(o.getUserID(), allOwners, allManagers, sb, depth + 1));
        allManagers.stream()
                .filter(m -> currentUserID.equals(m.getAppointerID()) && m.isAccepted())
                .forEach(m -> {
                    String mIndent = "  ".repeat(depth + 1);
                    User mUser = userRepository.getUserByID(m.getUserID());
                    String mDisplayName = (mUser != null) ? mUser.getName() : m.getUserID();
                    sb.append(mIndent).append("|-- ").append(mDisplayName).append(" (Manager)\n");
                });
    }

    public Response<String> sendMessageToUser(String token, String companyName, String targetUserId, String message) {
        try {
            if (!tokenService.validateToken(token)) throw new OwnerManagerException("Invalid token");
            String userID = tokenService.extractUserId(token);
            if (userRepository.isUserSuspendedNow(userID))
                throw new OwnerManagerException("User is suspended");
            boolean isOwner = treeOfRoleRepository.exitsOwner(userID, companyName);
            boolean isManager = treeOfRoleRepository.isManager(userID, companyName);
            if (!isOwner && !isManager)
                throw new OwnerManagerException("Not authorized to send messages for this company");
            notifier.notifyUser(targetUserId, "Message from " + companyName, message);
            return Response.success("success");
        } catch (OwnerManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    private void notifyMember(String userID, String title, String message) {
        try {
            notifier.notifyUser(userID, title, message);
        } catch (Exception e) {
            logger.warn("Failed to notify user {}: {}", userID, e.getMessage());
        }
    }
}
