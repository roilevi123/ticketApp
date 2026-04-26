package Appliction;

import Domain.AdminAggregate.iAdminRepository;
import Domain.Company.iCompanyRepository;
import Domain.Domains.AdminDomain;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.PurchasedOrderAggregate.PurchaseOrder;
import Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import Domain.Ticket.iTicketRepository;
import Domain.User.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AdminDomainTest {

    @Mock private iCompanyRepository companyRepository;
    @Mock private iTreeOfRoleRepository treeOfRoleRepository;
    @Mock private iAdminRepository adminRepository;
    @Mock private IUserRepository userRepository;
    @Mock private iPurchasedOrderRepository purchasedOrderRepository;
    @Mock private iTicketRepository ticketRepository;
    @Mock private Domain.Event.iEventRepository eventRepository;
    @InjectMocks
    private AdminDomain adminDomain;

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

        adminDomain.CloseCompany(company, ADMIN_NAME);

        verify(companyRepository).deleteCompany(company);
        verify(treeOfRoleRepository).deleteCompanyMangersAndOwners(company);
    }

    @Test
    void CloseCompany_Fail_NotAdmin() {
        String company = "Company1";

        adminDomain.CloseCompany(company, NOT_ADMIN);

        verify(companyRepository, never()).deleteCompany(anyString());
    }

    @Test
    void removeUser_Success() {
        String user = "user1";

        adminDomain.removeUser(user, ADMIN_NAME);

        verify(userRepository).deleteUser(user);
        verify(treeOfRoleRepository).deleteUserRoles(user);
    }

    @Test
    void removeUser_Fail_NotAdmin() {
        String user = "user1";

        adminDomain.removeUser(user, NOT_ADMIN);

        verify(userRepository, never()).deleteUser(anyString());
        verify(treeOfRoleRepository, never()).deleteUserRoles(anyString());
    }
    @Test
    void GetAllPurchasedOrders_Success() {
        List<String> tickets = List.of("T1");
        PurchaseOrder po = new PurchaseOrder("Comp", "Ev", tickets, "Buyer", "O1");

        when(purchasedOrderRepository.GetAllPurchasedOrders()).thenReturn(List.of(po));
        when(ticketRepository.getTicketsDescription(tickets)).thenReturn("Desc1");

        String result = adminDomain.GetAllPurchasedOrders(ADMIN_NAME);

        assertTrue(result.contains("Comp"));
        assertTrue(result.contains("Ev"));
        assertTrue(result.contains("Buyer"));
        assertTrue(result.contains("Desc1"));
    }

    @Test
    void GetAllPurchasedOrders_Fail_NotAdmin() {
        String result = adminDomain.GetAllPurchasedOrders(NOT_ADMIN);


}}