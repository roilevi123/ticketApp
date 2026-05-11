package Appliction;

import com.ticketing.ticketapp.Appliction.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ticketing.ticketapp.Domain.Discount.*;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class DiscountServiceTest {

    private iDiscountPolicyRepository discountRepo;
    private TokenService tokenService;
    private PurchasedService purchasedService;
    private DiscountService discountService;

    @BeforeEach
    void setUp() {
        discountRepo = mock(iDiscountPolicyRepository.class);
        tokenService = mock(TokenService.class);
        purchasedService = mock(PurchasedService.class);
        discountService = new DiscountService(discountRepo, tokenService, purchasedService);

        when(tokenService.validateToken(anyString())).thenReturn(true);
        when(tokenService.extractUsername(anyString())).thenReturn("user123");
        when(purchasedService.isAuthorized(anyString(), anyString())).thenReturn(true);
    }

    @Test
    void test1_CreateSimpleDiscount_Success() {
        String result = discountService.createSimpleDiscount("token", "e1", DiscountTargetType.EVENT, 10.0, "Comp");
        assertNotNull(result);
        assertNotEquals("Unauthorized", result);
        verify(discountRepo, times(1)).save(any(DiscountPolicy.class));
    }

    @Test
    void test2_QuantityDiscount_Met() {
        ArgumentCaptor<DiscountPolicy> captor = ArgumentCaptor.forClass(DiscountPolicy.class);
        discountService.createQuantityDiscount("token", "e1", DiscountTargetType.EVENT, 20.0, 5, "Comp");
        verify(discountRepo).save(captor.capture());

        PurchaseContext ctx = new PurchaseContext(5, "", new Date());
        assertEquals(20.0, captor.getValue().getRoot().calculateDiscount(100.0, ctx));
    }

    @Test
    void test3_QuantityDiscount_NotMet() {
        ArgumentCaptor<DiscountPolicy> captor = ArgumentCaptor.forClass(DiscountPolicy.class);
        discountService.createQuantityDiscount("token", "e1", DiscountTargetType.EVENT, 20.0, 5, "Comp");
        verify(discountRepo).save(captor.capture());

        PurchaseContext ctx = new PurchaseContext(4, "", new Date());
        assertEquals(0.0, captor.getValue().getRoot().calculateDiscount(100.0, ctx));
    }

    @Test
    void test4_TimeLimitedDiscount_Valid() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        Date deadline = cal.getTime();

        ArgumentCaptor<DiscountPolicy> captor = ArgumentCaptor.forClass(DiscountPolicy.class);
        discountService.createTimeLimitedDiscount("token", "e1", DiscountTargetType.EVENT, 15.0, deadline, "Comp");
        verify(discountRepo).save(captor.capture());

        PurchaseContext ctx = new PurchaseContext(1, "", new Date());
        assertEquals(15.0, captor.getValue().getRoot().calculateDiscount(100.0, ctx));
    }

    @Test
    void test5_TimeLimitedDiscount_Expired() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        Date deadline = cal.getTime();

        ArgumentCaptor<DiscountPolicy> captor = ArgumentCaptor.forClass(DiscountPolicy.class);
        discountService.createTimeLimitedDiscount("token", "e1", DiscountTargetType.EVENT, 15.0, deadline, "Comp");
        verify(discountRepo).save(captor.capture());

        PurchaseContext ctx = new PurchaseContext(1, "", new Date());
        assertEquals(0.0, captor.getValue().getRoot().calculateDiscount(100.0, ctx));
    }

    @Test
    void test6_CouponDiscount_Valid() {
        ArgumentCaptor<DiscountPolicy> captor = ArgumentCaptor.forClass(DiscountPolicy.class);
        discountService.createCouponDiscount("token", "e1", DiscountTargetType.EVENT, "PROMO", 25.0, "Comp");
        verify(discountRepo).save(captor.capture());

        PurchaseContext ctx = new PurchaseContext(1, "PROMO", new Date());
        assertEquals(25.0, captor.getValue().getRoot().calculateDiscount(100.0, ctx));
    }

    @Test
    void test7_CouponDiscount_Invalid() {
        ArgumentCaptor<DiscountPolicy> captor = ArgumentCaptor.forClass(DiscountPolicy.class);
        discountService.createCouponDiscount("token", "e1", DiscountTargetType.EVENT, "PROMO", 25.0, "Comp");
        verify(discountRepo).save(captor.capture());

        PurchaseContext ctx = new PurchaseContext(1, "WRONG", new Date());
        assertEquals(0.0, captor.getValue().getRoot().calculateDiscount(100.0, ctx));
    }

    @Test
    void test8_MaxDiscountPolicy_Success() {
        String p1Id = "p1", p2Id = "p2";
        DiscountPolicy p1 = new DiscountPolicy(p1Id, "e1", DiscountTargetType.EVENT, new ConditionalDiscount(10.0, null));
        DiscountPolicy p2 = new DiscountPolicy(p2Id, "e1", DiscountTargetType.EVENT, new ConditionalDiscount(30.0, null));

        when(discountRepo.getPolicy(p1Id)).thenReturn(p1);
        when(discountRepo.getPolicy(p2Id)).thenReturn(p2);

        ArgumentCaptor<DiscountPolicy> captor = ArgumentCaptor.forClass(DiscountPolicy.class);
        discountService.createMaxDiscountPolicy("token", "e1", DiscountTargetType.EVENT, Arrays.asList(p1Id, p2Id), "Comp");

        verify(discountRepo).save(captor.capture());
        verify(discountRepo).delete(p1Id);
        verify(discountRepo).delete(p2Id);

        PurchaseContext ctx = new PurchaseContext(1, "", new Date());
        assertEquals(30.0, captor.getValue().getRoot().calculateDiscount(100.0, ctx));
    }

    @Test
    void test9_SumDiscountPolicy_Success() {
        String p1Id = "p1", p2Id = "p2";
        DiscountPolicy p1 = new DiscountPolicy(p1Id, "e1", DiscountTargetType.EVENT, new ConditionalDiscount(10.0, null));
        DiscountPolicy p2 = new DiscountPolicy(p2Id, "e1", DiscountTargetType.EVENT, new ConditionalDiscount(5.0, null));

        when(discountRepo.getPolicy(p1Id)).thenReturn(p1);
        when(discountRepo.getPolicy(p2Id)).thenReturn(p2);

        ArgumentCaptor<DiscountPolicy> captor = ArgumentCaptor.forClass(DiscountPolicy.class);
        discountService.createSumDiscountPolicy("token", "e1", DiscountTargetType.EVENT, Arrays.asList(p1Id, p2Id), "Comp");

        verify(discountRepo).save(captor.capture());
        verify(discountRepo).delete(p1Id);
        verify(discountRepo).delete(p2Id);

        PurchaseContext ctx = new PurchaseContext(1, "", new Date());
        assertEquals(15.0, captor.getValue().getRoot().calculateDiscount(100.0, ctx));
    }

    @Test
    void test10_Unauthorized_Failure() {
        when(purchasedService.isAuthorized(anyString(), anyString())).thenReturn(false);
        String result = discountService.createSimpleDiscount("token", "e1", DiscountTargetType.EVENT, 10.0, "Comp");
        assertNull(result);
        verify(discountRepo, never()).save(any());
    }
}
