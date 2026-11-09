# Task Manager REST API (Core Java)

![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Dependencies](https://img.shields.io/badge/dependencies-none-brightgreen)
![Tests](https://img.shields.io/badge/tests-13%20passing-brightgreen)

A task manager REST API written in **core Java with no frameworks and no external libraries**. It uses the JDK's built-in
HTTP server, a layered design, file storage and its own automated tests, and it serves a small web page so you can use
it straight from the browser.

I built this project to practise core Java: object-oriented design, collections, streams, exceptions, file I/O and
building a REST API from scratch.

## Screenshots

<!-- Add your own screenshots: create a docs folder, save the images, and keep these lines. Delete this section if you have no images. -->
![Task Manager web page](docs/screenshot-home.png)

## Features
- Create, read, update and delete tasks (CRUD): title, description, status, priority and due date
- Filter by status and priority, search in title and description, sorted by priority and then due date
- Statistics: number of tasks by status and priority, and how many are overdue
- Input validation with clear JSON error messages (HTTP 400 for bad input, 404 for a missing task)
- Data is saved to a file (`data/tasks.jsonl`), so tasks are still there after a restart
- A small web page to try everything in the browser
- 13 automated tests

## Tech stack
- Java 17+ (tested with Java 21)
- `com.sun.net.httpserver.HttpServer` (built into the JDK)
- Plain `javac` and `java`, no Maven or Gradle required

## Java concepts used
| Area | Where it is used |
|---|---|
| OOP: encapsulation, interfaces, abstraction | `Task` class, `TaskRepository` interface |
| Enums | `Status`, `Priority` |
| Collections | `Map`, `List`, `ConcurrentHashMap`, `EnumMap` |
| Streams and comparators | filtering, searching and sorting in `TaskService` |
| `Optional` | looking up a task by id |
| Custom exceptions | `NotFoundException`, `ValidationException` |
| Modern Java | pattern matching with `instanceof`, switch expressions, text blocks |
| Date and time | `java.time.LocalDate`, overdue logic |
| File I/O | `java.nio.file`, saving and loading tasks |
| Concurrency basics | `AtomicLong` ids, `synchronized` saves, thread pool |

## Project structure
```
java-task-manager/
├── src/com/sathish/taskmanager/
│   ├── Main.java                  # starts the server
│   ├── model/                     # Task, Status, Priority
│   ├── repository/                # TaskRepository (interface), FileTaskRepository
│   ├── service/                   # TaskService: validation, filtering, sorting, statistics
│   ├── api/                       # TaskHandler (REST routes), HomeHandler (web page)
│   ├── exception/                 # NotFoundException, ValidationException
│   └── util/                      # Json: a small JSON reader and writer
├── test/com/sathish/taskmanager/TestRunner.java
├── run.bat, run.sh                # compile and start the app
├── test.bat, test.sh              # compile and run the tests
└── .vscode/settings.json
```

## How it is designed
The app has three layers. A request goes down, and the response comes back up:

`HTTP handler (api)  ->  TaskService (rules)  ->  TaskRepository (storage)`

- The **handler** reads the request and writes the JSON response. It turns exceptions into 400 and 404 responses.
- The **service** holds the business rules: validation, filtering, sorting and statistics.
- The **repository** is an interface, so the file storage can later be replaced with a database without changing the service.

## Getting started

### Requirements
- **JDK 17 or newer.** Check with `java -version` and `javac -version`.

### Run in VS Code
1. Install the **Extension Pack for Java**.
2. Open the project folder (the one that contains `src`).
3. Open `src/com/sathish/taskmanager/Main.java` and click **Run**.
4. Open http://localhost:8080 in your browser.

### Run from the terminal
Windows:
```
run.bat
```
Mac or Linux:
```
./run.sh
```
To use another port, set the `PORT` variable first (for example `set PORT=8081` on Windows).

### Run the tests
Windows: `test.bat` | Mac or Linux: `./test.sh` | or run `TestRunner.java` from VS Code.

Expected result:
```
PASS  Json parses a flat object
...
13 passed, 0 failed
```

## API
| Method | Path | Description |
|---|---|---|
| GET | `/api/tasks?status=&priority=&q=` | List tasks (all filters are optional) |
| POST | `/api/tasks` | Create a task |
| GET | `/api/tasks/{id}` | Get one task |
| PUT | `/api/tasks/{id}` | Update a task |
| DELETE | `/api/tasks/{id}` | Delete a task |
| GET | `/api/stats` | Counts by status and priority, and overdue tasks |

**Fields:** `title` (required, up to 100 characters), `description`, `status` (`TODO`, `IN_PROGRESS`, `DONE`),
`priority` (`LOW`, `MEDIUM`, `HIGH`), `dueDate` (for example `2026-12-31`).

### Example requests
Create a task:
```
curl -X POST http://localhost:8080/api/tasks -H "Content-Type: application/json" -d "{\"title\":\"Finish resume\",\"priority\":\"HIGH\",\"dueDate\":\"2026-12-31\"}"
```
Response (HTTP 201):
```json
{"id":1,"title":"Finish resume","description":"","status":"TODO","priority":"HIGH","dueDate":"2026-12-31","createdAt":"2026-11-08T18:25","overdue":false}
```
List high-priority tasks:
```
curl "http://localhost:8080/api/tasks?priority=HIGH"
```
Missing title (HTTP 400):
```json
{"error":"title is required"}
```
On Mac or Linux, use single quotes around the JSON. You can also use Postman.

## What I learned
- Designing a layered application and depending on an interface instead of a class
- Using streams, comparators and `Optional` for filtering and sorting
- Handling errors with custom exceptions and mapping them to HTTP status codes
- Writing a small JSON reader and writer to understand how JSON works
- Testing without a framework, and why tests are worth writing

## Limitations and next steps
- [ ] Convert to **Spring Boot** with Spring Data JPA and a MySQL or PostgreSQL database
- [ ] Replace `TestRunner` with **JUnit 5** and add Maven or Gradle
- [ ] Add authentication, pagination and logging
- [ ] Add Docker and a GitHub Actions workflow
