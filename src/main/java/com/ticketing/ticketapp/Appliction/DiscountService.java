package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.Discount.*;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DiscountService {
    private final iDiscountPolicyRepository discountRepo;
    private final TokenService tokenService;
    private final PurchasedService purchasedService;
    private static final Logger logger = LoggerFactory.getLogger(DiscountService.class);
    private IUserRepository userRepository;


    public DiscountService(iDiscountPolicyRepository discountRepo, TokenService tokenService, PurchasedService purchasedService, IUserRepository userRepository) {
        this.discountRepo = discountRepo;
        this.tokenService = tokenService;
        this.purchasedService = purchasedService;
        this.userRepository = userRepository;

    }

    public Response<String> createSimpleDiscount(String token, String targetId, DiscountTargetType type, double percentage, String companyName) {
        try {
            validateAuthority(token, companyName);
            String userID = tokenService.extractUserId(token);
            if(userRepository.isUserSuspendedNow(userID))
                throw new Exception("User is suspended");
            DiscountComponent simple = new ConditionalDiscount(percentage, null, "no conditions");
            return Response.success(savePolicy(targetId, type, simple));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> createQuantityDiscount(String token, String targetId, DiscountTargetType type, double percentage, int minQuantity, String companyName) {
        try {
            validateAuthority(token, companyName);
            String userID = tokenService.extractUserId(token);
            if(userRepository.isUserSuspendedNow(userID))
                throw new Exception("User is suspended");
            String conditionDesc = "quantity >= " + minQuantity;
            DiscountComponent discount = new ConditionalDiscount(percentage, ctx -> ctx.getQuantity() >= minQuantity, conditionDesc);
            return Response.success(savePolicy(targetId, type, discount));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> createTimeLimitedDiscount(String token, String targetId, DiscountTargetType type, double percentage, Date deadline, String companyName) {
        try {
            validateAuthority(token, companyName);
            String userID = tokenService.extractUserId(token);
            if(userRepository.isUserSuspendedNow(userID))
                throw new Exception("User is suspended");
            String conditionDesc = "purchase date before " + deadline.toString();
            DiscountComponent discount = new ConditionalDiscount(percentage, ctx -> ctx.getPurchaseDate().before(deadline), conditionDesc);
            return Response.success(savePolicy(targetId, type, discount));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> createCouponDiscount(String token, String targetId, DiscountTargetType type, String code, double percentage, String companyName) {
        try {
            validateAuthority(token, companyName);
            String userID = tokenService.extractUserId(token);
            if(userRepository.isUserSuspendedNow(userID))
                throw new Exception("User is suspended");
            DiscountComponent coupon = new CouponDiscount(code, percentage);
            return Response.success(savePolicy(targetId, type, coupon));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> createSumDiscountPolicy(String token, String targetId, DiscountTargetType type, List<String> existingPolicyIds, String companyName) {
        try {
            validateAuthority(token, companyName);
            String userID = tokenService.extractUserId(token);
            if(userRepository.isUserSuspendedNow(userID))
                throw new Exception("User is suspended");
            SumDiscountComposite sumComposite = new SumDiscountComposite();

            for (String id : existingPolicyIds) {
                DiscountPolicy existingPolicy = discountRepo.getPolicy(id);
                if (existingPolicy != null) {
                    sumComposite.add(existingPolicy.getRoot());
                    discountRepo.delete(id);
                } else {
                    throw new Exception("Policy not found: " + id);
                }
            }

            return Response.success(savePolicy(targetId, type, sumComposite));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> createMaxDiscountPolicy(String token, String targetId, DiscountTargetType type, List<String> existingPolicyIds, String companyName) {
        try {
            validateAuthority(token, companyName);
            String userID = tokenService.extractUserId(token);
            if(userRepository.isUserSuspendedNow(userID))
                throw new Exception("User is suspended");
            MaxDiscountComposite maxComposite = new MaxDiscountComposite();

            for (String id : existingPolicyIds) {
                DiscountPolicy existingPolicy = discountRepo.getPolicy(id);
                if (existingPolicy != null) {
                    maxComposite.add(existingPolicy.getRoot());
                    discountRepo.delete(id);
                } else {
                    throw new Exception("Policy not found: " + id);
                }
            }

            return Response.success(savePolicy(targetId, type, maxComposite));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    private void validateAuthority(String token, String companyName) throws Exception {
        if (!tokenService.validateToken(token)) throw new Exception("Invalid token");
        String userId = tokenService.extractUserId(token);
        if (!purchasedService.isAuthorized(companyName, userId)) throw new Exception("Unauthorized");
    }

    private String savePolicy(String targetId, DiscountTargetType type, DiscountComponent root) {
        String policyId = UUID.randomUUID().toString();
        DiscountPolicy policy = new DiscountPolicy(policyId, targetId, type, root);
        discountRepo.save(policy);
        return policyId;
    }

    public Response<List<DiscountPolicyDTO>> getDiscountsForEventAndCompany(String token, String eventId, String companyName) {
        try {
            validateAuthority(token, companyName);
            List<DiscountPolicy> policies = discountRepo.findByEventAndCompany(eventId, companyName);
            List<DiscountPolicyDTO> dtos = policies.stream()
                    .map(p -> new DiscountPolicyDTO(
                            p.getPolicyId(),
                            p.getTargetId(),
                            p.getTargetType().toString(),
                            p.getRoot().getDescription(),
                            0.0
                    ))
                    .collect(Collectors.toList());
            return Response.success(dtos);
        } catch (Exception e) {
            logger.error("Error retrieving discounts: " + e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<Double> calculatePriceAfterDiscounts(String token, String eventId, String companyName, double originalPrice, int quantity, String coupon) {
        try {
            if (!tokenService.validateToken(token)) {
                return Response.error("Invalid token");
            }

            String userID = tokenService.extractUserId(token);
            if(userRepository.isUserSuspendedNow(userID))
                throw new Exception("User is suspended");

            PurchaseContext context = new PurchaseContext(quantity, coupon, new Date());

            DiscountPolicy eventPolicy = discountRepo.findByEvent(eventId);
            DiscountPolicy companyPolicy = discountRepo.findByCompany(companyName);

            MaxDiscountComposite combinedRoot = new MaxDiscountComposite();

            if (eventPolicy != null) {
                combinedRoot.add(eventPolicy.getRoot());
            }
            if (companyPolicy != null) {
                combinedRoot.add(companyPolicy.getRoot());
            }

            double discountAmount = combinedRoot.calculateDiscount(originalPrice, context);
            return Response.success(originalPrice - discountAmount);

        } catch (Exception e) {
            logger.error("Error calculating discount: " + e.getMessage());
            return Response.error(e.getMessage());
        }
    }
}
