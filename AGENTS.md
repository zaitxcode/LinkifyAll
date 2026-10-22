# LinkifyAll Project - Arabic Analysis

## نظرة عامة على المشروع

**LinkifyAll** هو نموذج إضافة (Module) لـ Xposed/LSPosed يجعل النصوص غير النقرية روابط قابلة للضغط في جميع أنحاء النظام. يعمل على أجهزة AndroidRooted (Android 11.0 / API 30+) مع إطار عمل LSPosed أو Xposed مثبت.

## الميزات الأساسية

| الميزة | الوصف |
|--------|--------|
| 🌍 **شامل النظام** | يعمل في تقريباً أي تطبيق (وسائل التواصل، ملاحظات، سجلات النظام) |
| 🧠 **حل ذكي** | يستخدم wyrażenie Regex متقدم لاكتشاف `http`، `https`، `www`، والنطاقات الشائعة |
| 🖱️ **دعم العروض المخصصة** | يحاول فتح روابط ظاهرة من خلالNodes الوصولية في UI المخصصة |
| ⚡ **بدون تأخير** | منطق الربط محسن لض impact minimal على الأداء |
| 🛡️ **آمان أساسي** | يستبعد حزم `systemui` و `android` لمنع تعليق النظام |
| 🔄 **تحديثات تلقائية** | مدققUpdates يفحص إصدارات GitHub تلقائياً |

## الهيكل التقني

### 1. **Hook.kt** - المنطق الأساسي للإضافة
-extends `IXposedHookLoadPackage` ويقوم بما يلي:
- **اكتشاف وتظليل الروابط**: يستخدم pattern تعبير Regex (`urlPattern`) لاكتشاف الروابط في النص وتلوينها باللون cyan عبرForegroundColorSpan
- **معالجة أحداث اللمس**: يربك حدث `onTouchEvent` لـ TextView لالتقاط نقرات على الروابط المHighlighted وفتحها في المتصفح
- **دعم العروض المخصصة**: يربك `dispatchTouchEvent` لفئة View وViewGroup ويستخدم Android AccessibilityNodeInfo للبحث عن الروابط في العروض المخصصة التي لا تستخدم TextView قياسي
- **الاستثناءات**: تجاهل deliberate لحزم `android` و `com.android.systemui` لمنع تعليق النظام

### 2. **MainActivity.kt** - واجهة المستخدم
- نشاط الإعدادات (لا يظهر في درج التطبيق - مخفي purposely)
- **عناصر الواجهة**: شاشة قائمة ب卡片 تحتوي على حالة تنشيط الإضافة، رقم الإصدار، وأزرار الإجراءات
- **تنشيط الإضافة**: زر لتحويل الحالة بين Active/Inactive (لون أخضر/أحمر)
- **التحقق من التحديثات**: يفحص GitHub Releases للإصدارات الجديدة
- **التنزيل والتركيب**: ينزل APK عبر DownloadManager ويصيبvia root access (Magisk/KernelSU) أو intent قياسي
- **إخفاء أيقونة المنبسط**: يتم تعطيل ComponentName deliberately من الإخفاء من درج التطبيق

### 3. **UpdateChecker.kt** - نظام التحديثات
- يفحص API GitHub لأحدث إصدار
- ي parsed اسم الإصدار (مثل v0.5.0) وملاحظات الإصدار
- يجد رابط تنزيل APK من Assets الإصدار
- المقارنة بين الإصدار الحالي والأحدث لتحديد ما إذا كان تحديث متوفر
- يستخدم Kotlin coroutines لعمليات الشبكة في الخلفية

### 4. **AndroidManifest.xml** - ملف البيان
- **بيانات الإضافة**: `android:name="xposedmodule"` = `true`
- **الوصف**: `android:name="xposeddescription"`
- **النسخة الدنيا**: `android:name="xposedminversion"` = `93`
- **النشاط الرئيسي**: مُكوَّن مع intent filter `de.robv.android.xposed.category.MODULE_SETTINGS`
- **نشاطランشر**: معطل `android:enabled="false"` لإخفائه من الدرج
- **File Provider**: لتركيب APK بأمان via `androidx.core.content.FileProvider`
- **intents**: يتعامل مع بيانات schemes `http` و `https`

## كيفية العمل (تدفق المستخدم)

1. **التثبيت**: المستخدم ينزل APK ويَثبُت (يتطلب Root + LSPosed/Xposed)
2. **تفعيل الإضافة**: المستخدم يُفعل LinkifyAll في مدير LSPosed/Xposed (النطاق: System Framework + Target Apps)
3. **إعادة التشغيل**: مطلوب لتفعيل إضافة Xposed
4. **العمل التلقائي**:
   - whenever text is set in any TextView، يقوم module بفحص الروابط تلقائياً
   - تُHighlight الروابط باللون cyan
   - الضغط على رابط مُHighlighted يفتح المتصفح الافتراضي
   - بالنسبة للعروض المخصصة،module يستخدم خدمات الوصول للعثور على الروابط وفتحها

## تفاصيل تقنية

- **لغة البرمجة**: Kotlin
- **SDK الأدنى**: Android 11.0 (API 30)
- **المُعتمَذات**:
  - APIs framework Xposed
  - Kotlin coroutines (للتحقق من التحديثات)
  - مكتبات AndroidX (Activity، Core، SwipeRefreshLayout)
- **Pattern التعبيرات العادية**:pattern شامل يتوافق مع `http`، `https`، `www`، روابط mailto، tel، sms، geo، magnet، وعناوين IPv4

نجح المشروع في تنفيذ حل Linkification شامل للنظام وهو قوي وواعٍ للأداء، مع إجراءات أمان مناسبة لتجنب كسر وظائف النظام.