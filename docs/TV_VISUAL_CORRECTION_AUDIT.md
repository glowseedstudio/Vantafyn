# Vantafyn Android TV Visual Correction Audit

## 1. Identified Visual Discrepancies & Corrections

| Component / Layer | Prototype TV (Incorrect) | Correct Vantafyn Mobile Reference | Final TV Implementation |
| :--- | :--- | :--- | :--- |
| **App Background** | Flat programmatic CSS-like blue gradient (`#0E1320` -> `#070A12` -> `#050810`) | Cinematic dark nebula asset (`vantafyn_onboarding_nebula.png`) with atmospheric drift & Graphite scrims | `VantafynTvBackground` renders `R.drawable.vantafyn_onboarding_background` with TV overscan & vignette scrims |
| **Brand Logo / Icon** | Generic turquoise rounded square with white "V" letter | High-resolution official `vantafyn_logo.png` inside dark glass container with specular 1dp white border | Reusable `VantafynLogoBadge` with `CoreUiR.drawable.vantafyn_logo` |
| **Primary Buttons** | Flat cyan solid fill (`#00E5FF`) | 4-color horizontal accent gradient (`#31D7FF` -> `#5B8CFF` -> `#8B5CFF` -> `#C05CFF`) with specular border | `VantafynTvGlassButton(isPrimary = true)` uses `VantafynGradients.accentHorizontal()` with 1.06x focus scale |
| **Secondary Buttons** | Plain dark gray rectangle | Translucent dark navy glass (`#10182A`) with subtle border | `VantafynTvGlassButton(isPrimary = false)` with glass backing and cyan/violet focus edge |
| **Focus Indicator** | Simple cyan ring (`#00E5FF`) | Multi-stop gradient border (`VantafynGradients.accentHorizontal()`) with atmospheric backglow | `vantafynTvFocusable` modifier with `VantafynGradients.accentHorizontal()` |
| **Cards (Poster / Wide)** | Hard-edged dark boxes | Frosted glass surfaces (`VantafynColors.Surface` / `SurfaceHigh`) with progress indicators and specular borders | `VantafynTvPosterCard` / `VantafynTvWideCard` |
| **Setup Screens** | Plain forms with generic borders | Centered glass panels over deep nebula background | `TvWelcomeScreen`, `TvConnectServerScreen`, `TvLoginScreen`, `TvProfilePickerScreen` |
| **Sidebar** | Solid blue rail with "V" block | Dark navy glass over nebula, real logo, and user avatar at the top | `VantafynTvSidebar` with focusable user profile header |

---

## 2. Shared Tokens Directly Reused from `core-ui`
- `VantafynColors.Graphite` (`0xFF070A12`)
- `VantafynColors.Surface` (`0xFF141822`)
- `VantafynColors.SurfaceHigh` (`0xFF1B2030`)
- `VantafynColors.Ink` (`0xFFF5F7FF`)
- `VantafynColors.Muted` (`0xFFB8C0D8`)
- `VantafynColors.Primary` (`0xFF8EA2FF`)
- `VantafynColors.Secondary` (`0xFF62D6FF`)
- `VantafynGradients.accentHorizontal()`
- `CoreUiR.drawable.vantafyn_onboarding_background`
- `CoreUiR.drawable.vantafyn_logo`
