package org.tss.tm.service.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.tss.tm.common.enums.AlertStatus;
import org.tss.tm.dto.tenant.response.AlertDetailResponse;
import org.tss.tm.dto.tenant.response.AlertResponse;

public interface AlertService {
    Page<AlertResponse> getAllAlerts(String email, String alertCode, AlertStatus status, Pageable pageable);

    AlertResponse updateAlertStatus(String alertCode, AlertStatus status, String reason, String changedByEmail);

    @Transactional(readOnly = true)
    AlertDetailResponse getAlert(String email, String alertCode);
}
