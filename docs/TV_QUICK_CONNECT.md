# Vantafyn Android TV Quick Connect Architecture

## 1. Overview
Jellyfin Quick Connect allows users on Android TV to sign in seamlessly by generating a short-lived PIN code on the TV and authorizing it from another authenticated Jellyfin client (such as Vantafyn Mobile, Jellyfin Web, or the mobile browser) without typing passwords on the TV.

---

## 2. Shared Protocol & SDK Architecture
Android TV setup reuses the exact same robust `JellyfinQuickConnectRepository` implementation (`SdkJellyfinQuickConnectRepository`) and state machine as Vantafyn mobile:

- **Initiate**: Calls `quickConnectApi.initiateQuickConnect()` on the target server to obtain an ephemeral secret and display code.
- **Poll**: Polls `quickConnectApi.getQuickConnectState(secret)` every 2 seconds for up to 120 seconds.
- **Authenticate**: Once approved, calls `userApi.authenticateWithQuickConnect(secret)` to acquire the Jellyfin user access token, securely writes it into hardware Keystore storage, and transitions to the TV Home/Who's Watching experience.

---

## 3. UI & Visual Craftsmanship
- **Placement**: Offered on `TvSetupMethodScreen` as the middle option ("Quick Connect") between "Pair with mobile app" and "Sign in manually", as well as directly accessible from `TvLoginScreen`.
- **Animated Gradient Border**: The TV code card features `Modifier.vantafynAnimatedModalBorder(cornerRadius = 24.dp, strokeWidth = 2.dp)` utilizing Vantafyn's signature cyan/royal blue/deep violet palette with continuous hardware-accelerated motion.
- **Couch Ergonomics**: Formatted with large, high-contrast monospace typography (`42.sp`) visible from living room viewing distance.
- **D-Pad Navigation**: Provides clear remote-friendly actions: **Refresh Code** (primary action on expiry), **Sign In Manually**, and **Back**.

---

## 4. Security & Safety Principles
- **No Password Storage**: Passwords are never queried, transmitted, or written to disk.
- **No Secret Logging**: Quick Connect secrets and access tokens are strictly forbidden from log output.
- **Lifecycle Cleanliness**: Polling coroutines automatically terminate upon screen disposal, back navigation, or timeout.
