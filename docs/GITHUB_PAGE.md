# Vantafyn

Vantafyn is a premium Android client for Jellyfin, built for both Android phones and Android TV devices.

The goal is simple: make a private Jellyfin server feel like a polished streaming service without losing the control, privacy and flexibility that make self-hosted media worth using.

Vantafyn is designed around a calm, cinematic interface with strong readability, smooth motion, rich media artwork, family-friendly navigation and careful handling of real Jellyfin data. It is not trying to be a flashy server dashboard or a generic media browser. It is trying to feel like a refined home media app that belongs on a phone, a tablet, a TV and a living-room remote.

## What Vantafyn Is Trying To Achieve

Vantafyn exists to provide a modern Jellyfin experience with:

- A premium mobile interface for browsing, search, details, music, requests, downloads and playback.
- A TV-first experience for Android TV, Google TV and Fire OS.
- Real Jellyfin authentication, profiles, libraries, sessions and playback behaviour.
- A shared Kotlin core for Jellyfin API, session, media and integration logic.
- A consistent visual system across onboarding, home, libraries, details, music, downloads, requests and admin surfaces.
- Optional integrations that extend Jellyfin without making the base app dependent on them.

The project is intentionally Android-native. It uses Kotlin, Jetpack Compose, Compose for TV, AndroidX Media3 and the Jellyfin Kotlin SDK.

## Product Principles

Vantafyn is built around a few core principles:

- **Private by default**: no tracking, analytics or user profiling.
- **Human-readable design**: clean typography, clear controls and accessible contrast.
- **Premium without noise**: dark graphite surfaces, soft glass, refined motion and no cyberpunk styling.
- **Real data only**: no fake statistics, fake admin metrics or placeholder server behaviour pretending to be real.
- **TV-first where it matters**: D-pad focus, large-screen spacing and remote-friendly navigation.
- **Phone-first where it matters**: fast browsing, reliable playback, downloads, music controls and simple account switching.
- **Respect Jellyfin**: Vantafyn should work with normal Jellyfin servers and use official APIs wherever possible.

## Current State

Vantafyn's Android phone app is already in a highly usable state and is moving through final polish and hardening.

The mobile app already supports:

- Jellyfin login, Quick Connect and saved profile restore.
- Local and remote server endpoint fallback for the same Jellyfin server profile.
- Mobile home with real Jellyfin rows and configurable sections.
- Library browsing, filtering, pagination and media detail pages.
- Movie, episode, series and Live TV playback using AndroidX Media3.
- Music playback for long sessions with Android system media controls and minimal battery use.
- Downloads and offline browsing/playback.
- My List / Jellyfin favourites.
- Ombi requests integration.
- Watch Party and Swipe to Match discovery.
- Admin tools, active sessions, media statistics and server management surfaces.
- Companion Jellyfin plugin support for richer future features.

In daily use, the mobile app can browse libraries, play movies, TV series, music and Live TV, handle real Jellyfin sessions, manage downloads/offline playback, and expose a polished premium UI across the main phone experience.

The Android TV app is next. Its project structure and module boundaries are already in place, but the full TV experience still needs to be built out from the mobile foundation.

## Roadmap

### Near Term

- Finish remaining mobile polish and bug hardening.
- Add audiobook support.
- Add ebook support.
- Finalise Watch Party and Swipe to Match behaviour.
- Continue validating playback, downloads, offline mode, Quick Connect and local/remote server fallback.

### Request Services

- Keep Ombi support as an optional integration.
- Add Seerr service integrations so Requests is not tied to Ombi only.
- Keep per-user request behaviour and encrypted credential storage where the external service supports it.

### Android TV

- Bring the mobile foundation across to Android TV, Google TV and Fire OS.
- Build TV-first home rows, details, profile flow and playback controls.
- Tune spacing, focus states and remote navigation for couch-distance use.
- Keep TV design calm, readable and premium rather than simply scaling up the phone UI.

### Long Term

- Expand Vantafyn Companion plugin features where Jellyfin core APIs do not expose enough data.
- Explore richer household and family features.
- Continue improving statistics, offline browsing and advanced playback options where they make sense.
- Broader device testing across Android phones, tablets, Android TV, Google TV and Fire OS.

## Reference Projects

Vantafyn studies other Jellyfin and media clients for behaviour, architecture and UX ideas, including Wholphin, Findroid, Jellyfin Android TV, Finamp and others.

These projects are used as references only unless explicitly stated otherwise. Vantafyn does not copy GPL source into the application, and reference repositories are kept isolated from the Gradle project.

## Privacy And Trust

Vantafyn is intended for private media servers. It should not include analytics, trackers, telemetry or advertising SDKs.

The app stores only what it needs to connect to the user's Jellyfin server, such as server/profile data and authenticated session information. Passwords are not stored.

Where sensitive storage is involved, the goal is to keep that logic contained behind clear abstractions so it can be audited, hardened and improved without spreading secrets through the app.

## AI Assistance Disclosure

Vantafyn is an AI-assisted software project.

I want to be completely transparent about that. I have been developing software for around six years, and rather than building every part of Vantafyn entirely by hand, I use AI coding tools as part of my development workflow.

AI is primarily used to accelerate tasks such as:

- initial scaffolding and boilerplate
- basic feature implementation
- repetitive development work
- exploring possible solutions
- assisting with debugging and code review

This does not mean Vantafyn is blindly generated or "vibe coded."

I review the code that goes into the project, understand how it works, debug issues myself, manually optimise and refactor where necessary, and make the architectural, technical, UI and product decisions behind the application.

AI is an assistant, not the developer.

Used properly, I believe AI can be an extremely powerful development tool. The important part is having a strong understanding of the system you are building, knowing what you are asking the tool to do, and being capable of reviewing, correcting and improving what it produces.

Simply accepting generated code without understanding or validating it can lead to poor architecture, bugs, security problems and difficult-to-maintain software. That is not how Vantafyn is developed.

Ultimately, I am responsible for the code that ships.

I also understand that some people prefer not to use software developed with AI assistance, and that is completely their choice. I would rather be open about my development process than hide it.

Vantafyn is AI-assisted, human directed, human reviewed and human maintained.

## Status

Vantafyn is under active development and should be treated as pre-release software, but the Android phone app is already very usable.

The aim is to keep building carefully: real features, real Jellyfin behaviour, clean architecture, premium design and honest documentation.
