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
            logger.info("User of token {} is attempting to create a simple discount for the company: ", token, companyName);
            validateAuthority(token, companyName);
            String userID = tokenService.extractUserId(token);
            if(userRepository.isUserSuspendedNow(userID))
                throw new Exception("User is suspended");

            String discountId = UUID.randomUUID().toString();
            DiscountComponent simple = new ConditionalDiscount(discountId, percentage, null, "no conditions");
            logger.info("User {} created a simple discount for the company {} successfully", userID, companyName);
            return Response.success(savePolicy(targetId, type, simple));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> createQuantityDiscount(String token, String targetId, DiscountTargetType type, double percentage, int minQuantity, String companyName) {
        try {
            logger.info("User of token {} is attempting to create quantity discount for the company: ", token, companyName);
            validateAuthority(token, companyName);
            String userID = tokenService.extractUserId(token);
            if(userRepository.isUserSuspendedNow(userID))
                throw new Exception("User is suspended");

            String conditionDesc = "quantity >= " + minQuantity;
            String discountId = UUID.randomUUID().toString();
            DiscountComponent discount = new ConditionalDiscount(discountId, percentage, ctx -> ctx.getQuantity() >= minQuantity, conditionDesc);
            logger.info("User {} created quantity discount for the company {} successfully", userID, companyName);
            return Response.success(savePolicy(targetId, type, discount));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> createTimeLimitedDiscount(String token, String targetId, DiscountTargetType type, double percentage, Date deadline, String companyName) {
        try {
            logger.info("User of token {} is attempting to create a time limited discount for the company: ", token, companyName);
            validateAuthority(token, companyName);
            String userID = tokenService.extractUserId(token);
            if(userRepository.isUserSuspendedNow(userID))
                throw new Exception("User is suspended");

            String conditionDesc = "purchase date before " + deadline.toString();
            String discountId = UUID.randomUUID().toString();
            DiscountComponent discount = new ConditionalDiscount(discountId, percentage, ctx -> ctx.getPurchaseDate().before(deadline), conditionDesc);
            logger.info("User {} created a time limited discount for the company {} successfully", userID, companyName);
            return Response.success(savePolicy(targetId, type, discount));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> createCouponDiscount(String token, String targetId, DiscountTargetType type, String code, double percentage, String companyName) {
        try {
            logger.info("User of token {} is attempting to create coupon discount for the company: ", token, companyName);
            validateAuthority(token, companyName);
            String userID = tokenService.extractUserId(token);
            if(userRepository.isUserSuspendedNow(userID))
                throw new Exception("User is suspended");

            String discountId = UUID.randomUUID().toString();
            DiscountComponent coupon = new CouponDiscount(discountId, code, percentage);
            logger.info("User {} created coupon discount for the company {} successfully", userID, companyName);
            return Response.success(savePolicy(targetId, type, coupon));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> createSumDiscountPolicy(String token, String targetId, DiscountTargetType type, List<String> existingPolicyIds, String companyName) {
        try {
            logger.info("User of token {} is attempting to create sum discount policy for the company: ", token, companyName);
            validateAuthority(token, companyName);
            String userID = tokenService.extractUserId(token);
            if(userRepository.isUserSuspendedNow(userID))
                throw new Exception("User is suspended");

            String compositeId = UUID.randomUUID().toString();
            SumDiscountComposite sumComposite = new SumDiscountComposite(compositeId);

            for (String id : existingPolicyIds) {
                DiscountPolicy existingPolicy = discountRepo.getPolicy(id);
                if (existingPolicy != null) {
                    sumComposite.add(existingPolicy.getRoot());
                    discountRepo.delete(id);
                } else {
                    throw new Exception("Policy not found: " + id);
                }
            }
            logger.info("User {} created sum discount policy for the company {} successfully", userID, companyName);
            return Response.success(savePolicy(targetId, type, sumComposite));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> createMaxDiscountPolicy(String token, String targetId, DiscountTargetType type, List<String> existingPolicyIds, String companyName) {
        try {
            logger.info("User of token {} is attempting to create max discount policy for the company: ", token, companyName);
            validateAuthority(token, companyName);
            String userID = tokenService.extractUserId(token);
            if(userRepository.isUserSuspendedNow(userID))
                throw new Exception("User is suspended");

            String compositeId = UUID.randomUUID().toString();
            MaxDiscountComposite maxComposite = new MaxDiscountComposite(compositeId);

            for (String id : existingPolicyIds) {
                DiscountPolicy existingPolicy = discountRepo.getPolicy(id);
                if (existingPolicy != null) {
                    maxComposite.add(existingPolicy.getRoot());
                    discountRepo.delete(id);
                } else {
                    throw new Exception("Policy not found: " + id);
                }
            }
            logger.info("User {} created max discount policy for the company {} successfully", userID, companyName);
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
            logger.info("User of token {} is attempting to get all discount for the event {} of the company {}", token, eventId, companyName);
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
            logger.info("User of token {} got all discounts for the event {} of the company {} successfully", token, eventId, companyName);
            return Response.success(dtos);
        } catch (Exception e) {
            logger.error("Error retrieving discounts: " + e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<Double> calculatePriceAfterDiscounts(String token, String eventId, String companyName, double originalPrice, int quantity, String coupon) {
        try {
            logger.info("User of token {} is attempting to calculate price after discounts for the event {} of the company:", token, eventId, companyName);
            if (!tokenService.validateToken(token)) {
                return Response.error("Invalid token");
            }

            String userID = tokenService.extractUserId(token);
            if(userRepository.isUserSuspendedNow(userID))
                throw new Exception("User is suspended");

            PurchaseContext context = new PurchaseContext(quantity, coupon, new Date());

            DiscountPolicy eventPolicy = discountRepo.findByEvent(eventId);
            DiscountPolicy companyPolicy = discountRepo.findByCompany(companyName);

            String combinedRootId = UUID.randomUUID().toString();
            MaxDiscountComposite combinedRoot = new MaxDiscountComposite(combinedRootId);

            if (eventPolicy != null) {
                combinedRoot.add(eventPolicy.getRoot());
            }
            if (companyPolicy != null) {
                combinedRoot.add(companyPolicy.getRoot());
            }

            double discountAmount = combinedRoot.calculateDiscount(originalPrice, context);
            logger.info("User {} calculated price after discounts for the event {} of the company {} successfully", userID, eventId, companyName);
            return Response.success(originalPrice - discountAmount);

        } catch (Exception e) {
            logger.error("Error calculating discount: " + e.getMessage());
            return Response.error(e.getMessage());
        }
    }
}