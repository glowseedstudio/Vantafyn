# Vantafyn Android TV Navigation

**Document**: `docs/TV_NAVIGATION.md`  
**Updated**: Android TV Navigation, Pure Glass Rail, and Dynamic Spotlight Focus  

---

## 1. Pure Glass Navigation Rail Architecture

The Vantafyn Android TV sidebar is engineered as a lightweight, transparent dark glass rail that seamlessly integrates with ambient backdrops and dynamic hero artwork without feeling like a heavy opaque drawer or adding blue/navy tinting.

### Core Structure:
- **Surface**: High-translucency neutral dark glass (`Color(0x38000000)` to `Color(0x18000000)`) with a 1dp luminous glass border (`Color.White.copy(alpha = 0.08f)`).
- **Corner Radius**: `RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)`.
- **Expansion Physics**: Expands smoothly from `72.dp` (collapsed rail) to `240.dp` (expanded panel) via tuned spring physics (`dampingRatio = 0.82f, stiffness = 380f`).
- **Focus Mechanics**:
  - Automatically expands whenever any child inside the sidebar gains D-pad focus.
  - Automatically collapses whenever focus leaves the sidebar to the right (into content rows or hero buttons).
  - Navigating to the currently active route safely collapses the sidebar and restores focus to content.

---

## 2. Profile & Action Header

- **Single Top Anchor**: Current profile avatar is displayed prominently at the top with a soft ambient border.
- **Actions Row (Under Profile)**:
  - **Expanded**: Horizontal 3-button row for Search, Notifications, and Power/Exit.
  - **Collapsed**: Single compact Search icon row.

---

## 3. Dynamic Main Navigation (Scrollable Lower Section)

Main navigation items are dynamically derived from server capabilities and user library permissions:

1. **Home (`TvRoute.Home`)**: Default dynamic spotlight dashboard with Your Libraries, Continue Watching, Next Up, and latest media rows.
2. **Favorites / My List (`TvRoute.Favorites`)**: Direct access to user-favorited content.
3. **Dynamic Jellyfin Libraries**:
   - **Movies**: Routed via `TvRoute.Library(parentId, "Movies")` with `Icons.Rounded.Movie`.
   - **TV Shows**: Routed via `TvRoute.Library(parentId, "TV Shows")` with `Icons.Rounded.Tv`.
   - **Collections / BoxSets**: Routed via `TvRoute.Library(parentId, "Collections")` with `Icons.Rounded.VideoLibrary`.
   - **Music**: Routed via `TvRoute.Music` with `Icons.Rounded.MusicNote`.
   - **Live TV**: Routed via `TvRoute.Library` or dedicated tuner with `Icons.Rounded.LiveTv`.
   - **Generic Library**: Fallback for unclassified libraries.
4. **Requests (`TvRoute.Requests`)**: Ombi and media discovery integration (visible when Ombi is configured).
5. **Admin Dashboard (`TvRoute.Admin`)**: Strictly visible only to administrators (`session.user.isAdministrator == true`).
6. **Settings (`TvRoute.Settings`)**: Positioned at the bottom of the libraries list.
