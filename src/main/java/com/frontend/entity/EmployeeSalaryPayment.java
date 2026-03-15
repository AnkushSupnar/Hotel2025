package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity to record employee salary payments.
 * Tracks monthly salary payments with attendance-based calculations.
 */
@Entity
@Table(name = "employee_salary_payment",
       uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "salary_month", "salary_year"}))
public class EmployeeSalaryPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employees employee;

    @Column(name = "salary_month", nullable = false)
    private Integer salaryMonth;

    @Column(name = "salary_year", nullable = false)
    private Integer salaryYear;

    @Column(name = "total_days", nullable = false)
    private Integer totalDays;

    @Column(name = "absent_days", nullable = false)
    private Integer absentDays;

    @Column(name = "present_days", nullable = false)
    private Integer presentDays;

    @Column(name = "monthly_salary", nullable = false)
    private Float monthlySalary;

    @Column(name = "calculated_salary", nullable = false)
    private Float calculatedSalary;

    @Column(name = "paid_amount", nullable = false)
    private Float paidAmount;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (paidAt == null) {
            paidAt = LocalDateTime.now();
        }
    }

    public EmployeeSalaryPayment() {
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Employees getEmployee() { return employee; }
    public void setEmployee(Employees employee) { this.employee = employee; }

    public Integer getSalaryMonth() { return salaryMonth; }
    public void setSalaryMonth(Integer salaryMonth) { this.salaryMonth = salaryMonth; }

    public Integer getSalaryYear() { return salaryYear; }
    public void setSalaryYear(Integer salaryYear) { this.salaryYear = salaryYear; }

    public Integer getTotalDays() { return totalDays; }
    public void setTotalDays(Integer totalDays) { this.totalDays = totalDays; }

    public Integer getAbsentDays() { return absentDays; }
    public void setAbsentDays(Integer absentDays) { this.absentDays = absentDays; }

    public Integer getPresentDays() { return presentDays; }
    public void setPresentDays(Integer presentDays) { this.presentDays = presentDays; }

    public Float getMonthlySalary() { return monthlySalary; }
    public void setMonthlySalary(Float monthlySalary) { this.monthlySalary = monthlySalary; }

    public Float getCalculatedSalary() { return calculatedSalary; }
    public void setCalculatedSalary(Float calculatedSalary) { this.calculatedSalary = calculatedSalary; }

    public Float getPaidAmount() { return paidAmount; }
    public void setPaidAmount(Float paidAmount) { this.paidAmount = paidAmount; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
