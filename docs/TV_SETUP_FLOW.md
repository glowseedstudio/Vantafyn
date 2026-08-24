# Vantafyn Android TV Setup Flow

## 1. Setup Architecture
The Android TV onboarding flow adheres to the exact visual style of Vantafyn mobile while optimizing form factor ergonomics for remote control usage.

---

## 2. Steps & Visual Treatment
1. **Initial Background Pause**:
   - 2.0-second background-only reveal displaying the cosmic nebula background on fresh launch before presenting interactive controls.
2. **Welcome Screen (`TvWelcomeScreen`)**:
   - Official Vantafyn logo badge (`vantafyn_logo.png`).
   - Title: *"Welcome to Vantafyn"*.
   - Subtitle: *"Your Jellyfin library, built for the living room."*
   - Staggered one-shot reveal (`VantafynTvSetupReveal`).
   - "Get Started" gradient action button featuring a soft, breathing cyan/blue/violet aura pulse (`VantafynTvPulsingButtonGlow`) that permanently stops on activation.
3. **Setup Method Screen (`TvSetupMethodScreen`)**:
   - "Pair with mobile app" (honest info modal) & "Sign in manually".
   - Glass selection cards with `VantafynGradients.accentHorizontal()` focus border.
4. **Connect Server Screen (`TvConnectServerScreen`)**:
   - Glass text input box with single focus-driven lift and server connection validation.
   - Clear network error messages formatted with `setupFriendlyError()`.
5. **Server Found Screen (`TvServerConfirmScreen`)**:
   - Verified server details on obsidian glass card featuring the server administrator's profile picture or styled initials fallback.
6. **Sign In Screen (`TvLoginScreen`)**:
   - D-pad friendly username & password fields with single focus lift and password reveal toggle.
   - Smart focus: automatically lands on the **Password** field if the user profile is pre-filled, or **Username** if blank.
7. **Who's Watching Screen (`TvProfilePickerScreen`)**:
   - Circular profile avatars and "Add Profile" card with `1.08x` focus scaling, gradient borders, unclipped gradient password badges, and cinematic horizontal edge fades.

---

## 3. Screen Transitions
- Setup state transitions use `AnimatedContent` with top-to-bottom entrance and exit slides combined with alpha fades using `VantafynTvSetupCinematicEasing` (`CubicBezierEasing(0.19f, 1f, 0.22f, 1f)`).
