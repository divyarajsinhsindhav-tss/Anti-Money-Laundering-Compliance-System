package org.tss.tm.service.impl;

import jakarta.persistence.EntityManager;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.tss.tm.dto.tenant.response.CaseResponse;
import org.tss.tm.mapper.CaseMapper;
import org.tss.tm.entity.system.Tenant;
import org.tss.tm.service.interfaces.CaseService;
import org.tss.tm.service.interfaces.TenantService;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseServiceImpl implements CaseService {

    private final CaseRepo caseRepo;
    private final AlertRepo alertRepo;
    private final TenantUserRepo tenantUserRepo;
    private final TenantService tenantService;
    private final EntityManager entityManager;
    private final CaseMapper caseMapper;

    @Override
    @Transactional
    public AmlCase createCase(CreateCaseRequest request, String createdByEmail) {
        log.info("Creating case for alerts: {} assigned to user code: {}", request.getAlertCodes(),
                request.getAssignedToUserCode());

        TenantUser creator = tenantUserRepo.findByEmail(createdByEmail)
                .orElseThrow(() -> new ResourceNotFoundException("TenantUser", createdByEmail));

        TenantUser assignedTo = tenantUserRepo.findByUserCode(request.getAssignedToUserCode())
                .orElseThrow(() -> new ResourceNotFoundException("TenantUser", request.getAssignedToUserCode()));

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

        AmlCase savedCase = caseRepo.saveAndFlush(amlCase);
        entityManager.refresh(savedCase);

        for (Alert alert : alerts) {
            alert.setAmlCase(savedCase);
            alert.setAlertStatus(AlertStatus.IN_CASE);
        }
        alertRepo.saveAll(alerts);

        return savedCase;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CaseResponse> getAllCases(CaseStatus status, String email, boolean isAdmin, Pageable pageable) {
        log.info("Fetching cases for user: {}, status: {}, isAdmin: {}", email, status, isAdmin);
        Page<AmlCase> cases;

        if (isAdmin) {
            if (status != null) {
                cases = caseRepo.findAllByStatus(status, pageable);
            } else {
                cases = caseRepo.findAll(pageable);
            }
        } else {
            if (status != null) {
                cases = caseRepo.findAllByStatusAndAssignedTo_Email(status, email, pageable);
            } else {
                cases = caseRepo.findAllByAssignedTo_Email(email, pageable);
            }
        }

        return cases.map(caseMapper::toResponse);
    }
}
