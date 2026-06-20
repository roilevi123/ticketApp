package config;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Config.DataInitializer;
import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.Discount.DiscountTargetType;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Notification.INotificationRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.PurchasePolicy.PurchaseTargetType;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DataInitializerTest {

    private UserService userService;
    private CompanyService companyService;
    private EventService eventService;
    private LotteryService lotteryService;
    private TokenService tokenService;
    private iTicketRepository ticketRepository;
    private iPurchasedOrderRepository purchasedOrderRepository;
    private iAdminRepository adminRepository;
    private INotificationRepository notificationRepository;
    private DiscountService discountService;
    private PurchasePolicyService purchasePolicyService;

    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        companyService = mock(CompanyService.class);
        eventService = mock(EventService.class);
        lotteryService = mock(LotteryService.class);
        tokenService = mock(TokenService.class);
        ticketRepository = mock(iTicketRepository.class);
        purchasedOrderRepository = mock(iPurchasedOrderRepository.class);
        adminRepository = mock(iAdminRepository.class);
        notificationRepository = mock(INotificationRepository.class);
        discountService = mock(DiscountService.class);
        purchasePolicyService = mock(PurchasePolicyService.class);

        dataInitializer = new DataInitializer(
                userService,
                companyService,
                eventService,
                lotteryService,
                tokenService,
                ticketRepository,
                purchasedOrderRepository,
                adminRepository,
                notificationRepository,
                discountService,
                purchasePolicyService
        );

        ReflectionTestUtils.setField(dataInitializer, "initialStateEnabled", true);
        ReflectionTestUtils.setField(dataInitializer, "skipExisting", true);
        ReflectionTestUtils.setField(dataInitializer, "resetBeforeInit", false);

        when(tokenService.generateGuestToken()).thenReturn("guest-token");
    }

    private String createResourceFile(String content) throws Exception {
        String fileName = "initial-state-test-" + UUID.randomUUID() + ".txt";
        Path dir = Path.of("target", "test-classes");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(fileName), content);
        return fileName;
    }

    @Test
    void run_Disabled_DoesNothing() throws Exception {
        ReflectionTestUtils.setField(dataInitializer, "initialStateEnabled", false);
        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", "missing.txt");

        dataInitializer.run(mock(ApplicationArguments.class));

        verifyNoInteractions(userService);
        verifyNoInteractions(companyService);
        verifyNoInteractions(eventService);
    }

    @Test
    void initializeFromConfig_SkipsEmptyLinesCommentsAndHandlesSpaces() throws Exception {
        String file = createResourceFile("""
                
                # this is comment
                
                    register   roy   pass123   22   roy@test.com
                    login      roy   pass123
                """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        when(userService.register(eq("guest-token"), eq("roy"), eq("pass123"), eq(22), eq("roy@test.com")))
                .thenReturn(Response.success("registered"));

        when(userService.login(eq("guest-token"), eq("roy"), eq("pass123")))
                .thenReturn(Response.success("roy-token"));

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(userService).register("guest-token", "roy", "pass123", 22, "roy@test.com");
        verify(userService).login("guest-token", "roy", "pass123");
    }

    @Test
    void initializeFromConfig_CreateCompany_UsesLoggedInTokenAndConvertsUnderscoresToSpaces() throws Exception {
        String file = createResourceFile("""
                register roy pass123 22 roy@test.com
                login roy pass123
                create-company roy BGU_Events
                """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        when(userService.register(anyString(), eq("roy"), eq("pass123"), eq(22), eq("roy@test.com")))
                .thenReturn(Response.success("registered"));

        when(userService.login(anyString(), eq("roy"), eq("pass123")))
                .thenReturn(Response.success("roy-token"));

        when(companyService.CreateCompany(eq("BGU Events"), eq("roy-token")))
                .thenReturn(Response.success("company-token"));

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(companyService).CreateCompany("BGU Events", "roy-token");
    }

    @Test
    void initializeFromConfig_UnknownCommand_ThrowsWithLineNumber() throws Exception {
        String file = createResourceFile("""
                register roy pass123 22 roy@test.com
                bad-command something
                """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        when(userService.register(anyString(), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(Response.success("registered"));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> dataInitializer.run(mock(ApplicationArguments.class))
        );

        assertTrue(ex.getMessage().contains("Initial state failed"));
        assertTrue(ex.getCause().getMessage().contains("Initial-state failed at line 2"));
        assertTrue(ex.getCause().getMessage().contains("Unknown action: bad-command"));
    }

    @Test
    void initializeFromConfig_WrongArgumentCount_Throws() throws Exception {
        String file = createResourceFile("""
                register roy pass123
                """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> dataInitializer.run(mock(ApplicationArguments.class))
        );

        assertTrue(ex.getCause().getMessage().contains("Expected 4 arguments, got 2"));
    }

    @Test
    void initializeFromConfig_CreateCompanyWithoutLogin_ThrowsUserNotLoggedIn() throws Exception {
        String file = createResourceFile("""
                create-company roy BGU_Events
                """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> dataInitializer.run(mock(ApplicationArguments.class))
        );

        assertTrue(ex.getCause().getMessage().contains("User is not logged in: roy"));
    }

    @Test
    void initializeFromConfig_InvalidNumber_Throws() throws Exception {
        String file = createResourceFile("""
                register roy pass123 not_number roy@test.com
                """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> dataInitializer.run(mock(ApplicationArguments.class))
        );

        assertTrue(ex.getCause().getMessage().contains("Initial-state failed at line 1"));
    }

    @Test
    void initializeFromConfig_InvalidEnum_Throws() throws Exception {
        String file = createResourceFile("""
                register roy pass123 22 roy@test.com
                login roy pass123
                create-company roy BGU_Events
                create-event roy BGU_Events Event_One Artist_One BAD_TYPE 100 Tel_Aviv 5 2 2
                """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        when(userService.register(anyString(), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(Response.success("registered"));

        when(userService.login(anyString(), eq("roy"), eq("pass123")))
                .thenReturn(Response.success("roy-token"));

        when(companyService.CreateCompany(anyString(), anyString()))
                .thenReturn(Response.success("company-token"));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> dataInitializer.run(mock(ApplicationArguments.class))
        );

        assertTrue(ex.getCause().getMessage().contains("Initial-state failed at line 4"));
        assertTrue(ex.getCause().getMessage().contains("BAD_TYPE"));
    }

    @Test
    void initializeFromConfig_SkipExistingTrue_ContinuesAfterAlreadyExistsError() throws Exception {
        String file = createResourceFile("""
                register roy pass123 22 roy@test.com
                register dana pass123 23 dana@test.com
                """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);
        ReflectionTestUtils.setField(dataInitializer, "skipExisting", true);

        when(userService.register(anyString(), eq("roy"), anyString(), anyInt(), anyString()))
                .thenReturn(Response.error("User already exists"));

        when(userService.register(anyString(), eq("dana"), anyString(), anyInt(), anyString()))
                .thenReturn(Response.success("registered"));

        assertDoesNotThrow(() -> dataInitializer.run(mock(ApplicationArguments.class)));

        verify(userService).register(anyString(), eq("dana"), anyString(), eq(23), eq("dana@test.com"));
    }

    @Test
    void initializeFromConfig_SkipExistingFalse_ThrowsOnAlreadyExistsError() throws Exception {
        String file = createResourceFile("""
                register roy pass123 22 roy@test.com
                """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);
        ReflectionTestUtils.setField(dataInitializer, "skipExisting", false);

        when(userService.register(anyString(), eq("roy"), anyString(), anyInt(), anyString()))
                .thenReturn(Response.error("User already exists"));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> dataInitializer.run(mock(ApplicationArguments.class))
        );

        assertTrue(ex.getCause().getMessage().contains("User already exists"));
    }

    @Test
    void registerAdmin_Success_AddsAdminAndStoresToken() throws Exception {
        String file = createResourceFile("""
                registerAdmin admin pass123 30 admin@test.com
                create-company admin Admin_Company
                """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        when(userService.register(anyString(), eq("admin"), eq("pass123"), eq(30), eq("admin@test.com")))
                .thenReturn(Response.success("registered"));

        when(userService.login(anyString(), eq("admin"), eq("pass123")))
                .thenReturn(Response.success("admin-jwt"));

        when(tokenService.extractUserId("admin-jwt"))
                .thenReturn("admin-id");

        when(companyService.CreateCompany(eq("Admin Company"), eq("admin-jwt")))
                .thenReturn(Response.success("company-token"));

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(adminRepository).addAdmin("admin-id");
        verify(companyService).CreateCompany("Admin Company", "admin-jwt");
    }

    @Test
    void resetBeforeInit_CallsDeleteMethodsBeforeLoadingFile() throws Exception {
        String file = createResourceFile("""
                register roy pass123 22 roy@test.com
                """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);
        ReflectionTestUtils.setField(dataInitializer, "resetBeforeInit", true);

        when(userService.register(anyString(), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(Response.success("registered"));

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(purchasedOrderRepository).deleteAll();
        verify(ticketRepository).deleteAllTickets();
        verify(eventService).deleteAllEvents();
        verify(companyService).deleteAll();
        verify(userService).deleteAll();
        verify(adminRepository).deleteAll();
        verify(discountService).deleteAllPolicy();
        verify(purchasePolicyService).deleteAll();
    }

    @Test
    void createEvent_Success_ParsesNumbersEnumMapAndUnderscores() throws Exception {
        String file = createResourceFile("""
                register roy pass123 22 roy@test.com
                login roy pass123
                create-event roy BGU_Events Event_One Artist_One PLAY 150.5 Tel_Aviv 7 3 4
                """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        when(userService.register(anyString(), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(Response.success("registered"));

        when(userService.login(anyString(), eq("roy"), eq("pass123")))
                .thenReturn(Response.success("roy-token"));

        when(eventService.createEvent(
                eq("roy-token"),
                eq("Event One"),
                eq("Artist One"),
                eq(EventType.PLAY),
                eq(150.5),
                any(),
                eq("Tel Aviv"),
                eq("BGU Events"),
                argThat(map -> map.length == 3 && map[0].length == 4)
        )).thenReturn(Response.success("event-created"));

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(eventService).createEvent(
                eq("roy-token"),
                eq("Event One"),
                eq("Artist One"),
                eq(EventType.PLAY),
                eq(150.5),
                any(),
                eq("Tel Aviv"),
                eq("BGU Events"),
                any()
        );
    }

    @Test
    void discountAndPolicyIds_AreSavedAndUsedInCompositeCommands() throws Exception {
        String file = createResourceFile("""
                register roy pass123 22 roy@test.com
                login roy pass123
                discount-simple roy Event_One EVENT 10 BGU_Events d1
                discount-coupon roy Event_One EVENT SAVE20 20 BGU_Events d2
                discount-sum roy Event_One EVENT BGU_Events sum1 d1,d2,rawId
                policy-age roy Event_One EVENT 18 p1
                policy-quantity roy Event_One EVENT 1 4 p2
                policy-and roy Event_One EVENT and1 p1,p2,rawPolicy
                """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        when(userService.register(anyString(), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(Response.success("registered"));

        when(userService.login(anyString(), eq("roy"), eq("pass123")))
                .thenReturn(Response.success("roy-token"));

        when(discountService.createSimpleDiscount(anyString(), anyString(), any(), anyDouble(), anyString()))
                .thenReturn(Response.success("discount-id-1"));

        when(discountService.createCouponDiscount(anyString(), anyString(), any(), anyString(), anyDouble(), anyString()))
                .thenReturn(Response.success("discount-id-2"));

        when(discountService.createSumDiscountPolicy(
                eq("roy-token"),
                eq("Event One"),
                eq(DiscountTargetType.EVENT),
                eq(java.util.List.of("discount-id-1", "discount-id-2", "rawId")),
                eq("BGU Events")
        )).thenReturn(Response.success("sum-id"));

        when(purchasePolicyService.createAgeLimitPolicy(anyString(), anyString(), any(), anyInt()))
                .thenReturn(Response.success("policy-id-1"));

        when(purchasePolicyService.createQuantityLimitPolicy(anyString(), anyString(), any(), anyInt(), anyInt()))
                .thenReturn(Response.success("policy-id-2"));

        when(purchasePolicyService.createAndPolicy(
                eq("roy-token"),
                eq("Event One"),
                eq(PurchaseTargetType.EVENT),
                eq(java.util.List.of("policy-id-1", "policy-id-2", "rawPolicy"))
        )).thenReturn(Response.success("and-id"));

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(discountService).createSumDiscountPolicy(
                eq("roy-token"),
                eq("Event One"),
                eq(DiscountTargetType.EVENT),
                eq(java.util.List.of("discount-id-1", "discount-id-2", "rawId")),
                eq("BGU Events")
        );

        verify(purchasePolicyService).createAndPolicy(
                eq("roy-token"),
                eq("Event One"),
                eq(PurchaseTargetType.EVENT),
                eq(java.util.List.of("policy-id-1", "policy-id-2", "rawPolicy"))
        );
    }

    @Test
    void fileNotFound_ThrowsInitialStateFailed() {
        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", "not-existing-file.txt");

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> dataInitializer.run(mock(ApplicationArguments.class))
        );

        assertTrue(ex.getMessage().contains("Initial state failed"));
    }
    @Test
    void appointManager_Success_UsesAppointerTokenAndConvertsCompanyName() throws Exception {
        String file = createResourceFile("""
    login owner pass123
    appoint-manager owner manager1 BGU_Events MANAGE_INVENTORY
    """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        when(userService.login(anyString(), eq("owner"), eq("pass123")))
                .thenReturn(Response.success("owner-token"));

        when(companyService.AppointAManager(eq("manager1"), eq("BGU Events"), eq(Set.of()), eq("owner-token")))
                .thenReturn(Response.success("success"));

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(companyService).AppointAManager("manager1", "BGU Events", Set.of(), "owner-token");
    }

    @Test
    void approveManager_Success_UsesLoggedInUserToken() throws Exception {
        String file = createResourceFile("""
            login manager1 pass123
            approve-manager manager1 BGU_Events
            """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        when(userService.login(anyString(), eq("manager1"), eq("pass123")))
                .thenReturn(Response.success("manager-token"));

        when(companyService.ApproveAppointmentForManager(eq("manager-token"), eq("BGU Events")))
                .thenReturn(Response.success("success"));

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(companyService).ApproveAppointmentForManager("manager-token", "BGU Events");
    }

    @Test
    void appointOwner_Success_UsesAppointerTokenAndConvertsCompanyName() throws Exception {
        String file = createResourceFile("""
            login owner pass123
            appoint-owner owner newOwner BGU_Events
            """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        when(userService.login(anyString(), eq("owner"), eq("pass123")))
                .thenReturn(Response.success("owner-token"));

        when(companyService.AppointOwner(eq("newOwner"), eq("BGU Events"), eq("owner-token")))
                .thenReturn(Response.success("success"));

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(companyService).AppointOwner("newOwner", "BGU Events", "owner-token");
    }

    @Test
    void approveOwner_Success_UsesLoggedInUserToken() throws Exception {
        String file = createResourceFile("""
            login newOwner pass123
            approve-owner newOwner BGU_Events
            """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        when(userService.login(anyString(), eq("newOwner"), eq("pass123")))
                .thenReturn(Response.success("owner-token"));

        when(companyService.ApproveAppointmentForOwner(eq("owner-token"), eq("BGU Events")))
                .thenReturn(Response.success("success"));

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(companyService).ApproveAppointmentForOwner("owner-token", "BGU Events");
    }

    @Test
    void configureLottery_Success_ParsesMinutesAndMaxWinners() throws Exception {
        String file = createResourceFile("""
            login owner pass123
            configure-lottery owner BGU_Events Event_One 30 5
            """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        when(userService.login(anyString(), eq("owner"), eq("pass123")))
                .thenReturn(Response.success("owner-token"));

        when(lotteryService.configureLottery(
                eq("owner-token"),
                eq("BGU Events"),
                eq("Event One"),
                isNull(),
                any(),
                eq(5)
        )).thenReturn(Response.success("lottery-configured"));

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(lotteryService).configureLottery(
                eq("owner-token"),
                eq("BGU Events"),
                eq("Event One"),
                isNull(),
                any(),
                eq(5)
        );
    }

    @Test
    void discountQuantity_Success_ParsesMinQuantityAndSavesId() throws Exception {
        String file = createResourceFile("""
            login owner pass123
            discount-quantity owner Event_One EVENT 15.5 3 BGU_Events dq1
            discount-max owner Event_One EVENT BGU_Events max1 dq1,rawDiscount
            """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        when(userService.login(anyString(), eq("owner"), eq("pass123")))
                .thenReturn(Response.success("owner-token"));

        when(discountService.createQuantityDiscount(
                eq("owner-token"),
                eq("Event One"),
                eq(DiscountTargetType.EVENT),
                eq(15.5),
                eq(3),
                eq("BGU Events")
        )).thenReturn(Response.success("quantity-discount-id"));

        when(discountService.createMaxDiscountPolicy(
                eq("owner-token"),
                eq("Event One"),
                eq(DiscountTargetType.EVENT),
                eq(List.of("quantity-discount-id", "rawDiscount")),
                eq("BGU Events")
        )).thenReturn(Response.success("max-id"));

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(discountService).createQuantityDiscount(
                eq("owner-token"),
                eq("Event One"),
                eq(DiscountTargetType.EVENT),
                eq(15.5),
                eq(3),
                eq("BGU Events")
        );

        verify(discountService).createMaxDiscountPolicy(
                eq("owner-token"),
                eq("Event One"),
                eq(DiscountTargetType.EVENT),
                eq(List.of("quantity-discount-id", "rawDiscount")),
                eq("BGU Events")
        );
    }

    @Test
    void policyOr_Success_UsesSavedPolicyIds() throws Exception {
        String file = createResourceFile("""
            login owner pass123
            policy-age owner Event_One EVENT 18 p1
            policy-quantity owner Event_One EVENT 1 4 p2
            policy-or owner Event_One EVENT or1 p1,p2,rawPolicy
            """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        when(userService.login(anyString(), eq("owner"), eq("pass123")))
                .thenReturn(Response.success("owner-token"));

        when(purchasePolicyService.createAgeLimitPolicy(
                eq("owner-token"),
                eq("Event One"),
                eq(PurchaseTargetType.EVENT),
                eq(18)
        )).thenReturn(Response.success("age-policy-id"));

        when(purchasePolicyService.createQuantityLimitPolicy(
                eq("owner-token"),
                eq("Event One"),
                eq(PurchaseTargetType.EVENT),
                eq(1),
                eq(4)
        )).thenReturn(Response.success("quantity-policy-id"));

        when(purchasePolicyService.createOrPolicy(
                eq("owner-token"),
                eq("Event One"),
                eq(PurchaseTargetType.EVENT),
                eq(List.of("age-policy-id", "quantity-policy-id", "rawPolicy"))
        )).thenReturn(Response.success("or-policy-id"));

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(purchasePolicyService).createOrPolicy(
                eq("owner-token"),
                eq("Event One"),
                eq(PurchaseTargetType.EVENT),
                eq(List.of("age-policy-id", "quantity-policy-id", "rawPolicy"))
        );
    }

    @Test
    void registerAdmin_WhenUserAlreadyExists_StillLogsInAndAddsAdmin() throws Exception {
        String file = createResourceFile("""
            registerAdmin admin pass123 30 admin@test.com
            """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        when(userService.register(anyString(), eq("admin"), eq("pass123"), eq(30), eq("admin@test.com")))
                .thenReturn(Response.error("User already exists"));

        when(userService.login(anyString(), eq("admin"), eq("pass123")))
                .thenReturn(Response.success("admin-jwt"));

        when(tokenService.extractUserId("admin-jwt"))
                .thenReturn("admin-id");

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(userService).login(anyString(), eq("admin"), eq("pass123"));
        verify(adminRepository).addAdmin("admin-id");
    }

    @Test
    void registerAdmin_WhenRegisterFailsForOtherReason_Throws() throws Exception {
        String file = createResourceFile("""
            registerAdmin admin pass123 30 admin@test.com
            """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        when(userService.register(anyString(), eq("admin"), eq("pass123"), eq(30), eq("admin@test.com")))
                .thenReturn(Response.error("email invalid"));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> dataInitializer.run(mock(ApplicationArguments.class))
        );

        assertTrue(ex.getCause().getMessage().contains("email invalid"));

        verify(userService, never()).login(anyString(), anyString(), anyString());
        verify(adminRepository, never()).addAdmin(anyString());
    }

    @Test
    void serviceReturnsNullResponse_ThrowsResponseIsNull() throws Exception {
        String file = createResourceFile("""
            register roy pass123 22 roy@test.com
            """);

        ReflectionTestUtils.setField(dataInitializer, "initialStateFile", file);

        when(userService.register(anyString(), eq("roy"), eq("pass123"), eq(22), eq("roy@test.com")))
                .thenReturn(null);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> dataInitializer.run(mock(ApplicationArguments.class))
        );

        assertTrue(ex.getCause().getMessage().contains("Response is null"));
    }
}