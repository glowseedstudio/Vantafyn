# TV Setup Animation Polish Specification & Audit

## 1. Overview & Mobile Animation Reference
The Vantafyn TV setup animation system mirrors the cinematic, restrained motion design language established in the mobile app (`feature-home/src/main/java/dev/vantafyn/feature/home/HomeScreens.kt`).

### Mobile Motion Parameters Referenced:
- **Cinematic Easing**: `CubicBezierEasing(0.19f, 1.0f, 0.22f, 1.0f)` (`VantafynSetupCinematicEasing`).
- **Screen Transitions**: Coordinated top-to-bottom slide and fade (`fadeIn(900ms, delay=120ms) + slideInVertically(initialOffsetY=28px)` with `fadeOut(480ms) + slideOutVertically(targetOffsetY=-20px)`).
- **One-Shot Content Reveal**: Staggered cascading reveal from top to bottom (Header/Logo at `0ms`, Action/Buttons at `220ms`).
- **Reduced Motion Support**: Instant, clean transitions when system animator duration or transition scales are disabled.

---

## 2. TV Implementation Architecture

### A. Initial 2-Second Background Pause
- On fresh launch of TV setup, the screen renders only the deep cosmic nebula background (`VantafynOnboardingBackground`) for **2.0 seconds** (`2_000ms`).
- The pause is guarded by `rememberSaveable { mutableStateOf(true) }` so back navigation, screen rotation, or configuration recompositions do not re-trigger the delay.
- Focus is suppressed while the screen is in the background-only state to prevent ghost focus traps.

### B. Welcome Screen One-Shot Reveal (`VantafynTvSetupReveal`)
- Once the 2-second background pause resolves, the Welcome screen content cascades in from top to bottom:
  1. **Top Header**: Logo badge, title, and subtitle reveal with `delayMillis = 0`.
  2. **Action Group**: Primary "Get Started" button reveals with `delayMillis = 220ms`.
  3. **Focus Assignment**: Focus lands directly on the "Get Started" button once content is revealed.

### C. Pulsing Glow on First Welcome Button (`VantafynTvPulsingButtonGlow`)
- Behind the initial "Get Started" button on `TvWelcomeScreen`, a soft, breathing radial glow aura is rendered.
- **Colors**: Cyan (`#21D8FF`), Royal Blue (`#3E63FF`), and Violet (`#8B35FF`).
- **Animation Cycle**: 2,400ms duration with `RepeatMode.Reverse` breathing between `1.02x`–`1.12x` scale and `0.25`–`0.58` alpha.
- **Lifecycle Safety**: The pulse permanently ceases immediately upon user activation/click, and does not render on any other setup screens, home cards, or background tasks.

### D. Screen-to-Screen Top-to-Bottom Transitions
- All TV setup screens (`TvWelcomeScreen`, `TvSetupMethodScreen`, `TvConnectServerScreen`, `TvServerConfirmScreen`, `TvLoginScreen`, `TvProfilePickerScreen`) transition seamlessly via `AnimatedContent`.
- **Forward Progression**:
  - Outgoing phase slides upward (`targetOffsetY = -20`) while fading out (`480ms`).
  - Incoming phase slides downward into view (`initialOffsetY = 28`) while fading in (`900ms`, delay `120ms`).
- **Backward Navigation**:
  - Outgoing phase slides downward (`targetOffsetY = 20`) while fading out (`360ms`).
  - Incoming phase slides downward from above (`initialOffsetY = -24`) while fading in (`700ms`, delay `80ms`).

---

## 3. Focus & D-pad Safety Guarantees
1. **No Invisible Controls**: No buttons, fields, or cards are focusable while hidden during the 2-second initial pause.
2. **Deterministic Focus Targets**:
   - `TvWelcomeScreen` focuses "Get Started".
   - `TvConnectServerScreen` focuses the Server URL text box.
   - `TvServerConfirmScreen` focuses "Continue".
   - `TvLoginScreen` focuses the **Password** field if username is pre-filled, or **Username** if blank.
   - `TvProfilePickerScreen` focuses the first profile card in the horizontal scroller.
3. **No Focus Traps**: D-pad navigation never gets trapped in transitioning or offscreen composables.

---

## 4. Performance & Rendering Principles
- **No Heavy GPU Shaders / Dynamic Blurs Per Frame**: Glows utilize pre-calculated multi-stop alpha radial gradients and hardware-accelerated layer scale transforms.
- **Clean Animation Disposal**: Pulsing transitions are attached solely to the Welcome composable lifecycle and disposed immediately upon phase advancement.
- **Shared Codebase Integrity**: All TV animation enhancements remain strictly isolated to `app-tv`, leaving mobile setup logic in `feature-home` and `app-mobile` unaffected.
