package com.sathish.taskmanager.service;

import com.sathish.taskmanager.exception.NotFoundException;
import com.sathish.taskmanager.exception.ValidationException;
import com.sathish.taskmanager.model.Priority;
import com.sathish.taskmanager.model.Status;
import com.sathish.taskmanager.model.Task;
import com.sathish.taskmanager.repository.TaskRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Business rules: validation, searching, sorting and statistics. */
public class TaskService {
    private static final int MAX_TITLE = 100;
    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public Task create(Map<String, String> body) {
        Task task = new Task(repository.nextId(),
                requireTitle(body.get("title")),
                body.getOrDefault("description", ""),
                parseStatus(body.get("status"), Status.TODO),
                parsePriority(body.get("priority"), Priority.MEDIUM),
                parseDate(body.get("dueDate")),
                LocalDateTime.now().withNano(0));
        return repository.save(task);
    }

    public Task get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task " + id + " not found"));
    }

    /** Full update: every field is replaced (title is required). */
    public Task update(long id, Map<String, String> body) {
        Task task = get(id);
        task.update(requireTitle(body.get("title")),
                body.getOrDefault("description", ""),
                parseStatus(body.get("status"), Status.TODO),
                parsePriority(body.get("priority"), Priority.MEDIUM),
                parseDate(body.get("dueDate")));
        return repository.save(task);
    }

    public void delete(long id) {
        if (!repository.deleteById(id)) {
            throw new NotFoundException("Task " + id + " not found");
        }
    }

    /** Lists tasks, optionally filtered by status, priority and a search word, sorted by priority then due date. */
    public List<Task> list(String status, String priority, String query) {
        Status wantedStatus = status == null ? null : parseStatus(status, null);
        Priority wantedPriority = priority == null ? null : parsePriority(priority, null);
        String q = query == null ? null : query.toLowerCase();

        return repository.findAll().stream()
                .filter(t -> wantedStatus == null || t.getStatus() == wantedStatus)
                .filter(t -> wantedPriority == null || t.getPriority() == wantedPriority)
                .filter(t -> q == null || t.getTitle().toLowerCase().contains(q)
                        || t.getDescription().toLowerCase().contains(q))
                .sorted(Comparator.comparing(Task::getPriority).reversed()
                        .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /** Counts of tasks by status and priority, plus how many are overdue. */
    public Map<String, Object> stats(LocalDate today) {
        List<Task> all = repository.findAll();
        Map<Status, Long> byStatus = new EnumMap<>(Status.class);
        for (Status s : Status.values()) byStatus.put(s, 0L);
        all.forEach(t -> byStatus.merge(t.getStatus(), 1L, Long::sum));

        Map<Priority, Long> byPriority = new EnumMap<>(Priority.class);
        for (Priority p : Priority.values()) byPriority.put(p, 0L);
        all.forEach(t -> byPriority.merge(t.getPriority(), 1L, Long::sum));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", all.size());
        result.put("byStatus", byStatus);
        result.put("byPriority", byPriority);
        result.put("overdue", all.stream().filter(t -> t.isOverdue(today)).count());
        return result;
    }

    // ---------- validation helpers ----------
    private String requireTitle(String title) {
        if (title == null || title.isBlank()) throw new ValidationException("title is required");
        if (title.length() > MAX_TITLE) throw new ValidationException("title must be at most " + MAX_TITLE + " characters");
        return title.trim();
    }

    private Status parseStatus(String value, Status defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Status.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("status must be TODO, IN_PROGRESS or DONE");
        }
    }

    private Priority parsePriority(String value, Priority defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Priority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("priority must be LOW, MEDIUM or HIGH");
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("dueDate must look like 2026-12-31");
        }
    }
}
