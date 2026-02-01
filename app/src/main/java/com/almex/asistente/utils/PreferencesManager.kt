package com.almex.asistente.utils

import android.content.Context
import android.graphics.Color

object PreferencesManager {
    
    private const val PREFS_NAME = "almex_preferences"
    private const val KEY_BACKGROUND_IMAGE = "background_image_uri"
    private const val KEY_ACCENT_COLOR = "accent_color"
    private const val KEY_ACCENT_COLOR_INDEX = "accent_color_index"
    
    fun saveBackgroundImageUri(context: Context, uri: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BACKGROUND_IMAGE, uri).apply()
    }
    
    fun getBackgroundImageUri(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_BACKGROUND_IMAGE, "") ?: ""
    }
    
    fun saveAccentColor(context: Context, color: Int, index: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_ACCENT_COLOR, color)
            .putInt(KEY_ACCENT_COLOR_INDEX, index)
            .apply()
    }
    
    fun getAccentColor(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_ACCENT_COLOR, Color.parseColor("#2196F3")) // Azul por defecto
    }
    
    fun getAccentColorIndex(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_ACCENT_COLOR_INDEX, 0)
    }
}