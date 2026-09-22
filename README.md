PROTEA TRAVELS
Project Report
Purpose, Design Considerations, Version Control & CI/CD, and Use of AI Tools


Android Application — Kotlin · Jetpack Compose · Firebase
Prepared for module assessment submission

1. Introduction and Purpose of the Application
Protea Travels is a native Android application that lets a traveller search, compare and book flights, manage a personal trip list, and track price drops on routes they care about. It was built as a complete, working example of a mobile app backed by a real online service, covering account creation, a persisted user profile, a searchable flight catalogue, and full create-and-delete management of bookings and price alerts.

The application targets a South African traveller who wants a fast, curated booking experience rather than a generic international comparison site. The interface, copy and sample routes (Johannesburg, Cape Town, Durban and international connections such as London, Zanzibar and Dubai) are written from that point of view, and the “curated” positioning — a small set of hand-picked deals rather than an exhaustive search engine — is intentional: it keeps the amount of information on each screen small enough to be usable on a phone.

Functionally, the app demonstrates:

● Account registration and login against an online authentication service, with the password never stored or handled by the app's own code.
● A settings area that changes and persists user preferences (theme, currency, default departure airport, notification preference) to the cloud, not just to the device.
● A flight search with return/one-way trips, date validation, passenger count and cabin class, reading live data from an online database.
● A full booking lifecycle: search → results → confirm → view in “My Trips” → cancel, each step reflected immediately in the online database.
● Price alerts that can be created and removed, stored per user.
2. Design Considerations
2.1 Visual identity
The interface uses a three-colour palette: deep navy for structure and trust, a muted gold for calls to action and pricing, and green for confirmation and success states. The palette was chosen to read as premium and South African — the gold references both the currency (Rand) and the protea flower the app is named after — rather than defaulting to a generic blue “tech” palette. Rounded cards, a consistent 22–28px corner radius, and a cream background (rather than plain white) were used throughout so the app has a distinct, warm identity instead of looking like an unstyled Material Design template.

   
Figure 1. Home screen (left) and sign-in screen (right), showing the navy/gold/green palette and card-based layout.


2.2 Information architecture and navigation
New users move through a fixed, linear path — splash, welcome, register or sign in — that cannot be skipped, since every other screen depends on being authenticated. Once signed in, the app switches to a three-tab bottom navigation bar (Home, Trips, Settings), which was chosen over a side drawer because the app only has three top-level destinations and a bottom bar keeps them reachable with one thumb on a large phone. Search and Results sit above the tabs, as a temporary flow launched from Home, rather than being a fourth permanent tab, because they represent a task in progress rather than a place the user returns to.

2.3 Architecture
The app follows a single-source-of-truth MVVM pattern. One ViewModel (AppVM) holds all UI state as Compose state objects; every screen reads from it and calls its functions, and never talks to Firebase directly. A separate Repo object is the only class that imports the Firebase SDK, so the persistence layer is isolated behind a small, readable API (register, login, searchFlights, addBooking, and so on). This separation means the backend could be replaced — for example with a hand-written REST API — by rewriting Repo.kt alone, without touching any screen.

class AppVM : ViewModel() {
   var search by mutableStateOf(SearchParams())
   var results by mutableStateOf<List<Flight>>(emptyList())
   ...
   fun runSearch(onDone: () -> Unit) {
       // validation happens here, in the ViewModel,
       // never inside a Composable
       ...
       run { results = Repo.searchFlights(s.from, s.to); onDone() }
   }
}
Figure 2. Excerpt from AppVM.kt, showing UI state and validation kept out of the Composable screens.

2.4 Data model and security
Passwords are never handled by the app's own code. Registration and login call Firebase Authentication, which performs the hashing and storage; the client only ever holds a short-lived session token. Firestore, the online database, is organised as a flights collection (readable by any signed-in user) and a per-user document at users/{uid} with bookings and alerts stored as sub-collections beneath it. Firestore security rules enforce request.auth.uid == uid on every read and write to a user's own data, so one account can never read or modify another account's bookings, even if it guessed the document path.

This structure was chosen over a single flat collection specifically so the security rule could be a single, auditable line per resource, rather than a rule that has to inspect the content of every document to decide who owns it.

2.5 Input validation and resilience
Every user-facing form validates before it calls the network, so a mistake produces a clear message instead of a silent failure or a crash. Examples implemented in the app:

● Registration rejects a password under 8 characters and a mismatched confirmation before calling Firebase.
● Search rejects an empty origin or destination, a departure date in the past, and a return date before the departure date.
● Every Firebase call is wrapped in a try/catch in the ViewModel, so a network failure surfaces as a short message rather than closing the app.
2.6 Accessibility and usability
Tap targets on primary actions are a minimum of 56dp tall, text contrast was checked against both the light and dark colour schemes, and the app fully re-themes for dark mode rather than only dimming the status bar. Because the dark-mode preference is saved to Firestore rather than to local device storage, it follows the user's account to a different phone, which was treated as a usability requirement rather than a cosmetic one.

3. Version Control and CI/CD: GitHub and GitHub Actions
The project is structured to be pushed directly to a GitHub repository and built automatically by GitHub Actions, so that “hosted online” applies to the codebase and its build process, not only to the Firebase backend.

3.1 Repository structure
The repository root contains the standard Gradle project layout (settings.gradle.kts, the root and app build.gradle.kts files, the Gradle wrapper) plus three additions kept outside the app/src tree: a README.md for setup instructions, a mockups/ folder holding the design reference images, and a .github/workflows/ folder holding the CI configuration described below. A .gitignore excludes build/, .gradle/ and, critically, app/google-services.json, so the Firebase project key is never committed to source control.

