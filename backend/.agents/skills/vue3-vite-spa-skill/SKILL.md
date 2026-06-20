---
name: vue3-vite-spa-skill
description: Create or refactor independent Vue 3 + Vite single-page frontends for Spring Boot REST APIs with Vue Router, Axios, clear src organization, Vite proxy configuration, and no Spring Boot resources/static frontend. Use when Codex is asked to create, rebuild, or separate a Vue frontend, set up a frontend/backend split, or replace static Spring Boot pages with a standalone SPA.
---

# Vue 3 Vite SPA Skill

## Core Rules

- Keep the frontend independent from the Spring Boot backend. Do not create or edit frontend pages under `src/main/resources/static`.
- Prefer a sibling layout such as `blog/backend` and `blog/frontend` when the project is being separated.
- Use Vue 3 with Vite. Use ordinary JavaScript unless the existing frontend is already TypeScript-based.
- Use Vue Router for page routing and Axios for API calls.
- Keep Spring Boot responsible for REST API, business logic, and persistence only.

## Project Structure

Use a clear `src` layout:

```text
src/
  api/
  router/
  views/
  components/
  layouts/
  styles/
```

- Put Axios instance and API modules in `src/api`.
- Put route definitions in `src/router`.
- Put route-level screens in `src/views`.
- Put reusable UI pieces in `src/components`.
- Put shared shell/navigation components in `src/layouts` when needed.
- Put global tokens, resets, and page-level CSS in `src/styles`.

## API Integration

- Configure a Vite dev proxy for `/api` to the backend server whenever possible.
- Keep the frontend base URL configurable through Vite environment variables.
- Centralize request timeout, base URL, and error normalization in one Axios wrapper.
- If the backend lacks an endpoint, do not invent the frontend feature. Show only what the API can support.
- If CORS or proxy issues appear, prefer frontend proxy configuration first. Do not modify backend code unless explicitly asked.

## App Behavior

- Build the actual usable application as the first screen, not a marketing landing page.
- Include real routes for list, detail, create/edit, and any other backend-supported workflow.
- Ensure route transitions, empty states, and failed API calls do not leave blank screens.
- Keep mobile and desktop layouts usable from the first implementation.

## Verification

- Run the frontend build command before handoff, usually `npm run build`.
- Start the dev server on `http://localhost:5173` when the task asks for a runnable frontend.
- Verify at least one API-backed page can load through the configured proxy.
