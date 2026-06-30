package com.vida.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Money value object. Amount is BigDecimal with HALF_EVEN rounding to match
 * financial best practice. Currency is enforced at construction; operators
 * throw IllegalArgumentException on currency mismatch.
 */
data class Money(
    val amount: BigDecimal,
    val currency: Currency,
) : Comparable<Money> {

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return copy(amount = amount.add(other.amount))
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return copy(amount = amount.subtract(other.amount))
    }

    operator fun times(multiplier: BigDecimal): Money =
        copy(amount = amount.multiply(multiplier))

    operator fun times(multiplier: Int): Money = times(multiplier.toBigDecimal())

    operator fun div(divisor: BigDecimal): Money {
        require(divisor.signum() != 0) { "Cannot divide Money by zero" }
        return copy(amount = amount.divide(divisor, 10, RoundingMode.HALF_EVEN))
    }

    override operator fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return amount.compareTo(other.amount)
    }

    /** Convert to [target] using [rate] (multiplier: 1 unit of [currency] = [rate] units of [target]). */
    fun convertTo(target: Currency, rate: BigDecimal): Money {
        require(rate.signum() > 0) { "Conversion rate must be positive" }
        if (target == currency) return this
        return Money(
            amount = amount.multiply(rate).setScale(2, RoundingMode.HALF_EVEN),
            currency = target,
        )
    }

    fun isPositive(): Boolean = amount.signum() > 0
    fun isNegative(): Boolean = amount.signum() < 0
    fun isZero(): Boolean = amount.signum() == 0

    operator fun unaryMinus(): Money = copy(amount = amount.negate())

    private fun requireSameCurrency(other: Money) {
        require(other.currency == currency) {
            "Currency mismatch: $currency vs ${other.currency}"
        }
    }

    companion object {
        val ZERO_CUP: Money = Money(BigDecimal.ZERO, Currency.CUP)
        val ZERO_USD: Money = Money(BigDecimal.ZERO, Currency.USD)
        val ZERO_MLC: Money = Money(BigDecimal.ZERO, Currency.MLC)

        fun of(amount: String, currency: Currency): Money =
            Money(BigDecimal(amount), currency)

        /**
         * Reconstruct [Money] from minor units (2-decimal fixed-point).
         *
         * E.g. minorUnits=1234, currency=CUP → Money(12.34, CUP).
         */
        fun fromMinorUnits(minorUnits: Long, currency: Currency): Money =
            Money(
                amount = BigDecimal(minorUnits)
                    .divide(BigDecimal(100), 2, RoundingMode.HALF_EVEN),
                currency = currency,
            )
    }
}