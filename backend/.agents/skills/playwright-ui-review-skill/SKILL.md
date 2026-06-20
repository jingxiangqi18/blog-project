---
name: playwright-ui-review-skill
description: Review completed frontend pages in a real browser with Playwright by starting the app, capturing desktop and mobile screenshots, checking layout, console errors, network failures, and interaction flows, then applying at least one visual or interaction correction. Use after an initial Vue UI implementation needs visual QA and product polish.
---

# Playwright UI Review Skill

## Review Setup

- Start the frontend dev server before browser review.
- Start the backend only when the reviewed pages need live API data.
- Record the actual frontend URL, normally `http://localhost:5173`.
- Use the project's existing Playwright setup when available. If Playwright is not installed, use available browser tooling and state the limitation.

## Required Viewports

Inspect at least:

- Desktop: `1440x900`
- Mobile: `390x844`

Capture screenshots for the key routes being reviewed.

## What To Check

- Blank screens, failed route loads, console errors, and failed network calls.
- Text overflow, clipped controls, overlapping elements, and layout jumps.
- Loading, empty, and error states where reachable.
- Hover, active, focus, disabled, and selected states for primary controls.
- Mobile navigation, form usability, action placement, and readable spacing.

## Interaction Flow

Exercise the safest representative flow available:

- Article list to article detail.
- Article form validation.
- Create or edit flow only when it can be done without damaging user data, or when test data is clearly acceptable.
- Delete confirmation without confirming deletion unless the user asked for destructive testing.

## Correction Pass

- Apply at least one visual or interaction correction when issues are found.
- Rerun the relevant screenshot or browser check after correction.
- Run the frontend build command before final handoff when feasible.
- Do not modify backend code as part of UI review unless explicitly requested.
