---
name: design-polish-pass-skill
description: Perform a focused visual polish pass on completed Vue frontend screens without adding business features. Use when frontend functionality is complete but the UI lacks visual quality, consistency, hierarchy, spacing, typography refinement, button states, form layout quality, reading comfort, or mobile polish.
---

# Design Polish Pass Skill

## Scope

- Do not add new business features, routes, backend assumptions, or API behavior.
- Focus only on presentation, component composition, interaction clarity, and responsive quality.
- Keep the existing product structure intact unless a small layout adjustment clearly improves usability.

## Polish Checklist

- Normalize spacing scale across page shells, cards, forms, lists, and detail pages.
- Tune typography hierarchy for page titles, section titles, metadata, body text, labels, and helper text.
- Align card borders, shadows, radius, background surfaces, and hover states.
- Make primary, secondary, danger, disabled, and loading button states consistent.
- Improve form grouping, label alignment, validation placement, and action rows.
- Improve article reading comfort through max width, line height, paragraph rhythm, and metadata treatment.
- Fix mobile spacing, line wrapping, action placement, and overflow.

## Remove Low-Quality Signals

- Remove default template leftovers, generic demo copy, excessive explanatory text, and visual noise.
- Avoid cheap gradients, decorative blobs, heavy shadows, and redundant icons.
- Avoid making the app feel like a heavy traditional admin panel unless the user explicitly wants that.

## Verification

- Rebuild the frontend after changes when feasible.
- Use screenshots or browser inspection when the task involves visible UI.
- Report only the visual changes made and any remaining known limitations.
