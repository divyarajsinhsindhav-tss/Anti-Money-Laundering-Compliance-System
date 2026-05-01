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
import org.tss.tm.dto.tenant.request.UpdateCaseStatusRequest;
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
import org.tss.tm.dto.tenant.response.CaseDetailResponse;
import org.tss.tm.mapper.CaseMapper;
import org.tss.tm.mapper.AlertMapper;
import org.tss.tm.service.interfaces.CaseService;
import org.tss.tm.service.interfaces.TenantService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.tss.tm.entity.tenant.Customer;

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
    private final AlertMapper alertMapper;

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

        if (savedCase.getAlerts() == null) {
            savedCase.setAlerts(new ArrayList<>());
        }

        for (Alert alert : alerts) {
            alert.setAmlCase(savedCase);
            alert.setAlertStatus(AlertStatus.IN_CASE);
            savedCase.getAlerts().add(alert);
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

    @Override
    @Transactional(readOnly = true)
    public CaseDetailResponse getCase(String caseCode, String email, boolean isAdmin) {
        log.info("Case requested by email: {}, caseCode: {}", email, caseCode);

        AmlCase amlCase = caseRepo.findByCaseCode(caseCode)
                .orElseThrow(() -> new ResourceNotFoundException("Case", caseCode));

        if (!isAdmin && (amlCase.getAssignedTo() == null || !amlCase.getAssignedTo().getEmail().equals(email))) {
            throw new BusinessRuleException("You do not have permission to view this case", "ACCESS_DENIED");
        }

        List<Alert> caseAlerts = alertRepo.findAllByAmlCase_CaseCode(amlCase.getCaseCode());

        return CaseDetailResponse.builder()
                .caseResponse(caseMapper.toResponse(amlCase))
                .alerts(alertMapper.toResponseList(caseAlerts))
                .build();
    }

    @Override
    @Transactional
    public List<CaseResponse> autoGenerateCases(String createdByEmail) {
        log.info("Auto-generating cases from OPEN alerts for user: {}", createdByEmail);

        TenantUser creator = tenantUserRepo.findByEmail(createdByEmail)
                .orElseThrow(() -> new ResourceNotFoundException("TenantUser", createdByEmail));

        List<Alert> openAlerts = alertRepo.findAllByAlertStatus(AlertStatus.OPEN);
        log.info("Found {} OPEN alerts to process", openAlerts.size());

        if (openAlerts.isEmpty()) {
            return List.of();
        }

        // Group alerts by customer
        Map<Customer, List<Alert>> alertsByCustomer = openAlerts.stream()
                .collect(Collectors.groupingBy(Alert::getCustomer));

        List<AmlCase> generatedCases = new ArrayList<>();

        for (Map.Entry<Customer, List<Alert>> entry : alertsByCustomer.entrySet()) {
            Customer customer = entry.getKey();
            List<Alert> customerAlerts = entry.getValue();

            AmlCase amlCase = AmlCase.builder()
                    .createdBy(creator)
                    .status(CaseStatus.OPEN)
                    .notes("Auto-generated case for customer: " + customer.getFirstName() + " " + customer.getLastName()
                            + " (CIF: " + customer.getCif() + ")")
                    .build();

            // Save the case first
            AmlCase savedCase = caseRepo.saveAndFlush(amlCase);
            entityManager.refresh(savedCase);

            if (savedCase.getAlerts() == null) {
                savedCase.setAlerts(new ArrayList<>());
            }

            for (Alert alert : customerAlerts) {
                alert.setAmlCase(savedCase);
                alert.setAlertStatus(AlertStatus.IN_CASE);
                savedCase.getAlerts().add(alert);
            }
            alertRepo.saveAll(customerAlerts);
            generatedCases.add(savedCase);

            log.info("Generated case {} with {} alerts for customer {}", savedCase.getCaseCode(), customerAlerts.size(),
                    customer.getCif());
        }

        return generatedCases.stream().map(caseMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public CaseResponse assignCase(String caseCode, String assignedToUserCode) {
        log.info("Assigning case {} to user code {}", caseCode, assignedToUserCode);

        AmlCase amlCase = caseRepo.findByCaseCode(caseCode)
                .orElseThrow(() -> new ResourceNotFoundException("Case", caseCode));

        if (amlCase.getStatus() != CaseStatus.OPEN && amlCase.getStatus() != CaseStatus.UNDER_REVIEW) {
            throw new BusinessRuleException("Cases can only be assigned or reassigned when in OPEN or UNDER_REVIEW status", "INVALID_STATUS");
        }

        TenantUser assignedTo = tenantUserRepo.findByUserCode(assignedToUserCode)
                .orElseThrow(() -> new ResourceNotFoundException("TenantUser", assignedToUserCode));

        if (assignedTo.getRole() != UserRole.COMPLIANCE_OFFICER) {
            throw new BusinessRuleException("Cases can only be assigned to Compliance Officers", "INVALID_ASSIGNMENT");
        }

        amlCase.setAssignedTo(assignedTo);
        amlCase.setStatus(CaseStatus.UNDER_REVIEW);
        AmlCase savedCase = caseRepo.save(amlCase);

        return caseMapper.toResponse(savedCase);
    }

    @Override
    @Transactional
    public CaseDetailResponse updateCase(String caseCode, UpdateCaseStatusRequest request, String email) {
        log.info("Updating status for case {} to {} by {}", caseCode, request.getCaseStatus(), email);

        AmlCase amlCase = caseRepo.findByCaseCode(caseCode)
                .orElseThrow(() -> new ResourceNotFoundException("Case", caseCode));

        TenantUser user = tenantUserRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("TenantUser", email));

        if (user.getRole() != UserRole.BANK_ADMIN && (amlCase.getAssignedTo() == null || !amlCase.getAssignedTo().getEmail().equals(email))) {
            throw new BusinessRuleException("You do not have permission to update this case", "ACCESS_DENIED");
        }

        amlCase.setStatus(request.getCaseStatus());
        if (request.getCaseStatus() == CaseStatus.CLOSED) {
            amlCase.setClosedAt(java.time.LocalDateTime.now());
        }

        if (request.getReason() != null && !request.getReason().isEmpty()) {
            String currentNotes = amlCase.getNotes() != null ? amlCase.getNotes() : "";
            amlCase.setNotes(currentNotes + "\n\nStatus changed to " + request.getCaseStatus() + ". Reason: " + request.getReason());
        }

        AmlCase savedCase = caseRepo.save(amlCase);

        List<Alert> caseAlerts = alertRepo.findAllByAmlCase_CaseCode(savedCase.getCaseCode());

        return CaseDetailResponse.builder()
                .caseResponse(caseMapper.toResponse(savedCase))
                .alerts(alertMapper.toResponseList(caseAlerts))
                .build();
    }
}
