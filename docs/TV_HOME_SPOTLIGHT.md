# Vantafyn TV Home Dynamic Spotlight Architecture

## 1. Overview
Vantafyn TV implements a Wholphin/Netflix-style pinned top spotlight on TV Home. The hero backdrop is restricted strictly to the top spotlight region (`y = 0`, `height = 360dp`), while the full-screen persistent cosmic nebula background remains active beneath all scrolling content rows.

---

## 2. Visual & Structural Layering

```
+--------------------------------------------------------------------------------+
| Layer 1: Persistent Nebula Foundation (Full-screen base layer)                 |
+--------------------------------------------------------------------------------+
| Layer 2: Pinned Top Spotlight Backdrop Layer (Clipped to top 360dp, DstIn)     |
+--------------------------------------------------------------------------------+
| Layer 3: Fixed Spotlight Text Overlay (Title/Logo, Rating, Synopsis, Actions) |
+--------------------------------------------------------------------------------+
| Layer 4: Foreground Content Scroller (LazyColumn starting below hero overlay)  |
|          - Item 0: Media Libraries Rail ("Your Libraries", width 170dp)        |
|          - Items 1+: Dynamic Content Rails (Continue Watching, Recently Added) |
+--------------------------------------------------------------------------------+
| Layer 5: Transparent Dark Glass Sidebar Rail (Zero blue tint, overlay)        |
+--------------------------------------------------------------------------------+
```

---

## 3. Wholphin/Netflix Density Tuning

To provide an elegant 10-foot TV experience where multiple content rails are immediately accessible:
- **Spotlight Area**: Compact top layout (`top = 28dp`, `height = 360dp`).
- **Poster Cards**: Width `118dp`, Aspect Ratio `2:3` (Height `177dp`).
- **Wide Cards**: Width `192dp`, Aspect Ratio `16:9` (Height `108dp`).
- **Library Cards**: Width `170dp`.
- **Row Spacing**: `12dp` horizontal card spacing, `16dp` vertical section spacing.
- **Section Headers**: Compact `16sp` typography with `6dp` bottom margin.
- **Result**: On a 1080p TV display, the top spotlight, the Your Libraries rail, and the Continue Watching rail are immediately visible on screen.

## 4. Spotlight Transitions

Focused row cards update the shared spotlight item. The backdrop crossfades by high-resolution backdrop URL and the hero copy/actions crossfade by spotlight item inside a fixed-size overlay. The transition uses fade only and does not animate layout size, which prevents artwork or text jumps during D-pad navigation.

## 5. Library Rail Clean-Up

The `Your Libraries` row uses image labels on the cards and disables the generic under-card title/subtitle text. Normal media rows still show useful title/year/episode metadata below cards.
