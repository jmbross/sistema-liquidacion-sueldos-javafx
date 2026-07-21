package com.jmbross.payroll.repository;

import com.jmbross.payroll.domain.Worker;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

public final class JdbcWorkerRepository implements WorkerRepository {
    private final DataSource dataSource;

    public JdbcWorkerRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Worker> findById(long id) {
        return queryOne("SELECT * FROM workers WHERE id=?", id);
    }

    @Override
    public List<Worker> findAll() {
        return queryMany("SELECT * FROM workers ORDER BY last_name,first_name", null);
    }

    @Override
    public List<Worker> findByUser(long userId) {
        return queryMany(
                "SELECT w.* FROM workers w JOIN user_workers uw ON uw.worker_id=w.id WHERE uw.user_id=? ORDER BY w.last_name,w.first_name",
                userId);
    }

    @Override
    public Worker save(Worker worker) {
        String sql =
                "INSERT INTO workers (first_name,last_name,document_id,email,phone,gross_salary,active) VALUES (?,?,?,?,?,?,?)";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, worker, false);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No generated worker id");
                }
                return new Worker(
                        keys.getLong(1),
                        worker.firstName(),
                        worker.lastName(),
                        worker.documentId(),
                        worker.email(),
                        worker.phone(),
                        worker.grossSalary(),
                        worker.active());
            }
        } catch (SQLException exception) {
            throw failure("save worker", exception);
        }
    }

    @Override
    public Worker update(Worker worker) {
        if (worker.id() == null) {
            throw new IllegalArgumentException("Worker id is required");
        }
        String sql =
                "UPDATE workers SET first_name=?,last_name=?,document_id=?,email=?,phone=?,gross_salary=?,active=? WHERE id=?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, worker, true);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Worker not found");
            }
            return worker;
        } catch (SQLException exception) {
            throw failure("update worker", exception);
        }
    }

    @Override
    public void delete(long id) {
        mutate("DELETE FROM workers WHERE id=?", id, null, "delete worker");
    }

    @Override
    public void assignToUser(long userId, long workerId) {
        mutate("INSERT IGNORE INTO user_workers (user_id,worker_id) VALUES (?,?)", userId, workerId, "assign worker");
    }

    @Override
    public void unassignFromUser(long userId, long workerId) {
        mutate("DELETE FROM user_workers WHERE user_id=? AND worker_id=?", userId, workerId, "unassign worker");
    }

    private Optional<Worker> queryOne(String sql, long id) {
        List<Worker> rows = queryMany(sql, id);
        return rows.stream().findFirst();
    }

    private List<Worker> queryMany(String sql, Long argument) {
        List<Worker> workers = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (argument != null) {
                statement.setLong(1, argument);
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    workers.add(map(result));
                }
            }
            return workers;
        } catch (SQLException exception) {
            throw failure("query workers", exception);
        }
    }

    private void mutate(String sql, long first, Long second, String operation) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, first);
            if (second != null) {
                statement.setLong(2, second);
            }
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure(operation, exception);
        }
    }

    private static void bind(PreparedStatement statement, Worker worker, boolean includeId) throws SQLException {
        statement.setString(1, worker.firstName());
        statement.setString(2, worker.lastName());
        statement.setString(3, worker.documentId());
        statement.setString(4, worker.email());
        statement.setString(5, worker.phone());
        statement.setBigDecimal(6, worker.grossSalary());
        statement.setBoolean(7, worker.active());
        if (includeId) {
            statement.setLong(8, worker.id());
        }
    }

    private static Worker map(ResultSet result) throws SQLException {
        return new Worker(
                result.getLong("id"),
                result.getString("first_name"),
                result.getString("last_name"),
                result.getString("document_id"),
                result.getString("email"),
                result.getString("phone"),
                result.getBigDecimal("gross_salary"),
                result.getBoolean("active"));
    }

    private static DataAccessException failure(String operation, SQLException exception) {
        return new DataAccessException("Could not " + operation, exception);
    }
}
