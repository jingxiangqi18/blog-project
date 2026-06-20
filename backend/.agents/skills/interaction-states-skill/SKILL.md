---
name: interaction-states-skill
description: Add complete product-grade interaction states to Vue pages, including loading, empty, error, hover, active, form validation, delete confirmation, success messages, failure retry, and mobile responsive states. Use when Codex needs to lift a functional frontend from merely usable to product-like.
---

# Interaction States Skill

## Data Views

- Every API-backed view must include loading, empty, and error states.
- Use skeletons for first load when content shape is known.
- Use empty states when the request succeeds but returns no items.
- Use error states with a retry action when the request fails.
- Avoid blank pages during pending or failed requests.

## Mutations

- Disable submit buttons while requests are running.
- Show clear progress on save, publish, update, and delete actions.
- Show success feedback after completed mutations.
- Show failure feedback with a useful message and a retry path when reasonable.
- Preserve form input after failed submit unless clearing it is explicitly intended.

## Forms

- Add client-side validation for required fields, length limits, format constraints, and select values that the UI can know.
- Show validation near the relevant field.
- Keep backend validation errors visible in the same form flow.
- Place primary and secondary actions consistently.

## Destructive Actions

- Confirm destructive actions with a dialog.
- Include the entity name or title when available so the user understands what will be deleted.
- Keep cancel and confirm actions visually distinct.
- Do not perform destructive actions from hover-only controls on mobile.

## Interaction Details

- Add hover, active, focus-visible, disabled, and selected states for clickable cards, nav items, buttons, and controls.
- Make clickable cards distinguishable without overwhelming the content.
- Ensure keyboard focus is visible for buttons, links, and form fields.
- Keep layout stable when states change.

## Responsive States

- Verify mobile layouts for navigation, forms, article cards, detail pages, and action bars.
- Ensure actions remain reachable on narrow screens.
- Prevent text overflow, clipped buttons, and overlapping controls.
