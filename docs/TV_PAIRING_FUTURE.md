# Vantafyn TV Mobile Pairing Architecture (Future Roadmap)

## 1. Concept
To streamline initial TV onboarding, a future update will introduce a secure local/cloud pairing channel between the Vantafyn Mobile app and Vantafyn Android TV.

```mermaid
sequenceDiagram
    participant TV as Android TV App
    participant Companion as Jellyfin Companion Plugin / Signal Server
    participant Mobile as Vantafyn Mobile App

    TV->>Companion: Request New Pairing Session (Generates 6-Digit Code & QR)
    Companion-->>TV: Returns Session ID + Public Encryption Key
    TV->>TV: Displays 6-Digit Code & QR Code on TV Screen
    Mobile->>Mobile: User taps "Pair Android TV" & Scans QR or Types Code
    Mobile->>Companion: Submits Server URL + Encrypted User Token
    Companion-->>TV: Pushes Encrypted Credentials via WebSocket / SSE
    TV->>TV: Decrypts Token with Session Private Key
    TV->>TV: Validates Session & Transitions to TV Home
```

---

## 2. Security Requirements
1. **End-to-End Cryptography**: Ephemeral Diffie-Hellman / ECDH key exchange between TV and phone to prevent credential interception over local networks.
2. **Session Expiry**: Pairing codes must expire within 120 seconds.
3. **Explicit Confirmation**: Mobile user must confirm the target TV device name before sending access tokens.

---

## 3. Current Implementation Status
Because this protocol requires companion plugin support and cryptographic session negotiation, **no mock or fake pairing codes** are displayed in the current production build. Selecting "Pair with mobile app" directs users to manual sign-in with an informational notice.
