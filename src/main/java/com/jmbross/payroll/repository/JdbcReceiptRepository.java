package com.jmbross.payroll.repository;

import com.jmbross.payroll.domain.Receipt;
import com.jmbross.payroll.domain.ReceiptLine;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

public final class JdbcReceiptRepository implements ReceiptRepository {
    private final DataSource dataSource;

    public JdbcReceiptRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Receipt save(Receipt receipt) {
        String receiptSql =
                "INSERT INTO receipts (worker_id,period_date,gross_amount,deductions_amount,net_amount) VALUES (?,?,?,?,?)";
        String lineSql = "INSERT INTO receipt_lines (receipt_id,description,percentage,amount) VALUES (?,?,?,?)";
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long receiptId;
                try (PreparedStatement statement =
                        connection.prepareStatement(receiptSql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setLong(1, receipt.workerId());
                    statement.setDate(2, Date.valueOf(receipt.period()));
                    statement.setBigDecimal(3, receipt.grossAmount());
                    statement.setBigDecimal(4, receipt.deductionsAmount());
                    statement.setBigDecimal(5, receipt.netAmount());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("No generated receipt id");
                        }
                        receiptId = keys.getLong(1);
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(lineSql)) {
                    for (ReceiptLine line : receipt.lines()) {
                        statement.setLong(1, receiptId);
                        statement.setString(2, line.description());
                        statement.setBigDecimal(3, line.percentage());
                        statement.setBigDecimal(4, line.amount());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                connection.commit();
                return new Receipt(
                        receiptId,
                        receipt.workerId(),
                        receipt.period(),
                        receipt.grossAmount(),
                        receipt.deductionsAmount(),
                        receipt.netAmount(),
                        receipt.lines());
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Could not save receipt", exception);
        }
    }

    @Override
    public List<Receipt> findAll() {
        return query(
                "SELECT r.*,l.description,l.percentage,l.amount FROM receipts r LEFT JOIN receipt_lines l ON l.receipt_id=r.id ORDER BY r.period_date DESC,r.id,l.id",
                null);
    }

    @Override
    public List<Receipt> findByWorker(long workerId) {
        return query(
                "SELECT r.*,l.description,l.percentage,l.amount FROM receipts r LEFT JOIN receipt_lines l ON l.receipt_id=r.id WHERE r.worker_id=? ORDER BY r.period_date DESC,r.id,l.id",
                workerId);
    }

    private List<Receipt> query(String sql, Long workerId) {
        Map<Long, Builder> receipts = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (workerId != null) {
                statement.setLong(1, workerId);
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    long id = result.getLong("id");
                    Builder builder = receipts.computeIfAbsent(id, ignored -> new Builder(result));
                    String description = result.getString("description");
                    if (description != null) {
                        builder.lines.add(new ReceiptLine(
                                description, result.getBigDecimal("percentage"), result.getBigDecimal("amount")));
                    }
                }
            }
            return receipts.values().stream().map(Builder::build).toList();
        } catch (SQLException exception) {
            throw new DataAccessException("Could not query receipts", exception);
        }
    }

    private static final class Builder {
        private final long id;
        private final long workerId;
        private final java.time.LocalDate period;
        private final java.math.BigDecimal gross;
        private final java.math.BigDecimal deductions;
        private final java.math.BigDecimal net;
        private final List<ReceiptLine> lines = new ArrayList<>();

        private Builder(ResultSet result) {
            try {
                id = result.getLong("id");
                workerId = result.getLong("worker_id");
                period = result.getDate("period_date").toLocalDate();
                gross = result.getBigDecimal("gross_amount");
                deductions = result.getBigDecimal("deductions_amount");
                net = result.getBigDecimal("net_amount");
            } catch (SQLException exception) {
                throw new DataAccessException("Could not map receipt", exception);
            }
        }

        private Receipt build() {
            return new Receipt(id, workerId, period, gross, deductions, net, lines);
        }
    }
}
