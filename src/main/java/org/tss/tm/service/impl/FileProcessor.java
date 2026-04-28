package org.tss.tm.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.postgresql.copy.CopyIn;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.tss.tm.common.enums.JobStatus;
import org.tss.tm.service.interfaces.JobService;
import org.tss.tm.tenant.TenantContext;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

@Service
@Slf4j
public class FileProcessor {
    @Autowired
    private DataSource dataSource;
    @Autowired
    private JobService jobService;


    @Async("File Job Executor")
    public void loadTransactionCsv(UUID jobId, File file, String tenantSchemaName) {
        jobService.updateJobStatus(jobId, JobStatus.RUNNING);
        Connection conn = null;

        String createTempTableSql =
                "CREATE TEMP TABLE temp_staging (" +
                        "   txn_no TEXT, account_number TEXT, amount TEXT, txn_type TEXT, " +
                        "   direction TEXT, counterparty_account_no TEXT, counterparty_bank_ifsc TEXT, " +
                        "   swift_code TEXT, txn_timestamp TEXT, country_code TEXT" +
                        ") ON COMMIT DROP";

        String copySql =
                "COPY temp_staging (" +
                        "txn_no, account_number, amount, txn_type, direction, " +
                        "counterparty_account_no, counterparty_bank_ifsc, swift_code, txn_timestamp, country_code" +
                        ") FROM STDIN WITH (FORMAT csv, HEADER true)";

        try (InputStream inputStream =
                     new BufferedInputStream(new FileInputStream(file))) {


            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET search_path TO \"" + tenantSchemaName + "\", public");
            }

            try (PreparedStatement psCreate = conn.prepareStatement(createTempTableSql)) {
                psCreate.execute();
            }

            CopyManager copyManager =
                    new CopyManager(conn.unwrap(BaseConnection.class));

            CopyIn copyIn = copyManager.copyIn(copySql);

            byte[] buffer = new byte[1024 * 1024];
            int len;

            while ((len = inputStream.read(buffer)) != -1) {
                copyIn.writeToCopy(buffer, 0, len);
            }

            copyIn.endCopy();

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO staging_transactions " +
                            "(job_id, txn_no, account_number, amount, txn_type, direction, " +
                            "counterparty_account_no, counterparty_bank_ifsc, swift_code, txn_timestamp, country_code) " +
                            "SELECT ?, txn_no, account_number, amount, txn_type, direction, " +
                            "counterparty_account_no, counterparty_bank_ifsc, swift_code, txn_timestamp, country_code " +
                            "FROM temp_staging"
            )) {
                ps.setString(1, String.valueOf(jobId));
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("CALL validate_transactions(?)")) {
                ps.setString(1, String.valueOf(jobId));

                ps.execute();
            }

            conn.commit();
            jobService.updateJobStatus(jobId, JobStatus.COMPLETED);

            log.info("File ingestion completed for jobId={}", jobId);


        } catch (Exception e) {

            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }

            jobService.updateJobStatus(jobId, JobStatus.FAILED);
            throw new RuntimeException("COPY ingestion failed", e);

        } finally {
            log.info("CSV Loader Function Finished.");
            if (conn != null) {
                try {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("SET search_path TO public");
                    }
                    conn.close();
                } catch (SQLException ignored) {
                }
            }

            if (file.exists()) file.delete();
        }
    }

    @Async("File Job Executor")
    public void loadCustomerFile(UUID jobId, File file, String tenantSchemaName) {
        jobService.updateJobStatus(jobId, JobStatus.RUNNING);

        Connection conn = null;

        String createTempTableSql =
                "CREATE TEMP TABLE temp_staging (" +
                        "   cif TEXT, first_name TEXT, middle_name TEXT, last_name TEXT, " +
                        "   dob TEXT, income TEXT, account_number TEXT, " +
                        "   account_type TEXT, opened_at TEXT" +
                        ") ON COMMIT DROP";

        String copySql =
                "COPY temp_staging (" +
                        "cif, first_name, middle_name, last_name, dob, income, account_number, account_type, opened_at" +
                        ") FROM STDIN WITH (FORMAT csv, HEADER true)";

        try (InputStream inputStream =
                     new BufferedInputStream(new FileInputStream(file))) {

            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET search_path TO \"" + tenantSchemaName + "\", public");
            }

            try (PreparedStatement psCreate = conn.prepareStatement(createTempTableSql)) {
                psCreate.execute();
            }

            CopyManager copyManager =
                    new CopyManager(conn.unwrap(BaseConnection.class));

            CopyIn copyIn = copyManager.copyIn(copySql);

            byte[] buffer = new byte[1024 * 1024];
            int len;

            while ((len = inputStream.read(buffer)) != -1) {
                copyIn.writeToCopy(buffer, 0, len);
            }

            copyIn.endCopy();

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO staging_customers " +
                            "(job_id,cif, first_name, middle_name, last_name, dob, income, account_number, account_type, opened_at) " +
                            "SELECT ?,cif, first_name, middle_name, last_name, dob, income, account_number, account_type, opened_at " +
                            "FROM temp_staging"
            )) {
                ps.setString(1, String.valueOf(jobId));
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("CALL validate_customers(?)")) {
                ps.setString(1, String.valueOf(jobId));
                ps.execute();
            }

            conn.commit();
            jobService.updateJobStatus(jobId, JobStatus.COMPLETED);

            log.info("File ingestion completed for jobId={}", jobId);

        } catch (Exception e) {

            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }

            jobService.updateJobStatus(jobId, JobStatus.FAILED);
            throw new RuntimeException("COPY ingestion failed", e);

        } finally {
            log.info("CSV Loader Function Finished.");
            if (conn != null) {
                try {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("SET search_path TO public");
                    }
                    conn.close();
                } catch (SQLException ignored) {
                }
            }

            if (file.exists()) file.delete();
        }
    }
}
