# Vantafyn Android TV App Audit

## 1. Overview
The `app-tv` module provides a dedicated Android TV target for Vantafyn. While Android mobile targets touch-driven interaction and compact screens, Android TV requires a 10-foot viewing model, D-pad remote navigation, high-contrast focus rings, and overscan-safe layouts.

---

## 2. Module & Dependency Audit

### Gradle Setup (`app-tv/build.gradle.kts`)
- **Package / Namespace**: `dev.vantafyn.tv`
- **Current Compose BOM**: AndroidX Compose BOM (shared with mobile)
- **TV-Specific Libraries**:
  - `androidx.tv:tv-foundation`
  - `androidx.tv:tv-material`
- **Shared Module Dependencies**:
  - `:core-jellyfin`: API clients, session persistence, user profiles, social models.
  - `:core-media`: ExoPlayer playback engine, media sessions, audio service.
  - `:core-ui`: Vantafyn theme, color palettes, glassmorphism modifiers, typography.
  - `:feature-home`: ViewModels (`VantafynHomeViewModel`), home feeds, search logic, admin overview.
  - `:feature-library`: Library item models & navigation contracts.
  - `:feature-player`: Video player engine and playback controls.

---

## 3. Reusability Analysis

| Feature Area | Reusability from Mobile | TV-Specific Adaptations Required |
| :--- | :--- | :--- |
| **Authentication & Profiles** | Full (`VantafynHomeViewModel`, `core-jellyfin`) | Large profile cards on launch / D-pad switcher in settings |
| **Data Repositories** | 100% Shared (`JellyfinMediaRepository`, etc.) | None (queries, paging, and models are platform-agnostic) |
| **Home Screen Feed** | Data models & StateFlow shared | TV Hero backdrop banner + horizontal carousels |
| **Library Browsing** | Data models shared | TV poster grid (4-6 columns) with D-pad focus |
| **Search** | Query logic & search repository shared | TV on-screen keyboard support + large card grid |
| **Media Player** | `core-media` / ExoPlayer engine shared | 10-foot OSD with D-pad controls (play, seek, audio/subs) |
| **Admin & Settings** | Server info & basic toggle state shared | Simplified TV settings; redirection for mobile-only flows |

---

## 4. Focus & D-Pad Safety Hazards in Shared UI
1. **Touch Target Gestures**: Shared mobile components utilizing `pointerInput`, `swipeable`, or `detectTapGestures` cannot be activated with a standard TV remote.
2. **Bottom Navigation**: Mobile bottom navigation bars waste vertical space and create erratic D-pad navigation on widescreen displays.
3. **Small Card Text**: Phone-sized 12sp captions become unreadable at standard 3-meter TV distances; TV typography must start at 14sp-18sp for body and 24sp-36sp for titles.
4. **Unbounded Vertical Lists**: Infinite touch-scroll lists without focus grouping can cause D-pad focus to get lost or trapped off-screen.

---

## 5. TV Foundation Guidelines
- **Overscan Margins**: Ensure all TV screens maintain at least 24dp (horizontal) and 16dp (vertical) safe margins.
- **Predictable Focus Rings**: Every focusable element must provide visual feedback (1.05x subtle scale, neon accent border, outer glow).
- **Expanding Sidebar**: Use a collapsible left navigation rail to maximize screen real estate for media backdrops.
