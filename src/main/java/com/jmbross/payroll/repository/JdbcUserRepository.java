package com.jmbross.payroll.repository;

import com.jmbross.payroll.domain.Role;
import com.jmbross.payroll.domain.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

public final class JdbcUserRepository implements UserRepository {
    private final DataSource dataSource;

    public JdbcUserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw failure("find user by email", exception);
        }
    }

    @Override
    public Optional<User> findById(long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw failure("find user", exception);
        }
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY last_name, first_name";
        List<User> users = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                users.add(map(result));
            }
            return users;
        } catch (SQLException exception) {
            throw failure("list users", exception);
        }
    }

    @Override
    public User save(User user) {
        String sql =
                "INSERT INTO users (first_name,last_name,document_id,registration_id,email,password_hash,role,active) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, user, false);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No generated user id");
                }
                return new User(
                        keys.getLong(1),
                        user.firstName(),
                        user.lastName(),
                        user.documentId(),
                        user.registrationId(),
                        user.email(),
                        user.passwordHash(),
                        user.role(),
                        user.active());
            }
        } catch (SQLException exception) {
            throw failure("save user", exception);
        }
    }

    @Override
    public User update(User user) {
        if (user.id() == null) {
            throw new IllegalArgumentException("User id is required");
        }
        String sql =
                "UPDATE users SET first_name=?,last_name=?,document_id=?,registration_id=?,email=?,password_hash=?,role=?,active=? WHERE id=?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, user, true);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("User not found");
            }
            return user;
        } catch (SQLException exception) {
            throw failure("update user", exception);
        }
    }

    @Override
    public void delete(long id) {
        executeDelete("DELETE FROM users WHERE id=?", id, "delete user");
    }

    private void executeDelete(String sql, long id, String operation) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure(operation, exception);
        }
    }

    private static void bind(PreparedStatement statement, User user, boolean includeId) throws SQLException {
        statement.setString(1, user.firstName());
        statement.setString(2, user.lastName());
        statement.setString(3, user.documentId());
        statement.setString(4, user.registrationId());
        statement.setString(5, user.email());
        statement.setString(6, user.passwordHash());
        statement.setString(7, user.role().name());
        statement.setBoolean(8, user.active());
        if (includeId) {
            statement.setLong(9, user.id());
        }
    }

    private static User map(ResultSet result) throws SQLException {
        return new User(
                result.getLong("id"),
                result.getString("first_name"),
                result.getString("last_name"),
                result.getString("document_id"),
                result.getString("registration_id"),
                result.getString("email"),
                result.getString("password_hash"),
                Role.valueOf(result.getString("role")),
                result.getBoolean("active"));
    }

    private static DataAccessException failure(String operation, SQLException exception) {
        return new DataAccessException("Could not " + operation, exception);
    }
}
