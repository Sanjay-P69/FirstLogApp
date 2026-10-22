# FirstLog: instructions for Dad (Windows + Antigravity)

An APK is the Android app installation file. Building turns this project's files
into an APK. You do not need to understand the programming to follow these steps.

## 1. Get the files

Ask Devasy for the project folder, or clone
https://github.com/Sanjay-P69/FirstLogApp.git using Git. A clone includes the Git
history needed by the formatting checks; GitHub's Download ZIP does not.
Use a short folder such as `C:\Projects\FirstLogApp`, outside OneDrive.
Open Antigravity, choose Open Folder, and select that folder.

If Devasy sent `FirstLog-Dad-PRIVATE.zip`, extract it privately, then copy its
`.env`, `private-signing` folder, and `instructions.md` into the project root
(beside `gradlew.bat`). It is a private supplement to the Git clone, not the
complete app source. Windows Explorer may hide `.env`; enable hidden items.

For a signed release, also ask for these PRIVATE files through a trusted private
transfer: `.env` and `private-signing/firstlog-release.jks`. Keep a separate secure
backup. The key identifies the app for future updates; never replace or regenerate
it. The base64 file is another representation of the same private key.

Do not share the GitHub account token. Before sending `.env`, remove any line
starting `FATHER_ACCOUNT_GH=`. This token is only for GitHub administration and is
not required to build. Do not paste `.env` or key contents into Antigravity chat.

## 2. Install the tools once

Use a PC where your employer allows development tools. If installation is blocked,
ask your IT team; do not disable company security settings.

1. Install Git for Windows from https://git-scm.com/downloads/win.
2. Install Eclipse Temurin **JDK 21.0.8+9**, Windows x64, from
   https://adoptium.net/temurin/archive/?version=21. Select the archive version
   explicitly, rather than the latest Java release. Enable JAVA_HOME in its installer.
3. Install Android Studio from https://developer.android.com/studio. Start it once
   and finish the setup wizard. Android Studio supplies the Android SDK; you can
   continue using Antigravity to work on the project.
4. In Android Studio's SDK Manager, install Android SDK Platform **36**. In SDK
   Tools enable Show Package Details and install Android SDK Build-Tools **36.0.0**,
   Android SDK Command-line Tools, and Platform-Tools. Note the SDK Location shown
   there. Accept the license prompts.
5. Install Python 3 from https://www.python.org/downloads/windows/ and enable
   Add Python to PATH. Close and reopen Antigravity after installing tools.

## 3. Set the two paths

In the project root, use the private `.env` for a release. For a debug/test build,
copy `.env.example` to `.env` instead; no key or password is needed.
Open `.env` as a text file and change only `JAVA_HOME` and `ANDROID_HOME` to the
folders on your PC. Examples:

```dotenv
JAVA_HOME=C:/Program Files/Eclipse Adoptium/jdk-21.0.8.9-hotspot
ANDROID_HOME=C:/Users/Dad/AppData/Local/Android/Sdk
```

The Java folder must contain `bin/java.exe`; use the real installed folder name.
Use forward slashes, no surrounding quotes, and no spaces around `=`. Leave
passwords exactly as supplied. Keep the key at
`private-signing/firstlog-release.jks` under the project folder. Gradle does not
load `.env` automatically: always use the helper commands below.

## 4. Make the app

Open Antigravity's terminal and select PowerShell. Make sure its current folder
is the project folder (the folder containing `gradlew.bat`). Paste ONE command:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1 debug
```

This sets the execution policy only for this helper process. If your company
blocks scripts, ask IT. The first build downloads dependencies and can take a
while. Wait for `BUILD SUCCESSFUL` and the helper's `Success` message.
The test APK is `app/build/outputs/apk/debug/app-debug.apk`.

To make the signed release using the supplied private key:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1 release
```

The APK is `app/build/outputs/apk/release/app-release.apk`. The helper also checks
its signature. Transfer the APK to your Android phone and open it. Android may
ask you to allow this particular file source to install apps.

