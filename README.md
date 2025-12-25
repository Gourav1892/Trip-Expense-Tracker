# Trip Expense Tracker - Android App

This project is a comprehensive Trip Expense Tracker designed for Android using modern development practices.

## Prerequisites
*   **Android Studio:** Hedgehog (2023.1.1) or newer.
*   **Java:** JDK 17 or newer.
*   **Android SDK:** API Level 34 (Android 14) recommended.

## Tech Stack
*   **Kotlin**e
*   **Jetpack Compose** (UI)
*   **Room Database** (Local Storage/Offline)
*   **Hilt** (Dependency Injection)
*   **Coroutines & Flow** (Async)

## Setup Instructions
1.  Open Android Studio.
2.  Select **Open** and navigate to this directory (`trip_expense_tracker`).
3.  Allow Gradle to sync and download dependencies.
4.  Connect an Android device or start an Emulator.
5.  Run the application.

## Features
*   **Trip Management**: Create trips, add friends, set budgets.
*   **Expense Tracking**: detailed split options (Equal, Percentage, Shares).
*   **Settlements**: 
    *   Automatic debt calculation (Who owes whom).
    *   **Partial Payments**: Record cash payments ("Settle") to reduce debt instantly.
*   **Analytics**: Visualize spending by Category or Member.
*   **City Itinerary**: Plan visits, add destinations, and track expenses per city.

## Download
The latest debug APK is available in the root directory:
[Download APK](app-debug.apk)

## Current Status
*   [x] Project Structure Initialized
*   [x] Core Database Models (Trip, Expense, User)
*   [x] Repositories & Business Logic
*   [x] UI Screens (Compose)
*   [x] Partial Settlements & Payments

