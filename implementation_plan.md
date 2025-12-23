# Implementation Plan: Trip Expense Tracker (Android)

**Objective:** Design and build a comprehensive Android application for tracking expenses during trips, featuring offline support, smart insights, and detailed settlement logic.

## 1. Technology Stack

*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (Modern, native UI toolkit)
*   **Architecture:** MVVM (Model-View-ViewModel) with Clean Architecture principles.
*   **Local Database (Offline Mode):** Room Database (SQLite abstraction).
*   **Dependency Injection:** Hilt (Dagger).
*   **Asynchronous Programming:** Kotlin Coroutines & Flow.
*   **Navigation:** Jetpack Navigation Compose.
*   **Image Loading:** Coil.
*   **JSON Parsing:** Kotlin Serialization / Gson.
*   **Maps:** Google Maps SDK for Android (for "Display expenses on a map").

## 2. Core Architecture

The app will follow a single-activity architecture (`MainActivity`) using Jetpack Navigation to switch between Composable screens.

### Layers:
1.  **Data Layer:**
    *   **Local:** Room DAOs (`TripDao`, `ExpenseDao`, `UserDao`) for SQLite access.
    *   **Repository:** `TripRepository`, `ExpenseRepository` - mediates between local DB and (future) remote data sources.
2.  **Domain Layer (Optional for complexity):** Use Cases (e.g., `CalculateSettlementUseCase`, `CreateTripUseCase`) to encapsulate business logic.
3.  **UI Layer:**
    *   **ViewModels:** Manage state (`TripListViewModel`, `ExpenseDetailViewModel`) and expose data via `StateFlow`.
    *   **Screens:** Composable functions (`TripListScreen`, `AddExpenseScreen`, `AnalyticsScreen`).

## 3. Data Models (Room Entities)

*   **Trip:** `id`, `name`, `startDate`, `endDate`, `currency`, `backgroundImage`.
*   **Person (Contact):** `id`, `name`, `phoneNumber` (part of a Trip).
*   **Expense:** `id`, `tripId`, `title`, `amount`, `currency`, `date`, `paidByPersonId`, `categoryId`, `description`, `receiptImagePath`, `location (lat/long)`.
*   **ExpenseSplit:** `expenseId`, `personId`, `amountOwed`.
*   **Category:** `id`, `name`, `icon` (Predefined + Custom).
*   **Budget:** `tripId`, `limitAmount`, `alertThreshold`.

## 4. Feature Implementation Strategy

### Phase 1: MVP (Must-Have)
*   **Trip Management:** Create/Edit Trips. Add Participants (manual or contact picker).
*   **Expense Entry:** Form to input amount, payer, split logic (Equal/Exact), and category.
*   **Expense List:** Daily summary view grouped by date.
*   **Settlement Engine:** Algorithm to minimize transactions (e.g., "Simplify Debts").
    *   *Algorithm:* Calculate net balance for each person. Match positive balances (creditors) with negative balances (debtors).
*   **Offline Mode:** Room DB ensures all data is local-first.

### Phase 2: Enhancements
*   **Dashboard/Widgets:** Quick summary of "You Owe" / "You are Owed".
*   **Smart Suggestions:** Auto-fill category based on title (simple heuristic or local ML).
*   **Budgeting:** Progress bar of Amount Spent vs. Budget Limit.

### Phase 3: Advanced
*   **Analytics:** Pie charts for categories (using a Compose charting library like Vico or MPAndroidChart).
*   **Maps:** Google Maps Composable to showing pins for expenses.
*   **OCR:** CameraIntent to capture receipt -> ML Kit Text Recognition -> Parse amount/date.

## 5. UI/UX Design System (Aesthetics)
*   **Theme:** Material Design 3 (Material You).
*   **Colors:** Dynamic color support (pulls from wallpaper) or a custom vibrant travel palette (Teal/Orange/White).
*   **Dark Mode:** Fully supported.
*   **Animations:** Hero transitions for opening trip details; Animated visibility for expanding expense items.

## 6. Development Workflow (Agentic)
Since I am an AI Agent without an Android Emulator:
1.  **Structure**: I will generate the directory structure and Gradle build files.
2.  **Code generation**: I will write the Kotlin code for Models, DAOs, Repositories, ViewModels, and UI Composables.
3.  **Validation**: I will rely on linting/static analysis concepts (though I cannot run `lint` directly without a full JDK/Android SDK setup environment, I will ensure code syntactic correctness).
4.  **Delivery**: The output will be a project folder ready to be opened in Android Studio.

**User Decision Point:**
To immediately *visualize* the design and logic, I can **also** build a React/Next.js PWA version that mimics the mobile experience. This allows interactive testing of the "Settlement Algorithm" and flows right here in the browser.

*Recommendation:* Proceed with Android Project generation, but offer PWA as a rapid prototype option.
