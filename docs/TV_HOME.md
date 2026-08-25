# Vantafyn Android TV Home Screen & Architecture

**Document**: `docs/TV_HOME.md`  
**Status**: Active  

---

## 1. Architecture Overview

The Android TV Home screen (`TvHomeScreen.kt`) serves as the primary media discovery and resume hub for 10-foot television experiences. It operates within the `TvShellScreen` alongside the collapsible translucent navigation rail.

### Core Sections:
1. **Dynamic Hero Spotlight**:
   - Displays highlighted featured content with high-resolution 4K backdrops.
   - Smooth backdrop crossfading with subtle vignette masking.
   - Direct `Play` and `Details` action buttons styled with `VantafynTvGlassButton`.
2. **Continue Watching / Resume Row**:
   - Displays items with active playback checkpoints and resume progress bars.
3. **Next Up Row**:
   - Next unwatched episodes for serialized television content.
4. **Media Rows by Library**:
   - Latest Movies, Latest Shows, Recommended media grouped logically.

---

## 2. Interaction & Focus Flow

- **Navigation Rail Integration**:
  - D-pad `Left` on any media card or hero action shifts focus to the translucent sidebar.
  - D-pad `Right` from the sidebar seamlessly returns focus to the previously active media row or hero button.
- **Focus Preservation**:
  - Auto-scrolling row containers keep the focused poster card centered within the TV overscan safe viewport.
- **Translucent Layering**:
  - The translucent sidebar overlays subtly onto the left edge of the home canvas without clipping poster artwork or obstructing hero typography.
