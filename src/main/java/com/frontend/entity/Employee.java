package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an Employee
 */
@Entity
@Table(name = "employee")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "FNAME", nullable = false, length = 45)
    private String firstName;

    @Column(name = "MNAME", nullable = false, length = 45)
    private String middleName;

    @Column(name = "LNAME", nullable = false, length = 45)
    private String lastName;

    @Column(name = "ADDRESS", nullable = false, length = 100)
    private String address;

    @Column(name = "CONTACT", nullable = false, length = 45)
    private String contact;

    @Column(name = "DESIGNATION", nullable = false, length = 45)
    private String designation;

    @Column(name = "SALARY", nullable = false)
    private Float salary = 0.0f;

    @Column(name = "SALARYTYPE", nullable = false, length = 45)
    private String salaryType;

    @Column(name = "status", length = 45)
    private String status;

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
    public Employee() {
    }

    public Employee(String firstName, String middleName, String lastName, String address,
                   String contact, String designation, Float salary, String salaryType, String status) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.address = address;
        this.contact = contact;
        this.designation = designation;
        this.salary = salary;
        this.salaryType = salaryType;
        this.status = status;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public Float getSalary() {
        return salary;
    }

    public void setSalary(Float salary) {
        this.salary = salary;
    }

    public String getSalaryType() {
        return salaryType;
    }

    public void setSalaryType(String salaryType) {
        this.salaryType = salaryType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
     * Get full name of the employee
     */
    public String getFullName() {
        return firstName + " " + middleName + " " + lastName;
    }

    /**
     * Backward compatibility method for old code
     */
    public Integer getEmployeeId() {
        return id;
    }

    /**
     * Backward compatibility method for old code
     */
    public void setEmployeeId(Integer employeeId) {
        this.id = employeeId;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", address='" + address + '\'' +
                ", contact='" + contact + '\'' +
                ", designation='" + designation + '\'' +
                ", salary=" + salary +
                ", salaryType='" + salaryType + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
