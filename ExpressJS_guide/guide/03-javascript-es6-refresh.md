# 03 - JavaScript ES6+ Refresh

This chapter reviews modern JavaScript features used in Node.js backend codebases.

## 1. let, const, and scoping

```js
const port = 3000;
let counter = 0;
```

## 2. Arrow functions

```js
const add = (a, b) => a + b;
```

## 3. Destructuring

```js
const user = { id: 1, email: 'a@b.com' };
const { id, email } = user;
```

## 4. Spread and Rest

```js
const a = [1, 2];
const b = [3, 4];
const all = [...a, ...b];

const sum = (...nums) => nums.reduce((acc, n) => acc + n, 0);
```

## 5. Template literals

```js
const name = 'Miguel';
console.log(`Hello ${name}`);
```

## 6. Optional chaining and nullish coalescing

```js
const city = user?.address?.city ?? 'Unknown';
```

## 7. Async/Await

```js
async function getUser(id) {
  const res = await fetch(`https://api.example.com/users/${id}`);
  return res.json();
}
```

## 8. Modules (ESM)

```js
// math.js
export const add = (a, b) => a + b;

// app.js
import { add } from './math.js';
```

## 9. Promises and Promise.all

```js
const [a, b] = await Promise.all([taskA(), taskB()]);
```

## 10. Map, Set, and Record

```js
const seen = new Set();
const cache = new Map();
```

## 11. Default Parameters and Named Arguments

```js
function createUser(email, role = 'user') {
  return { email, role };
}
```

## 12. Array Helpers

```js
const users = [{ id: 1 }, { id: 2 }];
const ids = users.map((u) => u.id);
const active = users.filter((u) => u.id > 1);
```

## 13. Classes and Inheritance

```js
class BaseService {
  constructor(name) {
    this.name = name;
  }
}

class UserService extends BaseService {
  list() {
    return [];
  }
}
```

## 14. Error handling

```js
try {
  await doWork();
} catch (err) {
  console.error(err);
}
```

## Tips

- Prefer `const` for values that do not change.
- Avoid mixing `.then()` and `await` in the same function.
- Use `Promise.all` for parallel I/O calls.

---

[Previous: Setup](./02-setup-node-npm-and-typescript.md) | [Back to Index](./README.md) | [Next: Node Modules and Event Loop ->](./04-nodejs-modules-and-event-loop.md)
