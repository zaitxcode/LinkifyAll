package com.zaitxcode.linkifyall

import android.content.Context
import android.content.SharedPreferences

/**
 * مدير الإعدادات المشتركة بين MainActivity و Hook
 * يستخدم SharedPreferences للاحتفاظ بتفضيلات المستخدم
 */
object SettingsManager {
    private const val PREFS_NAME = "linkifyall_settings"
    private const val KEY_LINK_COLOR = "link_color"
    private const val KEY_OPEN_IN_EXTERNAL_BROWSER = "open_in_external_browser"
    private const val KEY_LOG_LEVEL = "log_level"
    private const val KEY_HIGHLIGHT_LINKS = "highlight_links"
    private const val KEY_CUSTOM_VIEW_SUPPORT = "custom_view_support"

    // القيم الافتراضية
    private const val DEFAULT_LINK_COLOR = 0xFF2196F3.toLong() // أزرق Material Design
    private const val DEFAULT_OPEN_IN_EXTERNAL_BROWSER = false
    private const val DEFAULT_LOG_LEVEL = "INFO"
    private const val DEFAULT_HIGHLIGHT_LINKS = true
    private const val DEFAULT_CUSTOM_VIEW_SUPPORT = true

    /**
     * الحصول على SharedPreferences
     */
    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** لون التمييه للرابط (ARGB) */
    fun getLinkColor(context: Context): Int =
        getPrefs(context).getLong(KEY_LINK_COLOR, DEFAULT_LINK_COLOR).toInt()

    /** تعيين لون التمييه */
    fun setLinkColor(context: Context, color: Int) {
        getPrefs(context).edit().putLong(KEY_LINK_COLOR, color.toLong()).apply()
    }

    /** فتح الروابط في متصفح خارجي بدلاً من AndroidHelper */
    fun getOpenInExternalBrowser(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_OPEN_IN_EXTERNAL_BROWSER, DEFAULT_OPEN_IN_EXTERNAL_BROWSER)

    /** تعيين خيار فتح الروابط في متصفح خارجي */
    fun setOpenInExternalBrowser(context: Context, value: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_OPEN_IN_EXTERNAL_BROWSER, value).apply()
    }

    /** مستوى التسجيل */
    fun getLogLevel(context: Context): String =
        getPrefs(context).getString(KEY_LOG_LEVEL, DEFAULT_LOG_LEVEL) ?: DEFAULT_LOG_LEVEL

    /** تعيين مستوى التسجيل */
    fun setLogLevel(context: Context, level: String) {
        getPrefs(context).edit().putString(KEY_LOG_LEVEL, level).apply()
    }

    /** تفعيل/تعطيل تظليل الروابط */
    fun getHighlightLinks(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_HIGHLIGHT_LINKS, DEFAULT_HIGHLIGHT_LINKS)

    /** تعيين تفعيل/تعطيل تظليل الروابط */
    fun setHighlightLinks(context: Context, value: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_HIGHLIGHT_LINKS, value).apply()
    }

    /** تفعيل/تعطيل دعم العروض المخصصة */
    fun getCustomViewSupport(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_CUSTOM_VIEW_SUPPORT, DEFAULT_CUSTOM_VIEW_SUPPORT)

    /** تعيين تفعيل/تعطيل دعم العروض المخصصة */
    fun setCustomViewSupport(context: Context, value: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_CUSTOM_VIEW_SUPPORT, value).apply()
    }
}