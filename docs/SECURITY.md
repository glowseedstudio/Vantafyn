# Vantafyn Security & Privacy Policy

Vantafyn is an open-source, privacy-first client for Jellyfin on Android and Android TV. We adhere to the principles of zero telemetry, local network boundaries, hardware-backed encryption, and minimal data exposure.

---

## 1. Supported Versions

Security fixes and maintenance patches are actively released for the latest production versions:

| Component | Supported Version | Status |
| :--- | :--- | :--- |
| **Vantafyn Mobile (Phone/Tablet/Auto)** | `>= 0.9.13` | :white_check_mark: Supported |
| **Vantafyn Android TV** | `>= 0.9.13` | :white_check_mark: Supported |
| **Vantafyn Companion (Jellyfin Plugin)** | `>= 0.1.0` (Jellyfin 10.11 & 12.0) | :white_check_mark: Supported |
| `< 0.9.13` | Older builds | :x: Unsupported (Upgrade recommended) |

---

## 2. Authentication & Credential Storage

- **Hardware-Backed Keystore Encryption**: All Jellyfin user access tokens, server session IDs, and secure secrets are encrypted at rest using **AES-256-GCM** backed by the `AndroidKeyStore`. Keys never leave secure enclave / TEE hardware.
- **Zero Password Persistence**: User passwords entered during login are used strictly for immediate server API authentication (`authenticateUserByName`) and are instantly wiped from memory. Passwords are never written to disk, SQLite databases, DataStore, SharedPreferences, crash dumps, or log output.
- **Log Privacy**: Access tokens, session secrets, pairing payloads, authorization headers, and typed credentials are explicitly scrubbed and forbidden from Android `Log`, Timber, and debugging utilities.

---

## 3. Local Network Privacy & Mobile-to-TV Security

### Mobile-to-TV Pairing
- **Strict LAN Isolation**: Mobile-to-TV pairing operates purely over the local area network (LAN). No cloud brokers, intermediate signaling servers, or external tracking services are involved.
- **Single-Use Ephemeral Codes**: Pairing codes generated on Android TV are 6 characters long and expire automatically after 300 seconds (5 minutes).
- **Brute-Force Rate Limiting**: The pairing server strictly limits failed verification attempts (maximum 4 tries) before permanently locking the session code and requiring a manual refresh.
- **Minimal Payload Principle**: The pairing payload transmits only the server URL, active user session token, and profile display metadata. Passwords, Ombi administrator secrets, and server master keys are never included.
- **Immediate Server Teardown**: The TV HTTP server and UDP discovery sockets terminate immediately upon receiving a valid payload, user cancellation, back navigation, or session timeout.

### Mobile-to-TV Remote Text Input
- **Active Focus Registration**: Only the actively focused text field on the TV accepts remote keystrokes. As soon as focus moves, the input receiver unregisters.
- **Sensitive Field Protection**: When typing into password or sensitive credential fields, the mobile device immediately clears input history upon transmission. Typed text is never persisted or cached.
- **No Automatic Submission**: Remote input only populates the text field on the TV; the user retains complete authority and must explicitly confirm actions via their TV remote.

---

## 4. Push Notifications & Companion Plugin Security

- **Open UnifiedPush Architecture**: Vantafyn does not use proprietary closed push relays, Firebase Cloud Messaging (FCM), or tracking analytics.
- **Self-Hosted Notification Routing**: Push notifications are routed exclusively through self-hosted UnifiedPush distributors (such as `ntfy.sh` or private `ntfy` servers) configured and owned by the server administrator or user.
- **No Secret Leaks in Push Payloads**: Push notification payloads contain high-level event metadata (e.g. badge unlock ID, sender username, room ID) without transmitting passwords, private media URLs, or authorization tokens in plain text.
- **Scoped Server-Side Storage**: In the Vantafyn Companion Jellyfin plugin, push registration endpoints are strictly tied to the authenticated Jellyfin user ID. Stale or deactivated endpoints are automatically pruned.

---

## 5. Third-Party Integrations & Admin Safeguards

### Ombi Requests
- **Admin Key Protection**: Normal server users never see or have access to the master Ombi API key.
- **Per-User Isolation**: When per-user Ombi authentication is enabled, requests are submitted strictly under each user's authenticated Ombi session token.

### Direct Streaming & Media Interception
- **Authenticated Stream URLs**: Media stream URLs utilize transient token query parameters supplied directly by the Jellyfin server.
- **TLS/HTTPS Enforcement**: Cleartext HTTP traffic is restricted; secure TLS communication is strongly recommended and enforced for non-LAN server connections. Custom and self-signed certificate trust options are isolated to user-explicit overrides.

---

## 6. Zero Telemetry & Privacy Guarantee

Vantafyn is 100% free and open source:
- **No Analytics SDKs**: Zero Google Analytics, Firebase Analytics, Segment, or telemetry frameworks.
- **No Advertising SDKs**: No ad networks, tracking pixels, or monetized data brokers.
- **No Background Phone-Home**: Vantafyn communicates only with the servers you explicitly enter (your Jellyfin server, optional Ombi instance, and your chosen UnifiedPush distributor).

---

## 7. Reporting a Vulnerability

We take the security and privacy of our community very seriously. If you believe you have found a security vulnerability in Vantafyn (Mobile, TV, or the Companion Plugin), please report it responsibly:

1. **Private Reporting**: Please **do not** report security vulnerabilities through public GitHub issues, discussions, or social media.
2. **Contact**: Submit a report via **[GitHub Private Security Advisory](https://github.com/glowseedstudio/Vantafyn/security/advisories/new)** or open a private report with the project maintainer.
3. **Information to Include**:
   - Component affected (Mobile app, Android TV, or Jellyfin Companion Plugin).
   - Detailed description of the vulnerability.
   - Steps or proof-of-concept (PoC) to reproduce the issue.
   - Any suggested remediations or patches if known.
4. **Response Timeline**:
   - We aim to acknowledge receipt of security reports within **48 hours**.
   - We will provide a status update and estimated resolution timeline within **7 business days**.
   - Coordinated public disclosure will occur only after a patch is published and release builds are made available.
