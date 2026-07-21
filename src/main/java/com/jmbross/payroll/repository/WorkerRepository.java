package com.jmbross.payroll.repository;

import com.jmbross.payroll.domain.Worker;
import java.util.List;
import java.util.Optional;

public interface WorkerRepository {
    Optional<Worker> findById(long id);

    List<Worker> findAll();

    List<Worker> findByUser(long userId);

    Worker save(Worker worker);

    Worker update(Worker worker);

    void delete(long id);

    void assignToUser(long userId, long workerId);

    void unassignFromUser(long userId, long workerId);
}
