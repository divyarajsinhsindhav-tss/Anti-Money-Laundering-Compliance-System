package org.tss.tm.service.interfaces;

import org.tss.tm.dto.tenant.request.CreateCaseRequest;
import org.tss.tm.entity.tenant.AmlCase;

public interface CaseService {
    AmlCase createCase(CreateCaseRequest request, String createdByEmail);
}
