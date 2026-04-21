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
}
