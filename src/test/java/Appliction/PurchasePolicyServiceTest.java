package Appliction;

import com.ticketing.ticketapp.Appliction.*;

import com.ticketing.ticketapp.Domain.PurchasePolicy.*;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
        //policyService = new PurchasePolicyService(policyRepo, tokenService);
    }

    @Test
    void testCreateAgeLimitPolicy_Success() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);

        Response<String> id = policyService.createAgeLimitPolicy(token, "E1", PurchaseTargetType.EVENT, 18);

        assertTrue(id.isSuccess());
        assertNotNull(id.getData());
        verify(policyRepo, times(1)).save(any(PurchasePolicy.class));
    }

    @Test
    void testCreateQuantityLimitPolicy_Success() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);

        Response<String> id = policyService.createQuantityLimitPolicy(token, "C1", PurchaseTargetType.COMPANY, 1, 5);

        assertTrue(id.isSuccess());
        assertNotNull(id.getData());
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

        Response<String> compositeId = policyService.createAndPolicy(token, "E1", PurchaseTargetType.EVENT, List.of("id1", "id2"));

        assertTrue(compositeId.isSuccess());
        assertNotNull(compositeId.getData());
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

        Response<String> compositeId = policyService.createOrPolicy(token, "E1", PurchaseTargetType.EVENT, List.of("id1"));

        assertTrue(compositeId.isSuccess());
        assertNotNull(compositeId.getData());
        verify(policyRepo).save(argThat(p -> p.getRoot() instanceof OrPurchaseComposite));
    }

    @Test
    void testCreatePolicy_InvalidToken_ReturnsNull() {
        String token = "invalid_token";
        when(tokenService.validateToken(token)).thenReturn(false);

        Response<String> id = policyService.createAgeLimitPolicy(token, "E1", PurchaseTargetType.EVENT, 18);

        assertTrue(id.isError());
        verify(policyRepo, never()).save(any());
    }

    @Test
    void testCreateAndPolicy_WithNonExistentComponent() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);
        when(policyRepo.getPolicy("non_existent")).thenReturn(null);

        Response<String> id = policyService.createAndPolicy(token, "E1", PurchaseTargetType.EVENT, List.of("non_existent"));

        assertTrue(id.isSuccess());
        assertNotNull(id.getData());
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

        Response<String> finalId = policyService.createAndPolicy(token, "E1", PurchaseTargetType.EVENT, List.of("id1", "id2"));

        assertTrue(finalId.isSuccess());
        assertNotNull(finalId.getData());
        verify(policyRepo, times(1)).save(argThat(p -> p.getPolicyId().equals(finalId.getData())));
    }

    @Test
    void testPolicyTargetType_AssignedCorrectly() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);

        policyService.createAgeLimitPolicy(token, "CompanyX", PurchaseTargetType.COMPANY, 16);

        verify(policyRepo).save(argThat(p -> p.getTargetType() == PurchaseTargetType.COMPANY));
    }

    @Test
    @DisplayName("Test getPoliciesForEventAndCompany - Success and Mapping")
    void testGetPoliciesForEventAndCompany_Success() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);

        PurchasePolicy p = new PurchasePolicy("p1", "E1", PurchaseTargetType.EVENT, new AgeLimitCondition(18));
        when(policyRepo.findByEventAndCompany("E1", "C1")).thenReturn(List.of(p));

        Response<List<PurchasePolicyDTO>> results = policyService.getPoliciesForEventAndCompany(token, "E1", "C1");

        assertTrue(results.isSuccess());
        assertEquals(1, results.getData().size());
        assertEquals("p1", results.getData().get(0).id());
        assertEquals("EVENT", results.getData().get(0).type());
        assertTrue(results.getData().get(0).description().contains("18"));
    }

    @Test
    @DisplayName("Test getPoliciesForEventAndCompany - Recursive Description")
    void testGetPoliciesDescription_Recursive() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);

        AndPurchaseComposite and = new AndPurchaseComposite();
        and.add(new AgeLimitCondition(18));
        and.add(new QuantityLimitCondition(1, 5));

        PurchasePolicy p = new PurchasePolicy("comp1", "E1", PurchaseTargetType.EVENT, and);
        when(policyRepo.findByEventAndCompany("E1", "C1")).thenReturn(List.of(p));

        Response<List<PurchasePolicyDTO>> results = policyService.getPoliciesForEventAndCompany(token, "E1", "C1");

        assertTrue(results.isSuccess());
        String desc = results.getData().get(0).description();
        assertTrue(desc.contains("18") && desc.contains("1") && desc.contains("5"));
    }

    @Test
    @DisplayName("Test getPoliciesForEventAndCompany - Invalid Token Returns Error")
    void testGetPolicies_InvalidToken() {
        String token = "invalid";
        when(tokenService.validateToken(token)).thenReturn(false);

        Response<List<PurchasePolicyDTO>> results = policyService.getPoliciesForEventAndCompany(token, "E1", "C1");

        assertTrue(results.isError());
        verify(policyRepo, never()).findByEventAndCompany(any(), any());
    }

    @Test
    @DisplayName("Test getPoliciesForEventAndCompany - Repository Exception Handling")
    void testGetPolicies_RepositoryException() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);
        when(policyRepo.findByEventAndCompany(any(), any())).thenThrow(new RuntimeException("DB Error"));

        Response<List<PurchasePolicyDTO>> results = policyService.getPoliciesForEventAndCompany(token, "E1", "C1");

        assertTrue(results.isError());
    }

    @Test
    @DisplayName("Test Create Age Policy - Repository Exception Handling")
    void testCreatePolicy_ExceptionDuringSave() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);
        doThrow(new RuntimeException("Save failed")).when(policyRepo).save(any());

        Response<String> id = policyService.createAgeLimitPolicy(token, "E1", PurchaseTargetType.EVENT, 18);

        assertTrue(id.isError());
    }

    @Test
    @DisplayName("Test Create AND Policy - Multiple Children Cleanup")
    void testAndPolicy_DeletesAllFoundComponents() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);

        PurchasePolicy p1 = new PurchasePolicy("id1", "E1", PurchaseTargetType.EVENT, new AgeLimitCondition(18));
        when(policyRepo.getPolicy("id1")).thenReturn(p1);
        when(policyRepo.getPolicy("id2")).thenReturn(null);

        policyService.createAndPolicy(token, "E1", PurchaseTargetType.EVENT, List.of("id1", "id2"));

        verify(policyRepo).delete("id1");
        verify(policyRepo, never()).delete("id2");
    }

    @Test
    @DisplayName("Test DTO TargetType String Conversion")
    void testDto_TargetTypeStringValue() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);

        PurchasePolicy p = new PurchasePolicy("p1", "C1", PurchaseTargetType.COMPANY, new AgeLimitCondition(0));
        when(policyRepo.findByEventAndCompany(any(), any())).thenReturn(List.of(p));

        Response<List<PurchasePolicyDTO>> results = policyService.getPoliciesForEventAndCompany(token, "E1", "C1");

        assertTrue(results.isSuccess());
        assertEquals("COMPANY", results.getData().get(0).type());
    }

    @Test
    @DisplayName("Test getPolicies - No Policies Found Returns Empty List")
    void testGetPolicies_EmptyResult() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);
        when(policyRepo.findByEventAndCompany("E1", "C1")).thenReturn(List.of());

        Response<List<PurchasePolicyDTO>> results = policyService.getPoliciesForEventAndCompany(token, "E1", "C1");

        assertTrue(results.isSuccess());
        assertNotNull(results.getData());
        assertEquals(0, results.getData().size());
    }

    @Test
    @DisplayName("Test validateToken logic inside Policy Creation")
    void testCreateQuantityPolicy_ValidatesTokenOnce() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);

        policyService.createQuantityLimitPolicy(token, "E1", PurchaseTargetType.EVENT, 1, 10);

        verify(tokenService, times(1)).validateToken(token);
    }

    @Test
    @DisplayName("Test OrPolicy - Correct Child Root Addition")
    void testOrPolicy_VerifyRootAddition() {
        String token = "valid_token";
        when(tokenService.validateToken(token)).thenReturn(true);

        AgeLimitCondition ageRoot = new AgeLimitCondition(18);
        PurchasePolicy p1 = new PurchasePolicy("id1", "E1", PurchaseTargetType.EVENT, ageRoot);
        when(policyRepo.getPolicy("id1")).thenReturn(p1);

        policyService.createOrPolicy(token, "E1", PurchaseTargetType.EVENT, List.of("id1"));

        verify(policyRepo).save(argThat(policy ->
            policy.getRoot() instanceof OrPurchaseComposite && policy.getTargetId().equals("E1")
        ));
    }
}
