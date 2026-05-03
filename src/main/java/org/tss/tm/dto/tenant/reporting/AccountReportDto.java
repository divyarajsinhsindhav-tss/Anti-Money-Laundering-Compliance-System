package org.tss.tm.dto.tenant.reporting;

import org.tss.tm.common.enums.AccountType;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountReportDto {
    private String accountNumber;
    private AccountType accountType;
    private LocalDateTime openedAt;
}
