package AcceptanceTest.users;


import Domain.Company.iCompanyRepository;
import Domain.Event.iEventRepository;
import Domain.Order.IActiveOrderRepository;
import Domain.Order.IPurchasedOrderRepository;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
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
    private IActiveOrderRepository iActiveOrderRepository;
    private iPurchasedOrderRepository iPurchasedOrderRepository;
    public initTheSystem(

            iTreeOfRoleRepository iTreeOfRoleRepository,
            iCompanyRepository iCompanyRepository,
            IUserRepository iUserRepository,
            IPasswordEncoder iPasswordEncoder,
            TokenService tokenService,
            iTicketRepository ticketRepository,
            iEventRepository eventRepository,
            iQueueRepository iQueueRepository,
            IActiveOrderRepository activeOrderRepository,
            iPurchasedOrderRepository purchasedOrderRepository
    ) {

        this.iTreeOfRoleRepository = iTreeOfRoleRepository;
        this.iCompanyRepository = iCompanyRepository;

        this.iUserRepository = iUserRepository;
        this.iPasswordEncoder = iPasswordEncoder;
        this.tokenService = tokenService;
        this.iTicketRepository = ticketRepository;
        this.iEventRepository = eventRepository;
        this.iQueueRepository = iQueueRepository;
        this.iActiveOrderRepository = activeOrderRepository;
        this.iPurchasedOrderRepository = purchasedOrderRepository;
    }
    public void init() {
        iActiveOrderRepository.deleteAllActiveOrders();
        iEventRepository.deleteAllEvents();
        iTreeOfRoleRepository.deleteAllRoles();
        iCompanyRepository.deleteAllCompany();
        iPurchasedOrderRepository.deleteAll();
        iQueueRepository.deleteAll();
        iTicketRepository.deleteAllTickets();
        iUserRepository.deleteAll();
        iPasswordEncoder=new PasswordEncoderImpl();
        tokenService.clearAllData();



    }
}
