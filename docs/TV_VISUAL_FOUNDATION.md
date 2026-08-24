# Vantafyn Android TV Visual Foundation

## 1. Unified Design Language (Mobile Parity)
The Vantafyn Android TV app directly derives its visual identity and design tokens from `core-ui`, sharing the same cinematic nebula background, obsidian glass surfaces, and vibrant cyan-to-violet accent gradients as the mobile app.

---

## 2. Background Architecture
- **Layer 1 (Cinematic Nebula)**: Renders `CoreUiR.drawable.vantafyn_onboarding_background` scaled `1.04x` with `ContentScale.Crop`.
- **Layer 2 (Base Dark Scrim)**: `Color.Black.copy(alpha = 0.52f)` ensures high contrast for TV viewing distances.
- **Layer 3 (Horizontal Readability Gradient)**: `Brush.horizontalGradient` with `VantafynColors.Graphite` (`0.72f` $\to$ `0.36f` $\to$ `Transparent`).
- **Layer 4 (Vertical Vignette Gradient)**: `Brush.verticalGradient` with `VantafynColors.Graphite` (`0.35f` $\to$ `Transparent` $\to$ `0.70f`).

---

## 3. Button System
- **Primary Glass Button (`isPrimary = true`)**:
  - Filled with `VantafynGradients.accentHorizontal()` (`#31D7FF` $\to$ `#5B8CFF` $\to$ `#8B5CFF` $\to$ `#C05CFF`).
  - Focused state: `1.06x` spring scale, specular border (`Color.White.copy(alpha = 0.55f)`), and atmospheric cyan/violet radial glow (`Color(0x388EA2FF)`).
- **Secondary Glass Button (`isPrimary = false`)**:
  - Translucent dark navy glass (`Color(0xCC141A2E)` $\to$ `Color(0xEE0B0F1C)`).
  - Focused state: `1.05x` spring scale and `VantafynGradients.accentHorizontal()` border.

---

## 4. Hardware-Accelerated Hero Fade
- `VantafynTvHero` applies `BlendMode.DstIn` on a vertical alpha gradient to dissolve the bottom 30% of the backdrop image directly into the persistent nebula background without hard rectangle edges.
