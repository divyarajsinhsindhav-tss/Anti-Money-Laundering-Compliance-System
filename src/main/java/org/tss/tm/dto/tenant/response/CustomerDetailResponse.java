package org.tss.tm.dto.tenant.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tss.tm.common.enums.AccountType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDetailResponse {
    private UUID customerId;
    private String cif;
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate dob;
    private BigDecimal income;
    private List<AccountResponse> accounts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountResponse {
        private String accountNumber;
        private AccountType accountType;
        private LocalDateTime openedAt;
    }

}