3.2 Branching and commit practice
The recommended workflow for this project is a short-lived feature-branch model: a branch per feature or fix (for example feature/price-alerts or fix/date-validation), a pull request into main once the feature builds and has been tested on a device or emulator, and a squash-merge so main keeps one clean commit per feature. Commit messages describe the change in the imperative mood (“Add price-alert delete button” rather than “Fixed stuff”), which keeps git log readable as a running history of the project for the report and for anyone marking it.

3.3 Continuous integration with GitHub Actions
A workflow at .github/workflows/android-ci.yml runs automatically on every push and pull request to main. It checks out the code, installs JDK 17, restores a cached Gradle dependency directory (so subsequent runs are faster), reconstructs app/google-services.json from a repository secret, then runs Android Lint and a debug build, finishing by uploading the resulting APK as a downloadable build artifact.

on:
 push:
   branches: [ "main" ]
 pull_request:
   branches: [ "main" ]
 
jobs:
 build:
   runs-on: ubuntu-latest
   steps:
     - uses: actions/checkout@v4
     - uses: actions/setup-java@v4
       with: { distribution: 'temurin', java-version: '17' }
     - run: echo '${{ secrets.GOOGLE_SERVICES_JSON }}' > app/google-services.json
     - run: ./gradlew lintDebug
     - run: ./gradlew assembleDebug
Figure 3. Core steps from .github/workflows/android-ci.yml (see the file itself for the complete, working version).

This gives the project two concrete benefits beyond what runs locally on one machine. First, it is an automatic, independent check that the app still compiles after every change — the same UnknownHostException-style dependency failures and Gradle misconfigurations discussed in this report's next section are caught on a clean machine, not only on the developer's own laptop. Second, it keeps the Firebase credentials file out of the repository entirely: the real google-services.json only ever exists on the developer's machine and inside the encrypted GitHub Actions secret, never in git history, which is the correct way to keep a backend key both usable in CI and safe from being pushed to a public repository by mistake.

3.4 How this supports the assessment's “hosted online” requirement
Together, the two pieces of infrastructure address the “hosted online” requirement from two different angles: Firebase Authentication and Firestore are the online API and database the running app connects to, while GitHub and GitHub Actions host and continuously verify the source code that produces that app. Screenshots of a green run in the Actions tab, alongside the Firebase console screenshots already used in the demo video, are the two pieces of evidence a marker can check independently of the recorded demonstration.

4. Use of AI Tools During This Assessment
An AI assistant (Claude, developed by Anthropic, accessed through claude.ai) was used throughout this assessment as a coding and documentation aid. Its use fell into three categories, each described below with a representative example. All AI-produced code and text was reviewed, tested on a device or emulator, and edited before being included in the final submission — the tool was used to accelerate implementation and debugging, not to replace understanding of how the app works.

4.1 Code generation
Claude was used to scaffold the initial project structure — the Gradle build files, the Kotlin data models, the Firebase repository layer, and the Jetpack Compose screens — from a written description of the required features and a set of reference screenshots showing the intended UI. For example, the data-access layer (Repo.kt) and the shared UI state holder (AppVM.kt) were generated in full, then reviewed and adjusted, including the validation rules described in Section 2.5.

Example prompt (paraphrased): “Build an Android app using Android Studio Kotlin, shows steps how to implement the downloaded folder… create an app for a South African flight-booking service based on these screens, with registration, login, settings, search, booking and price alerts, using Firebase for the backend.”

4.2 Debugging assistance
Build errors encountered in Android Studio were pasted directly into the chat, and Claude's diagnosis was used to resolve them. Two examples from this project:

● A Gradle sync failure (java.net.UnknownHostException: dl.google.com / repo.maven.apache.org) was diagnosed as a network/DNS connectivity problem rather than a project misconfiguration, leading to a DNS and proxy-settings fix rather than a wasted rewrite of the build files.
● A build failure at the :app:processDebugGoogleServices task (“No matching client found for package name 'com.protea.travels'”) was diagnosed as a mismatch between the applicationId in build.gradle.kts and the package name registered against the downloaded google-services.json, leading to the app being re-registered in the Firebase console under the correct package name.
4.3 Documentation, planning and image generation
Claude was used to draft the project README, the demo-video shot list and script (produced as a slide deck), and this report, based on the finished code and the assessment's stated rubric; all three were reviewed and edited afterwards. It was also used to generate a self-contained HTML/CSS mock-up of the full app flow (splash through settings) as a planning aid early in the project. That HTML mock-up was AI-generated; the final mock-up images embedded in this report and in the README are, by contrast, the original reference screenshots supplied at the start of the project, not AI-generated images — this distinction is stated here for clarity and academic honesty.

4.4 Citation
In line with common academic guidance for citing AI tools (check your own module or institution's required format, as this varies), AI assistance on this project can be cited as:

Anthropic. (2026). Claude (Sonnet) [Large language model].
https://claude.ai
 
Used for: code scaffolding (Kotlin/Jetpack Compose/Firebase),
debugging Gradle and Firebase configuration errors, and
drafting project documentation. All output reviewed, tested
and edited by the author before submission.
A record of the prompts and responses used is available as chat history and can be exported or included as an appendix if the module requires it.

5. Conclusion
Protea Travels demonstrates a complete mobile application built against a genuinely online backend: Firebase Authentication for secure registration and login, and Cloud Firestore for a live, per-user database covering settings, bookings and price alerts. Its visual and information-architecture decisions were made specifically for a South African, curated-travel context rather than left as framework defaults, its codebase is structured for version control and automated building through GitHub and GitHub Actions, and AI assistance used during its development is disclosed and cited above.
