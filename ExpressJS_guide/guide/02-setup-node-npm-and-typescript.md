# 02 - Setup: Node.js, npm, and TypeScript

This chapter sets up a modern TypeScript + Express 5 project with a clean build and dev workflow.

## 1. Install Node.js

Use an LTS version for production (Node 24.x as of 2026). Tools like `fnm` or `nvm` make switching versions easy.

```bash
node --version
npm --version
```

## 2. Initialize a Project

```bash
mkdir express-api
cd express-api
npm init -y
```

## 3. Install Runtime Dependencies

```bash
npm install express cors helmet dotenv
```

## 4. Install Dev Dependencies

```bash
npm install -D typescript tsx @types/node @types/express eslint prettier
```

## 5. Create tsconfig.json

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "NodeNext",
    "moduleResolution": "NodeNext",
    "outDir": "dist",
    "rootDir": "src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "sourceMap": true
  },
  "include": ["src"]
}
```

## 6. Create tsconfig.build.json

```json
{
  "extends": "./tsconfig.json",
  "compilerOptions": {
    "sourceMap": false,
    "declaration": true
  },
  "exclude": ["**/*.spec.ts", "**/*.test.ts"]
}
```

## 7. Update package.json Scripts

```json
{
  "type": "module",
  "scripts": {
    "dev": "tsx watch src/server.ts",
    "build": "tsc -p tsconfig.build.json",
    "start": "node dist/server.js",
    "lint": "eslint .",
    "format": "prettier --write ."
  }
}
```

## 8. Create Entry Files

```typescript
// src/app.ts
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';

export const app = express();

app.disable('x-powered-by');
app.use(helmet());
app.use(cors({ origin: '*' }));
app.use(express.json());

app.get('/health', (_req, res) => {
  res.json({ status: 'ok', ts: new Date().toISOString() });
});
```

```typescript
// src/server.ts
import { app } from './app.js';
import 'dotenv/config';

const port = Number(process.env.PORT ?? 3000);

app.listen(port, () => {
  console.log(`Server running on port ${port}`);
});
```

## 9. Environment Variables

Create a `.env` file for local development.

```env
PORT=3000
NODE_ENV=development
```

## 10. Run the App

```bash
npm run dev
```

## 11. Linting and Formatting (Recommended)

Create `.eslintrc.cjs`:

```javascript
module.exports = {
  env: { node: true, es2022: true },
  parserOptions: { ecmaVersion: 2022, sourceType: 'module' },
  extends: ['eslint:recommended'],
};
```

Create `.prettierrc`:

```json
{
  "singleQuote": true,
  "semi": true
}
```

## 12. Environment Validation (Optional but Recommended)

Validate environment variables on startup.

```typescript
// src/config/validate-env.ts
import { z } from 'zod';

const schema = z.object({
  PORT: z.coerce.number().default(3000),
  NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
});

export const env = schema.parse(process.env);
```

Use in `server.ts`:

```typescript
import { env } from './config/validate-env.js';
const port = env.PORT;
```

## Tips

- Keep `app.ts` free of `listen` so you can test it.
- Use `tsx` for fast dev reloads.
- Add linting in CI early.

---

[Previous: Introduction](./01-introduction-to-nodejs-and-express.md) | [Back to Index](./README.md) | [Next: JavaScript ES6+ Refresh ->](./03-javascript-es6-refresh.md)
