package Infastructure;

import com.ticketing.ticketapp.Domain.Notification.INotificationRepository;
import com.ticketing.ticketapp.Domain.Notification.Notification;
import com.ticketing.ticketapp.Infastructure.Broadcaster;
import com.ticketing.ticketapp.Infastructure.NotificationRepositoryImpl;
import com.ticketing.ticketapp.Infastructure.NotifierImpl;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotifierAndBroadcasterTest {

    // ── NotifierImpl ─────────────────────────────────────────────────────────

    private Broadcaster broadcaster;
    private NotifierImpl notifier;

    @BeforeEach
    void setUp() {
        broadcaster = mock(Broadcaster.class);
        notifier = new NotifierImpl(broadcaster);
    }

    @Test
    void notifyUser_CallsBroadcastWithJson() {
        notifier.notifyUser("user1", "Title", "Body");
        verify(broadcaster).broadcast(eq("user1"), contains("\"title\":\"Title\""));
        verify(broadcaster).broadcast(eq("user1"), contains("\"message\":\"Body\""));
    }

    @Test
    void notifyUserWithSender_CallsBroadcastWithSenderInJson() {
        notifier.notifyUserWithSender("user1", "sender99", "Title", "Body");
        verify(broadcaster).broadcast(eq("user1"), contains("\"senderId\":\"sender99\""));
        verify(broadcaster).broadcast(eq("user1"), contains("\"title\":\"Title\""));
    }

    @Test
    void broadcast_CallsBroadcastToAll() {
        notifier.broadcast("Alert", "System-wide message");
        verify(broadcaster).broadcastToAll(contains("\"title\":\"Alert\""));
        verify(broadcaster).broadcastToAll(contains("\"message\":\"System-wide message\""));
    }

    @Test
    void notifyUser_NullTitle_ProducesEmptyJsonString() {
        notifier.notifyUser("user1", null, "Body");
        verify(broadcaster).broadcast(eq("user1"), contains("\"title\":\"\""));
    }

    @Test
    void notifyUser_NullMessage_ProducesEmptyJsonString() {
        notifier.notifyUser("user1", "Title", null);
        verify(broadcaster).broadcast(eq("user1"), contains("\"message\":\"\""));
    }

    @Test
    void notifyUser_MessageWithSpecialChars_EscapesCorrectly() {
        notifier.notifyUser("user1", "A\"B", "line1\nline2");
        verify(broadcaster).broadcast(eq("user1"), contains("\\\""));
        verify(broadcaster).broadcast(eq("user1"), contains("\\n"));
    }

    @Test
    void notifyUser_MessageWithTab_EscapesCorrectly() {
        notifier.notifyUser("user1", "Title", "col1\tcol2");
        verify(broadcaster).broadcast(eq("user1"), contains("\\t"));
    }

    @Test
    void notifyUser_MessageWithBackslash_EscapesCorrectly() {
        notifier.notifyUser("user1", "Title", "C:\\path");
        verify(broadcaster).broadcast(eq("user1"), contains("\\\\"));
    }

    @Test
    void notifyUser_MessageWithCarriageReturn_EscapesCorrectly() {
        notifier.notifyUser("user1", "Title", "line1\rline2");
        verify(broadcaster).broadcast(eq("user1"), contains("\\r"));
    }

    // ── Broadcaster ──────────────────────────────────────────────────────────

    @Test
    void broadcaster_BroadcastToOfflineUser_SavesToRepo() {
        INotificationRepository notifRepo = mock(INotificationRepository.class);
        Broadcaster real = new Broadcaster(notifRepo);

        real.broadcast("user1", "Hello");

        verify(notifRepo).save("user1", "Hello");
    }

    @Test
    @SuppressWarnings("unchecked")
    void broadcaster_BroadcastToOnlineUser_CallsConnectionAsync() throws InterruptedException {
        INotificationRepository notifRepo = mock(INotificationRepository.class);
        Broadcaster real = new Broadcaster(notifRepo);
        Consumer<String> connection = mock(Consumer.class);

        real.register("user1", connection);
        real.broadcast("user1", "Hello");

        verify(notifRepo).save("user1", "Hello");
        verify(connection, timeout(500)).accept("Hello");
    }

    @Test
    @SuppressWarnings("unchecked")
    void broadcaster_Unregister_RemovesConnection() throws InterruptedException {
        INotificationRepository notifRepo = mock(INotificationRepository.class);
        when(notifRepo.getUnread("user1")).thenReturn(Collections.emptyList());
        Broadcaster real = new Broadcaster(notifRepo);
        Consumer<String> connection = mock(Consumer.class);

        real.register("user1", connection);
        real.unregister("user1");
        real.broadcast("user1", "After unregister");

        verify(notifRepo).save("user1", "After unregister");
        Thread.sleep(200);
        verify(connection, never()).accept(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void broadcaster_BroadcastToAll_SendsToAllActiveConnections() {
        INotificationRepository notifRepo = mock(INotificationRepository.class);
        when(notifRepo.getUnread(any())).thenReturn(Collections.emptyList());
        Broadcaster real = new Broadcaster(notifRepo);
        Consumer<String> conn1 = mock(Consumer.class);
        Consumer<String> conn2 = mock(Consumer.class);

        real.register("user1", conn1);
        real.register("user2", conn2);
        real.broadcastToAll("System alert");

        verify(notifRepo).save("BROADCAST", "System alert");
        verify(conn1, timeout(500)).accept("System alert");
        verify(conn2, timeout(500)).accept("System alert");
    }

    @Test
    @SuppressWarnings("unchecked")
    void broadcaster_RegisterWithUnreadNotifications_DeliversThem() throws InterruptedException {
        INotificationRepository notifRepo = mock(INotificationRepository.class);
        Notification unread = new Notification("n1", "user1", "Missed message");
        when(notifRepo.getUnread("user1")).thenReturn(List.of(unread));
        Broadcaster real = new Broadcaster(notifRepo);
        Consumer<String> connection = mock(Consumer.class);

        real.register("user1", connection);

        verify(connection, timeout(500)).accept("Missed message");
    }

    @Test
    @SuppressWarnings("unchecked")
    void broadcaster_RegisterWithNoUnreadNotifications_DoesNotCallConnection() throws InterruptedException {
        INotificationRepository notifRepo = mock(INotificationRepository.class);
        when(notifRepo.getUnread("user1")).thenReturn(Collections.emptyList());
        Broadcaster real = new Broadcaster(notifRepo);
        Consumer<String> connection = mock(Consumer.class);

        real.register("user1", connection);
        Thread.sleep(200);

        verify(connection, never()).accept(any());
    }

    // ── NotificationRepositoryImpl ───────────────────────────────────────────

    @Test
    void notificationRepo_Save_StoresNotification() {
        NotificationRepositoryImpl repo = new NotificationRepositoryImpl();
        repo.save("user1", "Hello");
        assertEquals(1, repo.getAll("user1").size());
        assertEquals("Hello", repo.getAll("user1").get(0).getMessage());
    }

    @Test
    void notificationRepo_GetAll_ReturnsEmptyListForUnknownUser() {
        NotificationRepositoryImpl repo = new NotificationRepositoryImpl();
        assertTrue(repo.getAll("unknown").isEmpty());
    }

    @Test
    void notificationRepo_GetUnread_ReturnsOnlyUnread() {
        NotificationRepositoryImpl repo = new NotificationRepositoryImpl();
        repo.save("user1", "msg1");
        repo.save("user1", "msg2");
        String firstId = repo.getAll("user1").get(0).getId();
        repo.markAsRead("user1", firstId);

        List<Notification> unread = repo.getUnread("user1");
        assertEquals(1, unread.size());
        assertEquals("msg2", unread.get(0).getMessage());
    }

    @Test
    void notificationRepo_MarkAsRead_SetsReadFlag() {
        NotificationRepositoryImpl repo = new NotificationRepositoryImpl();
        repo.save("user1", "msg1");
        String id = repo.getAll("user1").get(0).getId();

        assertFalse(repo.getAll("user1").get(0).isRead());
        repo.markAsRead("user1", id);
        assertTrue(repo.getAll("user1").get(0).isRead());
    }

    @Test
    void notificationRepo_MarkAsUnread_ClearsReadFlag() {
        NotificationRepositoryImpl repo = new NotificationRepositoryImpl();
        repo.save("user1", "msg1");
        String id = repo.getAll("user1").get(0).getId();
        repo.markAsRead("user1", id);
        assertTrue(repo.getAll("user1").get(0).isRead());

        repo.markAsUnread("user1", id);
        assertFalse(repo.getAll("user1").get(0).isRead());
    }

    @Test
    void notificationRepo_MarkAllAsRead_SetsAllAsRead() {
        NotificationRepositoryImpl repo = new NotificationRepositoryImpl();
        repo.save("user1", "msg1");
        repo.save("user1", "msg2");
        repo.markAllAsRead("user1");

        repo.getAll("user1").forEach(n -> assertTrue(n.isRead()));
    }

    @Test
    void notificationRepo_MarkAllAsRead_NoNotificationsForUser_DoesNotThrow() {
        NotificationRepositoryImpl repo = new NotificationRepositoryImpl();
        assertDoesNotThrow(() -> repo.markAllAsRead("unknown"));
    }

    @Test
    void notificationRepo_MarkAsRead_NoNotificationsForUser_DoesNotThrow() {
        NotificationRepositoryImpl repo = new NotificationRepositoryImpl();
        assertDoesNotThrow(() -> repo.markAsRead("unknown", "any-id"));
    }

    @Test
    void notificationRepo_MarkAsUnread_NoNotificationsForUser_DoesNotThrow() {
        NotificationRepositoryImpl repo = new NotificationRepositoryImpl();
        assertDoesNotThrow(() -> repo.markAsUnread("unknown", "any-id"));
    }

    @Test
    void notificationRepo_DeleteAll_ClearsAllNotifications() {
        NotificationRepositoryImpl repo = new NotificationRepositoryImpl();
        repo.save("user1", "msg1");
        repo.save("user2", "msg2");
        repo.deleteAll();
        assertTrue(repo.getAll("user1").isEmpty());
        assertTrue(repo.getAll("user2").isEmpty());
    }

    @Test
    void notificationRepo_MultipleUsers_AreIsolated() {
        NotificationRepositoryImpl repo = new NotificationRepositoryImpl();
        repo.save("user1", "msg1");
        repo.save("user2", "msg2");
        assertEquals(1, repo.getAll("user1").size());
        assertEquals(1, repo.getAll("user2").size());
    }

    // ── TokenService (additional coverage) ───────────────────────────────────

    @Test
    void tokenService_GenerateCompanyToken_FourArgs_IsValidAndExtractable() {
        TokenService ts = new TokenService();
        String token = ts.generateCompanyToken("uid1", "alice", "OWNER", "AcmeCorp");
        assertNotNull(token);
        assertTrue(ts.validateToken(token));
        assertEquals("uid1", ts.extractUserId(token));
        assertEquals("alice", ts.extractUsername(token));
    }

    @Test
    void tokenService_GenerateCompanyToken_FiveArgs_IsValidAndExtractable() {
        TokenService ts = new TokenService();
        String token = ts.generateCompanyToken("uid2", "bob", "MANAGER", "AcmeCorp",
                List.of("MANAGE_EVENTS", "VIEW_REPORTS"));
        assertNotNull(token);
        assertTrue(ts.validateToken(token));
        assertEquals("uid2", ts.extractUserId(token));
    }

    @Test
    void tokenService_IsBannedToken_ReturnsFalse_WhenUserNotBanned() {
        TokenService ts = new TokenService();
        String token = ts.generateMemberToken("uid1", "alice");
        assertFalse(ts.isBannedToken(token));
    }

    @Test
    void tokenService_IsBannedToken_ReturnsTrue_WhenUserBanned() {
        TokenService ts = new TokenService();
        String token = ts.generateMemberToken("uid1", "alice");
        ts.banUser("uid1");
        assertTrue(ts.isBannedToken(token));
    }

    @Test
    void tokenService_IsBannedToken_ReturnsFalse_ForInvalidToken() {
        TokenService ts = new TokenService();
        assertFalse(ts.isBannedToken("not.a.valid.jwt"));
    }

    @Test
    void tokenService_IsBannedToken_ReturnsFalse_AfterUnban() {
        TokenService ts = new TokenService();
        String token = ts.generateMemberToken("uid1", "alice");
        ts.banUser("uid1");
        assertTrue(ts.isBannedToken(token));
        ts.unbanUser("uid1");
        assertFalse(ts.isBannedToken(token));
    }
}
