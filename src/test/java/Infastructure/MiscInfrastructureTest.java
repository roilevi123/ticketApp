package Infastructure;

import com.ticketing.ticketapp.Domain.Discount.*;
import com.ticketing.ticketapp.Domain.PurchasePolicy.*;
import com.ticketing.ticketapp.Infastructure.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MiscInfrastructureTest {

    // --- AdminRepositoryImpl ---

    @Test
    void adminRepo_IsAdmin_ReturnsFalseByDefault() {
        AdminRepositoryImpl repo = new AdminRepositoryImpl();
        assertFalse(repo.isAdmin("anyone"));
    }

    @Test
    void adminRepo_DeleteAll_RunsWithoutError() {
        AdminRepositoryImpl repo = new AdminRepositoryImpl();
        assertDoesNotThrow(repo::deleteAll);
    }

    // --- PendingNotificationRepositoryImpl ---

    @Test
    void pendingNotification_SaveAndRetrieve() {
        PendingNotificationRepositoryImpl repo = new PendingNotificationRepositoryImpl();
        repo.save("user1", "Hello");
        repo.save("user1", "World");
        List<String> msgs = repo.retrieveAndDelete("user1");
        assertEquals(2, msgs.size());
        assertTrue(msgs.contains("Hello"));
        assertTrue(msgs.contains("World"));
    }

    @Test
    void pendingNotification_RetrieveNonExistent_ReturnsEmpty() {
        PendingNotificationRepositoryImpl repo = new PendingNotificationRepositoryImpl();
        List<String> msgs = repo.retrieveAndDelete("nobody");
        assertNotNull(msgs);
        assertTrue(msgs.isEmpty());
    }

    @Test
    void pendingNotification_RetrieveDeletesMessages() {
        PendingNotificationRepositoryImpl repo = new PendingNotificationRepositoryImpl();
        repo.save("user1", "msg");
        repo.retrieveAndDelete("user1");
        List<String> second = repo.retrieveAndDelete("user1");
        assertTrue(second.isEmpty());
    }

    // --- InMemoryDiscountPolicyRepository (direct, not spy) ---

    @Test
    void discountRepo_SaveAndGetPolicy() {
        InMemoryDiscountPolicyRepository repo = new InMemoryDiscountPolicyRepository();
        DiscountPolicy p = new DiscountPolicy("p1", "e1", DiscountTargetType.EVENT, new ConditionalDiscount(10.0, null, ""));
        repo.save(p);
        assertEquals(p, repo.getPolicy("p1"));
    }

    @Test
    void discountRepo_FindByEvent_ReturnsMatch() {
        InMemoryDiscountPolicyRepository repo = new InMemoryDiscountPolicyRepository();
        DiscountPolicy p = new DiscountPolicy("p1", "event1", DiscountTargetType.EVENT, new ConditionalDiscount(10.0, null, ""));
        repo.save(p);
        assertNotNull(repo.findByEvent("event1"));
    }

    @Test
    void discountRepo_FindByCompany_ReturnsMatch() {
        InMemoryDiscountPolicyRepository repo = new InMemoryDiscountPolicyRepository();
        DiscountPolicy p = new DiscountPolicy("p1", "compA", DiscountTargetType.COMPANY, new ConditionalDiscount(5.0, null, ""));
        repo.save(p);
        assertNotNull(repo.findByCompany("compA"));
    }

    @Test
    void discountRepo_Delete_RemovesPolicy() {
        InMemoryDiscountPolicyRepository repo = new InMemoryDiscountPolicyRepository();
        DiscountPolicy p = new DiscountPolicy("p1", "e1", DiscountTargetType.EVENT, new ConditionalDiscount(10.0, null, ""));
        repo.save(p);
        repo.delete("p1");
        assertNull(repo.getPolicy("p1"));
    }

    @Test
    void discountRepo_DeleteAll_ClearsAll() {
        InMemoryDiscountPolicyRepository repo = new InMemoryDiscountPolicyRepository();
        DiscountPolicy p = new DiscountPolicy("p1", "e1", DiscountTargetType.EVENT, new ConditionalDiscount(10.0, null, ""));
        repo.save(p);
        repo.deleteAll();
        assertNull(repo.getPolicy("p1"));
    }

    // --- InMemoryPurchasePolicyRepository ---

    @Test
    void purchasePolicyRepo_SaveAndGetPolicy() {
        InMemoryPurchasePolicyRepository repo = new InMemoryPurchasePolicyRepository();
        PurchasePolicy p = new PurchasePolicy("pp1", "e1", PurchaseTargetType.EVENT, null);
        repo.save(p);
        assertEquals(p, repo.getPolicy("pp1"));
    }

    @Test
    void purchasePolicyRepo_FindByEvent_ReturnsMatch() {
        InMemoryPurchasePolicyRepository repo = new InMemoryPurchasePolicyRepository();
        PurchasePolicy p = new PurchasePolicy("pp1", "event1", PurchaseTargetType.EVENT, null);
        repo.save(p);
        assertNotNull(repo.findByEvent("event1"));
    }

    @Test
    void purchasePolicyRepo_FindByCompany_ReturnsMatch() {
        InMemoryPurchasePolicyRepository repo = new InMemoryPurchasePolicyRepository();
        PurchasePolicy p = new PurchasePolicy("pp1", "compA", PurchaseTargetType.COMPANY, null);
        repo.save(p);
        assertNotNull(repo.findByCompany("compA"));
    }

    @Test
    void purchasePolicyRepo_FindByEventAndCompany_ReturnsBoth() {
        InMemoryPurchasePolicyRepository repo = new InMemoryPurchasePolicyRepository();
        PurchasePolicy e = new PurchasePolicy("pp1", "event1", PurchaseTargetType.EVENT, null);
        PurchasePolicy c = new PurchasePolicy("pp2", "compA", PurchaseTargetType.COMPANY, null);
        repo.save(e);
        repo.save(c);
        List<PurchasePolicy> results = repo.findByEventAndCompany("event1", "compA");
        assertEquals(2, results.size());
    }

    @Test
    void purchasePolicyRepo_Delete_RemovesPolicy() {
        InMemoryPurchasePolicyRepository repo = new InMemoryPurchasePolicyRepository();
        PurchasePolicy p = new PurchasePolicy("pp1", "event1", PurchaseTargetType.EVENT, null);
        repo.save(p);
        repo.delete("pp1");
        assertNull(repo.getPolicy("pp1"));
        assertNull(repo.findByEvent("event1"));
    }

    @Test
    void purchasePolicyRepo_DeleteAll_ClearsAll() {
        InMemoryPurchasePolicyRepository repo = new InMemoryPurchasePolicyRepository();
        PurchasePolicy p = new PurchasePolicy("pp1", "e1", PurchaseTargetType.EVENT, null);
        repo.save(p);
        repo.deleteAll();
        assertNull(repo.getPolicy("pp1"));
    }

    // --- BarcodeGeneratorMock ---

    @Test
    void barcodeGenerator_GenerateBarcode_ReturnsFormattedString() {
        BarcodeGeneratorMock gen = new BarcodeGeneratorMock();
        String barcode = gen.generateBarcode("event1", "ticket1");
        assertNotNull(barcode);
        assertTrue(barcode.startsWith("BARCODE-event1-ticket1-"));
    }

    @Test
    void barcodeGenerator_IsAvailable_ReturnsTrue() {
        BarcodeGeneratorMock gen = new BarcodeGeneratorMock();
        assertTrue(gen.isAvailable());
    }

    // --- PaymentServiceMock ---

    @Test
    void paymentMock_ProcessPayment_ReturnsTrue() {
        PaymentServiceMock svc = new PaymentServiceMock();
        assertTrue(svc.processPayment("4111111111111111", 100.0));
    }

    @Test
    void paymentMock_Refund_ReturnsTrue() {
        PaymentServiceMock svc = new PaymentServiceMock();
        assertTrue(svc.refund("4111111111111111", 100.0));
    }

    // --- SupplyServiceMock ---

    @Test
    void supplyMock_SupplyToEmail_ReturnsTrue() {
        SupplyServiceMock svc = new SupplyServiceMock();
        assertTrue(svc.supplyToEmail("user@test.com", "Your ticket content"));
    }
}
