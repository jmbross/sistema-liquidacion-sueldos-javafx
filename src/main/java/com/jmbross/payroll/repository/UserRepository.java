package com.jmbross.payroll.repository;

import com.jmbross.payroll.domain.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByEmail(String email);

    Optional<User> findById(long id);

    List<User> findAll();

    User save(User user);

    User update(User user);

    void delete(long id);
}
