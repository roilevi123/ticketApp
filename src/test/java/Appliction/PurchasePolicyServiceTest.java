package Appliction;

import Domain.PurchasePolicy.*;
import Infastructure.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PurchasePolicyServiceTest {

    private PurchasePolicyService policyService;
    private iPurchasePolicyRepository policyRepo;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        policyRepo = mock(iPurchasePolicyRepository.class);
        tokenService = mock(TokenService.class);
        policyService = new PurchasePolicyService(policyRepo, tokenService);
    }

    @Test
    void testCreateAgeLimitPolicy_Success() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);

        String id = policyService.createAgeLimitPolicy(token, "E1", PurchaseTargetType.EVENT, 18);

        assertNotNull(id);
        verify(policyRepo, times(1)).save(any(PurchasePolicy.class));
    }

    @Test
    void testCreateQuantityLimitPolicy_Success() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);

        String id = policyService.createQuantityLimitPolicy(token, "C1", PurchaseTargetType.COMPANY, 1, 5);

        assertNotNull(id);
        verify(policyRepo, times(1)).save(argThat(p -> p.getTargetId().equals("C1")));
    }

    @Test
    void testCreateAndPolicy_Success() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);

        PurchasePolicy p1 = new PurchasePolicy("id1", "E1", PurchaseTargetType.EVENT, new AgeLimitCondition(18));
        PurchasePolicy p2 = new PurchasePolicy("id2", "E1", PurchaseTargetType.EVENT, new QuantityLimitCondition(0, 10));

        when(policyRepo.getPolicy("id1")).thenReturn(p1);
        when(policyRepo.getPolicy("id2")).thenReturn(p2);

        String compositeId = policyService.createAndPolicy(token, "E1", PurchaseTargetType.EVENT, List.of("id1", "id2"));

        assertNotNull(compositeId);
        verify(policyRepo).delete("id1");
        verify(policyRepo).delete("id2");
        verify(policyRepo).save(any(PurchasePolicy.class));
    }

    @Test
    void testCreateOrPolicy_Success() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);

        PurchasePolicy p1 = new PurchasePolicy("id1", "E1", PurchaseTargetType.EVENT, new AgeLimitCondition(21));
        when(policyRepo.getPolicy("id1")).thenReturn(p1);

        String compositeId = policyService.createOrPolicy(token, "E1", PurchaseTargetType.EVENT, List.of("id1"));

        assertNotNull(compositeId);
        verify(policyRepo).save(argThat(p -> p.getRoot() instanceof OrPurchaseComposite));
    }

    @Test
    void testCreatePolicy_InvalidToken_ReturnsNull() {
        String token = "invalid_token";
        when(tokenService.validateToken(token)).thenReturn(false);

        String id = policyService.createAgeLimitPolicy(token, "E1", PurchaseTargetType.EVENT, 18);

        assertNull(id);
        verify(policyRepo, never()).save(any());
    }

    @Test
    void testCreateAndPolicy_WithNonExistentComponent() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);
        when(policyRepo.getPolicy("non_existent")).thenReturn(null);

        String id = policyService.createAndPolicy(token, "E1", PurchaseTargetType.EVENT, List.of("non_existent"));

        assertNotNull(id);
        verify(policyRepo, never()).delete(anyString());
    }

    @Test
    void testCreateComplexNestedPolicy_Success() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);

        PurchasePolicy p1 = new PurchasePolicy("id1", "E1", PurchaseTargetType.EVENT, new AgeLimitCondition(18));
        OrPurchaseComposite orComp = new OrPurchaseComposite();
        PurchasePolicy p2 = new PurchasePolicy("id2", "E1", PurchaseTargetType.EVENT, orComp);

        when(policyRepo.getPolicy("id1")).thenReturn(p1);
        when(policyRepo.getPolicy("id2")).thenReturn(p2);

        String finalId = policyService.createAndPolicy(token, "E1", PurchaseTargetType.EVENT, List.of("id1", "id2"));

        assertNotNull(finalId);
        verify(policyRepo, times(1)).save(argThat(p -> p.getPolicyId().equals(finalId)));
    }

    @Test
    void testPolicyTargetType_AssignedCorrectly() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);

        policyService.createAgeLimitPolicy(token, "CompanyX", PurchaseTargetType.COMPANY, 16);

        verify(policyRepo).save(argThat(p -> p.getTargetType() == PurchaseTargetType.COMPANY));
    }
}