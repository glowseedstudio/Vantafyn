# Vantafyn UI System

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
- Focus scaling: `1.04x` to `1.08x` with D-pad spring response.
- `BlendMode.DstIn` vertical hardware-accelerated hero dissolution into persistent background.
- Glass panels: `Color(0xD910182A)` with `BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))`.
- Setup Reveal: Staggered top-to-bottom entrance using `CubicBezierEasing(0.19f, 1.0f, 0.22f, 1.0f)`.
- Welcome Button Pulse: Breathing `#21D8FF` / `#3E63FF` / `#8B35FF` gradient aura that halts on activation.
