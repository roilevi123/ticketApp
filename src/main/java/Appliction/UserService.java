package Appliction;
import Domain.User.IUserRepository;
import Domain.User.User;

import Infastructure.TokenService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class UserService implements IAuth {


    private final IPasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final IUserRepository userRepository;
    private final TokenService tokenService;

    public UserService(IPasswordEncoder passwordEncoder,
                       IUserRepository userRepository,
                       TokenService tokenService
                       ) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    @Override
    public String register(String token, String username, String password) {
        try{
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            logger.info("Registering user " + username );
            String encodedPassword = passwordEncoder.encode(password);
            userRepository.Store(username, encodedPassword);
            logger.info("Registered user " + username + "successfully");
            return "success";
        }catch (Exception e){
            logger.error("Registering user " + username + " failed",e.getMessage());
            return e.getMessage();
        }

    }

    @Override
    public String login(String token, String username, String password) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            logger.info("User {} is trying to login", username);
            boolean user = userRepository.usernameExists(username);
            if (!user) {
                throw new RuntimeException( "User not found");
            }
            String passwordHash = userRepository.getUserPassword(username);
            if (!passwordEncoder.matches(password, passwordHash)) {
                logger.error("Invalid password for user {}", username);
                throw new RuntimeException("Invalid password");
            }
            User userObj = userRepository.getUserByUsername(username);
            String memberToken = tokenService.generateMemberToken(userObj.getID(), userObj.getName());
            logger.info("User {} logged in successfully", username);
            return memberToken;

        } catch (Exception e) {
            logger.error("Login failed for user {}", username, e);
            return null;
        }
    }

    @Override
    public String logout(String token) {
        try {
            logger.info("Logout requested");
            if (tokenService.validateToken(token)){
                tokenService.addBlacklistToken(token);
            logger.info("Logout completed successfully");
            return "success";
            }
        logger.error("Logout failed for token {}", token);
        throw new RuntimeException("Invalid token");
        } catch (Exception e) {
            logger.error("Logout failed", e);
            return e.getMessage();
        }
    }

    public String getUserInfo(String token) {
        try {
            logger.info("Getting user info");
            if (tokenService.validateToken(token)){
                String userId = tokenService.extractUserId(token);
                User user = userRepository.getUserByID(userId);
                if (user == null) {
                    logger.error("User {} not found while getting user info", userId);
                    throw new RuntimeException("User not found");
                }
                logger.info("User info retrieved successfully for user {}", userId);
                return user.getUserInfo();
            }
            logger.error("Invalid token provided for getting user info");
            throw new RuntimeException("Invalid token");
        } catch (Exception e) {
            logger.error("Failed to get user info", e);
            return e.getMessage();
        }
    }

    public String updateUserPassword(String token, String newPassword) {
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
            String encodedPassword = passwordEncoder.encode(newPassword);
            user.setPassword(encodedPassword);
            userRepository.save(user);
            logger.info("Password updated successfully for user {}", userId);
            return "success";
        } catch (Exception e) {
            logger.error("Failed to update password", e);
            return e.getMessage();
        }
    }

    public String updateUserName(String token, String newUsername) {
        try {
            logger.info("Updating user name");
            if (!tokenService.validateToken(token)) {
                logger.error("Invalid token provided for updating username");
                throw new RuntimeException("Invalid token");
            }
            String currentUserId = tokenService.extractUserId(token);
            User user = userRepository.getUserByID(currentUserId);
            if (user == null) {
                logger.error("User {} not found while updating username", currentUserId);
                throw new RuntimeException("User not found");
            }
            String oldUsername = userRepository.getUsernameByID(currentUserId);
            user.setName(newUsername);
            userRepository.save(user);
            logger.info("Username updated successfully from {} to {}", oldUsername, newUsername);
            return "success";
        } catch (Exception e) {
            logger.error("Failed to update username", e);
            return e.getMessage();
        }
    }

}
