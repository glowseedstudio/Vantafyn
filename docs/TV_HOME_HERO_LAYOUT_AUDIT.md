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
   - Positioned in `LazyColumn` Item 0 with `padding(start = 36.dp, end = 48.dp, top = 28.dp, bottom = 8.dp)`.
   - Title / Logo: up to 56dp height, 28sp font weight Black.
   - Metadata: Golden Star `★ Rating`, Year, Runtime, Official Rating badge, Genre tags.
   - Overview: up to 3 lines of clean typography (`13sp`, `lineHeight = 18sp`).
   - Actions: `Play` (Primary) and `Details` (Secondary) glass buttons.
4. **Navigation Invariance**:
   - Up from first row (`Your Libraries`) shifts focus to Hero Action buttons (`Play` / `Details`).
   - Down from Hero Action buttons enters the first item of `Your Libraries`.
   - Left from any content row enters the Sidebar.
   - Right from the Sidebar returns focus to the active content item.
