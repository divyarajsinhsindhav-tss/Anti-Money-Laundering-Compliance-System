package org.tss.tm.service.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.tss.tm.common.enums.CaseStatus;
import org.tss.tm.dto.tenant.request.CreateCaseRequest;
import org.tss.tm.dto.tenant.response.CaseResponse;
import org.tss.tm.entity.tenant.AmlCase;
import org.tss.tm.dto.tenant.response.CaseDetailResponse;

import java.util.List;

public interface CaseService {
    AmlCase createCase(CreateCaseRequest request, String createdByEmail);

    Page<CaseResponse> getAllCases(CaseStatus status, String email, boolean isAdmin, Pageable pageable);

    CaseDetailResponse getCase(String caseCode, String email, boolean isAdmin);

    List<CaseResponse> autoGenerateCases(String createdByEmail);

    CaseResponse assignCase(String caseCode, String assignedToUserCode);

    CaseDetailResponse updateCase(String caseCode, org.tss.tm.dto.tenant.request.UpdateCaseStatusRequest request, String email);
}
