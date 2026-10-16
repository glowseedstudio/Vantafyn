# Vantafyn Project Rules & Standard Operating Procedures

## Core Development Rules
- **No Debug Builds**: NEVER compile or install debug APKs (e.g., `assembleDebug`). Always build and install `assembleRelease` (`./gradlew assembleRelease` or `./gradlew :app-mobile:assembleRelease :app-tv:assembleRelease`).
- **Target Device**: The user tests on their Retroid Pocket handheld device. When installing via ADB, use `adb -s 719e8105 install -r ...`.

---

## GitHub Release & Version Bump Workflow
Whenever the user asks to "bump versions across apks and plugins, build required files, commit all changes and upload to github as a new release", execute this exact 6-step checklist:

### Step 1: Bump Versions in Source Files
1. **`app-mobile/build.gradle.kts`**:
   - Increment `versionCode` by 1.
   - Bump `versionName` (e.g. `0.9.76` -> `0.9.77`).
2. **`app-tv/build.gradle.kts`**:
   - Set matching `versionCode` and `versionName`.
3. **`companion-plugin/src/Vantafyn.Plugin.Companion/Vantafyn.Plugin.Companion.csproj`**:
   - Increment patch version in `<Version>` (e.g. `0.2.40` -> `0.2.41`).
4. **`companion-plugin/scripts/build.sh`**:
   - Update `VERSION="${1:-0.2.41}"`.
   - Update `TAG="${2:-v0.9.77}"`.
   - Update `CHANGELOG` with a summary of the release changes.

### Step 2: Build Companion Plugin
Run the plugin build script to compile .NET 9 and .NET 10 packages and update `manifest.json`:
```bash
bash companion-plugin/scripts/build.sh
```

### Step 3: Build Android Release APKs
Run the release Gradle assemble tasks with `BypassSandbox: true`:
```bash
./gradlew :app-mobile:assembleRelease :app-tv:assembleRelease
```

### Step 4: Stage Release Directory
Create and populate `release/<tag>/` (e.g., `release/v0.9.77/`):
```bash
mkdir -p release/<tag>
cp app-mobile/build/outputs/apk/release/app-mobile-release.apk release/<tag>/
cp app-tv/build/outputs/apk/release/app-tv-release.apk release/<tag>/
cp companion-plugin/artifacts/Vantafyn.Plugin.Companion_<version>_net9.zip release/<tag>/
cp companion-plugin/artifacts/Vantafyn.Plugin.Companion_<version>_net10.zip release/<tag>/
cp companion-plugin/manifest.json release/<tag>/
```
Create `release/<tag>/RELEASE_NOTES.md` documenting the release changes.

### Step 5: Git Commit, Tag & Push
Stage all modified tracked project files, commit, tag, and push:
```bash
git add app-mobile/ app-tv/ companion-plugin/ core-*/ feature-*/ scripts/ GEMINI.md
git commit -m "feat(<scope>): <summary>, and <tag> release"
git tag -a <tag> -m "Release <tag> - <summary>"
git push origin main --tags
```
*(Requires `BypassSandbox: true`)*

### Step 6: Upload Release to GitHub
Upload the release notes and all 4 binary assets using the automated script:
```bash
python3 scripts/upload_release.py <tag>
```
*(Requires `BypassSandbox: true`)*

This script automatically pulls the GitHub personal access token from git credential storage, checks or creates the GitHub release, and streams all APKs and plugin zips directly to GitHub.
