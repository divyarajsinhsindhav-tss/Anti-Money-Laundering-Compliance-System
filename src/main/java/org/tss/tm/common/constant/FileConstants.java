package org.tss.tm.common.constant;

import java.util.List;

public class FileConstants {
    public static final List<String> expectedTransactionHeader = List.of(
            "txn_no",
            "account_number",
            "amount",
            "txn_type",
            "direction",
            "counterparty_account_no",
            "counterparty_bank_ifsc",
            "swift_code",
            "txn_timestamp",
            "country_code"
    );

    public static final List<String> expectedCustomerHeader = List.of(
            "cif",
            "first_name",
            "middle_name",
            "last_name",
            "dob",
            "income",
            "account_number",
            "account_type",
            "opened_at"
    );
}
