package org.tss.tm.service.interfaces;

import org.tss.tm.dto.tenant.reporting.CasePdfReportDto;

public interface PdfGeneratorService {
    byte[] generateCasePdf(CasePdfReportDto reportDto);
}
