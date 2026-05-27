package Appliction;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.PurchasePolicy.*;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PurchasePolicyServiceExtendedTest {

    @Mock private iPurchasePolicyRepository policyRepo;
    @Mock private TokenService tokenService;
    @Mock private IUserRepository userRepository;
    private PurchasePolicyService policyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        policyService = new PurchasePolicyService(policyRepo, tokenService, userRepository);
    }

    // ── getMaxSeatsForEvent ───────────────────────────────────────────────────

    @Test
    void getMaxSeatsForEvent_BothPoliciesNull_ReturnsNullSuccess() {
        when(policyRepo.findByEvent("e1")).thenReturn(null);
        when(policyRepo.findByCompany("Corp")).thenReturn(null);

        var result = policyService.getMaxSeatsForEvent("e1", "Corp");

        assertTrue(result.isSuccess());
        assertNull(result.getData());
    }

    @Test
    void getMaxSeatsForEvent_OnlyEventPolicy_ReturnsEventMax() {
        PurchasePolicy eventPolicy = new PurchasePolicy("p1", "e1", PurchaseTargetType.EVENT,
                new QuantityLimitCondition(1, 5));
        when(policyRepo.findByEvent("e1")).thenReturn(eventPolicy);
        when(policyRepo.findByCompany("Corp")).thenReturn(null);

        var result = policyService.getMaxSeatsForEvent("e1", "Corp");

        assertTrue(result.isSuccess());
        assertEquals(5, result.getData());
    }

    @Test
    void getMaxSeatsForEvent_OnlyCompanyPolicy_ReturnsCompanyMax() {
        PurchasePolicy companyPolicy = new PurchasePolicy("p2", "Corp", PurchaseTargetType.COMPANY,
                new QuantityLimitCondition(1, 10));
        when(policyRepo.findByEvent("e1")).thenReturn(null);
        when(policyRepo.findByCompany("Corp")).thenReturn(companyPolicy);

        var result = policyService.getMaxSeatsForEvent("e1", "Corp");

        assertTrue(result.isSuccess());
        assertEquals(10, result.getData());
    }

    @Test
    void getMaxSeatsForEvent_BothPolicies_ReturnsMostRestrictive() {
        PurchasePolicy eventPolicy = new PurchasePolicy("p1", "e1", PurchaseTargetType.EVENT,
                new QuantityLimitCondition(1, 3));
        PurchasePolicy companyPolicy = new PurchasePolicy("p2", "Corp", PurchaseTargetType.COMPANY,
                new QuantityLimitCondition(1, 8));
        when(policyRepo.findByEvent("e1")).thenReturn(eventPolicy);
        when(policyRepo.findByCompany("Corp")).thenReturn(companyPolicy);

        var result = policyService.getMaxSeatsForEvent("e1", "Corp");

        assertTrue(result.isSuccess());
        assertEquals(3, result.getData()); // min(3, 8)
    }

    @Test
    void getMaxSeatsForEvent_AgeLimitPolicy_ReturnsNullMax() {
        // AgeLimitCondition.getMaxSeats() returns null
        PurchasePolicy agePolicy = new PurchasePolicy("p1", "e1", PurchaseTargetType.EVENT,
                new AgeLimitCondition(18));
        when(policyRepo.findByEvent("e1")).thenReturn(agePolicy);
        when(policyRepo.findByCompany("Corp")).thenReturn(null);

        var result = policyService.getMaxSeatsForEvent("e1", "Corp");

        assertTrue(result.isSuccess());
        assertNull(result.getData()); // null max = no restriction
    }

    // ── invalid token for create methods ─────────────────────────────────────

    @Test
    void createQuantityLimitPolicy_InvalidToken_ReturnsError() {
        when(tokenService.validateToken("bad")).thenReturn(false);

        var result = policyService.createQuantityLimitPolicy("bad", "e1", PurchaseTargetType.EVENT, 1, 5);

        assertTrue(result.isError());
        verify(policyRepo, never()).save(any());
    }

    @Test
    void createOrPolicy_InvalidToken_ReturnsError() {
        when(tokenService.validateToken("bad")).thenReturn(false);

        var result = policyService.createOrPolicy("bad", "e1", PurchaseTargetType.EVENT,
                java.util.List.of("p1"));

        assertTrue(result.isError());
        verify(policyRepo, never()).save(any());
    }

    @Test
    void getPoliciesForEventAndCompany_InvalidToken_ReturnsError() {
        when(tokenService.validateToken("bad")).thenReturn(false);

        var result = policyService.getPoliciesForEventAndCompany("bad", "e1", "Corp");

        assertTrue(result.isError());
        verify(policyRepo, never()).findByEventAndCompany(any(), any());
    }
}
