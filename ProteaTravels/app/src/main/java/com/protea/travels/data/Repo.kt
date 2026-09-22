package com.protea.travels.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/** All online calls: Firebase Auth (passwords hashed server-side) + Cloud Firestore (database). */
object Repo {
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

    val isLoggedIn get() = auth.currentUser != null
    val email get() = auth.currentUser?.email ?: ""
    private val uid get() = auth.currentUser!!.uid
    private fun userDoc() = db.collection("users").document(uid)

    suspend fun register(first: String, last: String, email: String, pw: String) {
        auth.createUserWithEmailAndPassword(email, pw).await()
        userDoc().set(
            mapOf(
                "firstName" to first, "lastName" to last, "email" to email,
                "darkMode" to false, "currency" to "ZAR", "notifications" to true, "defaultOrigin" to "JNB"
            )
        ).await()
    }

    suspend fun login(email: String, pw: String) { auth.signInWithEmailAndPassword(email, pw).await() }
    suspend fun resetPassword(email: String) { auth.sendPasswordResetEmail(email).await() }
    fun logout() = auth.signOut()

    suspend fun loadProfile(): Pair<String, Settings> {
        val d = userDoc().get().await()
        val s = Settings(
            d.getBoolean("darkMode") ?: false, d.getString("currency") ?: "ZAR",
            d.getBoolean("notifications") ?: true, d.getString("defaultOrigin") ?: "JNB"
        )
        return (d.getString("firstName") ?: "Traveller") to s
    }

    suspend fun saveSettings(s: Settings) {
        userDoc().set(
            mapOf("darkMode" to s.darkMode, "currency" to s.currency,
                "notifications" to s.notifications, "defaultOrigin" to s.defaultOrigin),
            SetOptions.merge()
        ).await()
    }

    suspend fun saveFirstName(name: String) {
        userDoc().set(mapOf("firstName" to name), SetOptions.merge()).await()
    }

    // ---------- Flights ----------
    suspend fun seedFlightsIfEmpty() {
        if (!db.collection("flights").limit(1).get().await().isEmpty) return
        // route, base price (ZAR), duration minutes
        val routes = listOf(
            Triple("JNB-CPT", 2450, 125), Triple("JNB-DUR", 1890, 65), Triple("JNB-PLZ", 2100, 100),
            Triple("CPT-DUR", 2300, 120), Triple("JNB-LHR", 9800, 690), Triple("JNB-ZNZ", 4890, 260),
            Triple("JNB-DXB", 7990, 480)
        )
        val airlines = listOf("Protea Air", "Kalahari Air", "Karoo Airways")
        val times = listOf("06:30", "12:15", "18:45")
        val batch = db.batch()
        for ((r, base, dur) in routes) {
            val (a, b) = r.split("-")
            for ((from, to) in listOf(a to b, b to a)) {
                for (i in 0..2) {
                    val dep = times[i]
                    val mins = dep.substring(0, 2).toInt() * 60 + dep.substring(3).toInt() + dur
                    val arr = String.format("%02d:%02d", (mins / 60) % 24, mins % 60)
                    val id = "$from-$to-${i + 1}"
                    batch.set(
                        db.collection("flights").document(id),
                        Flight(id, from, to, airlines[i], dep, arr, dur, base + i * 350 - (if (i == 0) 0 else 0))
                    )
                }
            }
        }
        batch.commit().await()
    }

    suspend fun searchFlights(from: String, to: String): List<Flight> =
        db.collection("flights").whereEqualTo("from", from).whereEqualTo("to", to).get().await()
            .documents.mapNotNull { it.toObject(Flight::class.java)?.also { f -> f.id = it.id } }
            .sortedBy { it.price }

    // ---------- Bookings ----------
    suspend fun addBooking(b: Booking) { userDoc().collection("bookings").add(b).await() }
    suspend fun bookings(): List<Booking> =
        userDoc().collection("bookings").get().await().documents
            .mapNotNull { it.toObject(Booking::class.java)?.also { b -> b.id = it.id } }
            .sortedBy { it.departDate }
    suspend fun deleteBooking(id: String) { userDoc().collection("bookings").document(id).delete().await() }

    // ---------- Price alerts ----------
    suspend fun addAlert(from: String, to: String) {
        userDoc().collection("alerts").add(PriceAlert("", from, to)).await()
    }
    suspend fun alerts(): List<PriceAlert> =
        userDoc().collection("alerts").get().await().documents
            .mapNotNull { it.toObject(PriceAlert::class.java)?.also { a -> a.id = it.id } }
    suspend fun deleteAlert(id: String) { userDoc().collection("alerts").document(id).delete().await() }
}
