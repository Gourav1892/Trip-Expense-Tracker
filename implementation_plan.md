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

## 7. Immediate Fixes (Phase 1.5)

### [TripDetailsViewModel](file:///c:/Users/Acer/.gemini/antigravity/scratch/trip_expense_tracker/app/src/main/java/com/example/tripexpensetracker/ui/trips/TripDetailsViewModel.kt)
- **Goal**: Resolve pending TODO for resending invitations.
- **Change**: Implement `resendInvite` function to call `repository.resendInvitation`.

## Verification Plan

### Manual Verification
1.  Open the App and navigate to a Trip where you are the owner.
2.  Go to the "Participants" list (likely via a bottom sheet or settings).
3.  Find a participant with "Invited" status.
4.  Trigger the "Resend Invite" action (if UI visualizes it) or verify the ViewModel logic via logging if UI is incomplete.

## 8. Feature Implementation: Budgeting (Phase 2)

### Overview
Allow users to set a total budget for a Trip and visualize their spending progress.

### Data Model Changes
**Firestore Schema**:
-   Current `trips/{tripId}` document.
-   Add field: `budget: Double? = null` (Total budget limit).
-   Add field: `budgetAlertThreshold: Double? = null` (Optional, e.g., warn at 80%).

### Repository Updates
**[TripRepository](file:///c:/Users/Acer/.gemini/antigravity/scratch/trip_expense_tracker/app/src/main/java/com/example/tripexpensetracker/data/repository/TripRepository.kt)**
-   `updateTripBudget(tripId: String, budget: Double)`: function to update the specific field.

### UI Components
1.  **Trip Settings / Edit Trip Screen**:
    -   Add "Total Budget" Input field.
2.  **Trip Details Screen (Dashboard)**:
    -   Add a **Budget Progress Bar** (LinearProgressIndicator).
    -   Show text: "$X spent of $Y budget".
    -   Color logic: Green (<75%), Yellow (75-90%), Red (>90%).

### Verification
-   Set a budget of 1000.
-   Add expenses totaling 500. Verify bar is 50%.

## 9. Feature Implementation: Smart Suggestions (Phase 2)

### Overview
Automatically suggest or select the Expense Category based on the input Title.

### Implementation Details
**Keyword Matching Logic**:
-   Create `CategorySuggester` object.
-   Map keywords to categories:
    -   Food: "lunch", "dinner", "breakfast", "coffee", "cafe", "restaurant", "market", "groceries"
    -   Transport: "taxi", "uber", "bus", "train", "flight", "gas", "fuel", "parking"
    -   Lodging: "hotel", "airbnb", "hostel", "room"
    -   Entertainment: "movie", "cinema", "museum", "ticket", "tour", "park"

**ViewModel Integration**:
-   **[AddEditExpenseViewModel](file:///c:/Users/Acer/.gemini/antigravity/scratch/trip_expense_tracker/app/src/main/java/com/example/tripexpensetracker/ui/expenses/AddEditExpenseViewModel.kt)**:
    -   Observe `title` changes.
    -   If `title` contains a keyword AND current category is default ("General"), update `category`.

### Verification
-   Type "Uber" -> Category switches to "Transport".
-   Type "Dinner at Place" -> Category switches to "Food".

## 10. Feature Implementation: Analytics (Phase 3)

### Overview
Create a dedicated "Analytics" screen to provide deeper insights into trip spending.

### Components
1.  **Analytics Screen (`AnalyticsScreen.kt`)**:
    -   **Top Bar**: Date Range Filter (All Time, Last 7 Days).
    -   **Summary Cards**: Total Spent, Average Daily Spend.
    -   **Charts**:
        -   **Category Breakdown**: Reuse `PieChart`.
        -   **Payer Breakdown**: Reuse `BarChart`.
        -   **Daily Trend**: [NEW] Simple Line Chart showing spending over time.

### Implementation Steps
1.  **Create `AnalyticsScreen`**: Scaffold the UI.
2.  **Add Entry Point**: Add an "Analytics" button to `TripDetailsScreen` app bar.
3.  **Implement Logic**: Filter expenses based on date range.
4.  **Visualize**: Aggregate data for the charts.

### Verification
-   Open a trip with expenses.
-   Tap "Analytics".
-   Verify Pie Chart matches the main screen.

## 11. Refinements (Phase 4)

### 11.1 Budget Alerts
**Goal**: Allow user to define an alert threshold (e.g., 80%) and show a warning when reached.
-   **UI**: Add `Slider` or `TextField` for "Alert Threshold (%)" in `AddEditTripScreen`.
-   **Logic**: In `TripSummaryCard`, if `spent >= (budget * threshold)`, show warning icon/text.

### 11.2 Data Export
**Goal**: Share trip expenses as text/CSV.
-   **UI**: Add "Export" button in `TripDetailsScreen` (Action Bar).
-   **Logic**: Generate CSV string from expenses list -> `Intent.ACTION_SEND`.

### 11.3 Chart Polish
**Goal**: Improve Daily Trend visualization.
-   **Improvement**: Use a path-based `Canvas` drawing for a true Line Chart instead of reusing Bar Chart blocks.






## 12. Visual Polish (Phase 5)

### 12.1 Premium Trip Cards
**Goal**: Make the main list visually striking.
- **UI**: Use `Card` with `Brush.verticalGradient` or `Brush.horizontalGradient`.
- **Content**:
    -   Trip Name (Large, Bold, White text).
    -   Countdown: "X days to go" or "Ongoing".
    -   Budget Progress Bar: Thin, elegant indicator at the bottom.

### 12.2 Immersive Detail Screen
**Goal**: Create a modern, app-like feel for the details view.
- **UI**: Use `LargeTopAppBar` (Material 3) with `TopAppBarDefaults.exitUntilCollapsedScrollBehavior`.
- **Behavior**: Large title shrinks to standard toolbar size upon scrolling.

### 12.3 Category Icons
**Goal**: Replace text headers with visual icons.
- **Implementation**: Create a `CategoryIcon` helper composable.
    -   Maps category name string to `Icons.Default.*`.
    -   Uses a colored `Surface` (Circle) behind the icon.
