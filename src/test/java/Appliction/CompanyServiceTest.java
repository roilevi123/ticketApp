package Appliction;

import com.ticketing.ticketapp.Appliction.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.ticketing.ticketapp.Domain.OwnerManagerTree.OwnerManagerException;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Permission;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.User.User;
import com.ticketing.ticketapp.Infastructure.TokenService;
import static org.mockito.Mockito.doThrow;

import org.springframework.dao.DataAccessResourceFailureException;

import com.ticketing.ticketapp.Domain.Event.iEventRepository;
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

    @InjectMocks
    private CompanyService companyService;
    @Mock
    private iEventRepository eventRepository;
    private final String TOKEN = "test_token";
    private final String USERNAME = "test_user";
    private final String COMPANY = "test_company";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        org.springframework.test.util.ReflectionTestUtils.setField(
                companyService,
                "eventRepository",
                eventRepository
        );
    }

    @Test
    void createCompany_Success() {
        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn(USERNAME);
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(tokenService.generateCompanyToken(anyString(), anyString(), anyString(), anyString())).thenReturn("mock-founder-token");
        when(userRepository.getUserByID(USERNAME)).thenReturn(mockUser);

        Response<String> status = companyService.CreateCompany(COMPANY, TOKEN);
        assertTrue(status.isSuccess());
        assertNotNull(status.getData());

        verify(companyRepository).store(COMPANY, USERNAME);
        verify(treeOfRoleRepository).storeOwner(USERNAME, COMPANY, "SYSTEM_FOUNDER");
    }

    @Test
    void createCompany_InvalidToken() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);

        assertThrows(OwnerManagerException.class, () -> companyService.CreateCompany(COMPANY, TOKEN));
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
    }

    @Test
    void appointAManager_Success() {
        String managerName = "new_manager";
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);
        User mockManagerUser = mock(User.class);
        when(mockManagerUser.getID()).thenReturn(managerName);
        when(userRepository.getUserByUsername(managerName)).thenReturn(mockManagerUser);

        Response<String> result = companyService.AppointAManager(managerName, COMPANY, permissions, TOKEN);
        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());
        verify(treeOfRoleRepository).storeManager(eq(managerName), eq(COMPANY), eq(permissions), eq(USERNAME));
    }

    @Test
    void appointAManager_Failure_NotAuthorizedAsOwner() {
        String managerName = "new_manager";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);

        assertThrows(OwnerManagerException.class,
                () -> companyService.AppointAManager(managerName, COMPANY, new HashSet<>(), TOKEN));
        verify(treeOfRoleRepository, never()).storeManager(anyString(), anyString(), anySet(), anyString());
    }

    @Test
    void approveAppointmentForManager_Success() {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        Manager mockManager = spy(new Manager(USERNAME, COMPANY, permissions, "Administrator"));

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
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
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
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
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
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
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.isManager(USERNAME, COMPANY)).thenReturn(true);

        Response<String> result = companyService.RejectAppointmentForManager(TOKEN, COMPANY);
        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());
        verify(treeOfRoleRepository).deleteManager(USERNAME, COMPANY);
    }

    @Test
    void rejectAppointmentForManager_Failure_UserNotFound() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);

        assertThrows(OwnerManagerException.class,
                () -> companyService.RejectAppointmentForManager(TOKEN, COMPANY));
        verify(treeOfRoleRepository, never()).deleteManager(anyString(), anyString());
    }

    @Test
    void rejectAppointmentForManager_Failure_ManagerNotFound() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.isManager(USERNAME, COMPANY)).thenReturn(false);

        assertThrows(OwnerManagerException.class,
                () -> companyService.RejectAppointmentForManager(TOKEN, COMPANY));
    }

    @Test
    void appointOwner_Success() {
        String newOwnerName = "new_owner";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);
        User mockOwnerUser = mock(User.class);
        when(mockOwnerUser.getID()).thenReturn(newOwnerName);
        when(userRepository.getUserByUsername(newOwnerName)).thenReturn(mockOwnerUser);

        Response<String> result = companyService.AppointOwner(newOwnerName, COMPANY, TOKEN);
        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());
        verify(treeOfRoleRepository).storeOwner(newOwnerName, COMPANY, USERNAME);
    }

    @Test
    void appointOwner_Failure_OwnerNotFoundInTree() {
        String newOwnerName = "new_owner";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);

        assertThrows(OwnerManagerException.class,
                () -> companyService.AppointOwner(newOwnerName, COMPANY, TOKEN));
        verify(treeOfRoleRepository, never()).storeOwner(anyString(), anyString(), anyString());
    }

    @Test
    void approveAppointmentForOwner_Success() {
        Owner mockOwner = spy(new Owner(USERNAME, COMPANY, "appointer_user"));

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
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
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
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
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
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
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
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
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.isOwner(USERNAME, COMPANY)).thenReturn(true);
        when(companyRepository.getCompanyFounder(COMPANY)).thenReturn(USERNAME);

        assertThrows(OwnerManagerException.class,
                () -> companyService.RejectAppointmentForOwner(TOKEN, COMPANY));
        verify(treeOfRoleRepository, never()).deleteOwner(anyString(), anyString());
    }

    @Test
    void rejectAppointmentForOwner_Failure_InvalidToken() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);

        assertThrows(OwnerManagerException.class,
                () -> companyService.RejectAppointmentForOwner(TOKEN, COMPANY));
        verify(treeOfRoleRepository, never()).deleteOwner(anyString(), anyString());
    }

    @Test
    void fireOwner_Success() {
        String ownerToFire = "owner_to_fire";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        User mockOwnerUser = mock(User.class);
        when(mockOwnerUser.getID()).thenReturn(ownerToFire);
        when(userRepository.getUserByUsername(ownerToFire)).thenReturn(mockOwnerUser);
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
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.isAppointerOwner(ownerToFire, COMPANY, USERNAME)).thenReturn(false);

        assertThrows(OwnerManagerException.class,
                () -> companyService.FireOwner(TOKEN, COMPANY, ownerToFire));
        verify(treeOfRoleRepository, never()).deleteOwner(anyString(), anyString());
    }

    @Test
    void fireManager_Success() {
        String managerToFire = "manager_to_fire";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        User mockManagerUser = mock(User.class);
        when(mockManagerUser.getID()).thenReturn(managerToFire);
        when(userRepository.getUserByUsername(managerToFire)).thenReturn(mockManagerUser);
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
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.isAppointerManager(managerToFire, COMPANY, USERNAME)).thenReturn(false);

        assertThrows(OwnerManagerException.class,
                () -> companyService.FireManager(TOKEN, COMPANY, managerToFire));
        verify(treeOfRoleRepository, never()).deleteManager(anyString(), anyString());
    }

    @Test
    void changeManagerPermissions_Success() {
        String managerName = "sub_manager";
        Set<Permission> newPermissions = Set.of(Permission.MANAGE_INVENTORY);
        Manager mockManager = spy(new Manager(managerName, COMPANY, new HashSet<>(), USERNAME));

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        User mockTargetUser = mock(User.class);
        when(mockTargetUser.getID()).thenReturn(managerName);
        when(userRepository.getUserByUsername(managerName)).thenReturn(mockTargetUser);
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
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.getManager(managerName, COMPANY)).thenReturn(mockManager);
        when(treeOfRoleRepository.isAppointerManager(managerName, COMPANY, USERNAME)).thenReturn(false);

        assertThrows(OwnerManagerException.class, () ->
                companyService.ChangeManagerPermissions(TOKEN, COMPANY, managerName, Set.of(Permission.MANAGE_INVENTORY)));
        verify(treeOfRoleRepository, never()).save(any(Manager.class));
        assertEquals(new HashSet<>(), treeOfRoleRepository.getManager(managerName, COMPANY).getPermissions());
    }

    @Test
    void freezeCompany_Success() {
        Company mockCompany = spy(new Company(COMPANY, USERNAME));
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
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
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
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
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
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

        assertThrows(OwnerManagerException.class,
                () -> companyService.unfreezeCompany(COMPANY, TOKEN));
        verifyNoInteractions(companyRepository);
    }

    @Test
    void getManagerPermissions_Success() {
        String managerName = "some_manager";
        Set<Permission> expectedPermissions = Set.of(Permission.MANAGE_INVENTORY, Permission.VIEW_PURCHASE_HISTORY);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);
        User mockManagerUser = mock(User.class);
        when(mockManagerUser.getID()).thenReturn(managerName);
        when(userRepository.getUserByUsername(managerName)).thenReturn(mockManagerUser);
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
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);

        assertThrows(OwnerManagerException.class,
                () -> companyService.GetManagerPermissions(TOKEN, COMPANY, managerName));
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
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(companyRepository.getCompany(COMPANY)).thenReturn(mockCompany);
        when(treeOfRoleRepository.getAllOwnersByCompany(COMPANY)).thenReturn(List.of(owner));
        when(treeOfRoleRepository.getAllManagersByCompany(COMPANY)).thenReturn(List.of(manager));
        when(userRepository.getUserByUsername(USERNAME)).thenReturn(ownerUser);
        when(userRepository.getUserByUsername("managerUsername")).thenReturn(managerUser);

        companyService.freezeCompany(COMPANY, TOKEN);

        verify(notifier).notifyUser(eq(USERNAME), anyString(), anyString());
        verify(notifier).notifyUser(eq("managerUsername"), anyString(), anyString());
    }

    @Test
    void replyToBuyer_Success() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);

        Response<String> result = companyService.replyToBuyer(TOKEN, COMPANY, "buyer1", "Hello buyer");

        assertTrue(result.isSuccess());
        verify(notifier, times(1)).notifyUser(eq("buyer1"), anyString(), anyString());
    }

    @Test
    void replyToBuyer_Failure_NotAuthorized() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.isManager(USERNAME, COMPANY)).thenReturn(false);

        assertThrows(OwnerManagerException.class,
                () -> companyService.replyToBuyer(TOKEN, COMPANY, "buyer1", "Hello"));
        verify(notifier, never()).notifyUser(eq("buyer1"), anyString(), anyString());
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

        assertThrows(OwnerManagerException.class,
                () -> companyService.getActiveCompanies(TOKEN));
    }

    @Test
    void getRoleTreeString_Success() {
        Company mockCompany = new Company(COMPANY, USERNAME);
        Owner owner = new Owner(USERNAME, COMPANY, "SYSTEM_FOUNDER");

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
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
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.getOwner(USERNAME, COMPANY)).thenReturn(null);

        assertThrows(OwnerManagerException.class,
                () -> companyService.GetRoleTreeString(TOKEN, COMPANY));
    }

    @Test
    void sendMessageToUser_Success() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);

        Response<String> result = companyService.sendMessageToUser(TOKEN, COMPANY, "user42", "Hello");

        assertTrue(result.isSuccess());
        verify(notifier, times(1)).notifyUser(eq("user42"), anyString(), eq("Hello"));
    }

    @Test
    void sendMessageToUser_Failure_NotAuthorized() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.isManager(USERNAME, COMPANY)).thenReturn(false);

        assertThrows(OwnerManagerException.class,
                () -> companyService.sendMessageToUser(TOKEN, COMPANY, "user42", "Hello"));
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

        OwnerManagerException ex = assertThrows(OwnerManagerException.class,
                () -> companyService.CreateCompany(COMPANY, TOKEN));
        assertEquals("User is suspended", ex.getMessage());

        verify(companyRepository, never()).store(anyString(), anyString());
        verify(treeOfRoleRepository, never()).storeOwner(anyString(), anyString(), anyString());
    }

    @Test
    void appointAManager_Failure_SuspendedUser() {
        String targetUsername = "new_manager";
        String mockUserId = "user-123";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(mockUserId);
        when(userRepository.isUserSuspendedNow(mockUserId)).thenReturn(true);

        OwnerManagerException ex = assertThrows(OwnerManagerException.class,
                () -> companyService.AppointAManager(targetUsername, COMPANY, new HashSet<>(), TOKEN));
        assertEquals("User is suspended", ex.getMessage());

        verify(treeOfRoleRepository, never()).exitsOwner(anyString(), anyString());
        verify(userRepository, never()).getUserByUsername(anyString());
        verify(treeOfRoleRepository, never()).storeManager(anyString(), anyString(), any(), anyString());
        verify(notifier, never()).notifyUser(anyString(), anyString(), anyString());
    }

    @Test
    void approveAppointmentForManager_Failure_SuspendedUser() {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        Manager mockManager = spy(new Manager(USERNAME, COMPANY, permissions, "Administrator"));

        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn(USERNAME);
        when(mockUser.getID()).thenReturn("user-123");

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.getManager(USERNAME, COMPANY)).thenReturn(mockManager);
        when(userRepository.getUserByID(USERNAME)).thenReturn(mockUser);
        when(userRepository.getUserByUsername(USERNAME)).thenReturn(mockUser);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(true);

        OwnerManagerException ex = assertThrows(OwnerManagerException.class,
                () -> companyService.ApproveAppointmentForManager(TOKEN, COMPANY));
        assertEquals("User is suspended", ex.getMessage());

        verify(treeOfRoleRepository, never()).save(any(Manager.class));
        assertFalse(mockManager.isAccepted());
    }

    @Test
    void rejectAppointmentForManager_Failure_SuspendedUser() {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        Manager mockManager = spy(new Manager(USERNAME, COMPANY, permissions, "Administrator"));

        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn(USERNAME);
        when(mockUser.getID()).thenReturn("user-123");

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.getManager(USERNAME, COMPANY)).thenReturn(mockManager);
        when(userRepository.getUserByID(USERNAME)).thenReturn(mockUser);
        when(userRepository.getUserByUsername(USERNAME)).thenReturn(mockUser);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(true);

        OwnerManagerException ex = assertThrows(OwnerManagerException.class,
                () -> companyService.RejectAppointmentForManager(TOKEN, COMPANY));
        assertEquals("User is suspended", ex.getMessage());

        verify(treeOfRoleRepository, never()).save(any(Manager.class));
        assertFalse(mockManager.isAccepted());
    }

    @Test
    void appointOwner_Failure_SuspendedUser() {
        String targetUsername = "new_owner";
        String mockUserId = "user-123";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(mockUserId);
        when(userRepository.isUserSuspendedNow(mockUserId)).thenReturn(true);

        OwnerManagerException ex = assertThrows(OwnerManagerException.class,
                () -> companyService.AppointOwner(targetUsername, COMPANY, TOKEN));
        assertEquals("User is suspended", ex.getMessage());

        verify(treeOfRoleRepository, never()).exitsOwner(anyString(), anyString());
        verify(userRepository, never()).getUserByUsername(anyString());
        verify(treeOfRoleRepository, never()).storeOwner(anyString(), anyString(), anyString());
        verify(notifier, never()).notifyUser(anyString(), anyString(), anyString());
    }

    @Test
    void approveAppointmentForOwner_Failure_SuspendedUser() {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        Manager mockManager = spy(new Manager(USERNAME, COMPANY, permissions, "Administrator"));

        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn(USERNAME);
        when(mockUser.getID()).thenReturn("user-123");

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.getManager(USERNAME, COMPANY)).thenReturn(mockManager);
        when(userRepository.getUserByID(USERNAME)).thenReturn(mockUser);
        when(userRepository.getUserByUsername(USERNAME)).thenReturn(mockUser);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(true);

        OwnerManagerException ex = assertThrows(OwnerManagerException.class,
                () -> companyService.ApproveAppointmentForOwner(TOKEN, COMPANY));
        assertEquals("User is suspended", ex.getMessage());

        verify(treeOfRoleRepository, never()).save(any(Manager.class));
        assertFalse(mockManager.isAccepted());
    }

    @Test
    void rejectAppointmentForOwner_Failure_SuspendedUser() {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        Manager mockManager = spy(new Manager(USERNAME, COMPANY, permissions, "Administrator"));

        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn(USERNAME);
        when(mockUser.getID()).thenReturn("user-123");

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.getManager(USERNAME, COMPANY)).thenReturn(mockManager);
        when(userRepository.getUserByID(USERNAME)).thenReturn(mockUser);
        when(userRepository.getUserByUsername(USERNAME)).thenReturn(mockUser);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(true);

        OwnerManagerException ex = assertThrows(OwnerManagerException.class,
                () -> companyService.RejectAppointmentForOwner(TOKEN, COMPANY));
        assertEquals("User is suspended", ex.getMessage());

        verify(treeOfRoleRepository, never()).save(any(Manager.class));
        assertFalse(mockManager.isAccepted());
    }

    @Test
    void FireOwner_Failure_SuspendedUser() {
        String ownerToFire = "owner_to_fire";
        String mockUserId = "user-123";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(tokenService.extractUserId(TOKEN)).thenReturn(mockUserId);
        when(userRepository.isUserSuspendedNow(mockUserId)).thenReturn(true);

        OwnerManagerException ex = assertThrows(OwnerManagerException.class,
                () -> companyService.FireOwner(TOKEN, COMPANY, ownerToFire));
        assertEquals("User is suspended", ex.getMessage());

        verify(treeOfRoleRepository, never()).deleteOwner(ownerToFire, COMPANY);
    }

    @Test
    void FireManager_Failure_SuspendedUser() {
        String managerToFire = "manager_to_fire";
        String mockUserId = "user-123";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(tokenService.extractUserId(TOKEN)).thenReturn(mockUserId);
        when(userRepository.isUserSuspendedNow(mockUserId)).thenReturn(true);

        OwnerManagerException ex = assertThrows(OwnerManagerException.class,
                () -> companyService.FireManager(TOKEN, COMPANY, managerToFire));
        assertEquals("User is suspended", ex.getMessage());

        verify(treeOfRoleRepository, never()).deleteManager(anyString(), anyString());
    }

    @Test
    void changeManagerPermissions_Failure_SuspendedUser() {
        String managerID = "sub_manager";
        String mockUserId = "user-123";
        Set<Permission> newPermissions = Set.of(Permission.MANAGE_INVENTORY);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(mockUserId);
        when(userRepository.isUserSuspendedNow(mockUserId)).thenReturn(true);

        OwnerManagerException ex = assertThrows(OwnerManagerException.class,
                () -> companyService.ChangeManagerPermissions(TOKEN, COMPANY, managerID, newPermissions));
        assertEquals("User is suspended", ex.getMessage());

        verify(treeOfRoleRepository, never()).getManager(anyString(), anyString());
        verify(treeOfRoleRepository, never()).save(any(Manager.class));
    }

    @Test
    void freezeCompany_Failure_SuspendedUser() {
        String mockUserId = "user-123";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(tokenService.extractUserId(TOKEN)).thenReturn(mockUserId);
        when(userRepository.isUserSuspendedNow(mockUserId)).thenReturn(true);

        OwnerManagerException ex = assertThrows(OwnerManagerException.class,
                () -> companyService.freezeCompany(COMPANY, TOKEN));
        assertEquals("User is suspended", ex.getMessage());

        verify(companyRepository, never()).getCompany(anyString());
        verify(companyRepository, never()).save(any(Company.class));
    }

    @Test
    void unfreezeCompany_Failure_SuspendedUser() {
        String mockUserId = "user-123";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(tokenService.extractUserId(TOKEN)).thenReturn(mockUserId);
        when(userRepository.isUserSuspendedNow(mockUserId)).thenReturn(true);

        OwnerManagerException ex = assertThrows(OwnerManagerException.class,
                () -> companyService.unfreezeCompany(COMPANY, TOKEN));
        assertEquals("User is suspended", ex.getMessage());

        verify(companyRepository, never()).getCompany(anyString());
        verify(companyRepository, never()).save(any(Company.class));
    }

    @Test
    void replyToBuyer_Failure_SuspendedUser() {
        String buyerId = "buyer-789";
        String mockUserId = "user-123";
        String message = "Hello from support";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(tokenService.extractUserId(TOKEN)).thenReturn(mockUserId);
        when(userRepository.isUserSuspendedNow(mockUserId)).thenReturn(true);

        OwnerManagerException ex = assertThrows(OwnerManagerException.class,
                () -> companyService.replyToBuyer(TOKEN, COMPANY, buyerId, message));
        assertEquals("User is suspended", ex.getMessage());

        verify(treeOfRoleRepository, never()).exitsOwner(anyString(), anyString());
        verify(treeOfRoleRepository, never()).isManager(anyString(), anyString());
        verify(notifier, never()).notifyUser(anyString(), anyString(), anyString());
    }

    @Test
    void sendMessageToUser_Failure_SuspendedUser() {
        String targetUserId = "target-user-456";
        String mockUserId = "user-123";
        String message = "Important update";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(mockUserId);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(mockUserId)).thenReturn(true);

        OwnerManagerException ex = assertThrows(OwnerManagerException.class,
                () -> companyService.sendMessageToUser(TOKEN, COMPANY, targetUserId, message));
        assertEquals("User is suspended", ex.getMessage());

        verify(treeOfRoleRepository, never()).exitsOwner(anyString(), anyString());
        verify(treeOfRoleRepository, never()).isManager(anyString(), anyString());
        verify(notifier, never()).notifyUser(anyString(), anyString(), anyString());
    }

    @Test
    void fireMember_Failure_SuspendedUser() {
        String memberUsername = "member_to_fire";
        String mockUserId = "user-123";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(mockUserId);
        when(userRepository.isUserSuspendedNow(mockUserId)).thenReturn(true);

        OwnerManagerException ex = assertThrows(OwnerManagerException.class,
                () -> companyService.FireMember(TOKEN, COMPANY, memberUsername));
        assertEquals("User is suspended", ex.getMessage());

        verify(userRepository, never()).getUserByUsername(anyString());
        verify(treeOfRoleRepository, never()).getManager(anyString(), anyString());
        verify(treeOfRoleRepository, never()).getOwner(anyString(), anyString());
    }

    @Test
    void closeCompany_Failure_SuspendedUser() {
        String mockUserId = "user-123";

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(mockUserId);
        when(userRepository.isUserSuspendedNow(mockUserId)).thenReturn(true);

        OwnerManagerException ex = assertThrows(OwnerManagerException.class,
                () -> companyService.closeCompany(COMPANY, TOKEN));
        assertEquals("User is suspended", ex.getMessage());

        verify(treeOfRoleRepository, never()).getOwner(anyString(), anyString());
        verify(companyRepository, never()).deleteCompany(anyString());
        verify(treeOfRoleRepository, never()).deleteCompanyMangersAndOwners(anyString());
    }
    @Test
    void fireMember_ManagerSuccess() {
        String memberUsername = "manager_user";
        String memberId = "manager_id";

        User targetUser = mock(User.class);
        when(targetUser.getID()).thenReturn(memberId);

        Manager manager = new Manager(memberId, COMPANY, Set.of(Permission.MANAGE_INVENTORY), USERNAME);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);
        when(userRepository.getUserByUsername(memberUsername)).thenReturn(targetUser);
        when(treeOfRoleRepository.getManager(memberId, COMPANY)).thenReturn(manager);

        Response<String> res = companyService.FireMember(TOKEN, COMPANY, memberUsername);

        assertTrue(res.isSuccess());
        assertEquals("success", res.getData());

        verify(treeOfRoleRepository).deleteManager(memberId, COMPANY);
        verify(notifier).notifyUser(eq(memberId), eq("Role Removed"), anyString());
    }

    @Test
    void fireMember_ManagerFailure_NotAppointer() {
        String memberUsername = "manager_user";
        String memberId = "manager_id";

        User targetUser = mock(User.class);
        when(targetUser.getID()).thenReturn(memberId);

        Manager manager = new Manager(memberId, COMPANY, Set.of(Permission.MANAGE_INVENTORY), "someone_else");

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);
        when(userRepository.getUserByUsername(memberUsername)).thenReturn(targetUser);
        when(treeOfRoleRepository.getManager(memberId, COMPANY)).thenReturn(manager);

        OwnerManagerException ex = assertThrows(
                OwnerManagerException.class,
                () -> companyService.FireMember(TOKEN, COMPANY, memberUsername)
        );

        assertEquals("You are not allowed to fire this manager", ex.getMessage());
        verify(treeOfRoleRepository, never()).deleteManager(anyString(), anyString());
    }

    @Test
    void fireMember_OwnerSuccess_WithCascadeDelete() {
        String ownerUsername = "owner_to_fire";
        String ownerId = "owner_id";

        String childOwnerId = "child_owner";
        String childManagerId = "child_manager";

        User targetUser = mock(User.class);
        when(targetUser.getID()).thenReturn(ownerId);

        Owner ownerToFire = new Owner(ownerId, COMPANY, USERNAME);
        Owner childOwner = new Owner(childOwnerId, COMPANY, ownerId);
        Manager childManager = new Manager(childManagerId, COMPANY, Set.of(), ownerId);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);
        when(userRepository.getUserByUsername(ownerUsername)).thenReturn(targetUser);
        when(treeOfRoleRepository.getManager(ownerId, COMPANY)).thenReturn(null);
        when(treeOfRoleRepository.getOwner(ownerId, COMPANY)).thenReturn(ownerToFire);

        when(treeOfRoleRepository.getAllOwnersByCompany(COMPANY))
                .thenReturn(List.of(ownerToFire, childOwner))
                .thenReturn(List.of(ownerToFire, childOwner));

        when(treeOfRoleRepository.getAllManagersByCompany(COMPANY))
                .thenReturn(List.of(childManager))
                .thenReturn(List.of(childManager));

        Response<String> res = companyService.FireMember(TOKEN, COMPANY, ownerUsername);

        assertTrue(res.isSuccess());
        assertEquals("success", res.getData());

        verify(treeOfRoleRepository).deleteOwner(childOwnerId, COMPANY);
        verify(treeOfRoleRepository).deleteManager(childManagerId, COMPANY);
        verify(treeOfRoleRepository).deleteOwner(ownerId, COMPANY);

        verify(notifier).notifyUser(eq(ownerId), eq("Role Removed"), anyString());
        verify(notifier).notifyUser(eq(childOwnerId), eq("Role Removed"), anyString());
        verify(notifier).notifyUser(eq(childManagerId), eq("Role Removed"), anyString());
    }

    @Test
    void fireMember_OwnerFailure_FounderCannotBeFired() {
        String ownerUsername = "founder_user";
        String ownerId = "founder_id";

        User targetUser = mock(User.class);
        when(targetUser.getID()).thenReturn(ownerId);

        Owner founder = new Owner(ownerId, COMPANY, iTreeOfRoleRepository.FOUNDER_APPOINTER);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);
        when(userRepository.getUserByUsername(ownerUsername)).thenReturn(targetUser);
        when(treeOfRoleRepository.getManager(ownerId, COMPANY)).thenReturn(null);
        when(treeOfRoleRepository.getOwner(ownerId, COMPANY)).thenReturn(founder);

        OwnerManagerException ex = assertThrows(
                OwnerManagerException.class,
                () -> companyService.FireMember(TOKEN, COMPANY, ownerUsername)
        );

        assertEquals("Founders cannot be fired", ex.getMessage());
        verify(treeOfRoleRepository, never()).deleteOwner(anyString(), anyString());
    }

    @Test
    void fireMember_OwnerFailure_NotAppointer() {
        String ownerUsername = "owner_user";
        String ownerId = "owner_id";

        User targetUser = mock(User.class);
        when(targetUser.getID()).thenReturn(ownerId);

        Owner owner = new Owner(ownerId, COMPANY, "someone_else");

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);
        when(userRepository.getUserByUsername(ownerUsername)).thenReturn(targetUser);
        when(treeOfRoleRepository.getManager(ownerId, COMPANY)).thenReturn(null);
        when(treeOfRoleRepository.getOwner(ownerId, COMPANY)).thenReturn(owner);

        OwnerManagerException ex = assertThrows(
                OwnerManagerException.class,
                () -> companyService.FireMember(TOKEN, COMPANY, ownerUsername)
        );

        assertEquals("You are not allowed to fire this owner", ex.getMessage());
        verify(treeOfRoleRepository, never()).deleteOwner(anyString(), anyString());
    }

    @Test
    void fireMember_Failure_UserHasNoRole() {
        String memberUsername = "regular_user";
        String memberId = "regular_id";

        User targetUser = mock(User.class);
        when(targetUser.getID()).thenReturn(memberId);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);
        when(userRepository.getUserByUsername(memberUsername)).thenReturn(targetUser);
        when(treeOfRoleRepository.getManager(memberId, COMPANY)).thenReturn(null);
        when(treeOfRoleRepository.getOwner(memberId, COMPANY)).thenReturn(null);

        OwnerManagerException ex = assertThrows(
                OwnerManagerException.class,
                () -> companyService.FireMember(TOKEN, COMPANY, memberUsername)
        );

        assertEquals("User has no role in this company", ex.getMessage());
    }

    @Test
    void fireMember_Failure_UserNotFound() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);
        when(userRepository.getUserByUsername("missing")).thenReturn(null);

        OwnerManagerException ex = assertThrows(
                OwnerManagerException.class,
                () -> companyService.FireMember(TOKEN, COMPANY, "missing")
        );

        assertEquals("User not found", ex.getMessage());
    }



    @Test
    void closeCompany_Failure_NotFounder() {
        Owner nonFounder = new Owner(USERNAME, COMPANY, "someone_else");

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);
        when(treeOfRoleRepository.getOwner(USERNAME, COMPANY)).thenReturn(nonFounder);

        OwnerManagerException ex = assertThrows(
                OwnerManagerException.class,
                () -> companyService.closeCompany(COMPANY, TOKEN)
        );

        assertEquals("Only the founder can close the company", ex.getMessage());

        verify(companyRepository, never()).deleteCompany(anyString());
        verify(eventRepository, never()).deleteCompanyEvent(anyString());
        verify(treeOfRoleRepository, never()).deleteCompanyMangersAndOwners(anyString());
    }

    @Test
    void closeCompany_Failure_UserNotOwner() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);
        when(treeOfRoleRepository.getOwner(USERNAME, COMPANY)).thenReturn(null);

        OwnerManagerException ex = assertThrows(
                OwnerManagerException.class,
                () -> companyService.closeCompany(COMPANY, TOKEN)
        );

        assertEquals("Only the founder can close the company", ex.getMessage());
    }

    @Test
    void notifyMember_DoesNotFail_WhenNotifierThrows() {
        String managerName = "manager_user";
        String managerId = "manager_id";

        User targetUser = mock(User.class);
        when(targetUser.getID()).thenReturn(managerId);

        Manager manager = new Manager(managerId, COMPANY, Set.of(), USERNAME);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);
        when(userRepository.getUserByUsername(managerName)).thenReturn(targetUser);
        when(treeOfRoleRepository.getManager(managerId, COMPANY)).thenReturn(manager);

        doThrow(new RuntimeException("notification failed"))
                .when(notifier)
                .notifyUser(anyString(), anyString(), anyString());

        Response<String> res = companyService.FireMember(TOKEN, COMPANY, managerName);

        assertTrue(res.isSuccess());
        verify(treeOfRoleRepository).deleteManager(managerId, COMPANY);
    }

    @Test
    void deleteAll_CallsRepositories() {
        companyService.deleteAll();

        verify(companyRepository).deleteAllCompany();
        verify(treeOfRoleRepository).deleteAllRoles();
    }

    @Test
    void getActiveCompanies_GuestToken_Success() {
        when(companyRepository.getActiveCompanies())
                .thenReturn(List.of(new Company(COMPANY, USERNAME)));

        Response<List<com.ticketing.ticketapp.Domain.Company.CompanyDTO>> res =
                companyService.getActiveCompanies("guest-temporary-token");

        assertTrue(res.isSuccess());
        assertEquals(1, res.getData().size());

        verify(tokenService, never()).validateToken(anyString());
    }

    @Test
    void createCompany_DatabaseFailure_ReturnsError() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);

        doThrow(new org.springframework.dao.DataAccessResourceFailureException("DB down"))
                .when(companyRepository)
                .store(COMPANY, USERNAME);

        Response<String> res = companyService.CreateCompany(COMPANY, TOKEN);

        assertTrue(res.isError());
        assertEquals("Database unavailable", res.getMessage());
    }

    @Test
    void getActiveCompanies_DatabaseFailure_ReturnsError() {
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("DB down"))
                .when(companyRepository)
                .getActiveCompanies();

        Response<List<com.ticketing.ticketapp.Domain.Company.CompanyDTO>> res =
                companyService.getActiveCompanies(null);

        assertTrue(res.isError());
        assertEquals("Database unavailable", res.getMessage());
    }

    @Test
    void closeCompany_DatabaseFailure_ReturnsError() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);

        doThrow(new org.springframework.dao.DataAccessResourceFailureException("DB down"))
                .when(treeOfRoleRepository)
                .getOwner(USERNAME, COMPANY);

        Response<String> res = companyService.closeCompany(COMPANY, TOKEN);

        assertTrue(res.isError());
        assertEquals("Database unavailable", res.getMessage());
    }

    @Test
    void fireMember_DatabaseFailure_ReturnsError() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);

        doThrow(new org.springframework.dao.DataAccessResourceFailureException("DB down"))
                .when(userRepository)
                .getUserByUsername("member");

        Response<String> res = companyService.FireMember(TOKEN, COMPANY, "member");

        assertTrue(res.isError());
        assertEquals("Database unavailable", res.getMessage());
    }
    @Test
    void closeCompany_Success_NotifiesAllOwnersAndManagers() {
        Owner founder = new Owner(USERNAME, COMPANY, iTreeOfRoleRepository.FOUNDER_APPOINTER);
        Owner owner2 = new Owner("owner2", COMPANY, USERNAME);
        Manager manager1 = new Manager("manager1", COMPANY, Set.of(), USERNAME);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);

        when(treeOfRoleRepository.getOwner(USERNAME, COMPANY)).thenReturn(founder);
        when(treeOfRoleRepository.getAllOwnersByCompany(COMPANY)).thenReturn(List.of(founder, owner2));
        when(treeOfRoleRepository.getAllManagersByCompany(COMPANY)).thenReturn(List.of(manager1));

        Response<String> result = companyService.closeCompany(COMPANY, TOKEN);

        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());

        verify(companyRepository).deleteCompany(COMPANY);
        verify(eventRepository).deleteCompanyEvent(COMPANY);
        verify(treeOfRoleRepository).deleteCompanyMangersAndOwners(COMPANY);

        verify(notifier).notifyUser(eq(USERNAME), eq("Company Closed"), anyString());
        verify(notifier).notifyUser(eq("owner2"), eq("Company Closed"), anyString());
        verify(notifier).notifyUser(eq("manager1"), eq("Company Closed"), anyString());
    }
    @Test
    void appointAManager_Failure_TargetUserNotFound() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);
        when(userRepository.getUserByUsername("missing_manager")).thenReturn(null);

        OwnerManagerException ex = assertThrows(
                OwnerManagerException.class,
                () -> companyService.AppointAManager(
                        "missing_manager",
                        COMPANY,
                        Set.of(Permission.MANAGE_INVENTORY),
                        TOKEN
                )
        );

        assertEquals("Target user not found", ex.getMessage());
        verify(treeOfRoleRepository, never())
                .storeManager(anyString(), anyString(), anySet(), anyString());
    }@Test
    void appointAManager_Failure_CannotAppointYourself() {
        User targetUser = mock(User.class);
        when(targetUser.getID()).thenReturn(USERNAME);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);
        when(userRepository.getUserByUsername("myself")).thenReturn(targetUser);

        OwnerManagerException ex = assertThrows(
                OwnerManagerException.class,
                () -> companyService.AppointAManager(
                        "myself",
                        COMPANY,
                        Set.of(Permission.MANAGE_INVENTORY),
                        TOKEN
                )
        );

        assertEquals("You cannot appoint yourself", ex.getMessage());
        verify(treeOfRoleRepository, never())
                .storeManager(anyString(), anyString(), anySet(), anyString());
    }
    @Test
    void appointOwner_Failure_TargetUserNotFound() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);
        when(userRepository.getUserByUsername("missing_owner")).thenReturn(null);

        OwnerManagerException ex = assertThrows(
                OwnerManagerException.class,
                () -> companyService.AppointOwner("missing_owner", COMPANY, TOKEN)
        );

        assertEquals("Target user not found", ex.getMessage());
        verify(treeOfRoleRepository, never())
                .storeOwner(anyString(), anyString(), anyString());
    }
    @Test
    void appointOwner_Failure_CannotAppointYourself() {
        User targetUser = mock(User.class);
        when(targetUser.getID()).thenReturn(USERNAME);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);
        when(userRepository.getUserByUsername("myself")).thenReturn(targetUser);

        OwnerManagerException ex = assertThrows(
                OwnerManagerException.class,
                () -> companyService.AppointOwner("myself", COMPANY, TOKEN)
        );

        assertEquals("You cannot appoint yourself", ex.getMessage());
        verify(treeOfRoleRepository, never())
                .storeOwner(anyString(), anyString(), anyString());
    }
    @Test
    void changeManagerPermissions_Failure_TargetUserNotFound() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);
        when(userRepository.getUserByUsername("missing_manager")).thenReturn(null);

        OwnerManagerException ex = assertThrows(
                OwnerManagerException.class,
                () -> companyService.ChangeManagerPermissions(
                        TOKEN,
                        COMPANY,
                        "missing_manager",
                        Set.of(Permission.MANAGE_INVENTORY)
                )
        );

        assertEquals("Target user not found", ex.getMessage());
        verify(treeOfRoleRepository, never()).save(any(Manager.class));
    }
    @Test
    void changeManagerPermissions_Failure_ManagerNotFound() {
        User targetUser = mock(User.class);
        when(targetUser.getID()).thenReturn("manager_id");

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);
        when(userRepository.getUserByUsername("manager")).thenReturn(targetUser);
        when(treeOfRoleRepository.getManager("manager_id", COMPANY)).thenReturn(null);

        OwnerManagerException ex = assertThrows(
                OwnerManagerException.class,
                () -> companyService.ChangeManagerPermissions(
                        TOKEN,
                        COMPANY,
                        "manager",
                        Set.of(Permission.MANAGE_INVENTORY)
                )
        );

        assertEquals("Manager not found", ex.getMessage());
        verify(treeOfRoleRepository, never()).save(any(Manager.class));
    }
    @Test
    void getManagerPermissions_Failure_TargetUserNotFound() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);
        when(userRepository.getUserByUsername("missing_manager")).thenReturn(null);

        OwnerManagerException ex = assertThrows(
                OwnerManagerException.class,
                () -> companyService.GetManagerPermissions(
                        TOKEN,
                        COMPANY,
                        "missing_manager"
                )
        );

        assertEquals("Target user not found", ex.getMessage());
        verify(treeOfRoleRepository, never()).getManagerPermissions(anyString(), anyString());
    }
    @Test
    void getManagerPermissions_DatabaseFailure_ReturnsError() {
        User managerUser = mock(User.class);
        when(managerUser.getID()).thenReturn("manager_id");

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);
        when(userRepository.getUserByUsername("manager")).thenReturn(managerUser);

        doThrow(new DataAccessResourceFailureException("DB down"))
                .when(treeOfRoleRepository)
                .getManagerPermissions("manager_id", COMPANY);

        Response<Set<Permission>> result =
                companyService.GetManagerPermissions(TOKEN, COMPANY, "manager");

        assertTrue(result.isError());
        assertEquals("Database unavailable", result.getMessage());
    }
    @Test
    void replyToBuyer_Success_AsManager() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);

        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.isManager(USERNAME, COMPANY)).thenReturn(true);

        Response<String> result =
                companyService.replyToBuyer(TOKEN, COMPANY, "buyer1", "hello");

        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());

        verify(notifier).notifyUser(
                eq("buyer1"),
                eq("Message from " + COMPANY),
                eq("hello")
        );
    }
    @Test
    void replyToBuyer_Failure_InvalidToken() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);

        OwnerManagerException ex = assertThrows(
                OwnerManagerException.class,
                () -> companyService.replyToBuyer(TOKEN, COMPANY, "buyer1", "hello")
        );

        assertEquals("Invalid token", ex.getMessage());

        verify(notifier, never()).notifyUser(anyString(), anyString(), anyString());
    }
    @Test
    void replyToBuyer_DatabaseFailure_ReturnsError() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);

        doThrow(new DataAccessResourceFailureException("DB down"))
                .when(treeOfRoleRepository)
                .exitsOwner(USERNAME, COMPANY);

        Response<String> result =
                companyService.replyToBuyer(TOKEN, COMPANY, "buyer1", "hello");

        assertTrue(result.isError());
        assertEquals("Database unavailable", result.getMessage());
    }
    @Test
    void sendMessageToUser_Success_AsManager() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);

        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.isManager(USERNAME, COMPANY)).thenReturn(true);

        Response<String> result =
                companyService.sendMessageToUser(TOKEN, COMPANY, "target1", "hello");

        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());

        verify(notifier).notifyUser(
                eq("target1"),
                eq("Message from " + COMPANY),
                eq("hello")
        );
    }@Test
    void sendMessageToUser_Failure_InvalidToken() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);

        OwnerManagerException ex = assertThrows(
                OwnerManagerException.class,
                () -> companyService.sendMessageToUser(TOKEN, COMPANY, "target1", "hello")
        );

        assertEquals("Invalid token", ex.getMessage());

        verify(notifier, never()).notifyUser(anyString(), anyString(), anyString());
    }
    @Test
    void sendMessageToUser_DatabaseFailure_ReturnsError() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(false);

        doThrow(new DataAccessResourceFailureException("DB down"))
                .when(treeOfRoleRepository)
                .exitsOwner(USERNAME, COMPANY);

        Response<String> result =
                companyService.sendMessageToUser(TOKEN, COMPANY, "target1", "hello");

        assertTrue(result.isError());
        assertEquals("DB down", result.getMessage());
    }
    @Test
    void getRoleTreeString_Success_WithOwnerAndManagerChildren() {
        Company company = new Company(COMPANY, USERNAME);

        Owner founder = new Owner(USERNAME, COMPANY, iTreeOfRoleRepository.FOUNDER_APPOINTER);
        founder.acceptAppointment();

        Owner childOwner = new Owner("owner_child", COMPANY, USERNAME);
        childOwner.acceptAppointment();

        Manager childManager = new Manager("manager_child", COMPANY, Set.of(), USERNAME);
        childManager.acceptAppointment();

        User founderUser = mock(User.class);
        when(founderUser.getName()).thenReturn("Founder Name");

        User ownerUser = mock(User.class);
        when(ownerUser.getName()).thenReturn("Owner Child Name");

        User managerUser = mock(User.class);
        when(managerUser.getName()).thenReturn("Manager Child Name");

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);

        when(treeOfRoleRepository.getOwner(USERNAME, COMPANY)).thenReturn(founder);
        when(treeOfRoleRepository.getAllOwnersByCompany(COMPANY))
                .thenReturn(List.of(founder, childOwner));
        when(treeOfRoleRepository.getAllManagersByCompany(COMPANY))
                .thenReturn(List.of(childManager));

        when(companyRepository.getCompany(COMPANY)).thenReturn(company);

        when(userRepository.getUserByID(USERNAME)).thenReturn(founderUser);
        when(userRepository.getUserByID("owner_child")).thenReturn(ownerUser);
        when(userRepository.getUserByID("manager_child")).thenReturn(managerUser);

        Response<String> result = companyService.GetRoleTreeString(TOKEN, COMPANY);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().contains("Founder Name"));
        assertTrue(result.getData().contains("Owner Child Name"));
        assertTrue(result.getData().contains("Manager Child Name"));
    }@Test
    void getRoleTreeString_Success_WhenUserObjectsMissing_UsesIds() {
        Company company = new Company(COMPANY, USERNAME);

        Owner founder = new Owner(USERNAME, COMPANY, iTreeOfRoleRepository.FOUNDER_APPOINTER);
        founder.acceptAppointment();

        Owner childOwner = new Owner("owner_child", COMPANY, USERNAME);
        childOwner.acceptAppointment();

        Manager childManager = new Manager("manager_child", COMPANY, Set.of(), USERNAME);
        childManager.acceptAppointment();

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);

        when(treeOfRoleRepository.getOwner(USERNAME, COMPANY)).thenReturn(founder);
        when(treeOfRoleRepository.getAllOwnersByCompany(COMPANY))
                .thenReturn(List.of(founder, childOwner));
        when(treeOfRoleRepository.getAllManagersByCompany(COMPANY))
                .thenReturn(List.of(childManager));

        when(companyRepository.getCompany(COMPANY)).thenReturn(company);

        when(userRepository.getUserByID(anyString())).thenReturn(null);

        Response<String> result = companyService.GetRoleTreeString(TOKEN, COMPANY);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().contains(USERNAME));
        assertTrue(result.getData().contains("owner_child"));
        assertTrue(result.getData().contains("manager_child"));
    }
    @Test
    void getRoleTreeString_Failure_InvalidToken() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);

        OwnerManagerException ex = assertThrows(
                OwnerManagerException.class,
                () -> companyService.GetRoleTreeString(TOKEN, COMPANY)
        );

        assertEquals("Invalid token", ex.getMessage());
    }
    @Test
    void getRoleTreeString_DatabaseFailure_ReturnsError() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);

        doThrow(new DataAccessResourceFailureException("DB down"))
                .when(treeOfRoleRepository)
                .getOwner(USERNAME, COMPANY);

        Response<String> result = companyService.GetRoleTreeString(TOKEN, COMPANY);

        assertTrue(result.isError());
        assertEquals("Database unavailable", result.getMessage());
    }

}
