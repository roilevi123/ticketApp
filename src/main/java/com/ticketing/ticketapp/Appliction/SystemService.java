package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.payment.CreditCardDetails;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SystemService {

    private final UserService userService;
    private final TokenService tokenService;
    private final iAdminRepository adminRepository;
    private final IPaymentService paymentService;
    private final ISupplyService supplyService;
    private static final Logger logger = LoggerFactory.getLogger(PurchasedService.class);


    private volatile boolean initialized = false;
    private volatile boolean open = false;

    public SystemService(UserService userService, TokenService tokenService,
                         iAdminRepository adminRepository,
                         IPaymentService paymentService,
                         ISupplyService supplyService) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.paymentService = paymentService;
        this.supplyService = supplyService;
    }

    public Response<String> initSystem(String token, String adminUsername, String adminPassword,
                                       int adminAge, String adminEmail) {
        if (initialized) {
            return Response.error("System already initialized");
        }
        logger.info("initializing system");
        CreditCardDetails dummyCard = new CreditCardDetails(
                "0000000000000000", // card_number
                "12",               // month
                "2030",             // year
                "System Check",     // holder
                "000",              // cvv
                "00000000"          // id
        );
        int paymentResult = paymentService.processPayment(dummyCard, 0.0, "USD");
        boolean paymentOk = ( paymentResult!=-1);
        boolean supplyOk = supplyService.supplyToEmail("system@check.internal", "ping");
        if (!paymentOk || !supplyOk) {
            return Response.error("External services unavailable");
        }

        Response<String> registerResp = userService.register(token, adminUsername, adminPassword, adminAge, adminEmail);
        if (registerResp.isError()) {
            return Response.error("Failed to create admin: " + registerResp.getMessage());
        }

        String loginGuestToken = tokenService.generateGuestToken();
        Response<String> loginResp = userService.login(loginGuestToken, adminUsername, adminPassword);
        if (loginResp.isError()) {
            return Response.error("Failed to authenticate admin after creation");
        }

        String adminUserId = tokenService.extractUserId(loginResp.getData());
        adminRepository.addAdmin(adminUserId);
        initialized = true;

        return Response.success("System initialized successfully. Admin '" + adminUsername + "' created.");
    }

    public Response<String> openSystem(String token) {
        if (!initialized) {
            return Response.error("System must be initialized before opening");
        }
        if (open) {
            return Response.error("System is already open");
        }

        String userId = tokenService.extractUserId(token);
        if (!adminRepository.isAdmin(userId)) {
            return Response.error("Unauthorized: admin access required to open the system");
        }

        open = true;
        return Response.success("System is now open");
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isOpen() {
        return open;
    }
}
