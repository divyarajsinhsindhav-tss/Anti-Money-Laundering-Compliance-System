package org.tss.tm.service.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.tss.tm.dto.tenant.reporting.CasePdfReportDto;
import org.tss.tm.service.interfaces.PdfGeneratorService;

import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class PdfGeneratorServiceImpl implements PdfGeneratorService {

    private final TemplateEngine templateEngine;

    public byte[] generateCasePdf(CasePdfReportDto reportDto) {

        try {
            Context context = new Context();
            context.setVariable("report", reportDto);

            String html = templateEngine.process("reporting/case-report",context);

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                PdfRendererBuilder builder = new PdfRendererBuilder();

                builder.useFastMode();
                builder.withHtmlContent(html, null);
                builder.toStream(outputStream);
                builder.run();

                return outputStream.toByteArray();
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate case PDF for report generation",e);
        }
    }
}