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
        return "";
    }


}
