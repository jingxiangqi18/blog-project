---
name: naive-ui-component-skill
description: Build modern Vue interfaces with Naive UI components instead of low-quality hand-rolled widgets. Use when Codex is asked to optimize Vue UI, create blog or CMS pages, forms, cards, lists, details, dialogs, messages, loading skeletons, empty states, or componentized frontend screens.
---

# Naive UI Component Skill

## Component Policy

- Prefer Naive UI components for standard interface behavior before writing custom widgets.
- Use `NButton`, `NCard`, `NForm`, `NInput`, `NSelect`, `NDialog`, `NModal`, `NMessageProvider`, `NSkeleton`, `NEmpty`, `NTag`, `NTabs`, `NDropdown`, and `NPagination` where they fit.
- Avoid mixing multiple component libraries unless the project already depends on them for a clear reason.
- Keep custom CSS for layout, spacing, visual hierarchy, and domain-specific composition, not for reimplementing common controls.

## App Setup

- Ensure the app is wrapped with the required Naive UI providers, especially message and dialog providers when those APIs are used.
- Add minimal theme overrides only when they support the product style. Do not create a large theme system for a small app.
- Keep component imports consistent with the project's existing style.

## Page Composition

- Build article lists with content-oriented cards, tags, dates, summaries, and actions instead of default tables unless tabular comparison is truly needed.
- Build detail pages with a comfortable reading layout and restrained metadata blocks.
- Build create/edit pages as lightweight CMS forms with clear field grouping, validation, and action placement.
- Use dialogs for destructive confirmation and short focused decisions.
- Use messages for short success and failure feedback after actions.

## Quality Bar

- Each interactive component must have disabled/loading states when a request is in progress.
- Forms must surface validation near the relevant field and use clear submit/cancel actions.
- Empty and loading states should use Naive UI primitives, not plain unstyled text.
- Cards and lists should have stable dimensions so hover, loading, and dynamic content do not cause distracting layout shifts.
