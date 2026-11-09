# Task Manager REST API (Core Java)

A task manager REST API written in **core Java 17+ with no external libraries**. It uses the JDK's built-in
HTTP server, a layered design, file persistence and its own tests. It also serves a small web page so you
can use it from the browser.

## Features
- Create, read, update and delete tasks (CRUD) with title, description, status, priority and due date
- Filter by status and priority, search by text, and sort by priority then due date
- Statistics: tasks by status and priority, and how many are overdue
- Input validation with clear error messages (HTTP 400 and 404)
- Tasks are saved to `data/tasks.jsonl`, so they are still there after a restart
- 13 automated tests

## Java concepts used
OOP (encapsulation, interfaces and abstraction), enums, pattern matching with `instanceof`, switch expressions, text blocks,
collections (`Map`, `List`, `ConcurrentHashMap`), streams and comparators, `Optional`, custom exceptions,
`java.time`, file I/O (`java.nio.file`), concurrency basics (`AtomicLong`, thread pool), and the
`com.sun.net.httpserver` HTTP server.

## Project structure
```
java-task-manager/
├── src/com/sathish/taskmanager/
│   ├── Main.java                      # starts the server
│   ├── model/        Task, Status, Priority
│   ├── repository/   TaskRepository (interface), FileTaskRepository
│   ├── service/      TaskService      # validation, filtering, sorting, stats
│   ├── api/          TaskHandler (REST routes), HomeHandler (web page)
│   ├── exception/    NotFoundException, ValidationException
│   └── util/         Json             # small JSON reader/writer
├── test/com/sathish/taskmanager/TestRunner.java
├── run.bat / run.sh, test.bat / test.sh
└── .vscode/settings.json
```

## Run in VS Code
1. Install **JDK 17 or newer** (for example Temurin from adoptium.net). Check with `java -version` and `javac -version`.
2. Install the VS Code extension **Extension Pack for Java**.
3. Open this folder in VS Code (File > Open Folder).
4. Open `src/com/sathish/taskmanager/Main.java` and click **Run** (above `public static void main`).
5. Open http://localhost:8080 in your browser. Stop with the red square or Ctrl+C.

To run the tests, open `test/com/sathish/taskmanager/TestRunner.java` and click **Run**. You should see 13 PASS lines.

## Run from the terminal
Windows: double-click or run `run.bat` (tests: `test.bat`). Mac/Linux: `./run.sh` (tests: `./test.sh`).

## API
| Method | Path | What it does |
|---|---|---|
| GET | `/api/tasks?status=&priority=&q=` | List tasks (all filters optional) |
| POST | `/api/tasks` | Create a task |
| GET | `/api/tasks/{id}` | Get one task |
| PUT | `/api/tasks/{id}` | Update a task |
| DELETE | `/api/tasks/{id}` | Delete a task |
| GET | `/api/stats` | Counts by status and priority, and overdue tasks |

Example with curl:
```
curl -X POST http://localhost:8080/api/tasks -H "Content-Type: application/json" ^
     -d "{\"title\":\"Finish resume\",\"priority\":\"HIGH\",\"dueDate\":\"2026-12-31\"}"
curl http://localhost:8080/api/tasks?priority=HIGH
```
(On Mac/Linux use `\` instead of `^` and single quotes around the JSON.) You can also use Postman.

Fields: `title` (required, up to 100 characters), `description`, `status` (TODO, IN_PROGRESS, DONE),
`priority` (LOW, MEDIUM, HIGH), `dueDate` (like 2026-12-31).

## Design notes
- `TaskService` depends on the `TaskRepository` interface, so the file storage can be replaced by a database.
- Errors are custom exceptions that the handler turns into HTTP 400 or 404 JSON responses.
- The JSON helper handles only flat objects, which is enough for this API.

## Limitations and next steps
- Convert to **Spring Boot** with Spring Data JPA and a MySQL or PostgreSQL database
- Replace `TestRunner` with **JUnit 5** and add Maven or Gradle
- Add authentication, pagination and logging
- Add Docker and a GitHub Actions workflow
