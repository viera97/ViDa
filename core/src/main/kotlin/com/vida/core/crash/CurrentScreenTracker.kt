package com.vida.core.crash

/**
 * Thread-safe singleton that tracks the currently visible screen name.
 *
 * The value is set from the [NavController] route callback in [ViDaApp].
 * It is accessed synchronously from the crash handler thread (non-main thread)
 * so the field is [Volatile] to guarantee visibility across threads.
 *
 * This tracker stores a screen/route name only — no user data, no parameters.
 */
object CurrentScreenTracker {

    @Volatile
    var currentScreen: String? = null
}