A release key cannot update an app installed with a different/debug key. Do not
uninstall an existing app casually: that can delete its recorded data. Ask Devasy
before changing an existing installation. No Play Store publication is performed.

## 5. Repeat and verify

To replicate a particular build, both PCs must use the same Git commit (revision),
the same tool versions, and the same release key. Do not ask the AI to upgrade
dependencies while replicating. Ask Devasy for the commit, then have Antigravity
check it out without discarding any local changes.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1 check
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1 reproduce
```

`check` runs formatting, static analysis, debug/release builds, both unit-test
variants, and both lint variants. `reproduce` builds the unsigned release twice
from clean outputs with task/build caches disabled. It writes both APKs and
`result.json` into `reproducibility-output/`; `match: true` means their SHA-256
checksums match. This check does not sign or distribute APKs.

Two matching builds on one machine do not prove all Windows/Linux PCs produce
identical bytes. The GitHub check runs on Ubuntu 24.04. Dependency files are not
yet checksum-verified or locked transitively, and the hosted runner/SDK platform
can receive updates. This is a repeatability check, not a fully hermetic build.
Debug APK checksums can differ because each PC uses its own debug signing key.

## 6. GitHub signing setup (repository administrator)

Open https://github.com/Sanjay-P69/FirstLogApp/settings/environments. Open or create
the environment named **release**. Restrict deployment branches to **main**.
Under Environment secrets, add these four names exactly:

| Secret name | Where to copy the entire value from |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | `private-signing/ANDROID_KEYSTORE_BASE64.txt` (one long line) |
| `ANDROID_KEYSTORE_PASSWORD` | The value after `ANDROID_KEYSTORE_PASSWORD=` in private `.env` |
| `ANDROID_KEY_ALIAS` | `firstlog-release` |
| `ANDROID_KEY_PASSWORD` | The value after `ANDROID_KEY_PASSWORD=` in private `.env` |

Do not add `ANDROID_KEYSTORE_PATH` as a secret: GitHub creates its own temporary
path. These are signing passwords, not your GitHub login password. Never put
them in repository source, Actions variables, issue comments, or screenshots.
GitHub does not reveal saved secret values; retain the private backup.

After the workflow changes are merged into main, open the repository's Actions
tab, select **Release APK**, then Run workflow on main. Or a push/merge to main
runs it automatically. Open the successful run, scroll to Artifacts, download
`release-apk-signed-...`, unzip it, and use its APK. An `unsigned` artifact is not
directly installable. Artifacts expire after 14 days, so retain distributed APKs.
The Android checks workflow also uploads a `reproducibility-...` comparison report.

## 7. A safe prompt to paste into Antigravity

> Read AGENTS.md, instructions.md, and docs/CI.md. Help me build this existing
> Android project on Windows. I am a beginner. Check that Temurin 21.0.8+9 and
> Android SDK platform 36 / Build-Tools 36.0.0 are installed. Help me set the two
> local paths without displaying any passwords or tokens. Run scripts/build.ps1
> debug first and explain any error in plain English. Do not upgrade dependencies,
> create a new release key, discard changes, uninstall my app, commit private files,
> or publish anything. Use my existing private signing key only when I request
> the release build. Report which commands actually passed.

## If something fails

- Java/SDK not found: check the first two paths in `.env` and restart the terminal.
- Download failure: check internet/proxy with IT; do not turn off certificate checks.
- Formatting baseline missing: use a full Git clone, not Download ZIP.
- Signature/update error: ask Devasy to check the installed app's signing identity.
- Other build errors: share the first error message, with passwords/tokens removed.
  Do not accept an AI saying a build passed unless the command actually succeeded.

References: [GitHub secrets](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets),
[Gradle daemon selection](https://docs.gradle.org/current/userguide/gradle_daemon.html),
[Android command-line builds](https://developer.android.com/build/building-cmdline).
