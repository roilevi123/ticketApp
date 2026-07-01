package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.PurchasePolicy.*;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class PurchasePolicyService {
    private final iPurchasePolicyRepository purchaseRepo;
    private final TokenService tokenService;
    private static final Logger logger = LoggerFactory.getLogger(PurchasePolicyService.class);
    private IUserRepository userRepository;

    public PurchasePolicyService(iPurchasePolicyRepository purchaseRepo, TokenService tokenService, IUserRepository userRepository) {
        this.purchaseRepo = purchaseRepo;
        this.tokenService = tokenService;
        this.userRepository=userRepository;
    }

    public Response<String> createAgeLimitPolicy(String token, String targetId, PurchaseTargetType type, int minAge) {
        try {
            logger.info("User of token {} is attempting to create age limit policy (min age: {}) for company {}", token, minAge,targetId);
            validateToken(token);
            String userID = tokenService.extractUserId(token);
            String username = tokenService.extractUsername(token);
            if ((userID != null && userRepository.isUserSuspendedNow(userID)) ||
                    (username != null && userRepository.isUserSuspendedNow(username))) {
                throw new Exception("User is suspended");
            }
            PurchaseComponent condition = new AgeLimitCondition(minAge);
            logger.info("User {} created age limit policy (min age: {}) for the company {} successfully", userID, minAge, targetId);
            return Response.success(saveToRepo(targetId, type, condition));
        }
        catch (DataAccessException e) {
            return Response.error("Database unavailable");
        }
        catch (Exception e) {
            logger.error("age limit policy creation failed", e);
            return Response.error(e.getMessage());
        }
    }

    public Response<String> createQuantityLimitPolicy(String token, String targetId, PurchaseTargetType type, int min, int max) {
        try {
            logger.info("User of token {} is attempting to create quantity limit policy (min: {}, max: {}) for company {}", token,min, max, targetId);
            validateToken(token);
            String userID = tokenService.extractUserId(token);
            String username = tokenService.extractUsername(token);
            if ((userID != null && userRepository.isUserSuspendedNow(userID)) ||
                    (username != null && userRepository.isUserSuspendedNow(username))) {
                throw new Exception("User is suspended");
            }
            PurchaseComponent condition = new QuantityLimitCondition(min, max);
            logger.info("User {} created quantity limit policy (min: {}, max: {}) for the company {} successfully", userID, min, max, targetId);
            return Response.success(saveToRepo(targetId, type, condition));
        }
        catch (DataAccessException e) {
            return Response.error("Database unavailable");
        }
        catch (Exception e) {
            logger.error("qoute quantity limit policy creation failed", e);
            return Response.error(e.getMessage());
        }
    }

    public Response<String> createAndPolicy(String token, String targetId, PurchaseTargetType type, List<String> componentIds) {
        try {
            logger.info("User of token {} is attempting to create AND policy for company {}", token, targetId);
            validateToken(token);
            String userID = tokenService.extractUserId(token);
            String username = tokenService.extractUsername(token);
            if ((userID != null && userRepository.isUserSuspendedNow(userID)) ||
                    (username != null && userRepository.isUserSuspendedNow(username))) {
                throw new Exception("User is suspended");
            }
            AndPurchaseComposite andComposite = new AndPurchaseComposite();

            for (String id : componentIds) {
                PurchasePolicy part = purchaseRepo.getPolicy(id);
                if (part != null) {
                    andComposite.add(part.getRoot());
                    purchaseRepo.delete(id);
                }
            }
            logger.info("User {} created AND policy for the company {} successfully", userID, targetId);
            return Response.success(saveToRepo(targetId, type, andComposite));
        }
        catch (DataAccessException e) {
            return Response.error("Database unavailable");
        }
        catch (Exception e) {
            logger.error("and policy creation failed");
            return Response.error(e.getMessage());
        }
    }

    public Response<String> createOrPolicy(String token, String targetId, PurchaseTargetType type, List<String> componentIds) {
        try {
            logger.info("User of token {} is attempting to create OR policy for company {}", token, targetId);
            validateToken(token);
            String userID = tokenService.extractUserId(token);
            String username = tokenService.extractUsername(token);
            if ((userID != null && userRepository.isUserSuspendedNow(userID)) ||
                    (username != null && userRepository.isUserSuspendedNow(username))) {
                throw new Exception("User is suspended");
            }
            OrPurchaseComposite orComposite = new OrPurchaseComposite();

            for (String id : componentIds) {
                PurchasePolicy part = purchaseRepo.getPolicy(id);
                if (part != null) {
                    orComposite.add(part.getRoot());
                    purchaseRepo.delete(id);
                }
            }
            logger.info("User {} created OR policy for the company {} successfully", userID, targetId);

            return Response.success(saveToRepo(targetId, type, orComposite));
        }
        catch (DataAccessException e) {
            return Response.error("Database unavailable");
        }
        catch (Exception e) {
            logger.error("or policy creation failed");
            return Response.error(e.getMessage());
        }
    }

    private String saveToRepo(String targetId, PurchaseTargetType type, PurchaseComponent root) {
        String policyId = UUID.randomUUID().toString();
        PurchasePolicy policy = new PurchasePolicy(policyId, targetId, type, root);
        purchaseRepo.save(policy);
        return policyId;
    }

    private void validateToken(String token) throws Exception {
        if (!tokenService.validateToken(token)) {
            throw new Exception("Invalid session token");
        }
    }

    /**
     * Returns the most restrictive seat cap enforced by the event-level and/or
     * company-level purchase policies.  Returns {@code null} (no cap) when
     * neither policy defines a quantity limit.
     */
    public Response<Integer> getMaxSeatsForEvent(String eventName, String companyName) {
        try {
            PurchasePolicy eventPolicy   = purchaseRepo.findByEvent(eventName);
            PurchasePolicy companyPolicy = purchaseRepo.findByCompany(companyName);

            Integer eventMax   = (eventPolicy   != null) ? eventPolicy.getRoot().getMaxSeats()   : null;
            Integer companyMax = (companyPolicy != null) ? companyPolicy.getRoot().getMaxSeats() : null;

            if (eventMax == null && companyMax == null) return Response.success(null);
            if (eventMax   == null) return Response.success(companyMax);
            if (companyMax == null) return Response.success(eventMax);
            return Response.success(Math.min(eventMax, companyMax));
        }
        catch (DataAccessException e) {
            return Response.error("Database unavailable");
        }
        catch (Exception e) {
            logger.error("Error getting max seats for event {}: {}", eventName, e.getMessage());
            return Response.error(e.getMessage());
        }
    }


    public Response<List<PurchasePolicyDTO>> getPoliciesForEventAndCompany(String token, String eventId, String companyName) {
        try {
            logger.info("User of token {} is attempting to get policies for event {} of the comapny {}", token, eventId, companyName);
            validateToken(token);

            List<PurchasePolicy> policies = purchaseRepo.findByEventAndCompany(eventId, companyName);
            List<PurchasePolicyDTO> dtos = policies.stream()
                    .map(p -> new PurchasePolicyDTO(
                            p.getPolicyId(),
                            p.getTargetId(),
                            p.getTargetType().toString(),
                            p.getRoot().getDescription()
                    ))
                    .collect(Collectors.toList());
            logger.info("User of token {} got policies for event {} of company {} successfully", token, eventId, companyName);
            return Response.success(dtos);
        }
        catch (DataAccessException e) {
            return Response.error("Database unavailable");
        }
        catch (Exception e) {
            logger.error("Error retrieving purchase policies: " + e.getMessage());
            return Response.error(e.getMessage());
        }
    }
    public void deleteAll(){
        purchaseRepo.deleteAll();
    }
}