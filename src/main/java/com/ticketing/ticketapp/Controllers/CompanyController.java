package com.ticketing.ticketapp.Controllers;

import org.springframework.web.bind.annotation.*;
import com.ticketing.ticketapp.Appliction.CompanyService;
import com.ticketing.ticketapp.Appliction.EventService;
import java.util.*;
import com.ticketing.ticketapp.Appliction.Response;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Appliction.PurchasePolicyService;
import com.ticketing.ticketapp.Appliction.DiscountService;
import com.ticketing.ticketapp.Domain.PurchasePolicy.*;
import com.ticketing.ticketapp.Infastructure.TokenService;
import com.ticketing.ticketapp.Domain.Discount.*;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Permission;
import com.ticketing.ticketapp.Appliction.PurchasedService;
import com.ticketing.ticketapp.Appliction.IPendingNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/company")
public class CompanyController {
    private final EventService eventService;
    private final CompanyService companyService;
    private final PurchasePolicyService purchasePolicyService;
    private final DiscountService discountService;
    private final PurchasedService purchasedService;
    private final IPendingNotificationRepository notificationRepository;
    

    public CompanyController(EventService eventService, CompanyService companyService, PurchasePolicyService purchasePolicyService, DiscountService discountService, PurchasedService purchasedService, IPendingNotificationRepository notificationRepository) {
        this.eventService = eventService;
        this.companyService = companyService;
        this.purchasePolicyService = purchasePolicyService;
        this.discountService = discountService;
        this.purchasedService = purchasedService;
        this.notificationRepository = notificationRepository;
    }

    @PostMapping("/open")
    public ResponseEntity<?> openCompany(
            @RequestAttribute("cleanToken") String token,
            @RequestBody CompanyRequestDTO companyRequest) {

        String companyName = companyRequest.getCompanyName();
        token = extractCleanToken(token);
        Response<?> response = companyService.CreateCompany(companyName, token);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.badRequest().body(response.getMessage());

    }

