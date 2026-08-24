# Vantafyn Android TV Setup Flow

## 1. Setup Architecture
The Android TV onboarding flow adheres to the exact visual style of Vantafyn mobile while optimizing form factor ergonomics for remote control usage.

---

## 2. Steps & Visual Treatment
1. **Welcome Screen (`TvWelcomeScreen`)**:
   - Official Vantafyn logo badge (`vantafyn_logo.png`).
   - Title: *"Welcome to Vantafyn"*.
   - Subtitle: *"Your Jellyfin library, built for the living room."*
   - Default-focused "Get Started" gradient action button.
2. **Setup Method Screen (`TvSetupMethodScreen`)**:
   - "Pair with mobile app" (honest info modal) & "Sign in manually".
   - Glass selection cards with `VantafynGradients.accentHorizontal()` focus border.
3. **Connect Server Screen (`TvConnectServerScreen`)**:
   - Glass text input box with server connection validation.
   - Clear network error messages formatted with `setupFriendlyError()`.
4. **Server Found Screen (`TvServerConfirmScreen`)**:
   - Verified server details on obsidian glass card.
5. **Sign In Screen (`TvLoginScreen`)**:
   - D-pad friendly username & password fields with password reveal toggle.
6. **Who's Watching Screen (`TvProfilePickerScreen`)**:
   - Circular avatars with `1.08x` focus scaling, gradient borders, and password protection badges.
