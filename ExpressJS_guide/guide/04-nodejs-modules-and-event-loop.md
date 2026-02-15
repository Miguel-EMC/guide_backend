# 04 - Node.js Modules and the Event Loop

Understanding Node's module system and event loop is essential for building scalable APIs.

## 1. CommonJS vs ESM

- CommonJS uses `require()` and `module.exports`.
- ESM uses `import` and `export`.

Modern TypeScript setups prefer ESM with `"type": "module"`.

## 2. The Event Loop (High Level)

Node uses a single-threaded event loop to manage many concurrent requests.

Key phases:

1. Timers
2. Pending callbacks
3. Idle/prepare
4. Poll (I/O)
5. Check (`setImmediate`)
6. Close callbacks

Microtasks (Promises, `queueMicrotask`) run between phases.

## 3. Blocking vs Non-Blocking

Blocking work freezes the event loop and slows all requests.

```js
// Bad: synchronous CPU-heavy work in a request
app.get('/heavy', (_req, res) => {
  const start = Date.now();
  while (Date.now() - start < 2000) {}
  res.json({ ok: true });
});
```

Prefer async I/O and offload CPU tasks to worker threads or queues.

## 4. When to Use Worker Threads

Use worker threads for:

- Image processing
- PDF generation
- ML inference

Example worker:

```js
// worker.js
import { parentPort } from 'worker_threads';

parentPort.on('message', (payload) => {
  const result = payload.n * 2;
  parentPort.postMessage(result);
});
```

```js
// main.js
import { Worker } from 'worker_threads';

const worker = new Worker(new URL('./worker.js', import.meta.url));
worker.postMessage({ n: 21 });
worker.on('message', (result) => console.log(result));
```

## 5. Streams and Backpressure

Streams help you process large payloads without loading everything in memory.

```js
import fs from 'fs';

const read = fs.createReadStream('./big-file.csv');
read.on('data', (chunk) => {
  // process chunk
});
```

Use backpressure to avoid memory spikes when streaming to slow clients.

## 6. Practical Advice

- Prefer async APIs (`fs/promises`).
- Keep request handlers fast.
- Move heavy work to queues.

---

[Previous: JavaScript ES6+ Refresh](./03-javascript-es6-refresh.md) | [Back to Index](./README.md) | [Next: Hello World ->](./05-hello-world-with-express.md)
