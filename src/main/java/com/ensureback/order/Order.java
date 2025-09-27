package com.ensureback.order;

import com.ensureback.merchant.Merchant;
import com.ensureback.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    public enum Status {
        PENDING_FULFILLMENT,
        SHIPPED,
        DELIVERED,
        CLOSED,
        CANCELLED
    }

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "buyer_email", nullable = false)
    private String buyerEmail;

    @ManyToOne
    @JoinColumn(name = "buyer_user_id")
    private User buyerUser;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_description")
    private String productDescription;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price_cents", nullable = false)
    private Integer unitPriceCents;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "is_digital", nullable = false)
    private boolean digital;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "expected_delivery_at")
    private OffsetDateTime expectedDeliveryAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Order() {
        // JPA
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public void setBuyerEmail(String buyerEmail) {
        this.buyerEmail = buyerEmail;
    }

    public User getBuyerUser() {
        return buyerUser;
    }

    public void setBuyerUser(User buyerUser) {
        this.buyerUser = buyerUser;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getUnitPriceCents() {
        return unitPriceCents;
    }

    public void setUnitPriceCents(Integer unitPriceCents) {
        this.unitPriceCents = unitPriceCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isDigital() {
        return digital;
    }

    public void setDigital(boolean digital) {
        this.digital = digital;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public OffsetDateTime getExpectedDeliveryAt() {
        return expectedDeliveryAt;
    }

    public void setExpectedDeliveryAt(OffsetDateTime expectedDeliveryAt) {
        this.expectedDeliveryAt = expectedDeliveryAt;
    }

    public OffsetDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(OffsetDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int calculateTotalAmountCents() {
        int qty = quantity != null ? quantity : 0;
        int unit = unitPriceCents != null ? unitPriceCents : 0;
        return Math.multiplyExact(qty, unit);
    }
}
