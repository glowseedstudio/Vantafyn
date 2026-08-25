# Android TV Sidebar & Navigation Audit

**Project**: Vantafyn Android TV  
**Document**: `docs/TV_SIDEBAR_NAV_AUDIT.md`  
**Status**: Active / Correction Pass  

---

## 1. Executive Summary & Objectives

The Android TV Home experience requires an updated navigation layer. While the existing focus highlighting and D-pad interaction model are responsive and visually polished, the previous sidebar implementation felt like a thick, heavy opaque card drawer rather than a modern, clear translucent navigation rail.

### Primary Objectives:
1. **Translucent Glass Surface**: Transform the sidebar from a heavy solid card into a clear, dark navy glass layer (`Color(0xB8080C18)` / `Color(0x9910182A)`) where background artwork and ambient lighting subtly show through.
2. **De-clutter Top Identity**: Remove the redundant Vantafyn app icon directly below the profile avatar. The user avatar anchors identity; duplicate branding beneath it wastes vertical real estate.
3. **Wholphin-Style Conceptual Structure**: Group dynamic library routes (Movies, TV Shows, Collections, Music, Live TV, Favorites) driven by enabled Jellyfin user libraries.
4. **Pinned Bottom Actions**: Separate secondary utility actions (Search, Notifications / What's New, Settings, Exit App / Power) into a pinned bottom container that does not scroll with the main library list.
5. **Preserve Signature Focus Highlight**: Maintain Vantafyn's glowing cyan-to-violet gradient borders, soft glow, and 1.04x focus scale.

---

## 2. Current Implementation Audit

| Feature | Previous State | Required State |
| :--- | :--- | :--- |
| **Backdrop Fill** | Heavy, dark opaque gradient (`Color(0xF0070A12)`) | High-translucency dark glass (`Color(0x66060A14)` to `Color(0x3B0D1527)`) |
| **Top Profile Area** | Avatar + redundant Vantafyn Logo Badge directly beneath | Avatar at top with Name & Server Name when expanded; NO redundant logo beneath |
| **Main Navigation** | Fixed static list (`Home`, `Library`, `Search`, `Music`, `Requests`, `Admin`, `Settings`) | Dynamic Jellyfin library routes (`Home`, `Favorites`, `Movies`, `TV Shows`, `Collections`, `Music`, `Live TV`, `Requests`, `Admin`, `Settings`) |
| **Settings Position** | In bottom action dock | At the bottom of the main scrollable list (under libraries) |
| **Bottom Actions (Expanded)** | Vertical column with text labels | Horizontal row of 3 icon buttons: `[ Search ] [ Notifications ] [ Power/Exit ]` |
| **Bottom Actions (Collapsed)**| Full column of icons | Single centered `Search` icon button |
| **Expand / Collapse** | Auto-expands on child focus (72dp to 240dp); collapses on focus loss | Preserved with tuned spring physics (`dampingRatio = 0.82f, stiffness = 380f`) |
| **Focus Style** | Vantafyn cyan/violet gradient border + subtle glow + 1.04x scale | **Strictly preserved** (do not replace with solid box highlights) |

---

## 3. Navigation Route Gating Rules

All routes rendered in the sidebar must strictly respect user permissions and server capabilities:

1. **Home (`TvRoute.Home`)**: Always visible. Default landing screen.
2. **Favorites / My List (`TvRoute.Favorites`)**: Visible when favorites exist or server supports user favorites.
3. **Dynamic Jellyfin Libraries**:
   - **Movies**: Visible if user has access to a `movies` collection.
   - **TV Shows / Series**: Visible if user has access to a `tvshows` collection.
   - **Collections / BoxSets**: Visible if user has access to a `boxsets` collection.
   - **Music (`TvRoute.Music`)**: Visible if user has a `music` library or audio capability enabled.
   - **Live TV**: Visible if server has Live TV tuner/channels configured.
   - **Generic Library (`TvRoute.Library`)**: Fallback for unclassified or custom folder views.
4. **Requests (`TvRoute.Requests`)**:
   - Visible **only** if Ombi / Requests is configured (`state.ombiConfigured` / `state.ombiRequestsEnabledForUsers` or `isAdmin`).
   - If not TV-ready, renders an honest, premium TV setup/coming-soon card without throwing errors.
5. **Admin (`TvRoute.Admin`)**:
   - Visible **strictly** to Jellyfin Administrators (`session.user.isAdministrator == true`).
   - Hidden from standard users.
6. **Settings (`TvRoute.Settings`)**: Pinned at the bottom of the scrollable libraries list.
7. **Bottom Actions**:
   - **Search (`TvRoute.Search`)**: Magnifying glass icon.
   - **Notifications (`TvRoute.Notifications`)**: Bell icon with amber unread indicator dot (`#FF9500`).
   - **Exit App (`PowerSettingsNew`)**: Power icon that prompts a safe TV confirmation dialog.

---

## 4. Wholphin Conceptual Reference

* **What we reference conceptually**:
  - Vertical split between primary content/library routes and fixed utility actions at the bottom.
  - Granular library categories (Movies, Series, BoxSets) replacing a single monolithic "Library" entry.
  - Safe power/exit action integration for TV form factors.
* **What is NOT copied**:
  - Source code, layouts, or assets.
  - Wholphin's rectangular purple focus ring (Vantafyn retains its signature rounded cyan/violet glow gradient).
  - Heavy opaque drawer styling.
