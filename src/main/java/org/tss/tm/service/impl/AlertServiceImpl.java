package org.tss.tm.service.impl;

import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tss.tm.common.enums.AlertStatus;
import org.tss.tm.common.enums.UserRole;
import org.tss.tm.dto.tenant.response.AlertDetailResponse;
import org.tss.tm.dto.tenant.response.AlertResponse;
import org.tss.tm.dto.tenant.response.CustomerResponse;
import org.tss.tm.dto.tenant.response.FinancialTransactionResponse;
import org.tss.tm.entity.tenant.*;
import org.tss.tm.exception.BusinessRuleException;
import org.tss.tm.exception.ResourceNotFoundException;
import org.tss.tm.mapper.AlertMapper;
import org.tss.tm.repository.AlertAuditRepo;
import org.tss.tm.repository.AlertInfoRepo;
import org.tss.tm.repository.AlertRepo;
import org.tss.tm.repository.TenantUserRepo;
import org.tss.tm.service.interfaces.AlertService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertServiceImpl implements AlertService {

    private final AlertRepo alertRepo;
    private final AlertMapper alertMapper;
    private final AlertAuditRepo alertAuditRepo;
    private final TenantUserRepo tenantUserRepo;
    private final AlertInfoRepo alertInfoRepo;

    @Override
    @Transactional
    public AlertResponse updateAlertStatus(String alertCode, AlertStatus status, String reason, String changedByEmail) {
        log.info("Updating status for alert {} to {} by {}", alertCode, status, changedByEmail);

        Alert alert = alertRepo.findByAlertCode(alertCode)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", alertCode));

        if (alert.getAlertStatus() == status) {
            return alertMapper.toResponse(alert);
        }

        TenantUser user = tenantUserRepo.findByEmail(changedByEmail)
                .orElseThrow(() -> new ResourceNotFoundException("TenantUser", changedByEmail));

        AlertStatus oldStatus = alert.getAlertStatus();
        alert.setAlertStatus(status);
        Alert savedAlert = alertRepo.save(alert);

        AlertAudit audit = AlertAudit.builder()
                .alert(savedAlert)
                .statusFrom(oldStatus)
                .statusTo(status)
                .changedBy(user)
                .reason(reason)
                .changedAt(java.time.LocalDateTime.now())
                .build();
        alertAuditRepo.save(audit);

        return alertMapper.toResponse(savedAlert);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AlertResponse> getAllAlerts(AlertStatus status, Pageable pageable) {
        Page<Alert> alerts;
        if (status != null) {
            alerts = alertRepo.findAllByAlertStatus(status, pageable);
        } else {
            alerts = alertRepo.findAll(pageable);
        }
        return alerts.map(alertMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public AlertDetailResponse getAlert(String email, String alertCode) {
        TenantUser user = tenantUserRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("TenantUser", email));
        Alert alert = alertRepo.findByAlertCode(alertCode)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", alertCode));

        if (!user.getRole().equals(UserRole.BANK_ADMIN)) {
            if (alert.getAmlCase() == null) {
                throw new BusinessRuleException("You are not allowed to access this resource");
            }
            TenantUser caseAssignedTo = alert.getAmlCase().getAssignedTo();
            if (caseAssignedTo == null) {
                throw new ResourceNotFoundException("Compliance Officer", email);
            }
            if (!caseAssignedTo.getUserCode().equals(user.getUserCode())) {
                throw new BusinessRuleException("You are not allowed to access this resource");
            }
        }

        List<AlertInfo> alertInfo = alertInfoRepo.findAllByAlert_AlertCode(alertCode);

        List<FinancialTransactionResponse> financialTransactionResponses = alertInfo.stream()
                .filter(info -> info.getTransaction() != null)
                .map(info -> {
                    var txn = info.getTransaction();
                    return org.tss.tm.dto.tenant.response.FinancialTransactionResponse.builder()
                            .txnNo(txn.getTxnNo())
                            .accountNo(txn.getAccount() != null ? txn.getAccount().getAccountNumber() : null)
                            .amount(txn.getAmount())
                            .txnType(txn.getTxnType())
                            .direction(txn.getDirection())
                            .counterpartyAccountNo(txn.getCounterpartyAccountNo())
                            .counterpartyBankIfsc(txn.getCounterpartyBankIfsc())
                            .txnTimestamp(txn.getTxnTimestamp())
                            .countryCode(txn.getCountryCode())
                            .build();
                }).toList();

        List<CustomerResponse> customerResponses = List.of(CustomerResponse.builder()
                .cif(alert.getCustomer().getCif())
                .firstName(alert.getCustomer().getFirstName())
                .middleName(alert.getCustomer().getMiddleName())
                .lastName(alert.getCustomer().getLastName())
                .dob(alert.getCustomer().getDob())
                .income(alert.getCustomer().getIncome())
                .build());

        return AlertDetailResponse.builder()
                .alert(alertMapper.toResponse(alert))
                .financialTransactionResponsesList(financialTransactionResponses)
                .customerResponsesList(customerResponses)
                .build();
    }

}
