# Coffee Tracker (client)

> Part of the [Coffee Tracker monorepo](../README.md). The Spring Boot API lives in [`server/`](../server).

A React single-page app for the Coffee Tracker API: sign in, manage beans and equipment, log
brews with live ratio and grind-size feedback, and browse stats.

**React 19 · TypeScript · Vite · TanStack Query · React Router · Tailwind CSS · Vitest + Testing Library + MSW**

## Running locally

Start the API first (see [server/README.md](../server/README.md)), then:

```bash
cd client
npm install
npm run dev        # http://localhost:5173
```

Vite proxies `/api` to `localhost:8080`, so the browser only ever talks to one origin.

| Script | What it does |
|---|---|
| `npm run dev` | Dev server with hot reload |
| `npm test` | Vitest + Testing Library + MSW (no backend needed) |
| `npm run lint` | oxlint |
| `npm run build` | Type-check (`tsc -b`) and build to `dist/` |
| `npm run preview` | Serve the production build with the production CSP |
| `npm run gen:api` | Regenerate `src/api/schema.d.ts` from the running API's OpenAPI spec |

## How it fits together

```
src/
├── api/          openapi-fetch client (typed by the generated schema.d.ts) + ApiError
├── auth/         token store (memory + sessionStorage), useAuth, route guard
├── hooks/        TanStack Query hooks per resource: queries, mutations, cache invalidation
├── components/   layout + small accessible UI primitives (Field, Button, Alert…)
├── pages/        one component per route
├── lib/          brewing helpers: grind ranges, ratio, labels, formatting
├── routes.tsx    route table (shared by the app and the tests)
└── test/         MSW server, render helpers, setup
```

- **End-to-end types.** `npm run gen:api` turns the server's OpenAPI spec into TypeScript.
  `api.GET('/api/v1/brews/{id}')` is checked against the real endpoint, its params and its
  response type. Change a Java DTO, regenerate, and the compiler points at every affected component.
- **Server state lives in TanStack Query**: caching, background refetch, and invalidation
  after mutations (logging a brew refreshes the list *and* the stats). No global store needed.
- **The server is the source of truth for validation.** The form shows the server's Problem
  Details: field errors under each input, rule violations (422) at the top. The grind-size
  hint is just an instant preview of the same rule.
- **Filters live in the URL** (`/brews?method=PHIN&sort=rating,desc`), so views are
  shareable and the back button works.
- **Accessible by construction**: labelled controls, errors linked with `aria-describedby`,
  `role="alert"` for messages. The tests query by label and role, so they fail if this breaks.

## Auth & security notes

- The JWT is kept in memory and mirrored to `sessionStorage` (survives refresh, cleared when
  the tab closes). It's readable by JavaScript, so XSS is the risk; mitigations are React's
  output escaping, no `dangerouslySetInnerHTML`, and a strict **Content-Security-Policy** in
  production (see [public/.htaccess](public/.htaccess)).
- Route guards are UX only; the API enforces auth on every request. A `401` from the API logs
  the user out, and logging out clears the query cache so no previous user's data lingers.

## Deployment

Hosted on DreamHost (Apache) at https://coffee.hassanhibbert.com. `npm run deploy` builds the
bundle and rsyncs `dist/` to the server; copy `.env.deploy.example` to `.env.deploy.local` and
fill it in first. `npm run deploy -- --dry-run` shows what would change without uploading.

- `VITE_API_BASE_URL` points the bundle at the API (on Render), and the API's
  `CORS_ALLOWED_ORIGINS` allows this site's origin.
- [public/.htaccess](public/.htaccess) ships with the build: the SPA rewrite (deep links serve
  `index.html`), the Content-Security-Policy and other security headers, and cache rules.
