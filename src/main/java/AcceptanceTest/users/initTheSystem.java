package AcceptanceTest.users;


import Domain.Company.iCompanyRepository;
import Domain.Event.iEventRepository;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.QueueAggregates.iQueueRepository;
import Domain.Ticket.iTicketRepository;
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
    private iTicketRepository iTicketRepository;
    private iEventRepository iEventRepository;
    private iQueueRepository iQueueRepository;

    public initTheSystem(

            iTreeOfRoleRepository iTreeOfRoleRepository,
            iCompanyRepository iCompanyRepository,
            IUserRepository iUserRepository,
            IPasswordEncoder iPasswordEncoder,
            TokenService tokenService,
            iTicketRepository ticketRepository,
            iEventRepository eventRepository,
            iQueueRepository iQueueRepository
    ) {

        this.iTreeOfRoleRepository = iTreeOfRoleRepository;
        this.iCompanyRepository = iCompanyRepository;

        this.iUserRepository = iUserRepository;
        this.iPasswordEncoder = iPasswordEncoder;
        this.tokenService = tokenService;
        this.iTicketRepository = ticketRepository;
        this.iEventRepository = eventRepository;
        this.iQueueRepository = iQueueRepository;
    }
    public void init() {
        iTreeOfRoleRepository.deleteAllRoles();
        iCompanyRepository.deleteAllCompany();
        iUserRepository.deleteAll();
        iPasswordEncoder=new PasswordEncoderImpl();
        tokenService.clearAllData();

        iTicketRepository.deleteAllTickets();
        iEventRepository.deleteAllEvents();
        iQueueRepository.deleteAll();

    }
}
