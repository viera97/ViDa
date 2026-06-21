package com.vida.data.db.security

/**
 * Development-only [PassphraseProvider] that returns a hardcoded passphrase.
 *
 * **Security note**: This is NOT suitable for production. The hardcoded passphrase
 * is trivially recoverable from the APK. Production builds MUST replace this with
 * a DataStore-backed or biometric-backed implementation.
 *
 * TODO(v2): replace with DataStore-backed passphrase, biometric unlock via :feature-*
 */
object DevPassphraseProvider : PassphraseProvider {

    private const val DEV_PASSPHRASE = "vida-dev-passphrase-2026"

    override fun getPassphrase(): ByteArray = DEV_PASSPHRASE.toByteArray()
}
