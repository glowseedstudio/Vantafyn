# Vantafyn Android TV Architecture

## 1. Overview
The Vantafyn Android TV module (`app-tv`) is designed specifically for 10-foot television navigation while sharing core data contracts, networking, session management, and state logic with the core modules.

```mermaid
graph TD
    subgraph Core Modules
        CoreJellyfin[core-jellyfin]
        CoreMedia[core-media]
        CoreUI[core-ui]
    end

    subgraph Feature Modules
        FeatureHome[feature-home - VantafynHomeViewModel]
        FeatureLibrary[feature-library]
        FeaturePlayer[feature-player]
    end

    subgraph TV App Layer (app-tv)
        TvMainActivity[TvMainActivity]
        TvShellScreen[TvShellScreen]
        TvBackground[VantafynTvBackground]
        TvSidebar[VantafynTvSidebar]
        TvHero[VantafynTvHero]
        TvHome[TvHomeScreen]
        TvLibrary[TvLibraryScreen]
        TvSearch[TvSearchScreen]
        TvDetail[TvDetailScreen]
        TvSettings[TvSettingsScreen]
    end

    CoreJellyfin --> FeatureHome
    CoreMedia --> FeatureHome
    CoreUI --> FeatureHome
    FeatureHome --> TvMainActivity
    TvMainActivity --> TvShellScreen
    TvShellScreen --> TvBackground
    TvShellScreen --> TvSidebar
    TvShellScreen --> TvHome
    TvShellScreen --> TvLibrary
    TvShellScreen --> TvSearch
    TvShellScreen --> TvDetail
    TvShellScreen --> TvSettings
    TvHome --> TvHero
```

---

## 2. Shared Background & Visual Layer
- **`VantafynTvBackground`**: Persistent dark nebula/graphite layer with atmospheric radial gradients and TV vignette scrim. Renders behind all screens to prevent reload flickers.
- **`VantafynTvHero`**: Wide cinematic backdrop with `BlendMode.DstIn` bottom fade directly into `VantafynTvBackground`. Left and top gradient scrims ensure text contrast.
- **`VantafynTvScreenScaffold`**: Enforces TV safe overscan padding (28dp horizontal, 20dp top, 32dp bottom) across all screens.

---

## 3. Navigation Hierarchy & Focus Architecture
- **Expanding Sidebar**: Collapsed rail (72dp) expands on focus (240dp) over the background.
- **D-Pad Focus Engine**: `vantafynTvFocusable` modifier providing 1.05x spring scale, neon cyan/violet border brush (`TvFocusTokens.FocusBorderBrush`), and radial glow.
- **Back-Stack State**: `TvNavigationState` handles routing, sidebar collapse/expand, and back-button traversal cleanly.
