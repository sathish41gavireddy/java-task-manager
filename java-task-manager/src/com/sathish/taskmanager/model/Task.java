package com.sathish.taskmanager.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A single task. Fields are private (encapsulation) and exposed through getters. */
public class Task {
    private final long id;
    private String title;
    private String description;
    private Status status;
    private Priority priority;
    private LocalDate dueDate;           // can be null
    private final LocalDateTime createdAt;

    public Task(long id, String title, String description, Status status,
                Priority priority, LocalDate dueDate, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Status getStatus() { return status; }
    public Priority getPriority() { return priority; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void update(String title, String description, Status status, Priority priority, LocalDate dueDate) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
    }

    /** A task is overdue if it has a due date in the past and is not finished. */
    public boolean isOverdue(LocalDate today) {
        return dueDate != null && dueDate.isBefore(today) && status != Status.DONE;
    }
}
