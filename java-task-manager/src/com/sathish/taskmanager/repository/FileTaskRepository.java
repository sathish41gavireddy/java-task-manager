package com.sathish.taskmanager.repository;

import com.sathish.taskmanager.model.Priority;
import com.sathish.taskmanager.model.Status;
import com.sathish.taskmanager.model.Task;
import com.sathish.taskmanager.util.Json;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps tasks in memory (ConcurrentHashMap) and saves them to a text file, one JSON object per line,
 * so the data is still there after the program restarts. Pass a null path for memory-only (used in tests).
 */
public class FileTaskRepository implements TaskRepository {
    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(0);
    private final Path file;

    public FileTaskRepository(Path file) {
        this.file = file;
        load();
    }

    @Override
    public synchronized Task save(Task task) {
        tasks.put(task.getId(), task);
        persist();
        return task;
    }

    @Override
    public Optional<Task> findById(long id) {
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public List<Task> findAll() {
        List<Task> list = new ArrayList<>(tasks.values());
        list.sort(Comparator.comparingLong(Task::getId));
        return list;
    }

    @Override
    public synchronized boolean deleteById(long id) {
        boolean removed = tasks.remove(id) != null;
        if (removed) persist();
        return removed;
    }

    @Override
    public long nextId() {
        return idCounter.incrementAndGet();
    }

    // ---------- file handling ----------
    private void persist() {
        if (file == null) return;
        List<String> lines = new ArrayList<>();
        for (Task t : findAll()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("description", t.getDescription());
            m.put("status", t.getStatus());
            m.put("priority", t.getPriority());
            m.put("dueDate", t.getDueDate());
            m.put("createdAt", t.getCreatedAt());
            lines.add(Json.toJson(m));
        }
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save tasks to " + file, e);
        }
    }

    private void load() {
        if (file == null || !Files.exists(file)) return;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank()) continue;
                Map<String, String> m = Json.parseObject(line);
                long id = Long.parseLong(m.get("id"));
                String due = m.get("dueDate");
                Task t = new Task(id, m.get("title"), m.get("description"),
                        Status.valueOf(m.get("status")), Priority.valueOf(m.get("priority")),
                        due == null ? null : LocalDate.parse(due),
                        LocalDateTime.parse(m.get("createdAt")));
                tasks.put(id, t);
                idCounter.accumulateAndGet(id, Math::max);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read tasks from " + file, e);
        }
    }
}
