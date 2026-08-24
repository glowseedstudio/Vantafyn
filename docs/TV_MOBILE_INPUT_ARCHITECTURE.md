# Vantafyn Mobile-to-TV Remote Text Input Architecture

## 1. Overview
Entering long URLs, complex passwords, search terms, and usernames using a D-pad remote on Android TV is cumbersome and error-prone. Vantafyn features a secure, real-time local network Remote Text Input bridge that allows users to type on their phone and instantly populate the actively focused text field on their Android TV.

---

## 2. Transport & Discovery
- **Discovery**: When the user opens the "Send text to TV" screen on mobile, it broadcasts a UDP discovery probe packet (`VANTAFYN_TV_DISCOVER_REQ:1`) on port `8766`.
- **TV Responder**: Any Android TV running Vantafyn in the foreground responds with a `TvDiscoveryBeacon` containing its device name and active HTTP port (default `8765`).
- **HTTP Payload**: Text is transmitted over a local HTTP `POST` request to `/api/v1/remote-input` using `TvRemoteInputPayload(text, fieldType, timestampEpochMs)`.

---

## 3. TV Target Focus Lifecycle
- **Active Focus Registration**: `VantafynTvTextField` automatically registers a `TvRemoteInputTarget` with `TvRemoteInputManager` upon gaining D-pad focus (`isFocused == true`).
- **De-registration**: As soon as focus moves away from the text field, the target is immediately unregistered.
- **Living Room Helper**: While a text field is focused on the TV, a subtle glass badge (*"Use your phone to type"*) with `Icons.Rounded.Smartphone` appears beneath the field.
- **No Active Target Rejection**: If no text field is currently focused on the TV, the TV cleanly returns `success = false` with `"No text field is currently focused on your TV"`.

---

## 4. Safety & Privacy Principles
- **No Password Retention**: Sent text is never stored in SharedPreferences, SQLite, Keystore, or history.
- **Zero Logging**: Typed text, especially credentials and passwords, is strictly forbidden from Logcat or debug statements.
- **Auto-Clear Sensitive Fields**: If the TV reports that the focused target is a sensitive password field (`isSensitive == true`), mobile immediately clears the input field upon transmission.
- **No Auto-Submit**: Receiving text on the TV only updates the focused field value. The user retains complete authority and manually presses "Connect", "Sign In", or "Search" using their remote.
- **Manual Typing Preserved**: On-screen TV keyboards and hardware remotes remain 100% functional.
