package Appliction;

import com.ticketing.ticketapp.Appliction.*;

import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.QueueAggregates.QueueEntry;
import com.ticketing.ticketapp.Appliction.INotifier;
import com.ticketing.ticketapp.Domain.QueueAggregates.iQueueRepository;
import com.ticketing.ticketapp.Infastructure.QueueRepositoryImpl;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
class QueueServiceTest {

    private QueueService queueService;
    private iQueueRepository queueRepository;

    @Mock
    private TokenService tokenService;
    @Mock
    private INotifier notifier;
    @Mock
    private iAdminRepository adminRepository;

    private final String EVENT_ID = "Event123";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        queueRepository = new QueueRepositoryImpl();
        queueRepository.initQueue(EVENT_ID);
        when(tokenService.validateToken(anyString())).thenReturn(true);
        when(tokenService.extractUserId(anyString())).thenAnswer(inv -> inv.getArgument(0));
        queueService = new QueueService(queueRepository, tokenService, notifier, adminRepository);
    }

    @Test
    void checkStatus_NewUser_Authorized() {
        Response<String> status = queueService.checkStatus("User1", EVENT_ID);
        assertTrue(status.isSuccess());
        assertEquals("AUTHORIZED", status.getData());
    }

    @Test
    void checkStatus_HitCapacity_Waiting() {
        for (int i = 0; i < 100; i++) {
            queueService.checkStatus("User" + i, EVENT_ID);
        }

        Response<String> status = queueService.checkStatus("Waiter1", EVENT_ID);
        assertTrue(status.isSuccess());
        assertEquals("WAITING_POSITION_1", status.getData());

        Response<String> status2 = queueService.checkStatus("Waiter2", EVENT_ID);
        assertTrue(status2.isSuccess());
        assertEquals("WAITING_POSITION_2", status2.getData());
    }

    @Test
    void checkStatus_ReleaseSpace_ProgressesQueue() {
        for (int i = 1; i <= 100; i++) {
            queueService.checkStatus("User" + i, EVENT_ID);
        }

        queueService.checkStatus("NextInLine", EVENT_ID);

        List<QueueEntry> queue = queueRepository.getQueue(EVENT_ID);
        queue.get(0).setGrantedAccessTime(System.currentTimeMillis() - (11 * 60 * 1000));

        Response<String> status = queueService.checkStatus("NextInLine", EVENT_ID);
        assertTrue(status.isSuccess());
        assertEquals("AUTHORIZED", status.getData());
    }

    @Test
    void checkStatus_UserAlreadyAuthorized_RemainsAuthorized() {
        queueService.checkStatus("User1", EVENT_ID);
        Response<String> status = queueService.checkStatus("User1", EVENT_ID);
        assertTrue(status.isSuccess());
        assertEquals("AUTHORIZED", status.getData());
    }

    @Test
    void checkStatus_OrderIntegrity_NoJumping() {
        for (int i = 0; i < 100; i++) {
            Response<String> res = queueService.checkStatus("Active" + i, EVENT_ID);
            assertTrue(res.isSuccess());
            assertEquals("AUTHORIZED", res.getData());
        }

        queueService.checkStatus("FirstWaiter", EVENT_ID);
        Response<String> status2Before = queueService.checkStatus("SecondWaiter", EVENT_ID);
        assertTrue(status2Before.isSuccess());
        assertEquals("WAITING_POSITION_2", status2Before.getData());

        queueRepository.removeFromQueue(EVENT_ID, "FirstWaiter");

        Response<String> status2After = queueService.checkStatus("SecondWaiter", EVENT_ID);
        assertTrue(status2After.isSuccess());
        assertEquals("WAITING_POSITION_1", status2After.getData());
    }
    @Test
    void setFlowRate_Success_AsAdmin() {
        String adminToken = "admin";
        when(tokenService.validateToken(adminToken)).thenReturn(true);
        when(tokenService.extractUserId(adminToken)).thenReturn("admin");
        when(adminRepository.isAdmin("admin")).thenReturn(true);

        Response<String> result = queueService.setFlowRate(adminToken, EVENT_ID, 50);

        assertTrue(result.isSuccess());
    }

    @Test
    void setFlowRate_Failure_InvalidToken() {
        String badToken = "bad_token";
        when(tokenService.validateToken(badToken)).thenReturn(false);

        Response<String> result = queueService.setFlowRate(badToken, EVENT_ID, 50);

        assertTrue(result.isError());
    }

    @Test
    void setFlowRate_Failure_NotAdmin() {
        String userToken = "regular_user";
        when(tokenService.validateToken(userToken)).thenReturn(true);
        when(tokenService.extractUserId(userToken)).thenReturn("regular_user");
        when(adminRepository.isAdmin("regular_user")).thenReturn(false);

        Response<String> result = queueService.setFlowRate(userToken, EVENT_ID, 50);

        assertTrue(result.isError());
    }

    @Test
    void getQueueForAdmin_Success_AsAdmin() {
        String adminToken = "admin";
        when(tokenService.validateToken(adminToken)).thenReturn(true);
        when(tokenService.extractUserId(adminToken)).thenReturn("admin");
        when(adminRepository.isAdmin("admin")).thenReturn(true);

        queueService.checkStatus("User1", EVENT_ID);
        queueService.checkStatus("User2", EVENT_ID);

        Response<List<QueueEntry>> result = queueService.getQueueForAdmin(adminToken, EVENT_ID);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    void getQueueForAdmin_Failure_InvalidToken() {
        String badToken = "bad_token";
        when(tokenService.validateToken(badToken)).thenReturn(false);

        Response<List<QueueEntry>> result = queueService.getQueueForAdmin(badToken, EVENT_ID);

        assertTrue(result.isError());
    }

    @Test
    void getQueueForAdmin_Failure_NotAdmin() {
        String userToken = "regular_user";
        when(tokenService.validateToken(userToken)).thenReturn(true);
        when(tokenService.extractUserId(userToken)).thenReturn("regular_user");
        when(adminRepository.isAdmin("regular_user")).thenReturn(false);

        Response<List<QueueEntry>> result = queueService.getQueueForAdmin(userToken, EVENT_ID);

        assertTrue(result.isError());
    }

    @Test
    void clearQueueForAdmin_Success_AsAdmin() {
        String adminToken = "admin";
        when(tokenService.validateToken(adminToken)).thenReturn(true);
        when(tokenService.extractUserId(adminToken)).thenReturn("admin");
        when(adminRepository.isAdmin("admin")).thenReturn(true);

        queueService.checkStatus("User1", EVENT_ID);
        queueService.checkStatus("User2", EVENT_ID);

        Response<String> result = queueService.clearQueueForAdmin(adminToken, EVENT_ID);

        assertTrue(result.isSuccess());

        Response<List<QueueEntry>> queueAfterClear =
                queueService.getQueueForAdmin(adminToken, EVENT_ID);

        assertTrue(queueAfterClear.isSuccess());
        assertTrue(queueAfterClear.getData().isEmpty());
    }

    @Test
    void clearQueueForAdmin_Failure_InvalidToken() {
        String badToken = "bad_token";
        when(tokenService.validateToken(badToken)).thenReturn(false);

        Response<String> result = queueService.clearQueueForAdmin(badToken, EVENT_ID);

        assertTrue(result.isError());
    }

    @Test
    void clearQueueForAdmin_Failure_NotAdmin() {
        String userToken = "regular_user";
        when(tokenService.validateToken(userToken)).thenReturn(true);
        when(tokenService.extractUserId(userToken)).thenReturn("regular_user");
        when(adminRepository.isAdmin("regular_user")).thenReturn(false);

        Response<String> result = queueService.clearQueueForAdmin(userToken, EVENT_ID);

        assertTrue(result.isError());
    }
}
