package org.tss.tm.service.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.tss.tm.common.enums.CaseStatus;
import org.tss.tm.dto.tenant.request.CreateCaseRequest;
import org.tss.tm.dto.tenant.response.CaseResponse;
import org.tss.tm.entity.tenant.AmlCase;

public interface CaseService {
    AmlCase createCase(CreateCaseRequest request, String createdByEmail);
    Page<CaseResponse> getAllCases(CaseStatus status, String email, boolean isAdmin, Pageable pageable);
}
