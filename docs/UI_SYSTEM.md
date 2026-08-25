# Vantafyn UI System

**Document**: `docs/UI_SYSTEM.md`  
**Status**: Active  

---

## 1. Unified Design Tokens (`core-ui`)
Vantafyn uses a unified design token system shared across Mobile, Automotive, and Android TV:

- **Nebula Background**: `CoreUiR.drawable.vantafyn_onboarding_background`
- **Official Brand Mark**: `CoreUiR.drawable.vantafyn_logo`
- **Accent Gradient**: `VantafynGradients.accentHorizontal()` (`#31D7FF` $\to$ `#5B8CFF` $\to$ `#8B5CFF` $\to$ `#C05CFF`)
- **Primary Ink**: `VantafynColors.Ink` (`0xFFF5F7FF`)
- **Muted Text**: `VantafynColors.Muted` (`0xFFB8C0D8`)
- **Surface**: `VantafynColors.Surface` (`0xFF141822`)
- **Surface High**: `VantafynColors.SurfaceHigh` (`0xFF1B2030`)
- **Graphite Base**: `VantafynColors.Graphite` (`0xFF070A12`)

---

## 2. 10-Foot Television Adaptation
- **Translucent Navigation Rail**: `Color(0xCC060A14)` to `Color(0x990D1527)` dark glass with a `1.dp` specular border (`Color.White.copy(alpha = 0.08f)`).
- **Focus Scaling**: `1.04x` to `1.08x` with D-pad spring response (`dampingRatio = 0.82f, stiffness = 380f`).
- **Focus Highlighting**: Glowing cyan-to-violet gradient stroke (`BorderStroke(1.5.dp, VantafynGradients.accentHorizontal())`) with atmospheric glow.
- **Hero Fade**: `BlendMode.DstIn` vertical hardware-accelerated hero dissolution into persistent background.
- **Pinned Bottom Actions**: Fixed utility dock for Search, Notifications, Settings, and safe App Exit.
