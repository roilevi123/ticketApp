package Appliction;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.Discount.*;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DiscountServiceExtendedTest {

    @Mock private iDiscountPolicyRepository discountRepo;
    @Mock private TokenService tokenService;
    @Mock private PurchasedService purchasedService;
    @Mock private IUserRepository userRepository;

    private DiscountService discountService;

    private final String TOKEN = "valid-token";
    private final String EVENT = "event1";
    private final String COMPANY = "Corp";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        discountService = new DiscountService(discountRepo, tokenService, purchasedService, userRepository);
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn("uid");
        when(purchasedService.isAuthorized(anyString(), anyString())).thenReturn(true);
    }

    // ── calculatePriceAfterDiscounts: policy branches ─────────────────────────

    @Test
    void calculatePriceAfterDiscounts_EventPolicyPresent_AppliesDiscount() {
        DiscountPolicy eventPolicy = new DiscountPolicy("p1", EVENT, DiscountTargetType.EVENT,
                new CouponDiscount("SAVE10", 10.0));
        when(discountRepo.findByEvent(EVENT)).thenReturn(eventPolicy);
        when(discountRepo.findByCompany(COMPANY)).thenReturn(null);

        var result = discountService.calculatePriceAfterDiscounts(TOKEN, EVENT, COMPANY, 100.0, 1, "SAVE10");

        assertTrue(result.isSuccess());
        assertEquals(90.0, result.getData(), 0.001);
    }

    @Test
    void calculatePriceAfterDiscounts_CompanyPolicyPresent_AppliesDiscount() {
        DiscountPolicy companyPolicy = new DiscountPolicy("p2", COMPANY, DiscountTargetType.COMPANY,
                new CouponDiscount("CORP20", 20.0));
        when(discountRepo.findByEvent(EVENT)).thenReturn(null);
        when(discountRepo.findByCompany(COMPANY)).thenReturn(companyPolicy);

        var result = discountService.calculatePriceAfterDiscounts(TOKEN, EVENT, COMPANY, 100.0, 1, "CORP20");

        assertTrue(result.isSuccess());
        assertEquals(80.0, result.getData(), 0.001);
    }

    @Test
    void calculatePriceAfterDiscounts_BothPolicies_MaxDiscountWins() {
        DiscountPolicy eventPolicy = new DiscountPolicy("p1", EVENT, DiscountTargetType.EVENT,
                new CouponDiscount("EVT5", 5.0));
        DiscountPolicy companyPolicy = new DiscountPolicy("p2", COMPANY, DiscountTargetType.COMPANY,
                new CouponDiscount("CORP30", 30.0));
        when(discountRepo.findByEvent(EVENT)).thenReturn(eventPolicy);
        when(discountRepo.findByCompany(COMPANY)).thenReturn(companyPolicy);

        var result = discountService.calculatePriceAfterDiscounts(TOKEN, EVENT, COMPANY, 100.0, 1, "CORP30");

        assertTrue(result.isSuccess());
        assertEquals(70.0, result.getData(), 0.001);
    }

    // ── createMaxDiscountPolicy: missing policy throws error ──────────────────

    @Test
    void createMaxDiscountPolicy_MissingPolicy_ReturnsError() {
        when(discountRepo.getPolicy("existing")).thenReturn(
                new DiscountPolicy("existing", EVENT, DiscountTargetType.EVENT,
                        new ConditionalDiscount(10.0, null, "")));
        when(discountRepo.getPolicy("missing")).thenReturn(null);

        var result = discountService.createMaxDiscountPolicy(TOKEN, EVENT, DiscountTargetType.EVENT,
                List.of("existing", "missing"), COMPANY);

        assertTrue(result.isError());
        assertTrue(result.getMessage().contains("Policy not found"));
        verify(discountRepo, never()).save(any());
    }

    // ── getDiscountsForEventAndCompany: unauthorized ───────────────────────────

    @Test
    void getDiscountsForEventAndCompany_Unauthorized_ReturnsError() {
        when(purchasedService.isAuthorized(anyString(), anyString())).thenReturn(false);

        var result = discountService.getDiscountsForEventAndCompany(TOKEN, EVENT, COMPANY);

        assertTrue(result.isError());
        verify(discountRepo, never()).findByEventAndCompany(any(), any());
    }

    @Test
    void getDiscountsForEventAndCompany_InvalidToken_ReturnsError() {
        when(tokenService.validateToken("bad")).thenReturn(false);

        var result = discountService.getDiscountsForEventAndCompany("bad", EVENT, COMPANY);

        assertTrue(result.isError());
        verify(discountRepo, never()).findByEventAndCompany(any(), any());
    }

    // ── createSumDiscountPolicy: unauthorized ─────────────────────────────────

    @Test
    void createSumDiscountPolicy_Unauthorized_ReturnsError() {
        when(purchasedService.isAuthorized(anyString(), anyString())).thenReturn(false);

        var result = discountService.createSumDiscountPolicy(TOKEN, EVENT, DiscountTargetType.EVENT,
                List.of("p1"), COMPANY);

        assertTrue(result.isError());
        verify(discountRepo, never()).save(any());
    }
}
