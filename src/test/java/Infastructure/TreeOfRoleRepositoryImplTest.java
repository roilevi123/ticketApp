package Infastructure;

import com.ticketing.ticketapp.Domain.OwnerManagerTree.*;
import com.ticketing.ticketapp.Infastructure.TreeOfRoleRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TreeOfRoleRepositoryImplTest {

    private TreeOfRoleRepositoryImpl repo;

    private static final String COMPANY = "TestCompany";
    private static final String FOUNDER = iTreeOfRoleRepository.FOUNDER_APPOINTER;

    @BeforeEach
    void setUp() {
        repo = new TreeOfRoleRepositoryImpl();
    }

    // --- Owner tests ---

    @Test
    void storeOwner_FounderAppointer_AutoAccepted() {
        repo.storeOwner("founder1", COMPANY, FOUNDER);
        assertTrue(repo.exitsOwner("founder1", COMPANY));
    }

    @Test
    void storeOwner_NonFounderAppointer_NotAutoAccepted() {
        repo.storeOwner("owner2", COMPANY, "someOtherOwner");
        assertFalse(repo.exitsOwner("owner2", COMPANY));
    }

    @Test
    void exitsOwner_NonExistent_ReturnsFalse() {
        assertFalse(repo.exitsOwner("nobody", COMPANY));
    }

    @Test
    void getOwner_ReturnsOwner() {
        repo.storeOwner("owner1", COMPANY, FOUNDER);
        Owner o = repo.getOwner("owner1", COMPANY);
        assertNotNull(o);
        assertEquals("owner1", o.getUserID());
    }

    @Test
    void isOwner_TrueAfterStore() {
        repo.storeOwner("owner1", COMPANY, FOUNDER);
        assertTrue(repo.isOwner("owner1", COMPANY));
    }

    @Test
    void isOwner_FalseForUnknown() {
        assertFalse(repo.isOwner("ghost", COMPANY));
    }

    @Test
    void isAppointerOwner_ReturnsTrue_WhenCorrectAppointer() {
        repo.storeOwner("owner1", COMPANY, "manualAppointer");
        Owner o = repo.getOwner("owner1", COMPANY);
        o.acceptAppointment();
        repo.save(o);
        assertTrue(repo.isAppointerOwner("owner1", COMPANY, "manualAppointer"));
    }

    @Test
    void isAppointerOwner_ReturnsFalse_WhenNotAccepted() {
        repo.storeOwner("owner1", COMPANY, "appointerX");
        assertFalse(repo.isAppointerOwner("owner1", COMPANY, "appointerX"));
    }

    @Test
    void isAppointerOwner_ReturnsFalse_WhenWrongAppointer() {
        repo.storeOwner("owner1", COMPANY, FOUNDER);
        assertFalse(repo.isAppointerOwner("owner1", COMPANY, "wrong"));
    }

    @Test
    void saveOwner_Success() {
        repo.storeOwner("owner1", COMPANY, FOUNDER);
        Owner o = repo.getOwner("owner1", COMPANY);
        repo.save(o);
        assertNotNull(repo.getOwner("owner1", COMPANY));
    }

    @Test
    void saveOwner_NotFound_Throws() {
        Owner o = new Owner("nobody", COMPANY, FOUNDER);
        assertThrows(RuntimeException.class, () -> repo.save(o));
    }

    @Test
    void deleteOwner_RemovesOwner() {
        repo.storeOwner("owner1", COMPANY, FOUNDER);
        repo.deleteOwner("owner1", COMPANY);
        assertFalse(repo.isOwner("owner1", COMPANY));
    }

    @Test
    void getAllOwnersByCompany_ReturnsCorrectOwners() {
        repo.storeOwner("o1", COMPANY, FOUNDER);
        repo.storeOwner("o2", COMPANY, FOUNDER);
        repo.storeOwner("o3", "OtherCompany", FOUNDER);
        List<Owner> owners = repo.getAllOwnersByCompany(COMPANY);
        assertEquals(2, owners.size());
    }

    // --- Manager tests ---

    @Test
    void storeManager_AddsManager() {
        repo.storeManager("mgr1", COMPANY, Set.of(Permission.MANAGE_INVENTORY), "owner1");
        assertTrue(repo.isManager("mgr1", COMPANY));
    }

    @Test
    void isManager_FalseForUnknown() {
        assertFalse(repo.isManager("nobody", COMPANY));
    }

    @Test
    void getManager_ReturnsManager() {
        repo.storeManager("mgr1", COMPANY, Set.of(), "owner1");
        Manager m = repo.getManager("mgr1", COMPANY);
        assertNotNull(m);
        assertEquals("mgr1", m.getUserID());
    }

    @Test
    void getManagerPermissions_ReturnsPermissions() {
        repo.storeManager("mgr1", COMPANY, Set.of(Permission.CHANGE_POLICIES), "owner1");
        Set<Permission> perms = repo.getManagerPermissions("mgr1", COMPANY);
        assertTrue(perms.contains(Permission.CHANGE_POLICIES));
    }

    @Test
    void managerPermitedToCreateUpdateDelete_WithPermission_ReturnsTrue() {
        repo.storeManager("mgr1", COMPANY, Set.of(Permission.MANAGE_INVENTORY), "owner1");
        Manager m = repo.getManager("mgr1", COMPANY);
        m.acceptAppointment();
        repo.save(m);
        assertTrue(repo.ManagerPermitedToCreateUpdateDelete("mgr1", COMPANY));
    }

    @Test
    void managerPermitedToCreateUpdateDelete_WithoutPermission_ReturnsFalse() {
        repo.storeManager("mgr1", COMPANY, Set.of(), "owner1");
        assertFalse(repo.ManagerPermitedToCreateUpdateDelete("mgr1", COMPANY));
    }

    @Test
    void managerPermitedToCreateUpdateDelete_NotManager_ReturnsFalse() {
        assertFalse(repo.ManagerPermitedToCreateUpdateDelete("nobody", COMPANY));
    }

    @Test
    void managerPermitToSeeTransactions_WithReportPermission_ReturnsTrue() {
        repo.storeManager("mgr1", COMPANY, Set.of(Permission.GENERATE_SALES_REPORTS), "owner1");
        assertTrue(repo.ManagerPermitToSeeTransactions("mgr1", COMPANY));
    }

    @Test
    void managerPermitToSeeTransactions_WithoutPermission_ReturnsFalse() {
        repo.storeManager("mgr1", COMPANY, Set.of(Permission.MANAGE_INVENTORY), "owner1");
        assertFalse(repo.ManagerPermitToSeeTransactions("mgr1", COMPANY));
    }

    @Test
    void managerPermitToSeeTransactions_NotManager_ReturnsFalse() {
        assertFalse(repo.ManagerPermitToSeeTransactions("nobody", COMPANY));
    }

    @Test
    void isAppointerManager_ReturnsTrue_WhenCorrectAppointer() {
        repo.storeManager("mgr1", COMPANY, Set.of(), "owner1");
        Manager m = repo.getManager("mgr1", COMPANY);
        m.acceptAppointment();
        repo.save(m);
        assertTrue(repo.isAppointerManager("mgr1", COMPANY, "owner1"));
    }

    @Test
    void isAppointerManager_ReturnsFalse_WhenNotAccepted() {
        repo.storeManager("mgr1", COMPANY, Set.of(), "owner1");
        assertFalse(repo.isAppointerManager("mgr1", COMPANY, "owner1"));
    }

    @Test
    void isAppointerManager_ReturnsFalse_WhenWrongAppointer() {
        repo.storeManager("mgr1", COMPANY, Set.of(), "owner1");
        Manager m = repo.getManager("mgr1", COMPANY);
        m.acceptAppointment();
        repo.save(m);
        assertFalse(repo.isAppointerManager("mgr1", COMPANY, "wrong"));
    }

    @Test
    void saveManager_NotFound_Throws() {
        Manager m = new Manager("nobody", COMPANY, Set.of(), "owner1");
        assertThrows(RuntimeException.class, () -> repo.save(m));
    }

    @Test
    void deleteManager_RemovesManager() {
        repo.storeManager("mgr1", COMPANY, Set.of(), "owner1");
        repo.deleteManager("mgr1", COMPANY);
        assertFalse(repo.isManager("mgr1", COMPANY));
    }

    @Test
    void getAllManagersByCompany_ReturnsCorrectManagers() {
        repo.storeManager("m1", COMPANY, Set.of(), "o1");
        repo.storeManager("m2", COMPANY, Set.of(), "o1");
        repo.storeManager("m3", "OtherCompany", Set.of(), "o1");
        List<Manager> managers = repo.getAllManagersByCompany(COMPANY);
        assertEquals(2, managers.size());
    }

    // --- Deletion tests ---

    @Test
    void deleteCompanyMangersAndOwners_RemovesAll() {
        repo.storeOwner("o1", COMPANY, FOUNDER);
        repo.storeManager("m1", COMPANY, Set.of(), "o1");
        repo.deleteCompanyMangersAndOwners(COMPANY);
        assertFalse(repo.isOwner("o1", COMPANY));
        assertFalse(repo.isManager("m1", COMPANY));
    }

    @Test
    void deleteAllRoles_ClearsEverything() {
        repo.storeOwner("o1", COMPANY, FOUNDER);
        repo.storeManager("m1", COMPANY, Set.of(), "o1");
        repo.deleteAllRoles();
        assertFalse(repo.isOwner("o1", COMPANY));
        assertFalse(repo.isManager("m1", COMPANY));
    }

    @Test
    void deleteUserRoles_RunsWithoutError() {
        repo.storeManager("user1", COMPANY, Set.of(), "owner1");
        repo.storeOwner("user1", "Company2", FOUNDER);
        assertDoesNotThrow(() -> repo.deleteUserRoles("user1"));
    }
}
