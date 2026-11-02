package com.zaitxcode.linkifyall

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat

/**
 * دوال مساعدة لإنشاء عناصر واجهة الإعدادات
 */
class SettingsUIHelpers(private val activity: Activity) {

    /** إنشاء إعداد لون التمييه */
    fun createColorSetting(): View {
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 15, 0, 15)
        }

        val title = TextView(activity).apply {
            text = "Link Highlight Color"
            textSize = 14f
            setTextColor(Color.parseColor("#1A1A1A"))
            setPadding(0, 0, 0, 10)
        }
        container.addView(title)

        val colors = listOf(
            0xFF2196F3.toLong(), // Blue (default)
            0xFF4CAF50.toLong(), // Green
            0xFFFF9800.toLong(), // Orange
            0xFFF44336.toLong(), // Red
            0xFF9C27B0.toLong(), // Purple
            0xFF00BCD4.toLong(), // Cyan
            0xFF795548.toLong(), // Brown
            0xFF607D8B.toLong(), // Blue Grey
        )

        val colorContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            weightSum = colors.size.toFloat()
        }

        val currentColor = SettingsManager.getLinkColor(activity)
        colors.forEachIndexed { index, colorInt ->
            val colorView = View(activity).apply {
                layoutParams = LinearLayout.LayoutParams(0, 60, 1f).apply {
                    setMargins(5, 0, 5, 0)
                }
                setBackgroundColor(colorInt.toInt())
                val isSelected = colorInt.toInt() == currentColor
                if (isSelected) {
                    setElevation(4f)
                }
                setOnClickListener {
                    SettingsManager.setLinkColor(activity, colorInt.toInt())
                    Toast.makeText(activity, "Link color updated", Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.recreate()
                }
            }
            colorContainer.addView(colorView)
        }

        container.addView(colorContainer)
        return container
    }

    /** إنشاء إعداد toggle عام */
    fun createToggleSetting(
        titleText: String,
        description: String,
        initialValue: Boolean,
        onChange: (Boolean) -> Unit
    ): View {
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 15, 0, 15)
        }

        val textContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(0, 0, 20, 0)
        }

        val title = TextView(activity).apply {
            text = titleText
            textSize = 14f
            setTextColor(Color.parseColor("#1A1A1A"))
        }
        textContainer.addView(title)

        val desc = TextView(activity).apply {
            text = description
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, 4, 0, 0)
        }
        textContainer.addView(desc)
        container.addView(textContainer)

        val toggle = Switch(activity).apply {
            isChecked = initialValue
            setOnCheckedChangeListener { _, isChecked ->
                onChange(isChecked)
                Toast.makeText(activity, "$titleText: ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
            }
        }
        container.addView(toggle)

        return container
    }

    /** إنشاء إعداد مستوى التسجيل */
    fun createLogLevelSetting(): View {
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 15, 0, 15)
        }

        val title = TextView(activity).apply {
            text = "Log Level"
            textSize = 14f
            setTextColor(Color.parseColor("#1A1A1A"))
            setPadding(0, 0, 0, 10)
        }
        container.addView(title)

        val levels = listOf("NONE", "ERROR", "WARN", "INFO", "DEBUG", "VERBOSE")
        val currentLevel = SettingsManager.getLogLevel(activity)

        val levelContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        levels.forEach { level ->
            val isSelected = level == currentLevel
            val btn = TextView(activity).apply {
                text = level
                textSize = 12f
                setTextColor(Color.parseColor(if (isSelected) "#FFFFFF" else "#1A1A1A"))
                setPadding(16, 8, 16, 8)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(if (isSelected) "#2196F3" else "#E0E0E0"))
                    cornerRadius = 16f
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(4, 0, 4, 0) }
                setOnClickListener {
                    SettingsManager.setLogLevel(activity, level)
                    LogManager.setLevelFromString(level)
                    Toast.makeText(activity, "Log level set to $level", Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.recreate()
                }
            }
            levelContainer.addView(btn)
        }

        val scroll = HorizontalScrollView(activity).apply {
            addView(levelContainer)
        }
        container.addView(scroll)

        return container
    }
}