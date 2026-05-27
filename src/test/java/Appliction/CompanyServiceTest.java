package Appliction;

import com.ticketing.ticketapp.Appliction.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.ticketing.ticketapp.Domain.Company.Company;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Manager;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Owner;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Permission;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.User.User;
import com.ticketing.ticketapp.Infastructure.TokenService;

class CompanyServiceTest {

    @Mock
    private iCompanyRepository companyRepository;
    @Mock
    private IUserRepository userRepository;
    @Mock
    private iTreeOfRoleRepository treeOfRoleRepository;
    @Mock
    private TokenService tokenService;
    @Mock
    private INotifier notifier;
    @Mock
    private IPendingNotificationRepository notificationRepository;

    @InjectMocks
    private CompanyService companyService;

    private final String TOKEN = "test_token";
    private final String USERNAME = "test_user";
    private final String COMPANY = "test_company";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createCompany_Success() {
        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn(USERNAME);
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.getUserByID(USERNAME)).thenReturn(mockUser);

        Response<String> status = companyService.CreateCompany(COMPANY, TOKEN);
        assertTrue(status.isSuccess());
        assertEquals("success", status.getData());

        verify(companyRepository).store(COMPANY, USERNAME);
        verify(treeOfRoleRepository).storeOwner(USERNAME, COMPANY, "SYSTEM_FOUNDER");
    }

    @Test
    void createCompany_InvalidToken() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);

        Response<String> status = companyService.CreateCompany(COMPANY, TOKEN);
        assertFalse(status.isSuccess());

        verifyNoInteractions(companyRepository);
        verifyNoInteractions(treeOfRoleRepository);
    }

    @Test
    void createCompany_UserNotFound() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.getUserByID(USERNAME)).thenReturn(null);

        Response<String> status = companyService.CreateCompany(COMPANY, TOKEN);
        assertFalse(status.isSuccess());

        verify(companyRepository, never()).store(anyString(), anyString());
        verify(treeOfRoleRepository, never()).storeOwner(anyString(), anyString(), anyString());
    }

    @Test
    void appointAManager_Success() {
        String managerName = "new_manager";
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);
        when(userRepository.usernameExists(managerName)).thenReturn(true);

        Response<String> result = companyService.AppointAManager(managerName, COMPANY, permissions, TOKEN);
        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());
        verify(treeOfRoleRepository).storeManager(eq(managerName), eq(COMPANY), eq(permissions), eq(USERNAME));
    }

    @Test
    void appointAManager_Failure_NotAuthorizedAsOwner() {
        String managerName = "new_manager";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);

        Response<String> result = companyService.AppointAManager(managerName, COMPANY, new HashSet<>(), TOKEN);
        assertFalse(result.isSuccess());
        verify(treeOfRoleRepository, never()).storeManager(anyString(), anyString(), anySet(), anyString());
    }

    @Test
    void approveAppointmentForManager_Success() {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        Manager mockManager = spy(new Manager(USERNAME, COMPANY, permissions, "Administrator"));

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.getManager(USERNAME, COMPANY)).thenReturn(mockManager);

        Response<String> result = companyService.ApproveAppointmentForManager(TOKEN, COMPANY);
        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());
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

        Response<String> result = companyService.ApproveAppointmentForManager(TOKEN, COMPANY);
        assertFalse(result.isSuccess());
        verify(treeOfRoleRepository, never()).save(any(Manager.class));
    }

    @Test
    void approveAppointmentForManager_Failure_AlreadyAccepted() {
        Set<Permission> permissions = new HashSet<>();
        Manager mockManager = spy(new Manager(USERNAME, COMPANY, permissions, "Administrator"));
        mockManager.acceptAppointment();

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.getManager(USERNAME, COMPANY)).thenReturn(mockManager);

        Response<String> result = companyService.ApproveAppointmentForManager(TOKEN, COMPANY);
        assertFalse(result.isSuccess());
        verify(treeOfRoleRepository, never()).save(any(Manager.class));
        assertEquals(true, mockManager.isAccepted());
        assertTrue(treeOfRoleRepository.getManager(USERNAME, COMPANY).isAccepted());
    }

    @Test
    void rejectAppointmentForManager_Success() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.isManager(USERNAME, COMPANY)).thenReturn(true);

        Response<String> result = companyService.RejectAppointmentForManager(TOKEN, COMPANY);
        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());
        verify(treeOfRoleRepository).deleteManager(USERNAME, COMPANY);
    }

    @Test
    void rejectAppointmentForManager_Failure_UserNotFound() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);

        Response<String> result = companyService.RejectAppointmentForManager(TOKEN, COMPANY);
        assertFalse(result.isSuccess());
        verify(treeOfRoleRepository, never()).deleteManager(anyString(), anyString());
    }

    @Test
    void rejectAppointmentForManager_Failure_ManagerNotFound() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.isManager(USERNAME, COMPANY)).thenReturn(false);

        Response<String> result = companyService.RejectAppointmentForManager(TOKEN, COMPANY);
        assertFalse(result.isSuccess());
    }

    @Test
    void appointOwner_Success() {
        String newOwnerName = "new_owner";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);
        when(userRepository.usernameExists(newOwnerName)).thenReturn(true);

        Response<String> result = companyService.AppointOwner(newOwnerName, COMPANY, TOKEN);
        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());
        verify(treeOfRoleRepository).storeOwner(newOwnerName, COMPANY, USERNAME);
    }

    @Test
    void appointOwner_Failure_OwnerNotFoundInTree() {
        String newOwnerName = "new_owner";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);

        Response<String> result = companyService.AppointOwner(newOwnerName, COMPANY, TOKEN);
        assertFalse(result.isSuccess());
        verify(treeOfRoleRepository, never()).storeOwner(anyString(), anyString(), anyString());
    }

    @Test
    void approveAppointmentForOwner_Success() {
        Owner mockOwner = spy(new Owner(USERNAME, COMPANY, "appointer_user"));

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.getOwner(USERNAME, COMPANY)).thenReturn(mockOwner);

        Response<String> result = companyService.ApproveAppointmentForOwner(TOKEN, COMPANY);
        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());
        verify(mockOwner).acceptAppointment();
        verify(treeOfRoleRepository).save(mockOwner);
        assertTrue(mockOwner.isAccepted());
        assertTrue(treeOfRoleRepository.getOwner(USERNAME, COMPANY).isAccepted());
    }

    @Test
    void approveAppointmentForOwner_Failure_OwnerNotFound() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.getOwner(USERNAME, COMPANY)).thenReturn(null);

        Response<String> result = companyService.ApproveAppointmentForOwner(TOKEN, COMPANY);
        assertFalse(result.isSuccess());
        verify(treeOfRoleRepository, never()).save(any(Owner.class));
    }

    @Test
    void approveAppointmentForOwner_Failure_AlreadyAccepted() {
        Owner mockOwner = spy(new Owner(USERNAME, COMPANY, "appointer_user"));
        mockOwner.acceptAppointment();

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.getOwner(USERNAME, COMPANY)).thenReturn(mockOwner);

        Response<String> result = companyService.ApproveAppointmentForOwner(TOKEN, COMPANY);
        assertFalse(result.isSuccess());
        verify(treeOfRoleRepository, never()).save(any(Owner.class));
        assertTrue(mockOwner.isAccepted());
        assertTrue(treeOfRoleRepository.getOwner(USERNAME, COMPANY).isAccepted());
    }

    @Test
    void rejectAppointmentForOwner_Success() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.isOwner(USERNAME, COMPANY)).thenReturn(true);
        when(companyRepository.getCompanyFounder(COMPANY)).thenReturn("different_user");

        Response<String> result = companyService.RejectAppointmentForOwner(TOKEN, COMPANY);
        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());
        verify(treeOfRoleRepository).deleteOwner(USERNAME, COMPANY);
        assertNull(treeOfRoleRepository.getOwner(USERNAME, COMPANY));
    }

    @Test
    void rejectAppointmentForOwner_Failure_IsFounder() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.isOwner(USERNAME, COMPANY)).thenReturn(true);
        when(companyRepository.getCompanyFounder(COMPANY)).thenReturn(USERNAME);

        Response<String> result = companyService.RejectAppointmentForOwner(TOKEN, COMPANY);
        assertFalse(result.isSuccess());
        verify(treeOfRoleRepository, never()).deleteOwner(anyString(), anyString());
    }

    @Test
    void rejectAppointmentForOwner_Failure_InvalidToken() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);

        Response<String> result = companyService.RejectAppointmentForOwner(TOKEN, COMPANY);
        assertFalse(result.isSuccess());
        verify(treeOfRoleRepository, never()).deleteOwner(anyString(), anyString());
    }

    @Test
    void fireOwner_Success() {
        String ownerToFire = "owner_to_fire";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.isAppointerOwner(ownerToFire, COMPANY, USERNAME)).thenReturn(true);

        Response<String> res = companyService.FireOwner(TOKEN, COMPANY, ownerToFire);
        assertTrue(res.isSuccess());
        assertEquals("success", res.getData());
        verify(treeOfRoleRepository).deleteOwner(ownerToFire, COMPANY);
        assertNull(treeOfRoleRepository.getOwner(ownerToFire, COMPANY));
    }

    @Test
    void fireOwner_Failure_NotTheAppointer() {
        String ownerToFire = "owner_to_fire";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.isAppointerOwner(ownerToFire, COMPANY, USERNAME)).thenReturn(false);

        Response<String> res = companyService.FireOwner(TOKEN, COMPANY, ownerToFire);
        assertFalse(res.isSuccess());
        verify(treeOfRoleRepository, never()).deleteOwner(anyString(), anyString());
    }

    @Test
    void fireManager_Success() {
        String managerToFire = "manager_to_fire";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.isAppointerManager(managerToFire, COMPANY, USERNAME)).thenReturn(true);

        Response<String> res = companyService.FireManager(TOKEN, COMPANY, managerToFire);
        assertTrue(res.isSuccess());
        assertEquals("success", res.getData());
        verify(treeOfRoleRepository).deleteManager(managerToFire, COMPANY);
    }

    @Test
    void fireManager_Failure_NotTheAppointer() {
        String managerToFire = "manager_to_fire";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.isAppointerManager(managerToFire, COMPANY, USERNAME)).thenReturn(false);

        Response<String> res = companyService.FireManager(TOKEN, COMPANY, managerToFire);
        assertFalse(res.isSuccess());
        verify(treeOfRoleRepository, never()).deleteManager(anyString(), anyString());
    }

    @Test
    void changeManagerPermissions_Success() {
        String managerName = "sub_manager";
        Set<Permission> newPermissions = Set.of(Permission.MANAGE_INVENTORY);
        Manager mockManager = spy(new Manager(managerName, COMPANY, new HashSet<>(), USERNAME));

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.getManager(managerName, COMPANY)).thenReturn(mockManager);
        when(treeOfRoleRepository.isAppointerManager(managerName, COMPANY, USERNAME)).thenReturn(true);

        Response<String> res = companyService.ChangeManagerPermissions(TOKEN, COMPANY, managerName, newPermissions);
        assertTrue(res.isSuccess());
        assertEquals("success", res.getData());
        verify(mockManager).setPermissions(newPermissions);
        verify(treeOfRoleRepository).save(mockManager);
        assertEquals(newPermissions, treeOfRoleRepository.getManager(managerName, COMPANY).getPermissions());
    }

    @Test
    void changeManagerPermissions_Failure_NotTheAppointer() {
        String managerName = "sub_manager";
        String realAppointer = "someone_else";
        Manager mockManager = new Manager(managerName, COMPANY, new HashSet<>(), realAppointer);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.getManager(managerName, COMPANY)).thenReturn(mockManager);
        when(treeOfRoleRepository.isAppointerManager(managerName, COMPANY, USERNAME)).thenReturn(false);

        Response<String> res = companyService.ChangeManagerPermissions(TOKEN, COMPANY, managerName, Set.of(Permission.MANAGE_INVENTORY));
        assertFalse(res.isSuccess());
        verify(treeOfRoleRepository, never()).save(any(Manager.class));
        assertEquals(new HashSet<>(), treeOfRoleRepository.getManager(managerName, COMPANY).getPermissions());
    }

    @Test
    void freezeCompany_Success() {
        Company mockCompany = spy(new Company(COMPANY, USERNAME));
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(companyRepository.getCompany(COMPANY)).thenReturn(mockCompany);

        Response<String> res = companyService.freezeCompany(COMPANY, TOKEN);
        assertTrue(res.isSuccess());
        assertEquals("success", res.getData());
        verify(mockCompany).freezeCompany(USERNAME);
        verify(companyRepository).save(mockCompany);
        assertEquals(false, mockCompany.getActive());
    }

    @Test
    void freezeCompany_Failure_NotFounder() {
        String otherUser = "other_user";
        Company mockCompany = new Company(COMPANY, USERNAME);
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(otherUser);
        when(companyRepository.getCompany(COMPANY)).thenReturn(mockCompany);

        Response<String> res = companyService.freezeCompany(COMPANY, TOKEN);
        assertFalse(res.isSuccess());
        verify(companyRepository, never()).save(any(Company.class));
        assertEquals(true, mockCompany.getActive());
    }

    @Test
    void freezeCompany_CompanyNotFound() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(companyRepository.getCompany(COMPANY)).thenReturn(null);

        Response<String> res = companyService.freezeCompany(COMPANY, TOKEN);
        assertFalse(res.isSuccess());
        verify(companyRepository, never()).save(any(Company.class));
    }

    @Test
    void unfreezeCompany_Success() {
        Company mockCompany = spy(new Company(COMPANY, USERNAME));
        mockCompany.setActive(false);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(companyRepository.getCompany(COMPANY)).thenReturn(mockCompany);

        Response<String> res = companyService.unfreezeCompany(COMPANY, TOKEN);
        assertTrue(res.isSuccess());
        assertEquals("success", res.getData());
        verify(mockCompany).unfreezeCompany(USERNAME);
        verify(companyRepository).save(mockCompany);
        assertEquals(true, mockCompany.getActive());
    }

    @Test
    void UnfreezeCompany_Failure_NotFounder() {
        String otherUser = "other_user";
        Company mockCompany = new Company(COMPANY, USERNAME);
        mockCompany.setActive(false);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(otherUser);
        when(companyRepository.getCompany(COMPANY)).thenReturn(mockCompany);

        Response<String> res = companyService.unfreezeCompany(COMPANY, TOKEN);
        assertFalse(res.isSuccess());
        verify(companyRepository, never()).save(any(Company.class));
        assertEquals(false, mockCompany.getActive());
    }

    @Test
    void unfreezeCompany_Failure_InvalidToken() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);

        Response<String> res = companyService.unfreezeCompany(COMPANY, TOKEN);
        assertFalse(res.isSuccess());
        verifyNoInteractions(companyRepository);
    }

    @Test
    void getManagerPermissions_Success() {
        String managerName = "some_manager";
        Set<Permission> expectedPermissions = Set.of(Permission.MANAGE_INVENTORY, Permission.VIEW_PURCHASE_HISTORY);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);
        when(treeOfRoleRepository.getManagerPermissions(managerName, COMPANY)).thenReturn(expectedPermissions);

        Response<Set<Permission>> result = companyService.GetManagerPermissions(TOKEN, COMPANY, managerName);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(expectedPermissions, result.getData());
        assertEquals(2, result.getData().size());
    }

    @Test
    void getManagerPermissions_Failure_NotAnOwner() {
        String managerName = "some_manager";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);

        Response<Set<Permission>> result = companyService.GetManagerPermissions(TOKEN, COMPANY, managerName);
        assertTrue(result.isError());
        verify(treeOfRoleRepository, never()).getManager(anyString(), anyString());
    }

    @Test
    void freezeCompany_NotifiesOwnersAndManagers() {
        Owner owner = new Owner(USERNAME, COMPANY, "SYSTEM_FOUNDER");
        Manager manager = new Manager("managerUsername", COMPANY, Set.of(), USERNAME);

        User ownerUser = mock(User.class);
        User managerUser = mock(User.class);
        when(ownerUser.getID()).thenReturn("owner-uuid");
        when(managerUser.getID()).thenReturn("manager-uuid");

        Company mockCompany = new Company(COMPANY, USERNAME);
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(companyRepository.getCompany(COMPANY)).thenReturn(mockCompany);
        when(treeOfRoleRepository.getAllOwnersByCompany(COMPANY)).thenReturn(List.of(owner));
        when(treeOfRoleRepository.getAllManagersByCompany(COMPANY)).thenReturn(List.of(manager));
        when(userRepository.getUserByUsername(USERNAME)).thenReturn(ownerUser);
        when(userRepository.getUserByUsername("managerUsername")).thenReturn(managerUser);

        companyService.freezeCompany(COMPANY, TOKEN);

        verify(notifier).notifyUser(eq("owner-uuid"), anyString(), anyString());
        verify(notifier).notifyUser(eq("manager-uuid"), anyString(), anyString());
    }

    @Test
    void replyToBuyer_Success() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);

        Response<String> result = companyService.replyToBuyer(TOKEN, COMPANY, "buyer1", "Hello buyer");

        assertTrue(result.isSuccess());
        verify(notificationRepository, times(1)).save(eq("buyer1"), anyString());
    }

    @Test
    void replyToBuyer_Failure_NotAuthorized() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.isManager(USERNAME, COMPANY)).thenReturn(false);

        Response<String> result = companyService.replyToBuyer(TOKEN, COMPANY, "buyer1", "Hello");

        assertTrue(result.isError());
        verify(notificationRepository, never()).save(any(), any());
    }

    @Test
    void getActiveCompanies_Success() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(companyRepository.getActiveCompanies()).thenReturn(List.of(new Company(COMPANY, USERNAME)));

        Response<java.util.List<com.ticketing.ticketapp.Domain.Company.CompanyDTO>> result =
                companyService.getActiveCompanies(TOKEN);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
    }

    @Test
    void getActiveCompanies_InvalidToken_ReturnsError() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);

        Response<?> result = companyService.getActiveCompanies(TOKEN);

        assertTrue(result.isError());
    }

    @Test
    void getRoleTreeString_Success() {
        Company mockCompany = new Company(COMPANY, USERNAME);
        Owner owner = new Owner(USERNAME, COMPANY, "SYSTEM_FOUNDER");

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.getOwner(USERNAME, COMPANY)).thenReturn(owner);
        when(treeOfRoleRepository.getAllOwnersByCompany(COMPANY)).thenReturn(List.of(owner));
        when(treeOfRoleRepository.getAllManagersByCompany(COMPANY)).thenReturn(List.of());
        when(companyRepository.getCompany(COMPANY)).thenReturn(mockCompany);

        Response<String> result = companyService.GetRoleTreeString(TOKEN, COMPANY);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().contains(USERNAME));
    }

    @Test
    void getRoleTreeString_Failure_NotOwner() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.getOwner(USERNAME, COMPANY)).thenReturn(null);

        Response<String> result = companyService.GetRoleTreeString(TOKEN, COMPANY);

        assertTrue(result.isError());
    }

    @Test
    void sendMessageToUser_Success() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);

        Response<String> result = companyService.sendMessageToUser(TOKEN, COMPANY, "user42", "Hello");

        assertTrue(result.isSuccess());
        verify(notifier, times(1)).notifyUser(eq("user42"), anyString(), eq("Hello"));
    }

    @Test
    void sendMessageToUser_Failure_NotAuthorized() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.isManager(USERNAME, COMPANY)).thenReturn(false);

        Response<String> result = companyService.sendMessageToUser(TOKEN, COMPANY, "user42", "Hello");

        assertTrue(result.isError());
        verify(notifier, never()).notifyUser(any(), any(), any());
    }

    @Test
    void createCompany_Failure_SuspendedUser() {
        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn(USERNAME);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.getUserByID(USERNAME)).thenReturn(mockUser);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(true);

        Response<String> status = companyService.CreateCompany(COMPANY, TOKEN);

        assertFalse(status.isSuccess());
        assertTrue(status.isError());
        assertEquals("User is suspended", status.getMessage());

        verify(companyRepository, never()).store(anyString(), anyString());
        verify(treeOfRoleRepository, never()).storeOwner(anyString(), anyString(), anyString());
    }
}
