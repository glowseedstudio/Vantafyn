# Wholphin TV Navigation Conceptual Audit

## 1. Context & Non-Infringement Notice
- **Reference**: Wholphin repository (`_reference/Wholphin/`).
- **License Constraint**: Wholphin is licensed under GPL-2.0. **No code, assets, package namespaces, or raw layout files are copied into Vantafyn.**
- **Purpose**: This document captures high-level ergonomic and architectural concepts for Android TV navigation to guide Vantafyn's original implementation.

---

## 2. Key Conceptual Findings from Wholphin

### A. Collapsed vs. Expanded Navigation Drawer
- **Collapsed Mode (Rail)**:
  - Width is minimized (~64dp to 72dp) showing only distinct route icons.
  - Leaves maximum screen width for content, backdrops, and carousels.
- **Expanded Mode (Drawer)**:
  - Expands smoothly (~220dp to 260dp) upon receiving D-pad focus on the rail or pressing `DPAD_LEFT` from the first item of a content row.
  - Reveals route labels, user profile avatar, server status, and contextual indicators.
  - Does not obscure content abruptly; dims or overlays with a subtle backdrop glass scrim.

### B. D-Pad Focus Transitions
- **Sidebar to Content**: Pressing `DPAD_RIGHT` or `DPAD_CENTER` on an active item closes/collapses the drawer and restores focus to the first interactive element in the current content view.
- **Content to Sidebar**: Pressing `DPAD_LEFT` from the leftmost item in any row navigates focus directly to the active sidebar item.
- **Back Button Contract**: Pressing `BACK` when inside content moves focus back to the sidebar instead of immediately exiting the application. Pressing `BACK` on the sidebar prompts exit or returns to Home.

### C. Route Grouping & Hierarchy
- **Primary Routes**: Top section contains main browsing destinations (`Home`, `Library`, `Search`, `Music`).
- **Secondary / Tools**: Middle section contains utility/integration features (`Requests`, `Watch Party`).
- **Account & System**: Bottom pinned section contains active User Profile, Server Status, and Settings.

---

## 3. Vantafyn Adaptations & Design Identity

| Feature | Wholphin Conceptual Reference | Vantafyn Proprietary Implementation |
| :--- | :--- | :--- |
| **Visual Styling** | Standard Material TV dark theme | Vantafyn Deep Navy/Obsidian glass, subtle cyan/violet glow, and floating blur borders |
| **Animation Curve** | Basic linear / standard spring | Vantafyn Cinematic Easing (`cubic-bezier(0.22, 1, 0.36, 1)`) with smooth width interpolation |
| **Active Indicator** | Generic selection highlight | Neon cyan pill glow (`0xFF00E5FF`) with subtle particle/gradient backdrop |
| **State Management** | Separate navigation managers | Integrated with `TvNavigationState` & `VantafynHomeViewModel` |
| **Profile Integration** | Basic server user icon | High-resolution Vantafyn avatar badge with active online status dot |
