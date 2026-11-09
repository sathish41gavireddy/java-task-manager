package com.sathish.taskmanager.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Serves a small web page at http://localhost:8080 to try the API from the browser. */
public class HomeHandler implements HttpHandler {
    private static final String PAGE = """
        <!DOCTYPE html>
        <html lang="en"><head><meta charset="utf-8"><title>Task Manager API</title>
        <style>
          body{font-family:Arial,sans-serif;max-width:720px;margin:30px auto;padding:0 12px;color:#222}
          h1{color:#1f3864} input,select,button{padding:8px;margin:3px;font-size:14px}
          button{background:#1f3864;color:#fff;border:0;border-radius:5px;cursor:pointer}
          table{border-collapse:collapse;width:100%;margin-top:12px}
          td,th{border-bottom:1px solid #ddd;padding:6px;text-align:left;font-size:14px}
          .over{color:#b00020;font-weight:bold} #msg{color:#b00020}
        </style></head><body>
        <h1>Task Manager (Core Java REST API)</h1>
        <div>
          <input id="title" placeholder="Task title" size="28">
          <select id="priority"><option>LOW</option><option selected>MEDIUM</option><option>HIGH</option></select>
          <input id="due" type="date">
          <button onclick="add()">Add task</button>
        </div>
        <div>
          Filter: <select id="fs" onchange="load()"><option value="">All status</option>
          <option>TODO</option><option>IN_PROGRESS</option><option>DONE</option></select>
          <input id="q" placeholder="Search..." oninput="load()">
        </div>
        <p id="msg"></p>
        <p id="stats"></p>
        <table><thead><tr><th>ID</th><th>Title</th><th>Priority</th><th>Status</th><th>Due</th><th></th></tr></thead>
        <tbody id="rows"></tbody></table>
        <script>
        const api = '/api/tasks';
        async function load() {
          const s = document.getElementById('fs').value, q = document.getElementById('q').value;
          const r = await fetch(api + '?status=' + encodeURIComponent(s) + '&q=' + encodeURIComponent(q));
          const list = await r.json();
          document.getElementById('rows').innerHTML = list.map(t =>
            `<tr><td>${t.id}</td><td>${esc(t.title)}</td><td>${t.priority}</td>
             <td><select onchange="setStatus(${t.id}, this.value)">
               ${['TODO','IN_PROGRESS','DONE'].map(x => `<option ${x===t.status?'selected':''}>${x}</option>`).join('')}
             </select></td>
             <td class="${t.overdue?'over':''}">${t.dueDate||''}${t.overdue?' (overdue)':''}</td>
             <td><button onclick="del(${t.id})">Delete</button></td></tr>`).join('');
          const st = await (await fetch('/api/stats')).json();
          document.getElementById('stats').textContent =
            `Total: ${st.total} | To do: ${st.byStatus.TODO} | In progress: ${st.byStatus.IN_PROGRESS} | Done: ${st.byStatus.DONE} | Overdue: ${st.overdue}`;
        }
        function esc(s){return s.replace(/[&<>]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;'}[c]));}
        async function add() {
          const body = {title: document.getElementById('title').value,
                        priority: document.getElementById('priority').value,
                        dueDate: document.getElementById('due').value};
          const r = await fetch(api, {method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(body)});
          const data = await r.json();
          document.getElementById('msg').textContent = r.ok ? '' : data.error;
          if (r.ok) document.getElementById('title').value = '';
          load();
        }
        async function setStatus(id, status) {
          const cur = await (await fetch(api + '/' + id)).json();
          await fetch(api + '/' + id, {method:'PUT', headers:{'Content-Type':'application/json'},
            body: JSON.stringify({title: cur.title, description: cur.description, priority: cur.priority,
                                  dueDate: cur.dueDate, status})});
          load();
        }
        async function del(id) { await fetch(api + '/' + id, {method:'DELETE'}); load(); }
        load();
        </script></body></html>
        """;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        byte[] bytes = PAGE.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
