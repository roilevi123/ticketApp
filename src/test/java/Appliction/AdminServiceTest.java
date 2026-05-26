package Appliction;

import com.ticketing.ticketapp.Appliction.*;

import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Manager;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Owner;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrder;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrderDTO;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.Ticket.Ticket;
import com.ticketing.ticketapp.Domain.Ticket.TicketDTO;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.User.Suspension;
import com.ticketing.ticketapp.Domain.User.User;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminServiceTest {

    @Mock private iCompanyRepository companyRepository;
    @Mock private iTreeOfRoleRepository treeOfRoleRepository;
    @Mock private iAdminRepository adminRepository;
    @Mock private IUserRepository userRepository;
    @Mock private iPurchasedOrderRepository purchasedOrderRepository;
    @Mock private iTicketRepository ticketRepository;
    @Mock private com.ticketing.ticketapp.Domain.Event.iEventRepository eventRepository;
    @Mock private TokenService tokenService;
    @Mock private INotifier notifier;
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

        var response = adminService.CloseCompany(company, ADMIN_NAME);

        assertTrue(response.isSuccess());
        verify(companyRepository).deleteCompany(company);
        verify(treeOfRoleRepository).deleteCompanyMangersAndOwners(company);
    }

    @Test
    void CloseCompany_Fail_NotAdmin() {
        String company = "Company1";

        var response = adminService.CloseCompany(company, NOT_ADMIN);

        assertFalse(response.isSuccess());
        verify(companyRepository, never()).deleteCompany(anyString());
    }

    @Test
    void removeUser_Success() {
        String user = "user1";

        var response = adminService.removeUser(user, ADMIN_NAME);

        assertTrue(response.isSuccess());
        verify(userRepository).deleteUser(user);
        verify(treeOfRoleRepository).deleteUserRoles(user);
    }

    @Test
    void removeUser_Fail_NotAdmin() {
        String user = "user1";

        var response = adminService.removeUser(user, NOT_ADMIN);

        assertFalse(response.isSuccess());
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

        var response = adminService.GetAllPurchasedOrders("admin");
        assertTrue(response.isSuccess());
        List<PurchaseOrderDTO> result = response.getData();
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
        var response = adminService.GetAllPurchasedOrders(NOT_ADMIN);
        assertFalse(response.isSuccess());
        assertNull(response.getData());
    }

    @Test
    void CloseCompany_NotifiesOwnersAndManagers() {
        String company = "Company1";
        Owner owner = new Owner("ownerUsername", company, "SYSTEM_FOUNDER");
        Manager manager = new Manager("managerUsername", company, Set.of(), "ownerUsername");

        User ownerUser = mock(User.class);
        User managerUser = mock(User.class);
        when(ownerUser.getID()).thenReturn("owner-uuid");
        when(managerUser.getID()).thenReturn("manager-uuid");
        when(treeOfRoleRepository.getAllOwnersByCompany(company)).thenReturn(List.of(owner));
        when(treeOfRoleRepository.getAllManagersByCompany(company)).thenReturn(List.of(manager));
        when(userRepository.getUserByUsername("ownerUsername")).thenReturn(ownerUser);
        when(userRepository.getUserByUsername("managerUsername")).thenReturn(managerUser);

        adminService.CloseCompany(company, ADMIN_NAME);

        verify(notifier).notifyUser(eq("owner-uuid"), anyString(), anyString());
        verify(notifier).notifyUser(eq("manager-uuid"), anyString(), anyString());
    }

    @Test
    void SuspendUser_Temporary_Success(){
        String targetUser="user1";
        int durationDays=7;
        User mockUser=mock(User.class);

        when(userRepository.getUserByID(targetUser)).thenReturn(mockUser);

        var response = adminService.suspendUser(targetUser,ADMIN_NAME,durationDays);

        assertTrue((response.isSuccess()));
        assertEquals("success",response.getData());

        verify(userRepository).addCurrentSuspension(eq(targetUser), any(Suspension.class));
        verify(notifier).notifyUser(eq(targetUser), eq("Account Suspended"), contains("until"));
    }

    @Test
    void SuspendUser_Permanently_Success(){
        String targetUser="user1";
        int durationDays=0;
        User mockUser=mock(User.class);

        when(userRepository.getUserByID(targetUser)).thenReturn(mockUser);

        var response = adminService.suspendUser(targetUser,ADMIN_NAME,durationDays);

        assertTrue((response.isSuccess()));
        assertEquals("success",response.getData());

        verify(userRepository).addCurrentSuspension(eq(targetUser), any(Suspension.class));
        verify(notifier).notifyUser(eq(targetUser), eq("Account Suspended"), contains("for good"));
    }

    @Test
    void SuspendUser_Fail_NotAdmin(){
        String targetUser = "user1";

        var response = adminService.suspendUser(targetUser, NOT_ADMIN, 5);

        assertFalse(response.isSuccess());
        assertEquals("Admin does not exist", response.getMessage());
        verify(userRepository, never()).addCurrentSuspension(anyString(), any());
    }

    @Test
    void suspendUser_Fail_UserNotFound() {
        String targetUser = "missingUser";
        when(userRepository.getUserByID(targetUser)).thenReturn(null);

        var response = adminService.suspendUser(targetUser, ADMIN_NAME, 5);

        assertFalse(response.isSuccess());
        assertEquals("User not found", response.getMessage());
        verify(userRepository, never()).addCurrentSuspension(anyString(), any());
    }

    @Test
    void cancelSuspension_Success() {
        String targetUser = "user1";
        User mockUser = mock(User.class);

        when(userRepository.getUserByID(targetUser)).thenReturn(mockUser);

        var response = adminService.cancelSuspension(targetUser, ADMIN_NAME);

        assertTrue(response.isSuccess());
        assertEquals("success", response.getData());

        verify(userRepository).cancelSuspension(targetUser);
        verify(notifier).notifyUser(eq(targetUser), eq("Account is no longer suspended"), anyString());
    }

    @Test
    void cancelSuspension_Fail_UserDoesNotExist() {
        String targetUser = "missingUser";
        when(userRepository.getUserByID(targetUser)).thenReturn(null);

        var response = adminService.cancelSuspension(targetUser, ADMIN_NAME);

        assertFalse(response.isSuccess());
        assertEquals("User does not exist", response.getMessage());
        verify(userRepository, never()).cancelSuspension(anyString());
    }
}
