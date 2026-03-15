package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing customers
 */
@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "CustomerKey", nullable = false, length = 45)
    private String customerKey;

    @Column(name = "FName", nullable = false, length = 45)
    private String firstName;

    @Column(name = "MName", nullable = false, length = 45)
    private String middleName;

    @Column(name = "LName", nullable = false, length = 45)
    private String lastName;

    @Column(name = "MobileNo", nullable = false, length = 45)
    private String mobileNo;

    @Column(name = "EmailId", nullable = false, length = 45)
    private String emailId;

    @Column(name = "FlatNo", nullable = false)
    private Integer flatNo;

    @Column(name = "StreetName", nullable = false, length = 45)
    private String streetName;

    @Column(name = "City", nullable = false, length = 45)
    private String city;

    @Column(name = "District", nullable = false, length = 45)
    private String district;

    @Column(name = "Taluka", nullable = false, length = 45)
    private String taluka;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public Customer() {
    }

    public Customer(String customerKey, String firstName, String middleName, String lastName,
                    String mobileNo, String emailId, Integer flatNo, String streetName,
                    String city, String district, String taluka) {
        this.customerKey = customerKey;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.mobileNo = mobileNo;
        this.emailId = emailId;
        this.flatNo = flatNo;
        this.streetName = streetName;
        this.city = city;
        this.district = district;
        this.taluka = taluka;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCustomerKey() {
        return customerKey;
    }

    public void setCustomerKey(String customerKey) {
        this.customerKey = customerKey;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMobileNo() {
        return mobileNo;
    }

    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public Integer getFlatNo() {
        return flatNo;
    }

    public void setFlatNo(Integer flatNo) {
        this.flatNo = flatNo;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getTaluka() {
        return taluka;
    }

    public void setTaluka(String taluka) {
        this.taluka = taluka;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Helper method to get full name
     */
    public String getFullName() {
        return firstName + " " + middleName + " " + lastName;
    }

    /**
     * Helper method to get full address
     */
    public String getFullAddress() {
        return flatNo + ", " + streetName + ", " + city + ", " + taluka + ", " + district;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", customerKey='" + customerKey + '\'' +
                ", firstName='" + firstName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", mobileNo='" + mobileNo + '\'' +
                ", emailId='" + emailId + '\'' +
                ", flatNo=" + flatNo +
                ", streetName='" + streetName + '\'' +
                ", city='" + city + '\'' +
                ", district='" + district + '\'' +
                ", taluka='" + taluka + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
