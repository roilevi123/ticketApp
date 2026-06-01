package com.ticketing.ticketapp.Domain.Discount;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Transient;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.function.Predicate;

@Entity
@DiscriminatorValue("CONDITIONAL")
public class ConditionalDiscount extends DiscountComponent {

    @Column(name = "percentage")
    private double percentage;

    @Column(name = "condition_description")
    private String conditionDescription;

    @Transient
    private Predicate<PurchaseContext> condition;

    protected ConditionalDiscount() {
        super();
    }

    public ConditionalDiscount(String id, double percentage, Predicate<PurchaseContext> condition, String conditionDescription) {
        super(id);
        this.percentage = percentage;
        this.condition = (condition != null) ? condition : ctx -> true;
        this.conditionDescription = conditionDescription;
    }

    @PostLoad
    private void rebuildConditionFromDescription() {
        if (this.conditionDescription == null || this.conditionDescription.equals("no conditions")) {
            this.condition = ctx -> true;
        } else if (this.conditionDescription.startsWith("quantity >= ")) {
            try {
                int minQty = Integer.parseInt(this.conditionDescription.replace("quantity >= ", "").trim());
                this.condition = ctx -> ctx.getQuantity() >= minQty;
            } catch (Exception e) {
                this.condition = ctx -> true;
            }
        } else if (this.conditionDescription.startsWith("purchase date before ")) {
            try {
                String dateStr = this.conditionDescription.replace("purchase date before ", "").trim();
                String pattern = "EEE MMM dd HH:mm:ss zzz yyyy";
                SimpleDateFormat parser = new SimpleDateFormat(pattern, Locale.US);
                Date deadline = parser.parse(dateStr);
                // הוספת בדיקה כדי לוודא שאין NullPointerException על תאריך
                this.condition = ctx -> ctx.getPurchaseDate() != null && ctx.getPurchaseDate().before(deadline);
            } catch (Exception e) {
                this.condition = ctx -> true;
            }
        } else {
            this.condition = ctx -> true;
        }
    }

    @Override
    public double calculateDiscount(double price, PurchaseContext context) {
        if (condition != null && condition.test(context)) {
            return price * (percentage / 100.0);
        }
        return 0;
    }

    public double getPercentage() {
        return percentage;
    }

    @Override
    public String getDescription() {
        return String.format("%.1f%% discount (condition: %s)", percentage, conditionDescription);
    }
}