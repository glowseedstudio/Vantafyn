# Vantafyn Android TV Navigation

## 1. Sidebar Architecture
- **Top Profile Header**:
  - Displays user avatar with focus glow. Clicking navigates to settings / profile switch.
- **Brand Header**:
  - High-resolution `vantafyn_logo.png` with "VANTAFYN" typography.
- **Navigation Routes**:
  - `Home`, `Library`, `Search`, `Music`, `Requests` (gated), `Admin` (admin only), `Settings`.
- **Focus & Expansion**:
  - Expands from 72dp to 240dp on focus with spring animation (`dampingRatio = 0.85f, stiffness = 450f`).
  - Focused item scale `1.04x` with `VantafynGradients.accentHorizontal()` border.
