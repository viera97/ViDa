package com.vida.data.db.security

/**
 * Interface for providing the SQLCipher database passphrase.
 *
 * Implementations control where the passphrase comes from (hardcoded dev key,
 * DataStore, biometric-backed Keystore, etc.).
 */
fun interface PassphraseProvider {

    /**
     * Returns the database passphrase as a byte array.
     *
     * @return passphrase bytes used to encrypt/decrypt the SQLCipher database.
     */
    fun getPassphrase(): ByteArray
}
