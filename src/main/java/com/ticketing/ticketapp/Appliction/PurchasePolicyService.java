package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.PurchasePolicy.*;
import com.ticketing.ticketapp.Infastructure.TokenService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PurchasePolicyService {
    private final iPurchasePolicyRepository purchaseRepo;
    private final TokenService tokenService;
    private static final Logger logger = LoggerFactory.getLogger(PurchasePolicyService.class);

    public PurchasePolicyService(iPurchasePolicyRepository purchaseRepo, TokenService tokenService) {
        this.purchaseRepo = purchaseRepo;
        this.tokenService = tokenService;
    }

    public String createAgeLimitPolicy(String token, String targetId, PurchaseTargetType type, int minAge) {
        try {
            validateToken(token);
            PurchaseComponent condition = new AgeLimitCondition(minAge);
            return saveToRepo(targetId, type, condition);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public String createQuantityLimitPolicy(String token, String targetId, PurchaseTargetType type, int min, int max) {
        try {
            validateToken(token);
            PurchaseComponent condition = new QuantityLimitCondition(min, max);
            return saveToRepo(targetId, type, condition);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public String createAndPolicy(String token, String targetId, PurchaseTargetType type, List<String> componentIds) {
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
            return saveToRepo(targetId, type, andComposite);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public String createOrPolicy(String token, String targetId, PurchaseTargetType type, List<String> componentIds) {
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
            return saveToRepo(targetId, type, orComposite);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
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
}
