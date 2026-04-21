package org.tss.tm.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.tss.tm.entity.common.ValidationError;
import org.tss.tm.service.interfaces.FileService;

import org.postgresql.copy.CopyManager;
import org.postgresql.copy.CopyIn;
import org.postgresql.core.BaseConnection;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class FileServiceImpl implements FileService {
    private final int errorLimit = 1000;

    @Autowired
    private DataSource dataSource;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override

    public CompletableFuture<List<ValidationError>> processFile(MultipartFile newFile) {
        File dir = new File(uploadDir);

        String name = newFile.getOriginalFilename();
        if (name == null || name.isBlank()) {
            throw new RuntimeException("Invalid file name");
        }

        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(uploadDir, System.currentTimeMillis() + "_" + newFile.getOriginalFilename());
        try {
            newFile.transferTo(file);
            return importCsv(file);
        } catch (IOException e) {
            throw new RuntimeException("File Upload Failed");
        }
    }

    @Async
    public CompletableFuture<List<ValidationError>> importCsv(File file) {
        List<ValidationError> errors = new ArrayList<>();

        try (
                Reader reader = new BufferedReader(new FileReader(file));
                Connection conn = dataSource.getConnection()
        ) {
            conn.setAutoCommit(false);
            CopyManager copyManager = new CopyManager(
                    conn.unwrap(BaseConnection.class)
            );

            String sql = "COPY staging_transactions (" +
                    "txn_no, account_number, amount, txn_type, direction, " +
                    "counterparty_account_no, counterparty_bank_ifsc, swift_code, txn_timestamp, country_code" +
                    ") FROM STDIN WITH (FORMAT csv)";

            CopyIn copyIn = copyManager.copyIn(sql);

            CSVParser parser = CSVFormat.DEFAULT
                    .withHeader()
                    .withIgnoreHeaderCase()
                    .withTrim()
                    .parse(reader);

            validateHeader(parser);

            int rowCounter = 1;

            for (CSVRecord record : parser) {
                List<String> row = new ArrayList<>();
                record.forEach(row::add);

                List<ValidationError> rowErrors = validator(row, rowCounter);

                if (rowErrors.isEmpty()) {
                    String csvLine = String.join(",", row) + "\n";
                    byte[] bytes = csvLine.getBytes(StandardCharsets.UTF_8);
                    copyIn.writeToCopy(bytes, 0, bytes.length);
                } else {
                    errors.addAll(rowErrors);
                    if (errors.size() >= errorLimit) {
                        copyIn.cancelCopy();
                        conn.rollback();
                        throw new RuntimeException("File Upload Failed");
                    }
                }
                rowCounter++;
            }
            copyIn.endCopy();
            conn.commit();

        } catch (IOException e) {
//            try { conn.rollback(); } catch (Exception ex) {}
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (file.exists()) file.delete();
        }
        return CompletableFuture.completedFuture(errors);
    }

    private void validateHeader(CSVParser parser) {

        List<String> expected = List.of(
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

        List<String> actual = parser.getHeaderNames();

        if (!actual.equals(expected)) {
            throw new RuntimeException("Invalid header");
        }
    }


    private List<ValidationError> validator(List<String> row, int rowNumber) {

        List<ValidationError> errors = new ArrayList<>();

        if (row.size() != 10) {
            errors.add(new ValidationError(rowNumber, "ROW", "Invalid column count", null));
            return errors;
        }

        String txnType = row.get(3);
        String direction = row.get(4);

        if (!row.get(0).matches("^[A-Za-z0-9_-]{1,50}$"))
            errors.add(new ValidationError(rowNumber, "txn_no", "Invalid txn_no", row.get(0)));

        if (!row.get(1).matches("^[0-9]{9,18}$"))
            errors.add(new ValidationError(rowNumber, "account_number", "Invalid account number", row.get(1)));

        if (!row.get(2).matches("^\\d+(\\.\\d{1,2})?$"))
            errors.add(new ValidationError(rowNumber, "amount", "Invalid amount", row.get(2)));

        if (!txnType.matches("^(DEBIT|CREDIT|REVERSAL)$"))
            errors.add(new ValidationError(rowNumber, "txn_type", "Invalid txn type", txnType));

        if (!direction.matches("^(IN|OUT)$"))
            errors.add(new ValidationError(rowNumber, "direction", "Invalid direction", direction));

        if (txnType.equals("DEBIT") && !direction.equals("OUT"))
            errors.add(new ValidationError(rowNumber, "direction", "DEBIT must be OUT", direction));

        if (txnType.equals("CREDIT") && !direction.equals("IN"))
            errors.add(new ValidationError(rowNumber, "direction", "CREDIT must be IN", direction));

        if (!row.get(5).matches("^[0-9]{9,18}$"))
            errors.add(new ValidationError(rowNumber, "counterparty_account_no", "Invalid account", row.get(5)));

        if (!row.get(6).matches("^[A-Z]{4}0[A-Z0-9]{6}$"))
            errors.add(new ValidationError(rowNumber, "ifsc", "Invalid IFSC", row.get(6)));

        if (!row.get(7).matches("^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$"))
            errors.add(new ValidationError(rowNumber, "swift", "Invalid SWIFT", row.get(7)));

        if (!row.get(8).matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$"))
            errors.add(new ValidationError(rowNumber, "timestamp", "Invalid timestamp", row.get(8)));

        if (!row.get(9).matches("^[A-Z]{2}$"))
            errors.add(new ValidationError(rowNumber, "country", "Invalid country", row.get(9)));

        return errors;
    }
}
