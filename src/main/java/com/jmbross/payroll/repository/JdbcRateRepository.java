package com.jmbross.payroll.repository;

import com.jmbross.payroll.domain.Rate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

public final class JdbcRateRepository implements RateRepository {
    private final DataSource dataSource;

    public JdbcRateRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Rate> findAll() {
        return query("SELECT * FROM rates ORDER BY worker_id,description", null);
    }

    @Override
    public List<Rate> findByWorker(long workerId) {
        return query("SELECT * FROM rates WHERE worker_id=? AND enabled=TRUE ORDER BY description", workerId);
    }

    @Override
    public Rate save(Rate rate) {
        String sql = "INSERT INTO rates (worker_id,description,percentage,enabled) VALUES (?,?,?,?)";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, rate, false);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No generated rate id");
                }
                return new Rate(
                        keys.getLong(1), rate.workerId(), rate.description(), rate.percentage(), rate.enabled());
            }
        } catch (SQLException exception) {
            throw failure("save rate", exception);
        }
    }

    @Override
    public Rate update(Rate rate) {
        if (rate.id() == null) {
            throw new IllegalArgumentException("Rate id is required");
        }
        String sql = "UPDATE rates SET worker_id=?,description=?,percentage=?,enabled=? WHERE id=?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, rate, true);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Rate not found");
            }
            return rate;
        } catch (SQLException exception) {
            throw failure("update rate", exception);
        }
    }

    @Override
    public void delete(long id) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM rates WHERE id=?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure("delete rate", exception);
        }
    }

    private List<Rate> query(String sql, Long workerId) {
        List<Rate> rates = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (workerId != null) {
                statement.setLong(1, workerId);
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rates.add(new Rate(
                            result.getLong("id"),
                            result.getLong("worker_id"),
                            result.getString("description"),
                            result.getBigDecimal("percentage"),
                            result.getBoolean("enabled")));
                }
            }
            return rates;
        } catch (SQLException exception) {
            throw failure("query rates", exception);
        }
    }

    private static void bind(PreparedStatement statement, Rate rate, boolean includeId) throws SQLException {
        statement.setLong(1, rate.workerId());
        statement.setString(2, rate.description());
        statement.setBigDecimal(3, rate.percentage());
        statement.setBoolean(4, rate.enabled());
        if (includeId) {
            statement.setLong(5, rate.id());
        }
    }

    private static DataAccessException failure(String operation, SQLException exception) {
        return new DataAccessException("Could not " + operation, exception);
    }
}
