# Voice Journal MVP (Android)

A local-first Android Studio starter for a voice-first personal journal. This is a source project scaffold, not a signed APK.

## Included

- Native Android app built with Kotlin + Jetpack Compose
- Local Room/SQLite persistence
- Create, edit, and delete journal entries
- Keyword search over saved entries
- Speech-to-text entry through Android's system speech-recognition activity, with manual typing fallback
- Optional date/time reminders using Android alarms and notifications
- No custom backend, cloud database, analytics SDK, or AI provider integration
- Android backup disabled in the manifest to avoid implicit system backup of the journal database

## Requirements

- Android Studio with JDK 17
- Android SDK Platform 35
- Android Gradle Plugin downloads (the first Gradle sync needs internet)
- An Android device or emulator, API 26+

## Run

1. Unzip the project.
2. Open the `voice-journal-mvp` folder in Android Studio.
3. Allow Gradle sync to download the declared dependencies.
4. Run the `app` configuration on a device/emulator.

## Privacy and speech recognition

Journal text is stored in the app's local Room database. This MVP does not save raw audio files; it invokes Android's speech-recognition activity and receives text back. The actual recognition service and whether audio is processed on-device or sent to a provider depends on the device, language settings, and selected speech service. Do not treat this first scaffold as a guarantee of offline or end-to-end-private speech recognition. The app provides typed entry as a fallback.

The app asks no custom server to store entries. There is no cloud AI in this build. Add cloud AI only after selecting a provider, documenting data handling, and implementing explicit consent and secure credential handling.

## Known MVP limitations / next engineering steps

- Speech recognition is delegated to the installed Android speech service; an explicit on-device-first `SpeechRecognizer` flow with capability checks can replace this in a later iteration.
- Search currently uses SQLite `LIKE`; migrate to Room FTS5 for efficient ranked full-text search as the journal grows.
- Reminders use Android alarms. Notification permission behavior should be tested on API 33+ devices, and users may need to grant exact-alarm access for precise reminders.
- Editing a reminder does not yet cancel the previously scheduled alarm; add stable reminder IDs and cancellation in the next iteration.
- Add automated tests, database export/import, structured tasks and measurements, and transcript review/consent refinements before relying on this for important records.
- No AI question-answering, reports/charts, cloud sync, accounts, or backend are included yet.

## Suggested next milestones

1. Build and run on a real Android device; fix device-specific speech/notification behavior.
2. Add tests and database export/import.
3. Add structured fields for tasks, goals, activities, and measurements while preserving original text.
4. Add local summaries and reports.
5. Integrate opt-in cloud AI with source-entry citations and data minimization. Add a small Node.js/TypeScript backend only if needed for secure provider credentials, sync, or accounts.
