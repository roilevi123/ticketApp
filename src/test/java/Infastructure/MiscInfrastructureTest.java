
        package Infastructure;

import com.ticketing.ticketapp.Domain.Discount.*;
import com.ticketing.ticketapp.Domain.PurchasePolicy.*;
import com.ticketing.ticketapp.Infastructure.*;
import com.ticketing.ticketapp.Appliction.IExternalTicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@ActiveProfiles("test")
@TestPropertySource(properties = {
                "repository.type=DB",
                "spring.datasource.url=jdbc:postgresql://136.115.146.17:5432/ticketapp_test_db",
                "spring.datasource.username=ticketapp_user",
                "spring.datasource.password=BGUticketapp1!",
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        })
@org.springframework.boot.test.context.SpringBootTest
@org.springframework.test.context.ContextConfiguration(classes = com.ticketing.ticketapp.TicketappApplication.class)
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "com.ticketing.ticketapp")
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "com.ticketing.ticketapp")
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
class MiscInfrastructureTest {

    @MockBean
    IExternalTicketService externalTicketService;
    @org.springframework.beans.factory.annotation.Autowired
    private com.ticketing.ticketapp.Domain.Discount.JpaDiscountPolicyRepository jpaDiscountPolicyRepository;

    @BeforeEach
    void setUp() {
        jpaDiscountPolicyRepository.deleteAll();
    }

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

    // --- InMemoryDiscountPolicyRepository (Converted to DB Adapter) ---

    @Test
    void discountRepo_SaveAndGetPolicy() {
        com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter repo =
                new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository);

        DiscountPolicy p = new DiscountPolicy("p1", "e1", DiscountTargetType.EVENT, new ConditionalDiscount("101", 10.0, null, ""));
        repo.save(p);

        DiscountPolicy fetched = repo.getPolicy("p1");
        assertNotNull(fetched);
        assertEquals(p.getPolicyId(), fetched.getPolicyId());
    }

    @Test
    void discountRepo_FindByEvent_ReturnsMatch() {
        com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter repo =
                new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository);

        DiscountPolicy p = new DiscountPolicy("p2", "event1", DiscountTargetType.EVENT, new ConditionalDiscount("102", 10.0, null, ""));
        repo.save(p);
        assertNotNull(repo.findByEvent("event1"));
    }

    @Test
    void discountRepo_FindByCompany_ReturnsMatch() {
        com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter repo =
                new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository);

        DiscountPolicy p = new DiscountPolicy("p3", "compA", DiscountTargetType.COMPANY, new ConditionalDiscount("103", 5.0, null, ""));
        repo.save(p);
        assertNotNull(repo.findByCompany("compA"));
    }

    @Test
    void discountRepo_Delete_RemovesPolicy() {
        com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter repo =
                new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository);

        DiscountPolicy p = new DiscountPolicy("p4", "e1", DiscountTargetType.EVENT, new ConditionalDiscount("104", 10.0, null, ""));
        repo.save(p);
        repo.delete("p4");
        assertNull(repo.getPolicy("p4"));
    }

    @Test
    void discountRepo_DeleteAll_ClearsAll() {
        com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter repo =
                new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository);

        DiscountPolicy p = new DiscountPolicy("p5", "e1", DiscountTargetType.EVENT, new ConditionalDiscount("105", 10.0, null, ""));
        repo.save(p);
        repo.deleteAll();
        assertNull(repo.getPolicy("p5"));
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

//    @Test
//    void paymentMock_ProcessPayment_ReturnsTrue() {
//        PaymentServiceMock svc = new PaymentServiceMock();
//        assertTrue(svc.processPayment("4111111111111111", 100.0));
//    }

//    @Test
//    void paymentMock_Refund_ReturnsTrue() {
//        PaymentServiceMock svc = new PaymentServiceMock();
//        assertTrue(svc.refund("4111111111111111", 100.0));
//    }

    // --- SupplyServiceMock ---

    @Test
    void supplyMock_SupplyToEmail_ReturnsTrue() {
        SupplyServiceMock svc = new SupplyServiceMock();
        assertTrue(svc.supplyToEmail("user@test.com", "Your ticket content"));
    }
}

