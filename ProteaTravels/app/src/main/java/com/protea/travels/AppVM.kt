package com.protea.travels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.protea.travels.data.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class AppVM : ViewModel() {
    var message by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
    var userName by mutableStateOf("")
    var settings by mutableStateOf(Settings())
    var search by mutableStateOf(SearchParams())
    var results by mutableStateOf<List<Flight>>(emptyList())
    var bookings by mutableStateOf<List<Booking>>(emptyList())
    var alerts by mutableStateOf<List<PriceAlert>>(emptyList())

    init { if (Repo.isLoggedIn) run { afterAuth() } }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy = true
            try { block() } catch (e: Exception) { message = e.localizedMessage ?: "Something went wrong" }
            busy = false
        }
    }

    private suspend fun afterAuth() {
        val (name, s) = Repo.loadProfile()
        userName = name; settings = s
        search = search.copy(from = s.defaultOrigin)
        try { Repo.seedFlightsIfEmpty() } catch (_: Exception) {}
        bookings = Repo.bookings(); alerts = Repo.alerts()
    }

    // ---------- Auth ----------
    fun register(first: String, last: String, email: String, pw: String, confirm: String, onOk: () -> Unit) {
        when {
            first.isBlank() || last.isBlank() -> message = "Enter your first and last name"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> message = "Enter a valid email"
            pw.length < 8 -> message = "Password must be at least 8 characters"
            pw != confirm -> message = "Passwords do not match"
            else -> run { Repo.register(first.trim(), last.trim(), email.trim(), pw); afterAuth(); onOk() }
        }
    }

    fun login(email: String, pw: String, onOk: () -> Unit) {
        if (email.isBlank() || pw.isBlank()) { message = "Enter email and password"; return }
        run { Repo.login(email.trim(), pw); afterAuth(); onOk() }
    }

    fun forgotPassword(email: String) {
        if (email.isBlank()) { message = "Enter your email first"; return }
        run { Repo.resetPassword(email.trim()); message = "Password reset email sent" }
    }

    fun logout() {
        Repo.logout(); bookings = emptyList(); alerts = emptyList(); results = emptyList()
        userName = ""; settings = Settings(); search = SearchParams()
    }

    // ---------- Settings ----------
    fun updateSettings(s: Settings) {
        settings = s
        run { Repo.saveSettings(s) }
    }
    fun saveName(name: String) {
        if (name.isBlank()) { message = "Name cannot be empty"; return }
        run { Repo.saveFirstName(name.trim()); userName = name.trim(); message = "Profile updated" }
    }

    // ---------- Search & booking ----------
    fun updateSearch(s: SearchParams) { search = s }

    fun runSearch(onDone: () -> Unit) {
        val s = search
        try {
            if (s.from.isEmpty() || s.to.isEmpty()) { message = "Choose origin and destination"; return }
            if (s.from == s.to) { message = "Origin and destination must differ"; return }
            if (s.depart.isEmpty()) { message = "Choose a departure date"; return }
            if (LocalDate.parse(s.depart).isBefore(LocalDate.now())) { message = "Departure cannot be in the past"; return }
            if (s.tripType == "Return") {
                if (s.ret.isEmpty()) { message = "Choose a return date"; return }
                if (LocalDate.parse(s.ret).isBefore(LocalDate.parse(s.depart))) { message = "Return must be after departure"; return }
            }
        } catch (e: Exception) { message = "Invalid date"; return }
        run {
            results = Repo.searchFlights(s.from, s.to)
            if (results.isEmpty()) message = "No flights found for that route"
            onDone()
        }
    }

    fun total(f: Flight): Int =
        (f.price * search.pax * (cabinMultiplier[search.cabin] ?: 1.0) * (if (search.tripType == "Return") 2 else 1)).toInt()

    fun book(f: Flight, onDone: () -> Unit) {
        val s = search
        run {
            Repo.addBooking(Booking("", f.from, f.to, f.airline, f.departTime, s.depart,
                if (s.tripType == "Return") s.ret else "", s.pax, s.cabin, s.tripType, total(f)))
            bookings = Repo.bookings(); message = "Booking confirmed!"; onDone()
        }
    }

    fun cancelBooking(id: String) = run { Repo.deleteBooking(id); bookings = Repo.bookings(); message = "Booking cancelled" }

    fun addAlert() {
        val s = search
        if (s.to.isEmpty() || s.from == s.to) { message = "Pick a valid route first"; return }
        if (alerts.any { it.from == s.from && it.to == s.to }) { message = "Alert already exists"; return }
        run { Repo.addAlert(s.from, s.to); alerts = Repo.alerts(); message = "Price alert added" }
    }
    fun removeAlert(id: String) = run { Repo.deleteAlert(id); alerts = Repo.alerts() }
}
//By Lesego van Heerden and Onnalenna Lonake//