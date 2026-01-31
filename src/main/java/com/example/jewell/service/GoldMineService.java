package com.example.jewell.service;

import com.example.jewell.exception.ResourceNotFoundException;
import com.example.jewell.model.Customer;
import com.example.jewell.model.GoldMineInstallment;
import com.example.jewell.model.GoldMinePlan;
import com.example.jewell.repository.CustomerRepository;
import com.example.jewell.repository.GoldMineInstallmentRepository;
import com.example.jewell.repository.GoldMinePlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GoldMineService {

    @Autowired
    private GoldMinePlanRepository planRepository;

    @Autowired
    private GoldMineInstallmentRepository installmentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Enroll a customer in a Gold 10+1 plan. Creates plan and 11 installment records.
     * Monthly amount must be between 500 and 100,000.
     */
    public GoldMinePlan createPlan(Long customerId, BigDecimal monthlyAmount) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        if (monthlyAmount == null || monthlyAmount.compareTo(GoldMinePlan.MIN_MONTHLY_AMOUNT) < 0) {
            throw new IllegalArgumentException("Monthly amount must be at least " + GoldMinePlan.MIN_MONTHLY_AMOUNT);
        }
        if (monthlyAmount.compareTo(GoldMinePlan.MAX_MONTHLY_AMOUNT) > 0) {
            throw new IllegalArgumentException("Monthly amount must not exceed " + GoldMinePlan.MAX_MONTHLY_AMOUNT);
        }

        LocalDate startDate = LocalDate.now();
        GoldMinePlan plan = new GoldMinePlan();
        plan.setCustomer(customer);
        plan.setMonthlyAmount(monthlyAmount);
        plan.setStartDate(startDate);
        plan.setStatus(GoldMinePlan.PlanStatus.ACTIVE);
        plan.setPaidCount(0);
        plan = planRepository.save(plan);

        for (int i = 1; i <= GoldMinePlan.DISCOUNT_INSTALLMENT_NUMBER; i++) {
            GoldMineInstallment inst = new GoldMineInstallment();
            inst.setPlan(plan);
            inst.setInstallmentNumber(i);
            inst.setAmountDue(i == GoldMinePlan.DISCOUNT_INSTALLMENT_NUMBER ? BigDecimal.ZERO : monthlyAmount);
            inst.setDueDate(startDate.plusMonths(i));
            inst.setStatus(GoldMineInstallment.InstallmentStatus.PENDING);
            plan.getInstallments().add(inst);
            installmentRepository.save(inst);
        }

        return planRepository.findById(plan.getId()).orElse(plan);
    }

    public Optional<GoldMinePlan> getPlan(Long planId) {
        return planRepository.findById(planId);
    }

    /** Get plan with installments loaded (use when you need installment list). */
    public Optional<GoldMinePlan> getPlanWithInstallments(Long planId) {
        Optional<GoldMinePlan> planOpt = planRepository.findById(planId);
        planOpt.ifPresent(p -> p.getInstallments().size()); // trigger lazy load
        return planOpt;
    }

    public List<GoldMinePlan> getPlansByCustomer(Long customerId) {
        return planRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public Page<GoldMinePlan> getPlansByCustomerPaginated(Long customerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return planRepository.findByCustomerId(customerId, pageable);
    }

    public Page<GoldMinePlan> getAllPlans(int page, int size, GoldMinePlan.PlanStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        if (status != null) {
            return planRepository.findByStatus(status, pageable);
        }
        return planRepository.findAll(pageable);
    }

    /**
     * Record payment for an installment (1-10). Validates amount and marks installment PAID.
     * After 10th payment, the plan is eligible for redeem (11th waived).
     */
    public GoldMinePlan recordPayment(Long planId, int installmentNumber, BigDecimal amount, String paymentReference) {
        GoldMinePlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));

        if (plan.getStatus() != GoldMinePlan.PlanStatus.ACTIVE) {
            throw new IllegalStateException("Plan is not active. Status: " + plan.getStatus());
        }
        if (installmentNumber < 1 || installmentNumber > GoldMinePlan.TOTAL_PAID_INSTALLMENTS) {
            throw new IllegalArgumentException("Installment number must be 1 to 10 for payment. Got: " + installmentNumber);
        }

        GoldMineInstallment inst = installmentRepository.findByPlanIdAndInstallmentNumber(planId, installmentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Installment " + installmentNumber + " not found for plan " + planId));

        if (inst.getStatus() != GoldMineInstallment.InstallmentStatus.PENDING) {
            throw new IllegalStateException("Installment " + installmentNumber + " is already " + inst.getStatus());
        }

        BigDecimal expected = plan.getMonthlyAmount();
        if (amount == null || amount.compareTo(expected) < 0) {
            throw new IllegalArgumentException("Amount must be at least " + expected + " for installment " + installmentNumber);
        }

        inst.setStatus(GoldMineInstallment.InstallmentStatus.PAID);
        inst.setPaidAt(LocalDateTime.now());
        inst.setPaymentReference(paymentReference);
        installmentRepository.save(inst);

        plan.setPaidCount(plan.getPaidCount() + 1);
        return planRepository.save(plan);
    }

    /**
     * Redeem the 11th installment (waive it). Allowed when paidCount == 10.
     */
    public GoldMinePlan redeem(Long planId) {
        GoldMinePlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));

        if (plan.getStatus() != GoldMinePlan.PlanStatus.ACTIVE) {
            throw new IllegalStateException("Plan is not active. Cannot redeem.");
        }
        if (plan.getPaidCount() != GoldMinePlan.TOTAL_PAID_INSTALLMENTS) {
            throw new IllegalStateException("All 10 installments must be paid before redeeming. Paid: " + plan.getPaidCount());
        }

        GoldMineInstallment eleventh = installmentRepository.findByPlanIdAndInstallmentNumber(planId, GoldMinePlan.DISCOUNT_INSTALLMENT_NUMBER)
                .orElseThrow(() -> new ResourceNotFoundException("11th installment not found for plan " + planId));

        eleventh.setStatus(GoldMineInstallment.InstallmentStatus.WAIVED);
        eleventh.setPaidAt(LocalDateTime.now());
        installmentRepository.save(eleventh);

        plan.setStatus(GoldMinePlan.PlanStatus.COMPLETED);
        return planRepository.save(plan);
    }

    /** Cancel an active plan. */
    public GoldMinePlan cancel(Long planId) {
        GoldMinePlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));
        if (plan.getStatus() != GoldMinePlan.PlanStatus.ACTIVE) {
            throw new IllegalStateException("Only active plans can be cancelled.");
        }
        plan.setStatus(GoldMinePlan.PlanStatus.CANCELLED);
        return planRepository.save(plan);
    }

    /** Calculator defaults for frontend (min/max monthly amount). */
    public java.util.Map<String, Object> getCalculatorDefaults() {
        return java.util.Map.of(
                "minMonthlyAmount", GoldMinePlan.MIN_MONTHLY_AMOUNT,
                "maxMonthlyAmount", GoldMinePlan.MAX_MONTHLY_AMOUNT,
                "totalPaidInstallments", GoldMinePlan.TOTAL_PAID_INSTALLMENTS,
                "discountInstallmentNumber", GoldMinePlan.DISCOUNT_INSTALLMENT_NUMBER
        );
    }
}
