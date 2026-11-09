package com.sathish.taskmanager.repository;

import com.sathish.taskmanager.model.Task;

import java.util.List;
import java.util.Optional;

/** Storage contract. The service only depends on this interface (abstraction). */
public interface TaskRepository {
    Task save(Task task);

    Optional<Task> findById(long id);

    List<Task> findAll();

    boolean deleteById(long id);

    /** Returns the next free id. */
    long nextId();
}
