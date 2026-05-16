package com.smsforwarder

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "sms_forwarder_prefs"
        private const val KEY_FORWARD_TO = "forward_to_number"
        private const val KEY_KEYWORDS = "keywords"
        private const val KEY_FORWARDING_ENABLED = "forwarding_enabled"
    }

    var forwardToNumber: String
        get() = prefs.getString(KEY_FORWARD_TO, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_FORWARD_TO, value).apply()
        }

    var isForwardingEnabled: Boolean
        get() = prefs.getBoolean(KEY_FORWARDING_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_FORWARDING_ENABLED, value).apply()
        }

    fun getKeywords(): Set<String> =
        prefs.getStringSet(KEY_KEYWORDS, emptySet()) ?: emptySet()

    fun addKeyword(keyword: String) {
        val updated = getKeywords().toMutableSet().also { it.add(keyword.trim().lowercase()) }
        prefs.edit().putStringSet(KEY_KEYWORDS, updated).apply()
    }

    fun removeKeyword(keyword: String) {
        val updated = getKeywords().toMutableSet().also { it.remove(keyword) }
        prefs.edit().putStringSet(KEY_KEYWORDS, updated).apply()
    }
}
