package org.tss.tm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tss.tm.common.enums.AlertStatus;
import org.tss.tm.common.enums.CaseStatus;
import org.tss.tm.common.enums.UserRole;
import org.tss.tm.dto.tenant.request.CreateCaseRequest;
import org.tss.tm.entity.tenant.Alert;
import org.tss.tm.entity.tenant.AmlCase;
import org.tss.tm.entity.tenant.TenantUser;
import org.tss.tm.exception.BusinessRuleException;
import org.tss.tm.exception.ResourceNotFoundException;
import org.tss.tm.repository.AlertRepo;
import org.tss.tm.repository.CaseRepo;
import org.tss.tm.repository.TenantUserRepo;
import org.tss.tm.service.interfaces.CaseService;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseServiceImpl implements CaseService {

    private final CaseRepo caseRepo;
    private final AlertRepo alertRepo;
    private final TenantUserRepo tenantUserRepo;

    @Override
    @Transactional
    public AmlCase createCase(CreateCaseRequest request, String createdByEmail) {
        log.info("Creating case for alerts: {} assigned to: {}", request.getAlertCodes(), request.getAssignedToUserId());

        TenantUser creator = tenantUserRepo.findByEmail(createdByEmail)
                .orElseThrow(() -> new ResourceNotFoundException("TenantUser", createdByEmail));

        TenantUser assignedTo = tenantUserRepo.findById(request.getAssignedToUserId())
                .orElseThrow(() -> new ResourceNotFoundException("TenantUser", request.getAssignedToUserId().toString()));

        if (assignedTo.getRole() != UserRole.COMPLIANCE_OFFICER) {
            throw new BusinessRuleException("Cases can only be assigned to Compliance Officers", "INVALID_ASSIGNMENT");
        }

        List<Alert> alerts = alertRepo.findAllByAlertCodeIn(request.getAlertCodes());
        if (alerts.size() != request.getAlertCodes().size()) {
            throw new ResourceNotFoundException("Alerts", request.getAlertCodes().toString());
        }

        AmlCase amlCase = AmlCase.builder()
                .createdBy(creator)
                .assignedTo(assignedTo)
                .status(CaseStatus.OPEN)
                .notes(request.getNotes())
                .build();

        AmlCase savedCase = caseRepo.save(amlCase);

        for (Alert alert : alerts) {
            alert.setAmlCase(savedCase);
            alert.setAlertStatus(AlertStatus.IN_CASE);
        }
        alertRepo.saveAll(alerts);

        return savedCase;
    }
}
