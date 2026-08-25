# TV Home Hero Layout Audit & Specifications

## Pinned Spotlight Region vs Full Screen Background

1. **Backdrop Region**:
   - Fixed at `Alignment.TopCenter`, `fillMaxWidth()`, `height(360.dp)`.
   - Strictly clipped with `clipToBounds()`.
   - Does NOT extend down into the scrolling rows area.
   - Fades out cleanly between 48% and 100% via `BlendMode.DstIn` gradient mask.
2. **Persistent Background Layer**:
   - The deep space cosmic nebula background (`VantafynNebulaBackground`) remains persistent across the entire viewport.
   - Lower rows scroll exclusively against the nebula, preventing blown-up or jarring background switches.
3. **Spotlight Text & Action Buttons**:
   - Positioned as a fixed overlay sibling above the `LazyColumn`, not as a scroll item.
   - Uses `padding(start = 36.dp, end = 48.dp, top = 28.dp, bottom = 8.dp)` inside a stable fixed-height region.
   - Title / Logo: up to 56dp height, 28sp font weight Black.
   - Metadata: Golden Star `★ Rating`, Year, Runtime, Official Rating badge, Genre tags.
   - Overview: up to 3 lines of clean typography (`13sp`, `lineHeight = 18sp`).
   - Actions: compact `Play` (Primary) and `Details` (Secondary) glass buttons.
4. **Navigation Invariance**:
   - Up from first row (`Your Libraries`) shifts focus to Hero Action buttons (`Play` / `Details`).
   - Down from Hero Action buttons enters the first item of `Your Libraries`.
   - Left from any content row enters the Sidebar.
   - Right from the Sidebar returns focus to the active content item.
5. **Focus Stability**:
   - Hero action focus is visual only and uses `graphicsLayer` scale.
   - The hero overlay is never brought into view by the row scroller.
   - Focus borders and scale do not change parent measured size.
