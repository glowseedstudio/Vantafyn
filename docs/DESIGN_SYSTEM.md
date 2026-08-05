# Vantafyn Design System

Vantafyn uses a quiet premium streaming style: dark graphite foundations, soft blue/violet accents, readable contrast, and restrained motion. The surface language should feel cinematic and calm, not neon, cyberpunk, or dashboard-like.

## Living Glass Surfaces

Shared glass primitives live in `core-ui`:

- `VantafynGlassSurface` is the base primitive.
- `VantafynGlassPanel` is for grouped settings, admin, lyrics, queues, and larger content blocks.
- `VantafynGlassCard` is for rows, cards, menu actions, and compact information containers.
- `VantafynGlassDock` is for persistent rails, mini players, and bottom navigation.

The glass system uses layered translucent graphite, subtle blue/violet lift, low-contrast borders, top-left highlights, and soft shadow/glow. It intentionally avoids broad blur effects so it remains reliable on phones, TV devices, and Fire OS hardware.

## Surface Variants

- `Panel`: larger grouped regions with calm separation.
- `Card`: repeated or tappable content blocks.
- `Dock`: navigation and persistent player surfaces.
- `Chip`: filters, segmented controls, and small actions.
- `Modal`: future modal/sheet surfaces.
- `Button`: future button sub-surfaces when a full gradient CTA is not appropriate.

Selected and focused states should add visible lift through a soft primary glow and brighter border, not a harsh outline. Disabled states reduce opacity and contrast without making text unreadable.

## Usage Rules

- Use glass surfaces for app chrome, settings, admin panels, context menus, mini-player, music queue/lyrics, profile/onboarding cards, and compact control groups.
- Keep media browsing screens content-first. Posters, hero art, and playback artwork should remain visually dominant.
- Do not layer multiple decorative cards inside each other. A glass panel can contain simple rows or controls, but avoid nested framed panels unless the hierarchy genuinely needs it.
- Keep text contrast strong: primary text uses `VantafynColors.Ink`, secondary text uses `VantafynColors.Muted`, and destructive actions use the muted red token.
- Avoid saturated glows, bright cyan outlines, heavy shadows, and busy borders.

## Backgrounds

Onboarding backgrounds use the supplied nebula imagery with dark scrims for contrast. Home, library, details, music, and admin screens should stay darker and content-focused unless a specific hero artwork area requires an image treatment.

## Motion

Use the existing `VantafynMotion` constants. Focus and selection should feel responsive and polished, but transitions should remain subtle.
