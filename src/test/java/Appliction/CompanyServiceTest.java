package Appliction;

import Domain.Company.Company;
import Domain.Company.iCompanyRepository;
import Domain.OwnerManagerTree.*;

import Domain.User.IUserRepository;
import Infastructure.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompanyServiceTest {

    @Mock
    private iCompanyRepository companyRepository;
    @Mock
    private IUserRepository userRepository;
    @Mock
    private iTreeOfRoleRepository treeOfRoleRepository;
    @Mock
    private TokenService tokenService;

    @InjectMocks
    private CompanyService companyService;

    private final String TOKEN = "test_token";
    private final String PASSWORD = "test_password";
    private final String USERNAME = "test_user";
    private final String COMPANY = "test_company";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createCompany_Success() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(userRepository.userExists(USERNAME)).thenReturn(true);

        String status=companyService.CreateCompany(COMPANY, TOKEN);
        assertEquals(status, "success");

        verify(companyRepository).store(COMPANY, USERNAME);
        verify(treeOfRoleRepository).storeOwner(USERNAME, COMPANY,"Administrator");
    }

    @Test
    void createCompany_InvalidToken() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);

        String status=companyService.CreateCompany(COMPANY, TOKEN);
        assertEquals(status, "failed");

        verifyNoInteractions(companyRepository);
        verifyNoInteractions(treeOfRoleRepository);
    }

    @Test
    void createCompany_UserNotFound() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);

        companyService.CreateCompany(COMPANY, TOKEN);
        String status=companyService.CreateCompany(COMPANY, TOKEN);
        assertEquals(status, "failed");

        verify(companyRepository, never()).store(anyString(), anyString());
        verify(treeOfRoleRepository, never()).storeOwner(anyString(), anyString(), anyString());
    }
    @Test
    void appointAManager_Success() {
        String managerName = "new_manager";
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);

        Owner mockOwner = spy(new Owner(USERNAME, COMPANY,"Administrator"));
        Manager expectedManager = new Manager(managerName, COMPANY, permissions,USERNAME);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);

        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);
        when(userRepository.userExists(managerName)).thenReturn(true);
        String result=companyService.AppointAManager(managerName, COMPANY, permissions, TOKEN);
        assertEquals(result, "success");
        verify(treeOfRoleRepository).storeManager(eq(managerName), eq(COMPANY), eq(permissions), eq(USERNAME));

    }
    @Test
    void appointAManager_Failure_NotAuthorizedAsOwner() {
        String managerName = "new_manager";
        Company mockCompany = mock(Company.class);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);

//        doThrow(new RuntimeException("not authorized")).when(mockCompany).isAuthorizedAsOwner(USERNAME);

        String result=companyService.AppointAManager(managerName, COMPANY, new HashSet<>(), TOKEN);
        assertEquals(result,"failed");
        verify(treeOfRoleRepository, never()).storeManager(anyString(), anyString(), anySet(),anyString());

    }
    @Test
    void approveAppointmentForManager_Success() {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        Manager mockManager = spy(new Manager(USERNAME, COMPANY, permissions,"Administrator"));

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.getManager(USERNAME, COMPANY)).thenReturn(mockManager);

        String result=companyService.ApproveAppointmentForManager(TOKEN, COMPANY);
        assertEquals(result, "success");
        verify(mockManager).acceptAppointment();
        verify(treeOfRoleRepository).save(mockManager);
        assertEquals(true, mockManager.isAccepted());
        assertTrue(treeOfRoleRepository.getManager(USERNAME, COMPANY).isAccepted());

    }
    @Test
    void approveAppointmentForManager_Failure_ManagerNotFound() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.getManager(USERNAME, COMPANY)).thenReturn(null);

        String result=companyService.ApproveAppointmentForManager(TOKEN, COMPANY);
        assertEquals(result, "failed");
        verify(treeOfRoleRepository, never()).save(any(Manager.class));
    }
    @Test
    void approveAppointmentForManager_Failure_AlreadyAccepted() {
        Set<Permission> permissions = new HashSet<>();
        Manager mockManager = spy(new Manager(USERNAME, COMPANY, permissions,"Administrator"));
        mockManager.acceptAppointment();

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.getManager(USERNAME, COMPANY)).thenReturn(mockManager);

        String result=companyService.ApproveAppointmentForManager(TOKEN, COMPANY);
        assertEquals(result, "failed");
        verify(treeOfRoleRepository, never()).save(any(Manager.class));
        assertEquals(true, mockManager.isAccepted());
        assertTrue(treeOfRoleRepository.getManager(USERNAME, COMPANY).isAccepted());

    }
    @Test
    void rejectAppointmentForManager_Success() {
        Manager mockManager = mock(Manager.class);
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.isManager(USERNAME, COMPANY)).thenReturn(true);

        String result=companyService.RejectAppointmentForManager(TOKEN, COMPANY);
        assertEquals("success",result);
        verify(treeOfRoleRepository).deleteManager(USERNAME, COMPANY);


    }

    @Test
    void rejectAppointmentForManager_Failure_UserNotFound() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);

        String result=companyService.RejectAppointmentForManager(TOKEN, COMPANY);
        assertEquals("failed",result);
        verify(treeOfRoleRepository, never()).deleteManager(anyString(), anyString());
    }
    @Test
    void rejectAppointmentForManager_Failure_ManagerNotFound() {
        Manager mockManager = mock(Manager.class);
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.isManager(USERNAME, COMPANY)).thenReturn(false);

        String result=companyService.RejectAppointmentForManager(TOKEN, COMPANY);
        assertEquals("failed",result);

    }
    @Test
    void appointOwner_Success() {
        String newOwnerName = "new_owner";
        Owner mockOwner = mock(Owner.class);
        Owner expectedNewOwner = new Owner(newOwnerName, COMPANY, USERNAME);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);
        when(userRepository.userExists(newOwnerName)).thenReturn(true);
        String result1=companyService.AppointOwner(newOwnerName, COMPANY, TOKEN);
        assertEquals(result1, "success");
        verify(treeOfRoleRepository).storeOwner(newOwnerName, COMPANY, USERNAME);


    }
    @Test
    void appointOwner_Failure_OwnerNotFoundInTree() {
        String newOwnerName = "new_owner";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);

        String result=companyService.AppointOwner(newOwnerName, COMPANY, TOKEN);
        assertEquals(result, "failed");
        verify(treeOfRoleRepository, never()).storeOwner(anyString(), anyString(), anyString());
    }



}