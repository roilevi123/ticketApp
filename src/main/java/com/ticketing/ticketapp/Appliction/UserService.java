package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.Notification.INotificationRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.User.User;
import com.ticketing.ticketapp.Domain.User.UserDTO;

import com.ticketing.ticketapp.Infastructure.TokenService;
import org.springframework.stereotype.Service;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserService implements IAuth {

    private final IPasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final IUserRepository userRepository;
    private final TokenService tokenService;
    private final INotificationRepository userNotificationRepository;
    private final INotifier notifier;
    private final iTreeOfRoleRepository roleRepository;

    public UserService(IPasswordEncoder passwordEncoder,
                       IUserRepository userRepository,
                       TokenService tokenService,
                       INotificationRepository userNotificationRepository,
                       INotifier notifier,
                       iTreeOfRoleRepository roleRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.userNotificationRepository = userNotificationRepository;
        this.notifier = notifier;
        this.roleRepository = roleRepository;
    }

    @Override
    public Response<String> register(String token, String username, String password, int age, String email) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            logger.info("Registering user " + username);
            String encodedPassword = passwordEncoder.encode(password);
            userRepository.Store(username, encodedPassword, age, email);
            logger.info("Registered user " + username + " successfully");
            return Response.success("success");
        } catch (Exception e) {
            logger.error("Registering user " + username + " failed", e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Override
    public Response<String> login(String token, String username, String password) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            logger.info("User {} is trying to login", username);
            boolean user = userRepository.usernameExists(username);
            if (!user) {
                throw new RuntimeException("User not found");
            }
            String passwordHash = userRepository.getUserPassword(username);
            if (!passwordEncoder.matches(password, passwordHash)) {
                logger.error("Invalid password for user {}", username);
                throw new RuntimeException("Invalid password");
            }
            User userObj = userRepository.getUserByUsername(username);
            String memberToken = tokenService.generateMemberToken(userObj.getID(), userObj.getName());
            logger.info("User {} logged in successfully", username);
            return Response.success(memberToken);
        } catch (Exception e) {
            logger.error("Login failed for user {}", username, e);
            return Response.error(e.getMessage());
        }
    }

    @Override
    public Response<String> logout(String token) {
        try {
            logger.info("Logout requested");
            if (tokenService.validateToken(token)) {
                tokenService.addBlacklistToken(token);
                logger.info("Logout completed successfully");
                return Response.success("success");
            }
            logger.error("Logout failed for token {}", token);
            throw new RuntimeException("Invalid token");
        } catch (Exception e) {
            logger.error("Logout failed", e);
            return Response.error(e.getMessage());
        }
    }

    public Response<String> updateUserProfile(String token, UserDTO request) {
        try {
            logger.info("Updating user profile");
            if (!tokenService.validateToken(token)) {
                logger.error("Invalid token provided for updating profile");
                throw new RuntimeException("Invalid token");
            }
            String userId = tokenService.extractUserId(token);
            User user = userRepository.getUserByID(userId);
            if (user == null) {
                logger.error("User {} not found while updating profile", userId);
                throw new RuntimeException("User not found");
            }

            if(userRepository.isUserSuspendedNow(userId))
                throw new Exception("User is suspended");


            user.setName(request.getName());
            user.setEmail(request.getEmail());
            
            userRepository.save(user); 
            
            logger.info("Profile updated successfully for user {}", userId);
            return Response.success("Profile updated successfully");
        } catch (Exception e) {
            logger.error("Failed to update profile", e);
            return Response.error(e.getMessage());
        }
    }

    @Override
    public Response<UserDTO> getUserProfile(String token) {
        try {
            logger.info("Getting user info");
            if (tokenService.validateToken(token)) {
                String userId = tokenService.extractUserId(token);
                User user = userRepository.getUserByID(userId);
                if (user == null) {
                    logger.error("User {} not found while getting user info", userId);
                    return Response.error("User not found");
                }
                logger.info("User profile retrieved successfully for user {}", userId);
                return Response.success(UserDTO.fromEntity(user));
            }
            logger.error("Invalid token provided for getting user info");
            return Response.error("Invalid token");
        } catch (Exception e) {
            logger.error("Failed to get user info", e);
            return Response.error(e.getMessage());
        }
    }

    @Override
    public Response<String> updateUserPassword(String token, String newPassword) {
        try {
            logger.info("Updating user password");
            if (!tokenService.validateToken(token)) {
                logger.error("Invalid token provided for updating password");
                throw new RuntimeException("Invalid token");
            }
            String userId = tokenService.extractUserId(token);
            User user = userRepository.getUserByID(userId);
            if (user == null) {
                logger.error("User {} not found while updating password", userId);
                throw new RuntimeException("User not found");
            }

            if(userRepository.isUserSuspendedNow(userId))
                throw new Exception("User is suspended");

            String encodedPassword = passwordEncoder.encode(newPassword);
            user.setPassword(encodedPassword);
            userRepository.save(user);
            logger.info("Password updated successfully for user {}", userId);
            return Response.success("success");
        } catch (Exception e) {
            logger.error("Failed to update password", e);
            return Response.error(e.getMessage());
        }
    }


    public Response<String> submitUserComplaint(String token, String targetRole, String messageContent) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            
            String username = tokenService.extractUsername(token);
            String userID = tokenService.extractUserId(token);

            if(userRepository.isUserSuspendedNow(userID))
                throw new RuntimeException("User is suspended");

            String senderId = tokenService.extractUserId(token);

            logger.info("User {} is submitting a complaint to {}", username, targetRole);
            String targetId = targetRole.equalsIgnoreCase("Admin") ? "SYSTEM_ADMIN" : targetRole;
            notifier.notifyUserWithSender(targetId, senderId, "Complaint from " + username, messageContent);
            logger.info("Successfully submitted complaint from {}", username);
            return Response.success("Complaint sent successfully");
            
        } catch (Exception e) {
            logger.error("Failed to submit complaint: {}", e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<List<String>> getUserCompanies(String token) {
        try {
            if (!tokenService.validateToken(token)) throw new RuntimeException("Invalid token");
            String userId = tokenService.extractUserId(token);
            List<String> companies = roleRepository.getUserCompanies(userId);
            return Response.success(companies);
        } catch (Exception e) {
            logger.error("Failed to fetch user companies", e);
            return Response.error(e.getMessage());
        }
    }

    public Response<String> switchCompanyContext(String token, String companyName) {
        try {
            if (!tokenService.validateToken(token)) throw new RuntimeException("Invalid token");
            String userId = tokenService.extractUserId(token);
            User user = userRepository.getUserByID(userId);

            String roleInCompany = roleRepository.getRoleInCompany(userId, companyName);
            if (roleInCompany.equals("MEMBER")) {
                throw new RuntimeException("User is not authorized in this company");
            }

            String companyToken;
            if ("MANAGER".equals(roleInCompany)) {
                List<String> permNames = roleRepository.getManagerPermissions(userId, companyName)
                        .stream().map(Enum::name).collect(java.util.stream.Collectors.toList());
                companyToken = tokenService.generateCompanyToken(userId, user.getName(), roleInCompany, companyName, permNames);
            } else {
                companyToken = tokenService.generateCompanyToken(userId, user.getName(), roleInCompany, companyName);
            }

            logger.info("User {} switched context to company {} with role {}", user.getName(), companyName, roleInCompany);
            return Response.success(companyToken);
        } catch (Exception e) {
            logger.error("Failed to switch company context", e);
            return Response.error(e.getMessage());
        }
    }

    public Response<List<String>> getUserNotifications(String token) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            String userId = tokenService.extractUserId(token);
            List<String> messages = userNotificationRepository.getAll(userId).stream()
                    .map(n -> n.getMessage())
                    .collect(java.util.stream.Collectors.toList());
            return Response.success(messages);
        } catch (Exception e) {
            logger.error("Failed to fetch notifications", e);
            return Response.error(e.getMessage());
        }
    }
}
