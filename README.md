# PrioBox – Minimalist Multi-Account Email Client

PrioBox is an Android email client designed around clarity and focus. It supports multiple IMAP accounts, VIP-only notifications, and a minimalist Jetpack Compose interface. The feature set and architectural goals are driven by the accompanying [PRD](PRD/PRD.md).

## Project Highlights

- Multi-account IMAP/SMTP support with account-specific signatures
- Room-based local cache wired to a repository + use-case domain layer
- VIP sender management with high-priority notifications
- Background WorkManager sync that only alerts for VIP senders
- Material 3 Compose UI following minimalist design principles

## Tech Stack

- Kotlin, Jetpack Compose (Material 3)
- MVVM + Repository pattern
- Room, WorkManager, Hilt (KSP-backed)
- Jakarta Mail (`android-mail` / `android-activation`)

## Getting Started

1. Install the Android SDK (compile/target 34, minSdk 26) and Java 17+.
2. From the project root, run:
   ```bash
   ./gradlew assembleDebug
   ```
3. Deploy the generated APK to a device or emulator:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. Configure at least one IMAP/SMTP account via the in-app settings screen.

## Project Structure

```
app/
├── src/main/java/com/priobox/
│   ├── App.kt
│   ├── data/
│   ├── domain/
│   ├── ui/
│   ├── utils/
│   └── worker/
└── src/main/res/
```

See the PRD for a complete breakdown of requirements, design pillars, and future enhancements.


