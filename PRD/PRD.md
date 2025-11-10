# 📄 Product Requirements Document (PRD)

**Project Title:** VIPMail – Minimalist Multi-Account Email Client
**Platform:** Android (target SDK 34+)
**Development Environment:** Cursor (with Gradle & Android SDK)
**Language:** Kotlin
**Architecture:** MVVM + Repository Pattern
**UI Framework:** Jetpack Compose (Material 3)
**Database:** Room (SQLite)
**Networking:** IMAP / SMTP (via Jakarta Mail or similar)

---

## 1. 🎯 Project Goal

Build a **clean, modern, minimalist email client** supporting:

* Multiple IMAP email accounts
* Plain-text signature per account
* VIP contact management
* Notifications **only for emails from VIP senders**
* Basic email actions: read, reply, forward, send
* Simple, elegant Material 3 UI

Focus on **simplicity, privacy, and clarity** — no visual clutter or unnecessary settings.

---

## 2. 🧱 Core Features

### 2.1 Multi-Account Support

* Add multiple IMAP accounts manually (server, port, credentials)
* Store credentials securely (later encrypted, e.g., Android Keystore)
* Switch between accounts in UI via dropdown or navigation drawer

### 2.2 Email Retrieval (IMAP)

* Fetch messages from INBOX via IMAP
* Retrieve message headers (Subject, From, Date)
* Retrieve full body on demand
* Local caching via Room
* Sync interval managed by WorkManager (every X minutes)

### 2.3 Email Sending (SMTP)

* Compose and send plain-text emails via SMTP
* Support for “Reply” and “Forward” (quoted plain text)
* Automatically append account signature if enabled

### 2.4 VIP Contacts

* Mark sender addresses as VIP per account
* Maintain a `vip_senders` table (email + account reference)
* Highlight VIP senders in UI (subtle chip or color)
* Only VIP emails trigger notifications

### 2.5 Notifications

* Background sync checks for new messages
* Send system notifications **only** for VIP senders
* Notification channel: `VIP_MAILS` (high priority)

### 2.6 Signatures

* Plain-text signature per account (editable in settings)
* Optional enable/disable toggle
* Automatically inserted when composing new, reply, or forward messages

### 2.7 Compose Screen

* Clean, minimal Compose UI for writing messages
* Fields:

  * From (account selector)
  * To (text input)
  * Subject
  * Body (multiline)
* Actions:

  * Send
  * Discard (back navigation)
* Automatically appends signature

---

## 3. 🎨 Design Principles

### 3.1 Visual Design

* **Material 3** theme with **Dynamic Color (Material You)**
* Minimalist layout: whitespace, subtle dividers, neutral palette
* Consistent typography hierarchy (Title, Body, Caption)
* No custom icons unless necessary — use Material Icons Extended

### 3.2 UX Principles

* Fewer clicks, logical flow
* Clearly visible VIP senders
* Simple account switcher
* Smooth scrolling (LazyColumn for lists)
* Non-blocking sync (WorkManager + coroutine background tasks)

---

## 4. 🧩 Architecture & Project Structure

### 4.1 Architecture Overview

Pattern: **MVVM + Repository + Room + WorkManager**

```
com.vipmail/
│
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   ├── EmailAccountDao.kt
│   │   │   ├── EmailMessageDao.kt
│   │   │   └── VipSenderDao.kt
│   ├── model/
│   │   ├── EmailAccount.kt
│   │   ├── EmailMessage.kt
│   │   └── VipSender.kt
│   ├── repository/
│   │   ├── MailRepository.kt
│   │   └── AccountRepository.kt
│   └── network/
│       ├── ImapService.kt
│       ├── SmtpService.kt
│       └── MailParser.kt
│
├── domain/
│   ├── usecase/
│   │   ├── FetchEmailsUseCase.kt
│   │   ├── SendEmailUseCase.kt
│   │   └── ToggleVipUseCase.kt
│
├── ui/
│   ├── main/
│   │   ├── MainActivity.kt
│   │   ├── Navigation.kt
│   │   └── components/
│   ├── inbox/
│   │   ├── InboxScreen.kt
│   │   └── InboxViewModel.kt
│   ├── compose/
│   │   ├── ComposeScreen.kt
│   │   └── ComposeViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── AccountEditScreen.kt
│   └── vip/
│       ├── VipListScreen.kt
│       └── VipViewModel.kt
│
├── worker/
│   └── ImapSyncWorker.kt
│
├── utils/
│   ├── Extensions.kt
│   └── NotificationHelper.kt
│
└── App.kt
```

---

## 5. ⚙️ Technical Details

### 5.1 Dependencies

* **Jetpack Compose** (Material3, Navigation, ViewModel)
* **Room** (Database)
* **WorkManager** (Background sync)
* **Jakarta Mail** (or similar IMAP/SMTP lib)
* **Kotlin Coroutines / Flow**
* **AndroidX Preference** (for simple settings)
* **Accompanist System UI Controller** (optional for dynamic colors)

### 5.2 Target

* **minSdk:** 26 (Android 8.0)
* **targetSdk:** 34 (Android 14)
* **compileSdk:** 34

---

## 6. 🧠 Development Flow in Cursor

1. **Initialize project in Cursor**

   * Kotlin + Compose template (`com.vipmail`)
   * Add dependencies manually in `build.gradle.kts`
2. **Write code and structure using Cursor AI assistance**

   * Generate Room entities, DAOs, and Repositories
   * Compose screens and navigation
3. **Run builds via terminal:**

   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. **Optionally open project in Android Studio** for:

   * Emulator testing
   * Logcat debugging
   * Compose Preview

---

## 7. 🚀 Future Enhancements (Post-MVP)

* OAuth2 for Gmail/Outlook
* Push IMAP (IDLE)
* Rich-text composer (HTML + signatures)
* Attachment handling
* Unified inbox view
* PGP/S-MIME support
* Dark mode custom palette
* Jetpack Compose Desktop version (cross-platform)

---

## 8. ✅ MVP Completion Criteria

The first MVP release is considered *complete* when:

* User can add at least one IMAP account
* Inbox loads and displays messages from the server
* VIP contacts can be added/removed
* Notifications appear only for VIP senders
* User can compose, reply, and forward emails with signature
* Minimalistic UI is stable and consistent with Material 3 guidelines