package org.tss.tm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tss.tm.common.enums.AlertStatus;
import org.tss.tm.dto.tenant.response.AlertResponse;
import org.tss.tm.entity.tenant.Alert;
import org.tss.tm.entity.tenant.AlertAudit;
import org.tss.tm.entity.tenant.TenantUser;
import org.tss.tm.exception.ResourceNotFoundException;
import org.tss.tm.mapper.AlertMapper;
import org.tss.tm.repository.AlertAuditRepo;
import org.tss.tm.repository.AlertRepo;
import org.tss.tm.repository.TenantRepo;
import org.tss.tm.repository.TenantUserRepo;
import org.tss.tm.service.interfaces.AlertService;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertServiceImpl implements AlertService {

    private final AlertRepo alertRepo;
    private final AlertMapper alertMapper;
    private final AlertAuditRepo alertAuditRepo;
    private final TenantUserRepo tenantUserRepo;
    private final TenantRepo tenantRepo;

    @Override
    @org.springframework.transaction.annotation.Transactional
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

}
