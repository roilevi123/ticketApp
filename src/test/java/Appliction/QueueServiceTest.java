package Appliction;

import com.ticketing.ticketapp.Appliction.*;

import com.ticketing.ticketapp.Domain.QueueAggregates.QueueEntry;
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

class QueueServiceTest {

    private QueueService queueService;
    private iQueueRepository queueRepository;

    @Mock
    private TokenService tokenService;

    private final String EVENT_ID = "Event123";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        queueRepository = new QueueRepositoryImpl();
        queueRepository.initQueue(EVENT_ID);
        // token == userId for test simplicity
        when(tokenService.validateToken(anyString())).thenReturn(true);
        when(tokenService.extractUserId(anyString())).thenAnswer(inv -> inv.getArgument(0));
        queueService = new QueueService(queueRepository, tokenService);
    }


    @Test
    void checkStatus_NewUser_Authorized() {
        String status = queueService.checkStatus("User1", EVENT_ID);
        assertEquals("AUTHORIZED", status);
    }

    @Test
    void checkStatus_HitCapacity_Waiting() {
        for (int i = 0; i < 100; i++) {
            queueService.checkStatus("User" + i, EVENT_ID);
        }

        String status = queueService.checkStatus("Waiter1", EVENT_ID);
        assertEquals("WAITING_POSITION_1", status);

        String status2 = queueService.checkStatus("Waiter2", EVENT_ID);
        assertEquals("WAITING_POSITION_2", status2);
    }

    @Test
    void checkStatus_ReleaseSpace_ProgressesQueue() {
        for (int i = 1; i <= 100; i++) {
            queueService.checkStatus("User" + i, EVENT_ID);
        }

        queueService.checkStatus("NextInLine", EVENT_ID);

        List<QueueEntry> queue = queueRepository.getQueue(EVENT_ID);
        queue.get(0).setGrantedAccessTime(System.currentTimeMillis() - (11 * 60 * 1000));

        String status = queueService.checkStatus("NextInLine", EVENT_ID);
        assertEquals("AUTHORIZED", status);
    }

    @Test
    void checkStatus_UserAlreadyAuthorized_RemainsAuthorized() {
        queueService.checkStatus("User1", EVENT_ID);
        String status = queueService.checkStatus("User1", EVENT_ID);
        assertEquals("AUTHORIZED", status);
    }

    @Test
    void checkStatus_OrderIntegrity_NoJumping() {
        for (int i = 0; i < 100; i++) {
            String res = queueService.checkStatus("Active" + i, EVENT_ID);
            assertEquals("AUTHORIZED", res);
        }

        queueService.checkStatus("FirstWaiter", EVENT_ID);
        String status2Before = queueService.checkStatus("SecondWaiter", EVENT_ID);
        assertEquals("WAITING_POSITION_2", status2Before);

        queueRepository.removeFromQueue(EVENT_ID, "FirstWaiter");

        String status2After = queueService.checkStatus("SecondWaiter", EVENT_ID);
        assertEquals("WAITING_POSITION_1", status2After);
    }
}
