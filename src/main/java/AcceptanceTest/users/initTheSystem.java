package AcceptanceTest.users;


import Domain.Company.iCompanyRepository;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.User.IUserRepository;
import Infastructure.PasswordEncoderImpl;
import Infastructure.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;

import Appliction.IPasswordEncoder;

public class initTheSystem {
    private iTreeOfRoleRepository iTreeOfRoleRepository;
    private iCompanyRepository iCompanyRepository;
    private IUserRepository iUserRepository;
    private IPasswordEncoder iPasswordEncoder;
    private TokenService tokenService;

    public initTheSystem(

            iTreeOfRoleRepository iTreeOfRoleRepository,
            iCompanyRepository iCompanyRepository,
            IUserRepository iUserRepository,
            IPasswordEncoder iPasswordEncoder,
            TokenService tokenService
    ) {

        this.iTreeOfRoleRepository = iTreeOfRoleRepository;
        this.iCompanyRepository = iCompanyRepository;

        this.iUserRepository = iUserRepository;
        this.iPasswordEncoder = iPasswordEncoder;
        this.tokenService = tokenService;
    }
    public void init() {
        iTreeOfRoleRepository.deleteAllRoles();
        iCompanyRepository.deleteAllCompany();
        iUserRepository.deleteAll();
        iPasswordEncoder=new PasswordEncoderImpl();
        tokenService.clearAllData();



    }
}
