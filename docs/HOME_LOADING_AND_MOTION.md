# Home Loading And Motion

## Loading State

Mobile Home keeps the graphite background stable while Jellyfin data loads. If no cached rows are available, Home shows a cinematic hero skeleton and a few faint smoked-glass row silhouettes instead of text-heavy placeholder cards.

## Cached Content

When real rows are already present, Home keeps them visible during refresh. Loading indicators only appear when the feed has no usable content yet, so app resume or quiet refresh should not blank the screen.

## Section Reveal

Home uses the same restrained motion language as Search:

- finite fade in
- slight upward slide
- staggered sections
- no springy bounce
- no fake rows masquerading as content

Reveal order follows the configured Home layout after the hero: My Media, Continue Watching, recently added rows, Live TV, Smart Rows, My List, then More Libraries when present.

## Hero Loading

The hero does not fall back to a welcome card during normal loading. If hero data is still loading, a dark cinematic hero skeleton is shown. If the server genuinely has no usable home content or libraries, Home shows one quiet empty state:

"Your library is ready"

"Add media to Jellyfin and Vantafyn will bring it to life here."

## Row Empty And Error Behavior

Rows with no Jellyfin-backed items are hidden. My List appears on Home only when real favorites exist; its empty state belongs on the dedicated My List screen. Home errors use the existing compact retry card and do not replace cached content.

## Smart Rows Rule

Home only shows Smart Rows that are backed by real Jellyfin results. Explanatory Smart Rows copy belongs in settings, not on Home.

## Performance

Skeleton motion is limited to a small number of visible placeholders. Real content reveal uses finite Compose animations with stable Lazy keys where rows and cards already provide them.
