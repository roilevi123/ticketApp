package Appliction;

import Domain.Order.IPurchasedOrderRepository;
import Domain.Order.PurchasedOrder;
import Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PurchasedOrderService {

    private IPurchasedOrderRepository purchasedOrderRepository;
    private TokenService tokenService;
    private static final Logger logger = LoggerFactory.getLogger(PurchasedOrderService.class);

    public PurchasedOrderService(IPurchasedOrderRepository purchasedOrderRepository, TokenService tokenService) {
        this.purchasedOrderRepository = purchasedOrderRepository;
        this.tokenService = tokenService;
    }

    public List<PurchasedOrder> getUserOrders(String token) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            String username = tokenService.extractUsername(token);
            
            logger.info("Fetching purchased orders for user: " + username);
            return purchasedOrderRepository.getOrdersByUser(username);
            
        } catch (Exception e) {
            logger.error("Failed to fetch user orders: " + e.getMessage());
            return null; 
        }
    }

    public List<PurchasedOrder> getCompanyOrders(String token, String companyId) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            
            logger.info("Fetching purchased orders for company: " + companyId);
            return purchasedOrderRepository.getOrdersByCompany(companyId);
            
        } catch (Exception e) {
            logger.error("Failed to fetch company orders: " + e.getMessage());
            return null;
        }
    }
}