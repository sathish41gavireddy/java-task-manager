package com.sathish.taskmanager;

import com.sathish.taskmanager.exception.NotFoundException;
import com.sathish.taskmanager.exception.ValidationException;
import com.sathish.taskmanager.model.Status;
import com.sathish.taskmanager.model.Task;
import com.sathish.taskmanager.repository.FileTaskRepository;
import com.sathish.taskmanager.service.TaskService;
import com.sathish.taskmanager.util.Json;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * A simple test runner with no libraries needed. Run it from VS Code (Run button) or the terminal.
 * Each test prints PASS or FAIL, and the program exits with code 1 if anything fails.
 */
public class TestRunner {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        run("Json parses a flat object", TestRunner::jsonParse);
        run("Json writes escaped text", TestRunner::jsonWrite);
        run("Json rejects bad input", TestRunner::jsonRejects);
        run("create uses defaults", TestRunner::createDefaults);
        run("create requires a title", TestRunner::createRequiresTitle);
        run("create rejects bad status, priority and date", TestRunner::createRejectsBadValues);
        run("get unknown id throws NotFoundException", TestRunner::getUnknown);
        run("update replaces fields", TestRunner::updateFields);
        run("delete removes a task", TestRunner::deleteTask);
        run("list filters by status, priority and search", TestRunner::listFilters);
        run("list sorts by priority then due date", TestRunner::listSorts);
        run("stats count tasks and overdue", TestRunner::statsCounts);
        run("tasks are saved to a file and reloaded", TestRunner::persistence);

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    // ---------- tests ----------
    static void jsonParse() {
        Map<String, String> m = Json.parseObject("{\"title\": \"Buy milk\", \"priority\":\"HIGH\", \"n\": 5, \"x\": null}");
        check("Buy milk".equals(m.get("title")), "title");
        check("HIGH".equals(m.get("priority")), "priority");
        check("5".equals(m.get("n")), "number");
        check(m.containsKey("x") && m.get("x") == null, "null value");
    }

    static void jsonWrite() {
        String out = Json.toJson(Map.of("a", "line1\n\"quoted\""));
        check(out.equals("{\"a\":\"line1\\n\\\"quoted\\\"\"}"), out);
    }

    static void jsonRejects() {
        expectValidation(() -> Json.parseObject("not json"));
        expectValidation(() -> Json.parseObject("{\"a\": \"unclosed}"));
        expectValidation(() -> Json.parseObject(null));
    }

    static void createDefaults() {
        TaskService s = newService();
        Task t = s.create(Map.of("title", "  Learn Java  "));
        check(t.getId() == 1, "id");
        check(t.getTitle().equals("Learn Java"), "title is trimmed");
        check(t.getStatus() == Status.TODO, "default status");
        check(t.getPriority().name().equals("MEDIUM"), "default priority");
        check(t.getDueDate() == null, "no due date");
    }

    static void createRequiresTitle() {
        TaskService s = newService();
        expectValidation(() -> s.create(Map.of()));
        expectValidation(() -> s.create(Map.of("title", "   ")));
        expectValidation(() -> s.create(Map.of("title", "x".repeat(101))));
    }

    static void createRejectsBadValues() {
        TaskService s = newService();
        expectValidation(() -> s.create(Map.of("title", "a", "status", "WRONG")));
        expectValidation(() -> s.create(Map.of("title", "a", "priority", "URGENT")));
        expectValidation(() -> s.create(Map.of("title", "a", "dueDate", "31-12-2026")));
    }

    static void getUnknown() {
        TaskService s = newService();
        try {
            s.get(99);
            check(false, "expected NotFoundException");
        } catch (NotFoundException expected) {
            check(true, "");
        }
    }

    static void updateFields() {
        TaskService s = newService();
        Task t = s.create(Map.of("title", "Old"));
        s.update(t.getId(), Map.of("title", "New", "status", "done", "priority", "high", "dueDate", "2030-01-01"));
        Task again = s.get(t.getId());
        check(again.getTitle().equals("New"), "title");
        check(again.getStatus() == Status.DONE, "status");
        check(again.getDueDate().equals(LocalDate.of(2030, 1, 1)), "date");
    }

    static void deleteTask() {
        TaskService s = newService();
        Task t = s.create(Map.of("title", "Temp"));
        s.delete(t.getId());
        try {
            s.delete(t.getId());
            check(false, "second delete should fail");
        } catch (NotFoundException expected) {
            check(s.list(null, null, null).isEmpty(), "list is empty");
        }
    }

    static void listFilters() {
        TaskService s = newService();
        s.create(Map.of("title", "Write report", "priority", "HIGH"));
        s.create(Map.of("title", "Buy milk", "priority", "LOW", "status", "DONE"));
        s.create(Map.of("title", "Call bank", "description", "about the report", "priority", "LOW"));
        check(s.list("DONE", null, null).size() == 1, "status filter");
        check(s.list(null, "LOW", null).size() == 2, "priority filter");
        check(s.list(null, null, "REPORT").size() == 2, "search in title and description");
        check(s.list("TODO", "HIGH", "write").size() == 1, "combined filter");
    }

    static void listSorts() {
        TaskService s = newService();
        s.create(Map.of("title", "low", "priority", "LOW"));
        s.create(Map.of("title", "high later", "priority", "HIGH", "dueDate", "2030-02-01"));
        s.create(Map.of("title", "high sooner", "priority", "HIGH", "dueDate", "2030-01-01"));
        List<Task> list = s.list(null, null, null);
        check(list.get(0).getTitle().equals("high sooner"), "first is " + list.get(0).getTitle());
        check(list.get(1).getTitle().equals("high later"), "second is " + list.get(1).getTitle());
        check(list.get(2).getTitle().equals("low"), "last is low");
    }

    static void statsCounts() {
        TaskService s = newService();
        s.create(Map.of("title", "late", "dueDate", "2020-01-01"));
        s.create(Map.of("title", "late but done", "dueDate", "2020-01-01", "status", "DONE"));
        s.create(Map.of("title", "future", "dueDate", "2099-01-01"));
        Map<String, Object> st = s.stats(LocalDate.of(2026, 11, 1));
        check(st.get("total").equals(3), "total");
        check(st.get("overdue").equals(1L), "overdue is " + st.get("overdue"));
    }

    static void persistence() throws Exception {
        Path temp = Files.createTempFile("tasks", ".jsonl");
        try {
            TaskService first = new TaskService(new FileTaskRepository(temp));
            first.create(Map.of("title", "Saved task", "description", "has \"quotes\"", "priority", "HIGH", "dueDate", "2030-05-05"));
            first.create(Map.of("title", "Second"));

            TaskService second = new TaskService(new FileTaskRepository(temp));   // simulates a restart
            check(second.list(null, null, null).size() == 2, "two tasks reloaded");
            Task reloaded = second.get(1);
            check(reloaded.getDescription().equals("has \"quotes\""), "quotes survive");
            check(second.create(Map.of("title", "Third")).getId() == 3, "ids continue after restart");
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    // ---------- tiny test helpers ----------
    private static TaskService newService() {
        return new TaskService(new FileTaskRepository(null));   // memory only
    }

    private static void expectValidation(Runnable action) {
        try {
            action.run();
            check(false, "expected ValidationException");
        } catch (ValidationException expected) {
            check(true, "");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void run(String name, ThrowingRunnable test) {
        try {
            test.run();
            passed++;
            System.out.println("PASS  " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("FAIL  " + name + "  ->  " + t.getMessage());
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
