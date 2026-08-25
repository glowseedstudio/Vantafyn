# Android TV Image Quality & Resolution Architecture

## 1. Overview & Problem Statement
Previously, TV hero and backdrop artwork exhibited visible pixelation on 1080p and 4K TV displays. The root causes were:
1. Low `maxWidth` parameters (e.g. `1100px` for hero backdrops, `520px` for logos).
2. Reuse of card thumbnail/primary URLs when generating spotlight items on row focus.
3. Lack of a centralized TV image policy.

---

## 2. Centralized Policy: `TvArtworkResolver`

To guarantee uncompressed, high-definition presentation across 10-foot interfaces, all TV image requests adhere to the `TvArtworkResolver` policy:

| Asset Type | Dimension / Param | Quality | Format | Target |
| :--- | :--- | :--- | :--- | :--- |
| **Hero Spotlight Backdrop** | `maxWidth = 1920` (or `2560`) | `90` | WEBP | Top Spotlight region |
| **Hero Logo Artwork** | `maxWidth = 800` | `90` | WEBP | Spotlight Brand Header |
| **Hero Poster Fallback** | `maxWidth = 600` | `90` | WEBP | Fallback Artwork |
| **Wide Rail Cards** | `maxWidth = 540` | `85` | WEBP | Continue Watching / Libraries |
| **Poster Rail Cards** | `maxWidth = 360` | `85` | WEBP | Recently Added / Movies / TV |

---

## 3. Dynamic Spotlight Image Resolution

When D-pad cursor focus changes across any card in content rows:
- `TvArtworkResolver.resolveSpotlightItem(card, serverUrl, heroMatch)` dynamically constructs the high-resolution 1920-width backdrop URL for the focused item ID:
  `${serverUrl}/Items/${card.id}/Images/Backdrop/0?maxWidth=1920&quality=90&format=WEBP`
- It never stretches or reuses the low-resolution 360px primary thumbnail.
- Image loaders utilize local disk and memory caching to prevent redundant network fetches on rapid D-pad traversal.
