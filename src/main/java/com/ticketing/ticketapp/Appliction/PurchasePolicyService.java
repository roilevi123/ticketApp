package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.PurchasePolicy.*;
import com.ticketing.ticketapp.Infastructure.TokenService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PurchasePolicyService {
    private final iPurchasePolicyRepository purchaseRepo;
    private final TokenService tokenService;
    private static final Logger logger = LoggerFactory.getLogger(PurchasePolicyService.class);
    private final UserService userService;

    public PurchasePolicyService(iPurchasePolicyRepository purchaseRepo, TokenService tokenService, UserService userService) {
        this.purchaseRepo = purchaseRepo;
        this.tokenService = tokenService;
        this.userService = userService;
    }

    public Response<String> createAgeLimitPolicy(String token, String targetId, PurchaseTargetType type, int minAge) {
        try {
            validateToken(token);
            PurchaseComponent condition = new AgeLimitCondition(minAge);
            return Response.success(saveToRepo(targetId, type, condition));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> createQuantityLimitPolicy(String token, String targetId, PurchaseTargetType type, int min, int max) {
        try {
            validateToken(token);
            PurchaseComponent condition = new QuantityLimitCondition(min, max);
            return Response.success(saveToRepo(targetId, type, condition));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> createAndPolicy(String token, String targetId, PurchaseTargetType type, List<String> componentIds) {
        try {
            validateToken(token);
            AndPurchaseComposite andComposite = new AndPurchaseComposite();

            for (String id : componentIds) {
                PurchasePolicy part = purchaseRepo.getPolicy(id);
                if (part != null) {
                    andComposite.add(part.getRoot());
                    purchaseRepo.delete(id);
                }
            }
            return Response.success(saveToRepo(targetId, type, andComposite));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> createOrPolicy(String token, String targetId, PurchaseTargetType type, List<String> componentIds) {
        try {
            validateToken(token);
            OrPurchaseComposite orComposite = new OrPurchaseComposite();

            for (String id : componentIds) {
                PurchasePolicy part = purchaseRepo.getPolicy(id);
                if (part != null) {
                    orComposite.add(part.getRoot());
                    purchaseRepo.delete(id);
                }
            }
            return Response.success(saveToRepo(targetId, type, orComposite));
        } catch (Exception e) {
            logger.error(e.getMessage());
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

    public Response<List<PurchasePolicyDTO>> getPoliciesForEventAndCompany(String token, String eventId, String companyName) {
        try {
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
            return Response.success(dtos);
        } catch (Exception e) {
            logger.error("Error retrieving purchase policies: " + e.getMessage());
            return Response.error(e.getMessage());
        }
    }
}
