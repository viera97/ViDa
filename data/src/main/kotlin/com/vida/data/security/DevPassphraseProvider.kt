package com.vida.data.security

/**
 * Development passphrase provider.
 * TODO(v2): replace with DataStore-backed passphrase, biometric unlock via :feature-*
 */
object DevPassphraseProvider : PassphraseProvider {
    private val DEV_PASSPHRASE = "vida-dev-passphrase-2026".toByteArray()

    override fun getPassphrase(): ByteArray = DEV_PASSPHRASE
}
