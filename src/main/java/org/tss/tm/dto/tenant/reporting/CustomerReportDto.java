package org.tss.tm.dto.tenant.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerReportDto {
    private String cif;
    private String fullName;
    private LocalDate dob;
    private BigDecimal income;
    private List<AccountReportDto> accounts;
}
