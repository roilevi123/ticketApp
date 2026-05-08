package Appliction;

import Domain.AdminAggregate.iAdminRepository;
import Domain.Company.iCompanyRepository;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.PurchasedOrderAggregate.PurchaseOrder;
import Domain.PurchasedOrderAggregate.PurchaseOrderDTO;
import Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import Domain.Ticket.Ticket;
import Domain.Ticket.TicketDTO;
import Domain.Ticket.iTicketRepository;
import Domain.User.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminServiceTest {

    @Mock private iCompanyRepository companyRepository;
    @Mock private iTreeOfRoleRepository treeOfRoleRepository;
    @Mock private iAdminRepository adminRepository;
    @Mock private IUserRepository userRepository;
    @Mock private iPurchasedOrderRepository purchasedOrderRepository;
    @Mock private iTicketRepository ticketRepository;
    @Mock private Domain.Event.iEventRepository eventRepository;
    @InjectMocks
    private AdminService adminService;

    private final String ADMIN_NAME = "admin";
    private final String NOT_ADMIN = "notAdmin";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(adminRepository.isAdmin(ADMIN_NAME)).thenReturn(true);
        when(adminRepository.isAdmin(NOT_ADMIN)).thenReturn(false);
    }

    @Test
    void CloseCompany_Success() {
        String company = "Company1";

        adminService.CloseCompany(company, ADMIN_NAME);

        verify(companyRepository).deleteCompany(company);
        verify(treeOfRoleRepository).deleteCompanyMangersAndOwners(company);
    }

    @Test
    void CloseCompany_Fail_NotAdmin() {
        String company = "Company1";

        adminService.CloseCompany(company, NOT_ADMIN);

        verify(companyRepository, never()).deleteCompany(anyString());
    }

    @Test
    void removeUser_Success() {
        String user = "user1";

        adminService.removeUser(user, ADMIN_NAME);

        verify(userRepository).deleteUser(user);
        verify(treeOfRoleRepository).deleteUserRoles(user);
    }

    @Test
    void removeUser_Fail_NotAdmin() {
        String user = "user1";

        adminService.removeUser(user, NOT_ADMIN);

        verify(userRepository, never()).deleteUser(anyString());
        verify(treeOfRoleRepository, never()).deleteUserRoles(anyString());
    }
    @Test
    void GetAllPurchasedOrders_Success() {
        List<String> tickets = List.of("T1");
        PurchaseOrder po1 = new PurchaseOrder("Comp", "Ev", tickets, "Buyer", "O1");

        when(purchasedOrderRepository.GetAllPurchasedOrders()).thenReturn(List.of(po1));
        List<Ticket> ticketList=new ArrayList<>();
        Ticket ticket1=new Ticket(1,1,"Ev","Comp","T1",100);
        ticket1.purchase();
        ticketList.add(ticket1);
        when(ticketRepository.getTickets(tickets)).thenReturn(ticketList);

        List<PurchaseOrderDTO> result = adminService.GetAllPurchasedOrders("admin");
        boolean isCompanyExist = false;
        boolean isEventExist = false;
        boolean isPurchased = false;
        boolean isUserExist = false;
        for (PurchaseOrderDTO po : result) {
            List<TicketDTO> ticketsList = po.tickets();
            if(po.buyer().equals("Buyer")){
                isUserExist = true;
            }
            for (TicketDTO ticket : ticketsList) {
                if(ticket.isPurchased()){
                    isPurchased = true;
                }
                if(ticket.company().equals("Comp")){
                    isCompanyExist = true;
                }
                if(ticket.event().equals("Ev")){
                    isEventExist = true;
                }


            }
        }
        assertNotNull(result);
        assertTrue(isCompanyExist);
        assertTrue(isEventExist);
        assertTrue(isPurchased);
        assertTrue(isUserExist);
    }

    @Test
    void GetAllPurchasedOrders_Fail_NotAdmin() {
        List<PurchaseOrderDTO> result = adminService.GetAllPurchasedOrders(NOT_ADMIN);
        assertNull(result);


}}