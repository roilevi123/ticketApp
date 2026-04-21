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
    public String register(String username, String password) {
        try{
            logger.info("Registering user " + username );
            String encodedPassword = passwordEncoder.encode(password);
            userRepository.Store(username, encodedPassword);
            logger.info("Registered user " + username + "successfully");
            return "success";
        }catch (Exception e){
            logger.error("Registering user " + username + " failed",e.getMessage());
            return "failed";
        }

    }

    @Override
    public String login(String username, String password) {
        try {
            logger.info("User {} is trying to login", username);
            boolean user = userRepository.userExists(username);
            String passwordHash = userRepository.getUserPassword(username);
            if (!user ) {
                throw new RuntimeException( "User not found");
            }
            if (!passwordEncoder.matches(password, passwordHash)) {
                logger.error("Invalid password for user {}", username);
                throw new RuntimeException("Invalid password");
            }
            String token = tokenService.generateToken(username);
            logger.info("User {} logged in successfully", username);
            return token;

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
                tokenService.addBlacklistToken(tokenService.extractUsername(token));
            logger.info("Logout completed successfully");
            return "success";
            }
        logger.error("Logout failed for token {}", token);
        throw new RuntimeException("Invalid token");
        } catch (Exception e) {
            logger.error("Logout failed", e);
            return "failed";
        }
    }

    public String getUserInfo(String token) {
        try {
            logger.info("Getting user info");
            if (tokenService.validateToken(token)){
                String username = tokenService.extractUsername(token);
                User user = userRepository.getUser(username);
                if (user == null) {
                    logger.error("User {} not found while getting user info", username);
                    throw new RuntimeException("User not found");
                }
                logger.info("User info retrieved successfully for user {}", username);
                return user.getUserInfo();
            }
            logger.error("Invalid token provided for getting user info");
            throw new RuntimeException("Invalid token");
        } catch (Exception e) {
            logger.error("Failed to get user info", e);
            return null;
        }
    }

    public String updateUserPassword(String token, String newPassword) {
        try {
            logger.info("Updating user password");
            if (!tokenService.validateToken(token)) {
                logger.error("Invalid token provided for updating password");
                throw new RuntimeException("Invalid token");
            }
            String username = tokenService.extractUsername(token);
            User user = userRepository.getUser(username);
            if (user == null) {
                logger.error("User {} not found while updating password", username);
                throw new RuntimeException("User not found");
            }
            String encodedPassword = passwordEncoder.encode(newPassword);
            user.setPassword(encodedPassword);
            userRepository.save(user);
            logger.info("Password updated successfully for user {}", username);
            return "success";
        } catch (Exception e) {
            logger.error("Failed to update password", e);
            return "failed";
        }
    }
}
