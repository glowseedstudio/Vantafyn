# Vantafyn Search UI

## Idle State

The mobile Search screen keeps the title and search field stable, then uses a quiet idle area instead of a placeholder card. The copy is user-facing and concise:

- "Search your universe"
- "Find movies, shows, music, people, and more."

The old "Start typing / Search runs after a short pause" implementation wording is intentionally removed.

## Quick Chips

Empty search shows quick filter chips for Movies, TV Shows, Episodes, People, Music, and Collections. Chips enter with a short staggered fade and slight upward slide. Selecting a chip applies the active visual/result filter where matching result types exist.

## Results

Search results still use the existing grouped layout and horizontal rows. Section headings and cards now enter with subtle finite fade/slide motion using stable item keys so query updates avoid full-screen blinking.

## Loading And No Results

Loading uses a small inline "Searching" treatment rather than a large card. No-results uses a centered, lightweight state with clear guidance and a text action to clear the query.

## Missing Artwork

Cards with no image URL render a premium graphite fallback with initials and subtle blue/violet accents. This avoids blank person, music, collection, and unknown artwork blocks.

## Motion And Performance

Animations are finite, staggered only for visible composed content, and avoid heavy blur or large-list infinite effects. The only idle pulse is a small decorative sparkle/search motif.

## Keyboard And Back

The search field remains stable and uses the existing text-field keyboard behavior. This pass does not aggressively force keyboard focus, so Android back behavior is left to the existing navigation/focus handling.
