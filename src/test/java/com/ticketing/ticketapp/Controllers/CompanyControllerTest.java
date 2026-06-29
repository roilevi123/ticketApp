package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrderDTO;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.SalesReportDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyControllerTest {

    @Mock private EventService eventService;
    @Mock private CompanyService companyService;
    @Mock private PurchasePolicyService purchasePolicyService;
    @Mock private DiscountService discountService;
    @Mock private PurchasedService purchasedService;
    @InjectMocks
    private CompanyController companyController;

    private static final String TOKEN = "test-token";

    // --- openCompany ---

    @Test
    void openCompany_Success_Returns200() {
        when(companyService.CreateCompany("MyCompany", TOKEN)).thenReturn(Response.success("ok"));

        CompanyRequestDTO dto = new CompanyRequestDTO();
        dto.setCompanyName("MyCompany");

        ResponseEntity<?> response = companyController.openCompany(TOKEN, dto);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void openCompany_Failure_Returns400() {
        when(companyService.CreateCompany("MyCompany", TOKEN)).thenReturn(Response.error("Name taken"));

        CompanyRequestDTO dto = new CompanyRequestDTO();
        dto.setCompanyName("MyCompany");

        ResponseEntity<?> response = companyController.openCompany(TOKEN, dto);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Name taken", ((Map<?,?>)response.getBody()).get("error"));
    }

    // --- assignRole ---

    @Test
    void assignRole_AsOwner_Success_Returns200() {
        when(companyService.AppointOwner("user42", "MyCompany", TOKEN))
                .thenReturn(Response.success("appointed"));

        AssignRoleRequestDTO dto = new AssignRoleRequestDTO();
        dto.setTargetUserId("user42");
        dto.setCompanyName("MyCompany");
        dto.setRole("OWNER");

        ResponseEntity<?> response = companyController.assignRole(TOKEN, dto);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Role assigned successfully pending approval",
                ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void assignRole_AsManager_Success_Returns200() {
        when(companyService.AppointAManager(eq("user42"), eq("MyCompany"), any(), eq(TOKEN)))
                .thenReturn(Response.success("appointed"));

        AssignRoleRequestDTO dto = new AssignRoleRequestDTO();
        dto.setTargetUserId("user42");
        dto.setCompanyName("MyCompany");
        dto.setRole("MANAGER");

        ResponseEntity<?> response = companyController.assignRole(TOKEN, dto);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void assignRole_InvalidRole_Returns400() {
        AssignRoleRequestDTO dto = new AssignRoleRequestDTO();
        dto.setTargetUserId("user42");
        dto.setCompanyName("MyCompany");
        dto.setRole("ADMIN");

        ResponseEntity<?> response = companyController.assignRole(TOKEN, dto);

        assertEquals(400, response.getStatusCode().value());
    }

    // --- removeOwner ---

    @Test
    void removeOwner_Success_Returns200() {
        when(companyService.FireOwner(TOKEN, "MyCompany", "owner42"))
                .thenReturn(Response.success("removed"));

        ResponseEntity<?> response = companyController.removeOwner(TOKEN, "owner42", "MyCompany");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Owner removed successfully", ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void removeOwner_Failure_Returns400() {
        when(companyService.FireOwner(TOKEN, "MyCompany", "owner42"))
                .thenReturn(Response.error("Not authorized"));

        ResponseEntity<?> response = companyController.removeOwner(TOKEN, "owner42", "MyCompany");

        assertEquals(400, response.getStatusCode().value());
    }

    // --- removeManager ---

    @Test
    void removeManager_Success_Returns200() {
        when(companyService.FireManager(TOKEN, "MyCompany", "mgr42"))
                .thenReturn(Response.success("removed"));

        ResponseEntity<?> response = companyController.removeManager(TOKEN, "mgr42", "MyCompany");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Manager removed successfully", ((Map<?, ?>) response.getBody()).get("message"));
    }

    // --- relinquishOwnership ---

    @Test
    void relinquishOwnership_Success_Returns200() {
        when(companyService.RejectAppointmentForOwner(TOKEN, "MyCompany"))
                .thenReturn(Response.success("done"));

        ResponseEntity<?> response = companyController.relinquishOwnership(TOKEN, "MyCompany");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Ownership relinquished successfully", ((Map<?, ?>) response.getBody()).get("message"));
    }

    // --- suspendCompany / reopenCompany ---

    @Test
    void suspendCompany_Success_Returns200() {
        when(companyService.freezeCompany("MyCompany", TOKEN)).thenReturn(Response.success("frozen"));

        ResponseEntity<?> response = companyController.suspendCompany(TOKEN, "MyCompany");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Company suspended successfully", ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void reopenCompany_Success_Returns200() {
        when(companyService.unfreezeCompany("MyCompany", TOKEN)).thenReturn(Response.success("unfrozen"));

        ResponseEntity<?> response = companyController.reopenCompany(TOKEN, "MyCompany");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Company reopened successfully", ((Map<?, ?>) response.getBody()).get("message"));
    }

    // --- getRoleHierarchyTree ---

    @Test
    void getRoleHierarchy_Success_Returns200() {
        when(companyService.GetRoleTreeString(TOKEN, "MyCompany")).thenReturn(Response.success("root->mgr"));

        ResponseEntity<?> response = companyController.getRoleHierarchyTree(TOKEN, "MyCompany");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("root->mgr", ((Map<?, ?>) response.getBody()).get("tree"));
    }

    @Test
    void getRoleHierarchy_Failure_Returns400() {
        when(companyService.GetRoleTreeString(TOKEN, "MyCompany")).thenReturn(Response.error("Not authorized"));

        ResponseEntity<?> response = companyController.getRoleHierarchyTree(TOKEN, "MyCompany");

        assertEquals(400, response.getStatusCode().value());
    }

    // --- getPurchaseHistory ---

    @Test
    void getCompanyPurchaseHistory_Success_Returns200() {
        PurchaseOrderDTO order = new PurchaseOrderDTO("o1", "buyer1", "MyCompany", "EventX", List.of());
        when(purchasedService.getCompanyTransaction("MyCompany", TOKEN))
                .thenReturn(Response.success(List.of(order)));

        ResponseEntity<?> response = companyController.getPurchaseHistory(TOKEN, "MyCompany");

        assertEquals(200, response.getStatusCode().value());
    }

    // --- getSubTreeSalesReport ---
//
//    @Test
//    void getSubTreeSalesReport_Success_Returns200() {
//        SalesReportDTO report = new SalesReportDTO(1000.0, 50, List.of());
//        when(purchasedService.getSubTreeSalesReport(TOKEN, "MyCompany"))
//                .thenReturn(Response.success(report));
//
//        ResponseEntity<?> response = companyController.getSubTreeSalesReport(TOKEN, "MyCompany");
//
//        assertEquals(200, response.getStatusCode().value());
//    }

    // --- replyToBuyerMessage ---

    @Test
    void replyToBuyerMessage_Success_Returns200() {
        when(companyService.replyToBuyer(TOKEN, "MyCompany", "buyer1", "Hello"))
                .thenReturn(Response.success("sent"));

        ReplyMessageRequestDTO dto = new ReplyMessageRequestDTO();
        dto.setCompanyName("MyCompany");
        dto.setBuyerId("buyer1");
        dto.setMessage("Hello");

        ResponseEntity<?> response = companyController.replyToBuyerMessage(TOKEN, dto);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Reply sent successfully to the buyer", ((Map<?, ?>) response.getBody()).get("message"));
    }
}
