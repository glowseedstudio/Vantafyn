# Vantafyn Security Model & Guidelines

## 1. Authentication & Token Storage
- **Keystore Encryption**: All Jellyfin user access tokens are encrypted with AES-256-GCM using hardware-backed keys provided by the Android Keystore (`AndroidKeyStore`).
- **No Password Persistence**: User passwords entered during login are used strictly for authentication against the Jellyfin server API (`authenticateUserByName`) and are immediately discarded from memory. Passwords are never written to disk, SQLite, SharedPreferences, or log output.
- **Log Privacy**: Access tokens, session secrets, pairing payloads, authorization headers, and passwords are never logged via Android `Log`, Timber, or external analytics.

---

## 2. Mobile-to-TV Pairing Security
- **Local Network Isolation**: Mobile-to-TV pairing operates strictly over the local network (LAN). No cloud brokers or external relay servers are involved.
- **Single-Use Ephemeral Codes**: Pairing codes generated on Android TV are 6 characters long and expire automatically after 300 seconds (5 minutes).
- **Brute-Force Rate Limiting**: The pairing server strictly limits failed attempts (maximum 4 tries) before permanently locking the session code and requiring manual refresh.
- **Minimal Payload Principle**: The pairing payload transmits only the server URL, active user session token, and profile display metadata. Passwords, Ombi API keys, and server admin secrets are strictly prohibited from the payload.
- **Immediate Server Teardown**: The TV HTTP server and UDP broadcast sockets terminate immediately upon receiving a valid payload, user cancellation, back navigation, or session timeout.

---

## 3. Playback & Network Interception Protection
- **Direct Stream Security**: Authenticated media stream URLs utilize transient token query parameters supplied directly by the Jellyfin server.
- **Custom Certificate Handling**: Cleartext HTTP traffic is disabled by default in production configurations, with secure TLS communication enforced for remote servers.
