package Appliction;

import Domain.AdminAggregate.iAdminRepository;
import Domain.Company.iCompanyRepository;
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

class AdminServiceTest {

    @Mock private iCompanyRepository companyRepository;
    @Mock private iTreeOfRoleRepository treeOfRoleRepository;
    @Mock private iAdminRepository adminRepository;
    @Mock private IUserRepository userRepository;
    @Mock private iPurchasedOrderRepository purchasedOrderRepository;
    @Mock private iTicketRepository ticketRepository;

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


}