package com.vida.core.premium

/**
 * Contract that premium features expose to the app module.
 *
 * The [:app] module depends on this interface only — never directly on
 * [:feature-premium]. Future premium feature modules will provide a Hilt-bound
 * implementation of this interface, keeping the free flavor free of premium
 * dependencies at compile time.
 */
interface PremiumFeatureProvider
