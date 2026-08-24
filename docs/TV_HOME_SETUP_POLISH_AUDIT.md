# Vantafyn Android TV Setup & Home Polish Audit

## 1. Current State Assessment

### Setup Flow
- **Welcome Screen (`TvWelcomeScreen`)**: Fully implemented with 10-foot typography, nebula background, and default-focused "Get Started" glass button.
- **Connect Server Screen (`TvConnectServerScreen`)**: Supports Android TV software and hardware keyboards, reachability testing, and human-readable error messages.
- **Server Found Screen (`TvServerConfirmScreen`)**: Shows verified server name, version, and address on a frosted glass card.
- **Sign In Screen (`TvLoginScreen`)**: Supports password visibility toggle, credential validation via `JellyfinAuthRepository`, and error reporting.
- **Who's Watching Screen (`TvProfilePickerScreen`)**: Shows large TV profile cards with user avatar, name, and admin/member tags.

### TV Sidebar
- **Profile Area Position**: Previously placed at the bottom; now moving to the **top** of the sidebar to match premium TV streaming standards (Netflix/Apple TV layout).
- **Profile Area Interactivity**: Fully focusable with 1.05x scale, avatar rendering (with initials fallback), and direct navigation to profile settings / profile switcher.
- **Route Gating**:
  - `Home`: Always visible.
  - `Library`: Always visible.
  - `Search`: Always visible.
  - `Requests`: Shown when Ombi is configured or requests are enabled for the current server.
  - `Admin`: Strictly gated to `session.user.isAdministrator == true`.
  - `Settings`: Always visible.

### TV Home & Media Rows
- **Hero Treatment (`VantafynTvHero`)**: Wide backdrop image with `BlendMode.DstIn` vertical fade dissolving into `VantafynTvBackground`. Left-side scrim ensures text legibility.
- **Fallback Hero**: Displays a styled Vantafyn banner if no featured hero items are returned by the server.
- **Real Jellyfin Data Rows**: Connected to `state.home?.sections` (Continue Watching, Latest Movies, Latest Shows, Next Up) and `state.libraries`.
- **Loading / Error / Empty States**: Clean glass shimmer loading and retry actions without raw debug text or unhandled exceptions.

### Requests / Ombi Integration
- Gated in the sidebar. When accessed without prior Ombi setup, displays a clean TV notice (*"Requests require setup. Use the Vantafyn mobile app to configure or link your account."*) with no exposure of API keys or sensitive administrative endpoints to standard users.
