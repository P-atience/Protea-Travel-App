# Protea Travels (Android · Kotlin · Jetpack Compose · Firebase)

## 1. Install first
- Android Studio (Koala or newer) with the bundled JDK 17
- SDK Platform 34 (SDK Manager > SDK Platforms > Android 14 "UpsideDownCake")
- An emulator (Pixel, API 34) or a physical phone with USB debugging
- A free Google account for Firebase

## 2. Create the online backend (Firebase)
1. https://console.firebase.google.com > Add project > "ProteaTravels"
2. Project overview > Add app > Android. Package name MUST be: com.protea.travels. Register app.
3. Download google-services.json and put it in: ProteaTravels/app/google-services.json
4. Build > Authentication > Get started > Sign-in method > enable Email/Password
5. Build > Firestore Database > Create database > Start in production mode > pick a region (e.g. europe-west / africa if offered)
6. Firestore > Rules tab > paste the contents of firestore.rules > Publish

## 3. Open and run
1. Unzip. Android Studio > File > Open > select the ProteaTravels folder.
2. Let Gradle sync (needs internet). If asked about the Gradle wrapper, accept.
   If sync says the wrapper jar is missing: Settings > Build Tools > Gradle > "Use Gradle from: gradle-wrapper.properties" or choose the bundled Gradle.
3. Do NOT accept "upgrade AGP/Kotlin" prompts until it runs once (versions are pinned to work together).
4. Run > app. First login seeds the flights collection in Firestore.

## 4. Dependencies used (already in app/build.gradle.kts)
- Jetpack Compose BOM + Material3 + material-icons-extended
- navigation-compose, lifecycle-viewmodel-compose, activity-compose
- Firebase BoM: firebase-auth-ktx, firebase-firestore-ktx
- kotlinx-coroutines-play-services (.await() on Firebase tasks)
- Plugin: com.google.gms.google-services

## 5. Rubric mapping
| Requirement | Where |
|---|---|
| Register / login | AuthScreens.kt, Repo.register/login. Passwords are hashed and stored by Firebase Auth; the app never stores them. |
| Settings | SettingsScreen (name, dark mode, currency, default airport, notifications) saved to Firestore per user |
| Online API + DB | Firebase Auth + Cloud Firestore (hosted by Google, accessed over HTTPS) |
| Flight search (return / one-way, dates, passengers, cabin) | SearchScreen + Repo.searchFlights |
| Results, booking, my trips (create/read/delete) | ResultsScreen, BookingsScreen |
| Deals and price alerts (create/read/delete) | HomeScreen |

## 6. Demo script
Register > sign out > sign in > tap a deal > search return flight > book > see it in Trips > cancel > add a price alert > change settings (dark mode, currency) > sign out and back in to show settings persisted in the cloud.

