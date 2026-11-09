package com.sathish.taskmanager.api;

import com.sathish.taskmanager.exception.NotFoundException;
import com.sathish.taskmanager.exception.ValidationException;
import com.sathish.taskmanager.model.Task;
import com.sathish.taskmanager.service.TaskService;
import com.sathish.taskmanager.util.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles every request under /api. Routes:
 *   GET    /api/tasks?status=&priority=&q=   list tasks
 *   POST   /api/tasks                        create a task
 *   GET    /api/tasks/{id}                   get one task
 *   PUT    /api/tasks/{id}                   update a task
 *   DELETE /api/tasks/{id}                   delete a task
 *   GET    /api/stats                        counts and overdue tasks
 */
public class TaskHandler implements HttpHandler {
    private final TaskService service;

    public TaskHandler(TaskService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (ValidationException e) {
            send(exchange, 400, error(e.getMessage()));
        } catch (NotFoundException e) {
            send(exchange, 404, error(e.getMessage()));
        } catch (NumberFormatException e) {
            send(exchange, 400, error("id must be a number"));
        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, error("Internal server error"));
        } finally {
            exchange.close();
        }
    }

    private void route(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        String[] parts = path.split("/");           // "", "api", "tasks", "5"

        if (parts.length == 3 && parts[2].equals("stats") && method.equals("GET")) {
            send(ex, 200, Json.toJson(service.stats(LocalDate.now())));
            return;
        }
        if (parts.length < 3 || !parts[2].equals("tasks")) {
            throw new NotFoundException("Unknown path: " + path);
        }

        if (parts.length == 3) {                    // /api/tasks
            if (method.equals("GET")) {
                Map<String, String> q = queryParams(ex.getRequestURI().getRawQuery());
                List<Map<String, Object>> out = new ArrayList<>();
                for (Task t : service.list(q.get("status"), q.get("priority"), q.get("q"))) out.add(toMap(t));
                send(ex, 200, Json.toJson(out));
            } else if (method.equals("POST")) {
                Task created = service.create(Json.parseObject(readBody(ex)));
                send(ex, 201, Json.toJson(toMap(created)));
            } else {
                send(ex, 405, error("Method not allowed"));
            }
        } else if (parts.length == 4) {             // /api/tasks/{id}
            long id = Long.parseLong(parts[3]);
            switch (method) {
                case "GET" -> send(ex, 200, Json.toJson(toMap(service.get(id))));
                case "PUT" -> send(ex, 200, Json.toJson(toMap(service.update(id, Json.parseObject(readBody(ex))))));
                case "DELETE" -> {
                    service.delete(id);
                    ex.sendResponseHeaders(204, -1);
                }
                default -> send(ex, 405, error("Method not allowed"));
            }
        } else {
            throw new NotFoundException("Unknown path: " + path);
        }
    }

    // ---------- helpers ----------
    static Map<String, Object> toMap(Task t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("title", t.getTitle());
        m.put("description", t.getDescription());
        m.put("status", t.getStatus());
        m.put("priority", t.getPriority());
        m.put("dueDate", t.getDueDate());
        m.put("createdAt", t.getCreatedAt());
        m.put("overdue", t.isOverdue(LocalDate.now()));
        return m;
    }

    private static String error(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", message);
        return Json.toJson(m);
    }

    private static String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static Map<String, String> queryParams(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) return map;
        for (String pair : rawQuery.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            if (!value.isBlank()) map.put(key, value);
        }
        return map;
    }

    static void send(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