    @PostMapping("/events")
    public ResponseEntity<?> configureEventsAndSeating(
            @RequestAttribute("cleanToken") String token, 
            @RequestBody EventRequestDTO request) {

        Response<String> response;
        token = extractCleanToken(token);
        var existingEventResponse = eventService.getEvent(token, request.getCompanyName(), request.getEventName());
        
        if (existingEventResponse.isSuccess()) {
            response = eventService.UpdateEvent(
                token, 
                request.getEventName(),
                request.getArtistName(),
                request.getEventType(),
                request.getPrice(),
                request.getDate(),
                request.getLocation(),
                request.getCompanyName(),
                request.getMap(),
                request.getRating() 
            );
        } else {
            response = eventService.createEvent(
                token,
                request.getEventName(),
                request.getArtistName(),
                request.getEventType(),
                request.getPrice(),
                request.getDate(),
                request.getLocation(),
                request.getCompanyName(),
                request.getMap()
            );
        }

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Event configured successfully", "data", response.getData()));
        }
        
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @PostMapping("/policies/purchase/age-limit")
    public ResponseEntity<?> createAgeLimitPolicy(
            @RequestAttribute("cleanToken") String token, 
            @RequestBody AgeLimitRequestDTO request) {

        token = extractCleanToken(token);
        Response<String> response = purchasePolicyService.createAgeLimitPolicy(
                token, 
                request.getTargetId(), 
                request.getType(), 
                request.getMinAge()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Age limit policy created", "policyId", response.getData()));
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @PostMapping("/policies/purchase/quantity-limit")
    public ResponseEntity<?> createQuantityLimitPolicy(
            @RequestAttribute("cleanToken") String token, 
            @RequestBody QuantityLimitRequestDTO request) {

        token = extractCleanToken(token);
        Response<String> response = purchasePolicyService.createQuantityLimitPolicy(
                token, 
                request.getTargetId(), 
                request.getType(), 
                request.getMin(),
                request.getMax()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Quantity limit policy created", "policyId", response.getData()));
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @PostMapping("/policies/purchase/combine-and")
    public ResponseEntity<?> createAndPolicy(
            @RequestAttribute("cleanToken") String token, 
            @RequestBody CombinePoliciesRequestDTO request) {

        token = extractCleanToken(token);
        Response<String> response = purchasePolicyService.createAndPolicy(
                token, 
                request.getTargetId(), 
                request.getType(), 
                request.getComponentIds()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "AND composite policy created", "policyId", response.getData()));
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @PostMapping("/policies/purchase/combine-or")
    public ResponseEntity<?> createOrPolicy(
            @RequestAttribute("cleanToken") String token, 
            @RequestBody CombinePoliciesRequestDTO request) {

        token = extractCleanToken(token);
        Response<String> response = purchasePolicyService.createOrPolicy(
                token, 
                request.getTargetId(), 
                request.getType(), 
                request.getComponentIds()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "OR composite policy created", "policyId", response.getData()));
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @PostMapping("/policies/discount/simple")
    public ResponseEntity<?> createSimpleDiscount(
            @RequestAttribute("cleanToken") String token, 
            @RequestBody SimpleDiscountRequestDTO request) {

        token = extractCleanToken(token);
        Response<String> response = discountService.createSimpleDiscount(
                token, request.getTargetId(), request.getType(), 
                request.getPercentage(), request.getCompanyName()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Simple discount created", "policyId", response.getData()));
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @PostMapping("/policies/discount/quantity")
    public ResponseEntity<?> createQuantityDiscount(
            @RequestAttribute("cleanToken") String token, 
            @RequestBody QuantityDiscountRequestDTO request) {

        token = extractCleanToken(token);
        Response<String> response = discountService.createQuantityDiscount(
                token, request.getTargetId(), request.getType(), 
                request.getPercentage(), request.getMinQuantity(), request.getCompanyName()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Quantity discount created", "policyId", response.getData()));
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @PostMapping("/policies/discount/time-limited")
    public ResponseEntity<?> createTimeLimitedDiscount(
            @RequestAttribute("cleanToken") String token, 
            @RequestBody TimeLimitedDiscountRequestDTO request) {

        token = extractCleanToken(token);
        Response<String> response = discountService.createTimeLimitedDiscount(
                token, request.getTargetId(), request.getType(), 
                request.getPercentage(), request.getDeadline(), request.getCompanyName()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Time-limited discount created", "policyId", response.getData()));
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @PostMapping("/policies/discount/coupon")
    public ResponseEntity<?> createCouponDiscount(
            @RequestAttribute("cleanToken") String token, 
            @RequestBody CouponDiscountRequestDTO request) {

        token = extractCleanToken(token);
        Response<String> response = discountService.createCouponDiscount(
                token, request.getTargetId(), request.getType(), 
                request.getCode(), request.getPercentage(), request.getCompanyName()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Coupon discount created", "policyId", response.getData()));
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @PostMapping("/policies/discount/combine-sum")
    public ResponseEntity<?> createSumDiscountPolicy(
            @RequestAttribute("cleanToken") String token, 
            @RequestBody CombineDiscountsRequestDTO request) {

        token = extractCleanToken(token);
        Response<String> response = discountService.createSumDiscountPolicy(
                token, request.getTargetId(), request.getType(), 
                request.getExistingPolicyIds(), request.getCompanyName()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Sum composite discount created", "policyId", response.getData()));
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @PostMapping("/policies/discount/combine-max")
    public ResponseEntity<?> createMaxDiscountPolicy(
            @RequestAttribute("cleanToken") String token, 
            @RequestBody CombineDiscountsRequestDTO request) {
        
        token = extractCleanToken(token);
        Response<String> response = discountService.createMaxDiscountPolicy(
                token, request.getTargetId(), request.getType(), 
                request.getExistingPolicyIds(), request.getCompanyName()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Max composite discount created", "policyId", response.getData()));
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @PostMapping("/assign-role")
    public ResponseEntity<?> assignRole(
            @RequestAttribute("cleanToken") String token, 
            @RequestBody AssignRoleRequestDTO request) {

        token = extractCleanToken(token);
        Response<String> response;

        if ("MANAGER".equalsIgnoreCase(request.getRole())) {
            token = extractCleanToken(token);
            response = companyService.AppointAManager(
                    request.getTargetUserId(), 
                    request.getCompanyName(), 
                    request.getPermissions(), 
                    token
            );
        } else if ("OWNER".equalsIgnoreCase(request.getRole())) {
            response = companyService.AppointOwner(
                    request.getTargetUserId(), 
                    request.getCompanyName(), 
                    token
            );
        } else {
            return ResponseEntity.badRequest().body("Invalid role specified. Must be 'MANAGER' or 'OWNER'.");
        }

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Role assigned successfully pending approval"));
        }
        
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @GetMapping("/messages")
    public ResponseEntity<?> getInboxMessages(@RequestAttribute("cleanToken") String token) {
        
        List<String> messages = notificationRepository.retrieveAndDelete("BGU Events");
        
        if (messages == null) {
            messages = new ArrayList<>();
        }
        
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/reply-message")
    public ResponseEntity<?> replyToBuyerMessage(
            @RequestAttribute("cleanToken") String token, 
            @RequestBody ReplyMessageRequestDTO request) {
        
        token = extractCleanToken(token);
        Response<String> response = companyService.replyToBuyer(
                token, 
                request.getCompanyName(), 
                request.getBuyerId(), 
                request.getMessage()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Reply sent successfully to the buyer"));
        }
        
        return ResponseEntity.badRequest().body(response.getMessage());
    }


    @GetMapping("/{companyName}/purchase-history")
    public ResponseEntity<?> getPurchaseHistory(
            @RequestAttribute("cleanToken") String token, 
            @PathVariable("companyName") String companyName) {

        token = extractCleanToken(token);
        Response<?> response = purchasedService.getCompanyTransaction(companyName, token);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }


    @DeleteMapping("/owner")
    public ResponseEntity<?> removeOwner(
            @RequestAttribute("cleanToken") String token, 
            @RequestParam("ownerId") String ownerId,
            @RequestParam("companyName") String companyName) {

        token = extractCleanToken(token);
        Response<String> response = companyService.FireOwner(token, companyName, ownerId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Owner removed successfully"));
        }
        
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @DeleteMapping("/owner/relinquish")
    public ResponseEntity<?> relinquishOwnership(
            @RequestAttribute("cleanToken") String token, 
            @RequestParam("companyName") String companyName) {

        token = extractCleanToken(token);
        Response<String> response = companyService.RejectAppointmentForOwner(token, companyName);
        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Ownership relinquished successfully"));
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @DeleteMapping("/manager")
    public ResponseEntity<?> removeManager(
            @RequestAttribute("cleanToken") String token, 
            @RequestParam("managerId") String managerId,
            @RequestParam("companyName") String companyName) {
        
        token = extractCleanToken(token);
        Response<String> response = companyService.FireManager(token, companyName, managerId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Manager removed successfully"));
        }
        
        return ResponseEntity.badRequest().body(response.getMessage());
    }


    @GetMapping("/{companyName}/sales-report")
    public ResponseEntity<?> getSubTreeSalesReport(
            @RequestAttribute("cleanToken") String token,
            @PathVariable("companyName") String companyName) {

        token = extractCleanToken(token);
        Response<?> response = purchasedService.getSubTreeSalesReport(token, companyName);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @PutMapping("/manager/permissions")
    public ResponseEntity<?> changeManagerPermissions(
            @RequestAttribute("cleanToken") String token, 
            @RequestBody ChangePermissionsRequestDTO request) {

        token = extractCleanToken(token);
        Response<String> response = companyService.ChangeManagerPermissions(
                token, 
                request.getCompanyName(), 
                request.getManagerId(), 
                request.getPermissions()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Manager permissions updated successfully"));
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }


    @PutMapping("/{companyName}/suspend")
    public ResponseEntity<?> suspendCompany(
            @RequestAttribute("cleanToken") String token, 
            @PathVariable("companyName") String companyName) {

        token = extractCleanToken(token);
        Response<String> response = companyService.freezeCompany(companyName, token);

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Company suspended successfully"));
        }
        
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @PutMapping("/{companyName}/reopen")
    public ResponseEntity<?> reopenCompany(
            @RequestAttribute("cleanToken") String token, 
            @PathVariable("companyName") String companyName) {

        token = extractCleanToken(token);
        Response<String> response = companyService.unfreezeCompany(companyName, token);

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Company reopened successfully"));
        }
        
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @GetMapping("/{companyName}/hierarchy")
    public ResponseEntity<?> getRoleHierarchyTree(
            @RequestAttribute("cleanToken") String token, 
            @PathVariable("companyName") String companyName) {

        token = extractCleanToken(token);

        Response<String> response = companyService.GetRoleTreeString(token, companyName);

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("tree", response.getData()));
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    private String extractCleanToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader; 
    }

    @PostMapping("/policies/purchase/bulk")
    public ResponseEntity<?> createBulkPolicy(
            @RequestAttribute("cleanToken") String token, 
            @RequestBody BulkPolicyRequestDTO request) {

        token = extractCleanToken(token);
        List<String> createdPolicyIds = new ArrayList<>();

        try {
            PurchaseTargetType targetTypeEnum = PurchaseTargetType.valueOf(request.getType().toUpperCase());
            for (ConditionDTO condition : request.getRuleset().getConditions()) {
                Response<String> res = null;

                if ("user.age".equals(condition.getField())) {
                    int minAge = Integer.parseInt(condition.getValue().toString());
                    res = purchasePolicyService.createAgeLimitPolicy(
                        token, request.getTargetId(), targetTypeEnum, minAge);
                } 
                else if ("cart.ticket_count".equals(condition.getField())) {
                    int maxTickets = Integer.parseInt(condition.getValue().toString());
                    res = purchasePolicyService.createQuantityLimitPolicy(
                        token, request.getTargetId(), targetTypeEnum, 1, maxTickets);
                }

                if (res != null && res.isSuccess()) {
                    createdPolicyIds.add(res.getData());
                } else if (res != null) {
                    return ResponseEntity.badRequest().body("Failed creating sub-policy: " + res.getMessage());
                }
            }

            if (createdPolicyIds.size() == 1) {
                return ResponseEntity.ok(Map.of("message", "Single policy applied", "policyId", createdPolicyIds.get(0)));
            }

            Response<String> finalResponse;
            String mainOperator = request.getRuleset().getOperator();

            if ("OR".equalsIgnoreCase(mainOperator)) {
                finalResponse = purchasePolicyService.createOrPolicy(
                    token, request.getTargetId(), targetTypeEnum, createdPolicyIds);
            } else {
                finalResponse = purchasePolicyService.createAndPolicy(
                    token, request.getTargetId(), targetTypeEnum, createdPolicyIds);
            }

            if (finalResponse.isSuccess()) {
                return ResponseEntity.ok(Map.of("message", "Bulk policy compiled and applied", "policyId", finalResponse.getData()));
            }

            return ResponseEntity.badRequest().body(finalResponse.getMessage());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid target type specified");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid policy data format: " + e.getMessage());
        }
    }

}

class CompanyRequestDTO {
    private String companyName;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
}

class EventRequestDTO {
    private String eventName;
    private String artistName;
    private EventType eventType; 
    private double price;
    private Date date;
    private String location;
    private String companyName;
    private MapArea[][] map; 
    private double rating; 

    public String getEventName() { return eventName; }
    public String getArtistName() { return artistName; }
    public EventType getEventType() { return eventType; }
    public double getPrice() { return price; }
    public Date getDate() { return date; }
    public String getLocation() { return location; }
    public String getCompanyName() { return companyName; }
    public MapArea[][] getMap() { return map; }
    public double getRating() { return rating; }

    public void setEventName(String eventName) { this.eventName = eventName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public void setPrice(double price) { this.price = price; }
    public void setDate(Date date) { this.date = date; }
    public void setLocation(String location) { this.location = location; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public void setMap(MapArea[][] map) { this.map = map; }
    public void setRating(double rating) { this.rating = rating; }
}


class AgeLimitRequestDTO {
    private String targetId;
    private PurchaseTargetType type;
    private int minAge;

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public PurchaseTargetType getType() { return type; }
    public void setType(PurchaseTargetType type) { this.type = type; }
    public int getMinAge() { return minAge; }
    public void setMinAge(int minAge) { this.minAge = minAge; }
}

class QuantityLimitRequestDTO {
    private String targetId;
    private PurchaseTargetType type;
    private int min;
    private int max;

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public PurchaseTargetType getType() { return type; }
    public void setType(PurchaseTargetType type) { this.type = type; }
    public int getMin() { return min; }
    public void setMin(int min) { this.min = min; }
    public int getMax() { return max; }
    public void setMax(int max) { this.max = max; }
}

class CombinePoliciesRequestDTO {
    private String targetId;
    private PurchaseTargetType type;
    private List<String> componentIds;

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public PurchaseTargetType getType() { return type; }
    public void setType(PurchaseTargetType type) { this.type = type; }
    public List<String> getComponentIds() { return componentIds; }
    public void setComponentIds(List<String> componentIds) { this.componentIds = componentIds; }
}

class SimpleDiscountRequestDTO {
    private String targetId;
    private DiscountTargetType type;
    private double percentage;
    private String companyName;

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public DiscountTargetType getType() { return type; }
    public void setType(DiscountTargetType type) { this.type = type; }
    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
}

class QuantityDiscountRequestDTO {
    private String targetId;
    private DiscountTargetType type;
    private double percentage;
    private int minQuantity;
    private String companyName;

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public DiscountTargetType getType() { return type; }
    public void setType(DiscountTargetType type) { this.type = type; }
    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
    public int getMinQuantity() { return minQuantity; }
    public void setMinQuantity(int minQuantity) { this.minQuantity = minQuantity; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
}

class TimeLimitedDiscountRequestDTO {
    private String targetId;
    private DiscountTargetType type;
    private double percentage;
    private Date deadline;
    private String companyName;

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public DiscountTargetType getType() { return type; }
    public void setType(DiscountTargetType type) { this.type = type; }
    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
}

class CouponDiscountRequestDTO {
    private String targetId;
    private DiscountTargetType type;
    private String code;
    private double percentage;
    private String companyName;

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public DiscountTargetType getType() { return type; }
    public void setType(DiscountTargetType type) { this.type = type; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
}

class CombineDiscountsRequestDTO {
    private String targetId;
    private DiscountTargetType type;
    private List<String> existingPolicyIds;
    private String companyName;

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public DiscountTargetType getType() { return type; }
    public void setType(DiscountTargetType type) { this.type = type; }
    public List<String> getExistingPolicyIds() { return existingPolicyIds; }
    public void setExistingPolicyIds(List<String> existingPolicyIds) { this.existingPolicyIds = existingPolicyIds; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
}

class AssignRoleRequestDTO {
    private String targetUserId;
    private String companyName;
    private String role; 
    private Set<Permission> permissions; 

    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Set<Permission> getPermissions() { return permissions; }
    public void setPermissions(Set<Permission> permissions) { this.permissions = permissions; }
}

class ReplyMessageRequestDTO {
    private String companyName;
    private String buyerId;
    private String message;

    public String getCompanyName() { return companyName; }
    public String getBuyerId() { return buyerId; }
    public String getMessage() { return message; }

    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public void setMessage(String message) { this.message = message; }
}

class ChangePermissionsRequestDTO {
    private String companyName;
    private String managerId;
    private Set<Permission> permissions;

    public String getCompanyName() { 
        return companyName; 
    }
    public String getManagerId() { 
        return managerId; 
    }
    public Set<Permission> getPermissions() { 
        return permissions; 
    }

    public void setCompanyName(String companyName) { 
        this.companyName = companyName; 
    }
    public void setManagerId(String managerId) { 
        this.managerId = managerId; 
    }
    public void setPermissions(Set<Permission> permissions) { 
        this.permissions = permissions; 
    }
}

class BulkPolicyRequestDTO {
    private String targetId;      
    private String type;
    private RuleSetDTO ruleset;

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public RuleSetDTO getRuleset() { return ruleset; }
    public void setRuleset(RuleSetDTO ruleset) { this.ruleset = ruleset; }
}

class RuleSetDTO {
    private String operator; 
    private List<ConditionDTO> conditions;

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public List<ConditionDTO> getConditions() { return conditions; }
    public void setConditions(List<ConditionDTO> conditions) { this.conditions = conditions; }
}

class ConditionDTO {
    private String field;
    private String operator;
    private Object value; 

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
}