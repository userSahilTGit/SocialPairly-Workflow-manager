package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.*;
import com.SocialPairly_Workflow_Manager.entity.*;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.BankDetailsRepository;
import com.SocialPairly_Workflow_Manager.repository.PaymentRepository;
import com.SocialPairly_Workflow_Manager.repository.PlanUpgradeRequestRepository;
import com.SocialPairly_Workflow_Manager.repository.RefundRepository;
import com.SocialPairly_Workflow_Manager.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);
    private static final BigDecimal PROCESSING_FEE_RATE = new BigDecimal("0.05");

    private final RefundRepository refundRepository;
    private final BankDetailsRepository bankDetailsRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanUpgradeRequestRepository planUpgradeRequestRepository;
    private final CurrentUserService currentUserService;
    private final EmailService emailService;
    private final UserTokenService userTokenService;

    public RefundService(RefundRepository refundRepository,
                         BankDetailsRepository bankDetailsRepository,
                         PaymentRepository paymentRepository,
                         SubscriptionRepository subscriptionRepository,
                         PlanUpgradeRequestRepository planUpgradeRequestRepository,
                         CurrentUserService currentUserService,
                         EmailService emailService,
                         UserTokenService userTokenService) {
        this.refundRepository = refundRepository;
        this.bankDetailsRepository = bankDetailsRepository;
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planUpgradeRequestRepository = planUpgradeRequestRepository;
        this.currentUserService = currentUserService;
        this.emailService = emailService;
        this.userTokenService = userTokenService;
    }

    @Transactional(readOnly = true)
    public Optional<RefundDto> getCurrentUserRefund() {
        User user = currentUserService.getCurrentUser();
        return refundRepository.findFirstByUser_IdOrderByCreatedAtDesc(user.getId())
                .map(this::toUserRefundDto);
    }

    @Transactional
    public RefundDto submitRefundRequest(RefundRequestDto request) {
        User user = currentUserService.getCurrentUser();

        List<RefundStatus> terminalStatuses = List.of(RefundStatus.Completed, RefundStatus.Rejected);
        if (refundRepository.existsByUser_IdAndStatusNotIn(user.getId(), terminalStatuses)) {
            throw new BadRequestException("You already have an active refund request in progress");
        }

        List<PlanUpgradeStatus> upgradeTerminalStatuses =
                List.of(PlanUpgradeStatus.Completed, PlanUpgradeStatus.Rejected);
        if (planUpgradeRequestRepository.existsByUser_IdAndStatusNotIn(user.getId(), upgradeTerminalStatuses)) {
            throw new BadRequestException("You cannot request a refund while a plan upgrade request is active");
        }

        Subscription subscription = subscriptionRepository
                .findActiveSubscriptionsForUser(user.getId(), LocalDateTime.now())
                .stream()
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No active subscription found to discontinue"));

        List<Payment> payments = paymentRepository.findBySubscription_Id(subscription.getId());
        Payment payment = payments.stream()
                .filter(p -> "succeeded".equalsIgnoreCase(p.getStatus()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No successful payment found for your subscription"));

        Refund refund = new Refund();
        refund.setUser(user);
        refund.setPayment(payment);
        refund.setStatus(RefundStatus.Initiated);
        refund.setAction(RefundAction.Requested);
        refund.setReason(request.reason().trim());

        refund = refundRepository.save(refund);
        log.info("Refund request created id={} userId={}", refund.getRefundId(), user.getId());

        emailService.sendRefundRequestReceivedEmail(user, refund, subscription.getPlan().getPlanName());

        return toUserRefundDto(refund);
    }

    @Transactional
    public RefundDto submitSlot(Long refundId, SlotRequestDto request) {
        Refund refund = getUserRefundOrThrow(refundId);

        if (refund.getAction() != RefundAction.Requested_a_Call) {
            throw new BadRequestException("A consultation slot is not required at this stage");
        }
        if (request.slot().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Please select a future date and time");
        }

        refund.setSlot(request.slot());
        refund.setAction(RefundAction.Provided_Slot);
        refund = refundRepository.save(refund);

        log.info("Refund slot submitted id={}", refundId);
        return toUserRefundDto(refund);
    }

    @Transactional
    public RefundDto submitBankDetails(Long refundId, BankDetailsRequestDto request) {
        Refund refund = getUserRefundOrThrow(refundId);

        if (refund.getAction() != RefundAction.Approved) {
            throw new BadRequestException("Bank details are not required at this stage");
        }
        if (bankDetailsRepository.findByRefund_RefundId(refundId).isPresent()) {
            throw new BadRequestException("Bank details have already been submitted");
        }

        BankDetails details = new BankDetails();
        details.setUserId(refund.getUser().getId());
        details.setRefund(refund);
        details.setAccountHolderName(request.accountHolderName().trim());
        details.setBankName(request.bankName().trim());
        details.setAccountNumber(request.accountNumber().trim());
        details.setAccountType(request.accountType().trim());
        details.setAbaRoutingNumber(request.abaRoutingNumber().trim());
        details.setRecipientsAddress(request.recipientsAddress().trim());
        bankDetailsRepository.save(details);

        refund.setAction(RefundAction.Provided_Bank_Details);
        refund = refundRepository.save(refund);

        log.info("Bank details submitted for refund id={}", refundId);
        return toUserRefundDto(refund);
    }

    @Transactional(readOnly = true)
    public List<AdminRefundListDto> listRefundsForAdmin() {
        return refundRepository.findAllWithDetails().stream()
                .map(AdminRefundListDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminRefundDetailDto getRefundDetailForAdmin(Long refundId) {
        Refund refund = refundRepository.findByIdWithDetails(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found: " + refundId));
        BankDetails bankDetails = bankDetailsRepository.findByRefund_RefundId(refundId).orElse(null);
        return AdminRefundDetailDto.from(refund, bankDetails);
    }

    @Transactional
    public AdminRefundDetailDto adminRequestCall(Long refundId) {
        Refund refund = getAdminRefundOrThrow(refundId);
        validateAdminActionAllowed(refund);

        refund.setStatus(RefundStatus.In_Progress);
        refund.setAction(RefundAction.Requested_a_Call);
        refundRepository.save(refund);

        emailService.sendAdminRequestedCallEmail(refund.getUser());

        log.info("Admin requested call for refund id={}", refundId);
        return getRefundDetailForAdmin(refundId);
    }

    @Transactional
    public AdminRefundDetailDto adminApprove(Long refundId) {
        Refund refund = getAdminRefundOrThrow(refundId);
        validateAdminActionAllowed(refund);

        if (refund.getAction() == RefundAction.Closed) {
            throw new BadRequestException("This refund request has been closed");
        }

        refund.setStatus(RefundStatus.In_Progress);
        refund.setAction(RefundAction.Approved);
        refundRepository.save(refund);

        emailService.sendRefundApprovedEmail(refund.getUser());

        log.info("Admin approved refund id={}", refundId);
        return getRefundDetailForAdmin(refundId);
    }

    @Transactional
    public AdminRefundDetailDto adminClose(Long refundId) {
        Refund refund = getAdminRefundOrThrow(refundId);
        validateAdminActionAllowed(refund);

        refund.setStatus(RefundStatus.Rejected);
        refund.setAction(RefundAction.Closed);
        refundRepository.save(refund);

        log.info("Admin closed/rejected refund id={}", refundId);
        return getRefundDetailForAdmin(refundId);
    }

    @Transactional
    public AdminRefundDetailDto adminCompletePayout(Long refundId) {
        Refund refund = getAdminRefundOrThrow(refundId);

        if (refund.getStatus() == RefundStatus.Completed) {
            throw new BadRequestException("Refund payout has already been finalized");
        }
        if (refund.getAction() != RefundAction.Provided_Bank_Details) {
            throw new BadRequestException("Bank details must be provided before finalizing payout");
        }

        Payment payment = refund.getPayment();
        User user = refund.getUser();
        List<Subscription> activeSubscriptions =
                subscriptionRepository.findActiveSubscriptionsForUser(user.getId(), LocalDateTime.now());
        Plan plan = resolvePlanForRefund(payment, activeSubscriptions);

        BigDecimal refundAmount = calculateNetRefundAmount(payment.getAmount());
        RefundTokenAdjustment tokenAdjustment =
                userTokenService.applyPlanTokenClawbackOnRefund(user, plan, payment.getAmount());

        if (tokenAdjustment.hasTokenUsageDeduction()) {
            refundAmount = refundAmount.subtract(tokenAdjustment.tokenUsageCost())
                    .max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        payment.setAmountRefunded(refundAmount);
        paymentRepository.save(payment);

        refund.setStatus(RefundStatus.Completed);
        refundRepository.save(refund);

        cancelSubscriptions(activeSubscriptions);

        emailService.sendRefundFinalizedEmail(user, refund, refundAmount, tokenAdjustment);

        log.info("Admin finalized refund payout id={} amount={} tokenAdjustment={}",
                refundId, refundAmount, tokenAdjustment);
        return getRefundDetailForAdmin(refundId);
    }

    private Plan resolvePlanForRefund(Payment payment, List<Subscription> activeSubscriptions) {
        if (payment != null && payment.getSubscription() != null && payment.getSubscription().getPlan() != null) {
            return payment.getSubscription().getPlan();
        }
        return activeSubscriptions.stream()
                .map(Subscription::getPlan)
                .filter(p -> p != null)
                .findFirst()
                .orElse(null);
    }

    private void cancelSubscriptions(List<Subscription> subscriptions) {
        for (Subscription subscription : subscriptions) {
            subscription.setStatus("cancelled");
            subscription.setCanceledAt(LocalDateTime.now());
            subscription.setCancelAtPeriodEnd(false);
            subscriptionRepository.save(subscription);
        }
    }

    private BigDecimal calculateNetRefundAmount(BigDecimal grossAmount) {
        BigDecimal fee = grossAmount.multiply(PROCESSING_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        return grossAmount.subtract(fee).setScale(2, RoundingMode.HALF_UP);
    }

    private RefundDto toUserRefundDto(Refund refund) {
        BigDecimal netRefund = null;
        if (refund.getStatus() == RefundStatus.Completed) {
            netRefund = refund.getPayment().getAmountRefunded();
            // Legacy fallback only when amount was never written (null). Zero is a valid
            // payout after plan-token usage deductions.
            if (netRefund == null) {
                netRefund = calculateNetRefundAmount(refund.getPayment().getAmount());
            }
        }
        return RefundDto.from(refund, netRefund);
    }

    private Refund getUserRefundOrThrow(Long refundId) {
        User user = currentUserService.getCurrentUser();
        Refund refund = refundRepository.findByIdWithDetails(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found: " + refundId));
        if (!refund.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You do not have access to this refund request");
        }
        return refund;
    }

    private Refund getAdminRefundOrThrow(Long refundId) {
        return refundRepository.findByIdWithDetails(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found: " + refundId));
    }

    private void validateAdminActionAllowed(Refund refund) {
        if (refund.getStatus() == RefundStatus.Completed) {
            throw new BadRequestException("This refund has already been completed");
        }
        if (refund.getStatus() == RefundStatus.Rejected || refund.getAction() == RefundAction.Closed) {
            throw new BadRequestException("This refund request has been closed");
        }
    }
}
