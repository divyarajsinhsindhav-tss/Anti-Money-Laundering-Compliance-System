package org.tss.tm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tss.tm.dto.tenant.reporting.*;
import org.tss.tm.entity.system.Rule;
import org.tss.tm.entity.system.Scenario;
import org.tss.tm.entity.tenant.*;
import org.tss.tm.repository.AlertInfoRepo;
import org.tss.tm.repository.CaseRepo;
import org.tss.tm.service.interfaces.CaseReportService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CaseReportServiceImpl implements CaseReportService {

    private final CaseRepo caseRepo;
    private final AlertInfoRepo alertInfoRepo;

    @Transactional(readOnly = true)
    public CasePdfReportDto buildCaseReport(String caseCode) {

        AmlCase amlCase = caseRepo.findDetailedByCaseCode(caseCode)
                .orElseThrow(() ->
                        new RuntimeException("Case not found: " + caseCode));

        if (amlCase.getAlerts() == null || amlCase.getAlerts().isEmpty()) {
            throw new RuntimeException("No alerts found for case: " + caseCode);
        }

        Alert firstAlert = amlCase.getAlerts().get(0);
        Customer customer = firstAlert.getCustomer();


        CaseReportDto caseDto = mapCase(amlCase);
        CustomerReportDto customerDto = mapCustomer(customer);
        List<AlertReportDto> alertDtos = new ArrayList<>();

        for (Alert alert : amlCase.getAlerts()) {

            List<AlertInfo> alertInfos =
                    alertInfoRepo.findDetailedByAlertCode(alert.getAlertCode());

            LinkedHashMap<UUID, RuleReportDto> uniqueRules = new LinkedHashMap<>();

            LinkedHashMap<UUID, TransactionReportDto> uniqueTransactions =
                    new LinkedHashMap<>();

            for (AlertInfo alertInfo : alertInfos) {


                if (alertInfo.getRule() != null) {

                    Rule rule = alertInfo.getRule();
                    uniqueRules.putIfAbsent(
                            rule.getRuleId(),
                            mapRule(rule)
                    );
                }

                if (alertInfo.getTransaction() != null) {

                    FinancialTransaction txn =
                            alertInfo.getTransaction();

                    uniqueTransactions.putIfAbsent(
                            txn.getTransactionId(),
                            mapTransaction(txn)
                    );
                }
            }

            AlertReportDto alertDto = AlertReportDto.builder()
                    .alertCode(alert.getAlertCode())
                    .alertStatus(alert.getAlertStatus())
                    .createdAt(alert.getCreatedAt())
                    .scenario(mapScenario(alert.getScenario()))
                    .rulesBroken(
                            new ArrayList<>(uniqueRules.values())
                    )
                    .involvedTransactions(
                            new ArrayList<>(uniqueTransactions.values())
                    )
                    .build();

            alertDtos.add(alertDto);
        }

        return CasePdfReportDto.builder()
                .caseInfo(caseDto)
                .customer(customerDto)
                .alerts(alertDtos)
                .build();
    }


    private CaseReportDto mapCase(AmlCase amlCase) {

        return CaseReportDto.builder()
                .caseCode(amlCase.getCaseCode())
                .createdBy(
                        amlCase.getCreatedBy() != null
                                ? mapTenantUser(amlCase.getCreatedBy())
                                : null
                )
                .assignedTo(
                        amlCase.getAssignedTo() != null
                                ? mapTenantUser(amlCase.getAssignedTo())
                                : null
                )
                .status(amlCase.getStatus())
                .notes(amlCase.getNotes())
                .createdAt(amlCase.getCreatedAt())
                .closedAt(amlCase.getClosedAt())
                .build();
    }

    private CustomerReportDto mapCustomer(Customer customer) {

        List<AccountReportDto> accountDtos =
                customer.getAccounts()
                        .stream()
                        .map(this::mapAccount)
                        .collect(Collectors.toList());

        return CustomerReportDto.builder()
                .cif(customer.getCif())
                .fullName(buildFullName(
                        customer.getFirstName(),
                        customer.getMiddleName(),
                        customer.getLastName()
                ))
                .dob(customer.getDob())
                .income(customer.getIncome())
                .accounts(accountDtos)
                .build();
    }

    private AccountReportDto mapAccount(Account account) {

        return AccountReportDto.builder()
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .openedAt(account.getOpenedAt())
                .build();
    }

    private ScenarioReportDto mapScenario(Scenario scenario) {

        return ScenarioReportDto.builder()
                .scenarioCode(scenario.getScenarioCode())
                .scenarioName(scenario.getScenarioName())
                .description(scenario.getDescription())
                .build();
    }

    private RuleReportDto mapRule(Rule rule) {

        return RuleReportDto.builder()
                .ruleCode(rule.getRuleCode())
                .ruleName(rule.getRuleName())
                .description(rule.getDescription())
                .ruleCategory(rule.getRuleCategory())
                .build();
    }

    private TransactionReportDto mapTransaction(FinancialTransaction txn) {

        return TransactionReportDto.builder()
                .txnNo(txn.getTxnNo())
                .accountNumber(
                        txn.getAccount() != null
                                ? txn.getAccount().getAccountNumber()
                                : null
                )
                .accountType(
                        txn.getAccount() != null
                                ? txn.getAccount().getAccountType()
                                : null
                )
                .amount(txn.getAmount())
                .txnType(txn.getTxnType())
                .direction(txn.getDirection())
                .counterpartyAccountNo(txn.getCounterpartyAccountNo())
                .counterpartyBankIfsc(txn.getCounterpartyBankIfsc())
                .txnTimestamp(txn.getTxnTimestamp())
                .countryCode(txn.getCountryCode())
                .build();
    }

    private TenantUserReportDto mapTenantUser(TenantUser user) {

        return TenantUserReportDto.builder()
                .userCode(user.getUserCode())

                .fullName(buildFullName(
                        user.getFirstName(),
                        null,
                        user.getLastName()
                ))

                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    private String buildFullName(
            String firstName,
            String middleName,
            String lastName
    ) {

        StringBuilder sb = new StringBuilder();

        if (firstName != null) {
            sb.append(firstName).append(" ");
        }

        if (middleName != null && !middleName.isBlank()) {
            sb.append(middleName).append(" ");
        }

        if (lastName != null) {
            sb.append(lastName);
        }

        return sb.toString().trim();
    }
}