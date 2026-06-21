package com.vida.data.security

interface PassphraseProvider {
    fun getPassphrase(): ByteArray
}
