package com.sathish.taskmanager;

import com.sathish.taskmanager.api.HomeHandler;
import com.sathish.taskmanager.api.TaskHandler;
import com.sathish.taskmanager.repository.FileTaskRepository;
import com.sathish.taskmanager.service.TaskService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/** Starts the Task Manager REST API on http://localhost:8080 (change the port with the PORT variable). */
public class Main {
    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        FileTaskRepository repository = new FileTaskRepository(Path.of("data", "tasks.jsonl"));
        TaskService service = new TaskService(repository);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", new TaskHandler(service));
        server.createContext("/", new HomeHandler());
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("Task Manager running at http://localhost:" + port);
        System.out.println("Press Ctrl+C to stop.");
    }
}
