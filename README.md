# StudySphere

**Your entire academic life, organized in one place.**

StudySphere is a native, offline-first Android app for students, built with Kotlin and Jetpack Compose. It brings a study planner, assignments, Pomodoro focus timer, notes, spaced-repetition flashcards, exam tracker, analytics and reminders into one app. All data stays on your device.

## Features

- **Onboarding**: 4 intro screens plus setup (name, academic level, subjects, daily goal, theme). Shown only once.
- **Home dashboard**: greeting, daily progress ring, today's schedule, pending tasks, quick actions, weekly stats, upcoming exams. All numbers are calculated from stored data.
- **Planner and calendar**: day / week / month views, add/edit/delete/complete sessions, invalid time prevention, overlap warnings. The Calendar also shows task deadlines and exams.
- **Tasks**: priorities, statuses, deadlines, subjects, search, filters, sorting, overdue highlighting, delete confirmation.
- **Focus mode**: Pomodoro (25/5/15 or custom), pause/resume/reset/skip, subject and label. Runs in a foreground service using a persisted end time, so it stays accurate in the background and is restored after app recreation. Sound notification when a session ends.
- **Notes**: list or grid, search, pin, subjects, tags, autosave, character and word count.
- **Flashcards**: decks, cards, flip-to-reveal study mode, Easy/Medium/Hard rating, spaced repetition, shuffle, summary and study history.
- **Exam tracker**: countdowns, preparation progress, upcoming and past sections.
- **Analytics**: totals, streaks, weekly bars, monthly trend, subject donut, completed vs pending tasks, focus statistics.
- **Subjects**: colour and icon, detail page with related sessions, tasks, notes, decks and exams. Deleting a subject never deletes related items.
- **Notifications**: session, deadline, exam, daily reminders (WorkManager) and focus completion. Each category can be toggled.
- **Settings**: dark / light / system theme, goals, timer durations, JSON export / import (Storage Access Framework, validated, with confirmation), clear data, privacy info.

## Tech stack

Kotlin 1.9.24, Jetpack Compose (BOM 2024.06.00) with Material 3, Navigation Compose, MVVM with ViewModel and StateFlow, Room 2.6.1 (KSP), WorkManager, DataStore Preferences, kotlinx.serialization (backups), Gradle Kotlin DSL, Android Gradle Plugin 8.5.2. minSdk 26, targetSdk/compileSdk 34.

## Architecture

```
UI (Compose screens) -> StudyViewModel -> StudyRepository -> Room (single source of truth)
                                      \-> PrefsRepository -> DataStore
TimerEngine (persisted state) <- TimerService (foreground) ; ReminderWorker (WorkManager)
```

## Project structure

```
app/src/main/java/com/studysphere/app/
  data/          Room entities, DAO, database, repositories, backup
  domain/        Pure logic: streaks, analytics, task/session rules, spaced repetition, timer math
  focus/         TimerEngine + TimerService
  notifications/ Notification channels and ReminderWorker
  ui/theme/      Colours, typography, shapes
  ui/components/ Design system (cards, fields, sheets, charts, rings)
  ui/navigation/ Navigation graph and bottom bar
  ui/screens/    Onboarding, Home, Calendar/Planner, Tasks, Focus, Notes, Flashcards, Exams, Analytics, Subjects, Profile/Settings
  viewmodel/     StudyViewModel
  util/          Date helpers
app/src/test/        Unit tests (streaks, timer math, tasks, sessions, spaced repetition, analytics, backup)
app/src/androidTest/ Room database tests
```

## Requirements

Android Studio Koala (2024.1.1) or newer, JDK 17 (bundled with Android Studio), Android SDK 34, an Android 8.0+ phone or emulator.

## Run it

1. Open Android Studio, choose **Open**, select the `StudySphere` folder.
2. Let Gradle sync finish (first sync downloads dependencies; Android Studio fetches Gradle 8.7 from `gradle/wrapper/gradle-wrapper.properties`).
3. Select a device (emulator, or a phone with USB debugging) and press **Run**.

Build an APK: **Build > Build Bundle(s) / APK(s) > Build APK(s)**. The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Tests

```
./gradlew testDebugUnitTest          # unit tests
./gradlew connectedDebugAndroidTest  # database tests (device/emulator needed)
```

## Database

Room database `studysphere.db` (version 1): subjects, study_sessions, tasks, notes, flashcard_decks, flashcards, flashcard_reviews, examinations, focus_sessions. Items reference subjects with `ON DELETE SET NULL`; cards and reviews cascade with their deck. Profile and preferences live in DataStore. Migrations go in `AppDatabase.MIGRATIONS`; destructive migration is never used.

## Permissions

- `POST_NOTIFICATIONS`: reminders and timer alerts (Android 13+).
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE`: keeps the focus timer running in the background.
No internet permission. Exported backups are plain JSON, so store them safely.

## Troubleshooting

- *Gradle sync fails*: check internet access, then **File > Invalidate Caches > Restart**; make sure Android Studio uses JDK 17.
- *SDK not found*: install Android SDK 34 via SDK Manager.
- *No timer notification*: allow notifications for StudySphere in Android settings.
- *Reminders arrive late*: battery optimisation can delay WorkManager jobs; they are not exact by design.

## Screenshots

Add screenshots to a `screenshots/` folder and link them here.

## Future ideas

Optional encrypted cloud sync, home-screen widgets, rich-text notes, per-subject goals, localisation.
