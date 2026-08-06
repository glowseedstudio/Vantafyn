# WispBench Music Player Audit

Reference inspected:

`/home/glowseed/Documents/Project Folders/Backups/WispBench`

Useful concepts adapted:

- compact mini-player with artwork plus previous/play-next controls;
- delayed marquee behavior for long titles;
- full Now Playing route with large art, top actions, progress, and queue access;
- music-reactive background direction;
- queue as a first-class player surface.

Not brought over:

- YearForge/old branding;
- visualizer code;
- unrelated debug or tool screens;
- Flutter source files.

Vantafyn implementation is native Compose/Kotlin and keeps playback bound to the existing Media3 foreground service/controller.
