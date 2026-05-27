package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.springframework.stereotype.Service;

@Service
public class SystemService {

    private final UserService userService;
    private final TokenService tokenService;
    private final iAdminRepository adminRepository;
    private final IPaymentService paymentService;
    private final ISupplyService supplyService;

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

        boolean paymentOk = paymentService.processPayment("0000000000000000", 0.0);
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
