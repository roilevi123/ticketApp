package Appliction;

import Domain.QueueAggregates.QueueEntry;
import Domain.QueueAggregates.iQueueRepository;
import Infastructure.QueueRepositoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class QueueServiceTest {

    private QueueService queueService;
    private iQueueRepository queueRepository;
    private final String EVENT_ID = "Event123";

    @BeforeEach
    void setUp() {
        queueRepository = new QueueRepositoryImpl();
        queueRepository.initQueue(EVENT_ID);
        queueService = new QueueService(queueRepository);
    }


    @Test
    void checkStatus_NewUser_Authorized() {
        String status = queueService.checkStatus(EVENT_ID, "User1");
        assertEquals("AUTHORIZED", status);
    }

    @Test
    void checkStatus_HitCapacity_Waiting() {
        for (int i = 0; i < 100; i++) {
            queueService.checkStatus(EVENT_ID, "User" + i);
        }

        String status = queueService.checkStatus(EVENT_ID, "Waiter1");
        assertEquals("WAITING_POSITION_1", status);

        String status2 = queueService.checkStatus(EVENT_ID, "Waiter2");
        assertEquals("WAITING_POSITION_2", status2);
    }

    @Test
    void checkStatus_ReleaseSpace_ProgressesQueue() {
        for (int i = 1; i <= 100; i++) {
            queueService.checkStatus(EVENT_ID, "User" + i);
        }

        queueService.checkStatus(EVENT_ID, "NextInLine");

        List<QueueEntry> queue = queueRepository.getQueue(EVENT_ID);
        queue.get(0).setGrantedAccessTime(System.currentTimeMillis() - (11 * 60 * 1000));

        String status = queueService.checkStatus(EVENT_ID, "NextInLine");
        assertEquals("AUTHORIZED", status);
    }

    @Test
    void checkStatus_UserAlreadyAuthorized_RemainsAuthorized() {
        queueService.checkStatus(EVENT_ID, "User1");
        String status = queueService.checkStatus(EVENT_ID, "User1");
        assertEquals("AUTHORIZED", status);
    }

    @Test
    void checkStatus_OrderIntegrity_NoJumping() {
        for (int i = 0; i < 100; i++) {
            String res = queueService.checkStatus(EVENT_ID, "Active" + i);
            assertEquals("AUTHORIZED", res);
        }

        queueService.checkStatus(EVENT_ID, "FirstWaiter");
        String status2Before = queueService.checkStatus(EVENT_ID, "SecondWaiter");
        assertEquals("WAITING_POSITION_2", status2Before);

        queueRepository.removeFromQueue(EVENT_ID, "FirstWaiter");

        String status2After = queueService.checkStatus(EVENT_ID, "SecondWaiter");
        assertEquals("WAITING_POSITION_1", status2After);
    }
}