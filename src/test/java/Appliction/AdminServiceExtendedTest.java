package Appliction;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.Order.ActiveOrder;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrder;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminServiceExtendedTest {

    @Mock private iCompanyRepository companyRepository;
    @Mock private iTreeOfRoleRepository treeOfRoleRepository;
    @Mock private iAdminRepository adminRepository;
    @Mock private IUserRepository userRepository;
    @Mock private iPurchasedOrderRepository purchasedOrderRepository;
    @Mock private iTicketRepository ticketRepository;
    @Mock private iEventRepository eventRepository;
    @Mock private TokenService tokenService;
    @Mock private INotifier notifier;
    @Mock private IActiveOrderRepository activeOrderRepository;
    @InjectMocks private AdminService adminService;

    private static final String ADMIN = "admin";
    private static final String NOT_ADMIN = "notAdmin";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(adminRepository.isAdmin(ADMIN)).thenReturn(true);
        when(adminRepository.isAdmin(NOT_ADMIN)).thenReturn(false);
    }

    // ── sendMessageToUser (was missing @Test in original file) ────────────────

    @Test
    void sendMessageToUser_Success_NotifiesUser() {
        var result = adminService.sendMessageToUser(ADMIN, "user42", "Hello");
        assertTrue(result.isSuccess());
        verify(notifier).notifyUser(eq("user42"), anyString(), eq("Hello"));
    }

    // ── getPurchaseHistory ────────────────────────────────────────────────────

    @Test
    void getPurchaseHistory_NotAdmin_ReturnsError() {
        var result = adminService.getPurchaseHistory(NOT_ADMIN, null, null, null);
        assertTrue(result.isError());
        verify(purchasedOrderRepository, never()).GetAllPurchasedOrders();
        verify(purchasedOrderRepository, never()).getPurchasedOrdersForCompany(any());
        verify(purchasedOrderRepository, never()).getPurchasedOrdersForUser(any());
    }

    @Test
    void getPurchaseHistory_FilterByCompany_QueriesCompanyRepo() {
        PurchaseOrder po = new PurchaseOrder("Corp", "Event", List.of(), "buyer", "o1");
        when(purchasedOrderRepository.getPurchasedOrdersForCompany("Corp")).thenReturn(List.of(po));
        when(ticketRepository.getTickets(any())).thenReturn(List.of());

        var result = adminService.getPurchaseHistory(ADMIN, null, "Corp", null);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        verify(purchasedOrderRepository).getPurchasedOrdersForCompany("Corp");
        verify(purchasedOrderRepository, never()).GetAllPurchasedOrders();
    }

    @Test
    void getPurchaseHistory_FilterByBuyerId_QueriesUserRepo() {
        PurchaseOrder po = new PurchaseOrder("Corp", "Event", List.of(), "buyer1", "o1");
        when(purchasedOrderRepository.getPurchasedOrdersForUser("buyer1")).thenReturn(List.of(po));
        when(ticketRepository.getTickets(any())).thenReturn(List.of());

        var result = adminService.getPurchaseHistory(ADMIN, "buyer1", null, null);

        assertTrue(result.isSuccess());
        verify(purchasedOrderRepository).getPurchasedOrdersForUser("buyer1");
        verify(purchasedOrderRepository, never()).GetAllPurchasedOrders();
        verify(purchasedOrderRepository, never()).getPurchasedOrdersForCompany(any());
    }

    @Test
    void getPurchaseHistory_NoFilters_ReturnsAll() {
        when(purchasedOrderRepository.GetAllPurchasedOrders()).thenReturn(List.of());

        var result = adminService.getPurchaseHistory(ADMIN, null, null, null);

        assertTrue(result.isSuccess());
        verify(purchasedOrderRepository).GetAllPurchasedOrders();
    }

    @Test
    void getPurchaseHistory_BlankCompany_FallsThroughToAll() {
        when(purchasedOrderRepository.GetAllPurchasedOrders()).thenReturn(List.of());

        var result = adminService.getPurchaseHistory(ADMIN, null, "   ", null);

        assertTrue(result.isSuccess());
        verify(purchasedOrderRepository).GetAllPurchasedOrders();
    }

    @Test
    void getPurchaseHistory_FilterByEventId_FiltersResults() {
        PurchaseOrder match = new PurchaseOrder("Corp", "TargetEvent", List.of(), "buyer", "o1");
        PurchaseOrder noMatch = new PurchaseOrder("Corp", "OtherEvent", List.of(), "buyer", "o2");
        when(purchasedOrderRepository.GetAllPurchasedOrders()).thenReturn(List.of(match, noMatch));
        when(ticketRepository.getTickets(any())).thenReturn(List.of());

        var result = adminService.getPurchaseHistory(ADMIN, null, null, "TargetEvent");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals("buyer", result.getData().get(0).buyer());
    }

    @Test
    void getPurchaseHistory_CompanyFilterWithEventId_FiltersCorrectly() {
        PurchaseOrder match = new PurchaseOrder("Corp", "TargetEvent", List.of(), "buyer", "o1");
        PurchaseOrder noMatch = new PurchaseOrder("Corp", "OtherEvent", List.of(), "buyer", "o2");
        when(purchasedOrderRepository.getPurchasedOrdersForCompany("Corp")).thenReturn(List.of(match, noMatch));
        when(ticketRepository.getTickets(any())).thenReturn(List.of());

        var result = adminService.getPurchaseHistory(ADMIN, null, "Corp", "TargetEvent");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
    }

    // ── getSystemAnalytics ────────────────────────────────────────────────────

    @Test
    void getSystemAnalytics_Success_ReturnsTotals() {
        when(purchasedOrderRepository.GetAllPurchasedOrders()).thenReturn(List.of(
                new PurchaseOrder("C", "E", List.of(), "b", "o1"),
                new PurchaseOrder("C", "E", List.of(), "b", "o2")
        ));
        when(activeOrderRepository.getAllActiveOrders()).thenReturn(
                List.of(new ActiveOrder("C", "E", List.of(), "u", "ao1", new Date()))
        );

        var result = adminService.getSystemAnalytics(ADMIN);

        assertTrue(result.isSuccess());
        Map<String, Long> data = result.getData();
        assertEquals(2L, data.get("totalPurchases"));
        assertEquals(1L, data.get("activeOrders"));
    }

    @Test
    void getSystemAnalytics_NotAdmin_ReturnsError() {
        var result = adminService.getSystemAnalytics(NOT_ADMIN);
        assertTrue(result.isError());
        verify(purchasedOrderRepository, never()).GetAllPurchasedOrders();
    }

    // ── isAdmin ───────────────────────────────────────────────────────────────

    @Test
    void isAdmin_ReturnsTrue_ForAdmin() {
        assertTrue(adminService.isAdmin(ADMIN));
    }

    @Test
    void isAdmin_ReturnsFalse_ForNonAdmin() {
        assertFalse(adminService.isAdmin(NOT_ADMIN));
    }

    // ── broadcastMessage ──────────────────────────────────────────────────────

    @Test
    void broadcastMessage_Success_BroadcastsToAll() {
        var result = adminService.broadcastMessage(ADMIN, "Alert", "System maintenance");
        assertTrue(result.isSuccess());
        verify(notifier).broadcast("Alert", "System maintenance");
    }

    @Test
    void broadcastMessage_NotAdmin_ReturnsError() {
        var result = adminService.broadcastMessage(NOT_ADMIN, "Alert", "Message");
        assertTrue(result.isError());
        verify(notifier, never()).broadcast(any(), any());
    }
}
