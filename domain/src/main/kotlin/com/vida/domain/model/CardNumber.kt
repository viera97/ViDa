package com.vida.domain.model

/**
 * Masked card number. Stores only the 16-character masked form (`first6******last4`).
 *
 * The full card number is never persisted; it exists only as a String argument to
 * [fromFull] at construction time and is discarded immediately after the masked
 * representation is built. This is a Q8-locked privacy decision.
 */
@JvmInline
value class CardNumber(val masked: String) {

    init {
        require(masked.length == 16) { "Masked card number must be 16 characters" }
        require(masked.substring(6, 12) == "******") {
            "Masked card number must have ****** in positions 6..12"
        }
        require(masked.substring(0, 6).all { it.isDigit() }) {
            "First 6 of masked card number must be digits"
        }
        require(masked.substring(12, 16).all { it.isDigit() }) {
            "Last 4 of masked card number must be digits"
        }
    }

    companion object {
        /**
         * Build a [CardNumber] from a full card number string.
         * Only the first 6 and last 4 digits are kept; the middle is masked.
         * The full number is never persisted.
         *
         * @throws IllegalArgumentException if [full] is not exactly 16 digits.
         */
        fun fromFull(full: String): CardNumber {
            require(full.length == 16) { "Card number must be 16 characters" }
            require(full.all { it.isDigit() }) { "Card number must be all digits" }
            return CardNumber(full.substring(0, 6) + "******" + full.substring(12, 16))
        }

        /**
         * Build a [CardNumber] from already-truncated components. Useful when the
         * UI never sees the full number (e.g., manual entry of first 6 / last 4).
         *
         * @throws IllegalArgumentException if [first6] or [last4] is not the expected shape.
         */
        fun fromFirst6Last4(first6: String, last4: String): CardNumber {
            require(first6.length == 6 && first6.all { it.isDigit() }) {
                "First 6 must be 6 digits"
            }
            require(last4.length == 4 && last4.all { it.isDigit() }) {
                "Last 4 must be 4 digits"
            }
            return CardNumber(first6 + "******" + last4)
        }
    }
}