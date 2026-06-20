---
name: modern-frontend-design-skill
description: Apply restrained modern visual design to Vue frontends for personal blogs, content management systems, and student portfolio projects. Use when Codex is asked to make pages good-looking, modern, clean, portfolio-ready, or more designed, especially avoiding default-template, course-assignment, cheap-gradient, or heavy-admin aesthetics.
---

# Modern Frontend Design Skill

## Design Direction

- Aim for a restrained product feel inspired by Notion, Linear, Medium, and Vercel dashboards without copying their branding.
- Make the interface modern, clean, simple, and intentional. It should look like a small real product, not a default template or class exercise.
- Prefer calm neutral surfaces, strong typography hierarchy, precise spacing, and one purposeful accent color.
- Avoid large decorative gradients, noisy shadows, excessive borders, visual clutter, and generic admin-dashboard heaviness.

## Layout Principles

- Use comfortable whitespace and clear grouping between title, metadata, body content, cards, forms, and actions.
- Keep page sections unframed unless a card is representing one repeated item or one focused tool area.
- Do not nest cards inside cards.
- Use border radius of 8px or less unless the existing design system already uses a different rule.
- Use stable responsive constraints for repeated cards, toolbar controls, form groups, and fixed-format elements.

## Blog And CMS Screens

- Article list pages should feel like modern content products: summaries, metadata, tags, and clear primary actions.
- Article detail pages should prioritize reading comfort with a narrow readable measure, good line-height, and subdued metadata.
- Editor pages should feel like a lightweight CMS: clear labels, focused form layout, predictable action placement, and no decorative distractions.
- Empty states should be quiet and useful, with a direct action when the backend supports it.

## Typography And Color

- Use hero-scale type only for true hero areas. Use tighter headings inside cards, panels, and tool surfaces.
- Do not scale font size with viewport width.
- Keep letter spacing at `0` unless a specific design need exists.
- Avoid one-note palettes dominated by a single hue family. Add enough neutral contrast and one controlled accent.
- Ensure text never overlaps controls or adjacent content on mobile or desktop.

## Content Tone

- Use concise interface copy. Do not fill the app with explanations of features, keyboard shortcuts, or how to use the UI.
- Prefer direct labels such as `Publish`, `Save`, `Delete`, `Edit`, `Retry`, and `Back`.
