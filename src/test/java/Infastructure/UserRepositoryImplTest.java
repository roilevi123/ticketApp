package Infastructure;

import com.ticketing.ticketapp.Domain.User.User;
import com.ticketing.ticketapp.Infastructure.UserRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryImplTest {

    private UserRepositoryImpl repo;

    @BeforeEach
    void setUp() {
        repo = new UserRepositoryImpl();
    }

    @Test
    void store_CreatesUser() {
        User user = repo.Store("alice", "pass", 25, "alice@test.com");
        assertNotNull(user);
        assertEquals("alice", user.getName());
    }

    @Test
    void store_DuplicateUsername_Throws() {
        repo.Store("alice", "pass", 25, "alice@test.com");
        assertThrows(RuntimeException.class, () -> repo.Store("alice", "pass2", 26, "alice2@test.com"));
    }

    @Test
    void getUserPassword_Found_ReturnsPassword() {
        repo.Store("bob", "secret123", 30, "bob@test.com");
        assertNotNull(repo.getUserPassword("bob"));
    }

    @Test
    void getUserPassword_NotFound_Throws() {
        assertThrows(RuntimeException.class, () -> repo.getUserPassword("nobody"));
    }

    @Test
    void getUserByUsername_Found_ReturnsUser() {
        repo.Store("carol", "pw", 22, "carol@test.com");
        User found = repo.getUserByUsername("carol");
        assertNotNull(found);
        assertEquals("carol", found.getName());
    }

    @Test
    void getUserByUsername_NotFound_ReturnsNull() {
        assertNull(repo.getUserByUsername("ghost"));
    }

    @Test
    void getUserByID_Found_ReturnsUser() {
        User user = repo.Store("dave", "pw", 20, "dave@test.com");
        assertNotNull(repo.getUserByID(user.getID()));
    }

    @Test
    void getUserByID_NotFound_ReturnsNull() {
        assertNull(repo.getUserByID("non-existent-id"));
    }

    @Test
    void getUsernameByID_Found_ReturnsName() {
        User user = repo.Store("eve", "pw", 28, "eve@test.com");
        assertEquals("eve", repo.getUsernameByID(user.getID()));
    }

    @Test
    void getUsernameByID_NotFound_Throws() {
        assertThrows(RuntimeException.class, () -> repo.getUsernameByID("bad-id"));
    }

    @Test
    void usernameExists_TrueAfterStore() {
        repo.Store("frank", "pw", 33, "frank@test.com");
        assertTrue(repo.usernameExists("frank"));
    }

    @Test
    void usernameExists_FalseForUnknown() {
        assertFalse(repo.usernameExists("nobody"));
    }

    @Test
    void save_UpdatesEmail() {
        User user = repo.Store("grace", "pw", 27, "old@test.com");
        User copy = repo.getUserByID(user.getID());
        copy.setEmail("new@test.com");
        repo.save(copy);
        assertEquals("new@test.com", repo.getUserByUsername("grace").getEmail());
    }

    @Test
    void save_RenamesUser_UpdatesUsernameIndex() {
        User user = repo.Store("henry", "pw", 35, "h@test.com");
        User copy = repo.getUserByID(user.getID());
        copy.setName("henry-new");
        repo.save(copy);
        assertNotNull(repo.getUserByUsername("henry-new"));
        assertNull(repo.getUserByUsername("henry"));
    }

    @Test
    void save_NotFound_Throws() {
        User phantom = new User("phantom", "pw", 20, "p@test.com");
        assertThrows(RuntimeException.class, () -> repo.save(phantom));
    }

    @Test
    void save_UsernameTaken_Throws() {
        repo.Store("alice", "pw", 25, "a@test.com");
        User bob = repo.Store("bob", "pw", 30, "b@test.com");
        User bobCopy = repo.getUserByID(bob.getID());
        bobCopy.setName("alice");
        assertThrows(RuntimeException.class, () -> repo.save(bobCopy));
    }

    @Test
    void deleteAll_ClearsAllUsers() {
        repo.Store("grace", "pw", 27, "g@test.com");
        repo.deleteAll();
        assertNull(repo.getUserByUsername("grace"));
    }

    @Test
    void deleteUser_RemovesById() {
        User user = repo.Store("iris", "pw", 31, "iris@test.com");
        repo.deleteUser(user.getID());
        assertNull(repo.getUserByID(user.getID()));
        assertNull(repo.getUserByUsername("iris"));
    }

    @Test
    void deleteUser_NonExistent_DoesNotThrow() {
        assertDoesNotThrow(() -> repo.deleteUser("non-existent-id"));
    }
}
