package Appliction;

import com.ticketing.ticketapp.Appliction.OrderService;
import com.ticketing.ticketapp.Appliction.OrderTransactionException;
import com.ticketing.ticketapp.Appliction.PurchasedService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTransactionalConfigTest {

    @Test
    void orderUseCaseEntryPoints_AreTransactional() throws NoSuchMethodException {
        Method reserve = OrderService.class.getMethod("reserveTickets", String.class, String.class, String.class, java.util.List.class, String.class);
        Method readActive = OrderService.class.getMethod("getActiveOrderTickets", String.class, String.class);
        Method purchase = PurchasedService.class.getMethod("PurchaseTicket", String.class, String.class, String.class, String.class, com.ticketing.ticketapp.Domain.payment.CreditCardDetails.class);

        assertTrue(reserve.isAnnotationPresent(Transactional.class));
        assertTrue(readActive.isAnnotationPresent(Transactional.class));
        assertTrue(purchase.isAnnotationPresent(Transactional.class));
    }

    @Test
    void transactionalException_IsRuntimeException() {
        assertEquals(RuntimeException.class, OrderTransactionException.class.getSuperclass());
    }
}
