<h1 align="center">ساعة العمق · Depth Clock</h1>

<p align="center">
  ساعة شاشة القفل <b>ورا الشخص</b> بالصورة — مثل «تأثير العمق» بالآيفون — على تلفونات سامسونگ وأندرويد.
</p>

<div dir="rtl">

## شنو يسوّي

تختار صورة من المعرض، والتطبيق يفصل الشخص عن الخلفية على الجهاز نفسه، ويحط ساعة شاشة القفل بين الخلفية والشخص.
النتيجة: الساعة تبين وكأنها واكفة ورا راسك، مثل iOS 16 وما بعده.

سامسونگ نفسها ما تسوّي هالشي لصور المعرض — ميزة «الساعة التكيّفية» بـ One UI تلف الأرقام **حول** الشخص، مو وراه.

## المميزات

- **فصل الشخص على الجهاز** بـ ML Kit Subject Segmentation — الصورة ما تطلع من التلفون.
- **24 شكل جاهز** للساعة: آيفون، طويلة رفيعة، عمودية، لونين، إطار، ديجيتال، بكسل، طباشير…
- **27 خط**: خطوط التلفون المتغيّرة (Roboto Flex، One UI Sans، أرقام سامسونگ) + 17 خط مفتوح المصدر.
- **تحكّم كامل**: الحجم، الطول، العرض، السُمك، تباعد الأرقام، المسافة بين السطرين، الوضوح.
- **ألوان من الصورة** + ألوان جاهزة + لون مخصّص، ودقائق باهتة أو إطار.
- **سحب الساعة لأي مكان** ويّا لزگة على نص الشاشة، وقرص للتكبير، وتأطير الصورة بالسحب والقرص.
- **معاينة مطابقة**: نفس الكود يرسم المعاينة وشاشة القفل الحقيقية.
- تنبيه إذا الشخص مغطّي أغلب الساعة.
- الاختيار من «المعرض» مال سامسونگ مباشرة، أو من «مشاركة» بالاستوديو.

## شلون يشتغل

التطبيق يحط **خلفية حية** (`WallpaperService`) على **شاشة القفل بس**، وترسم بالترتيب:

```
الصورة  →  الساعة  →  الشخص المقصوص  →  التاريخ
```

ترسم مرة وحدة بالدقيقة (`ACTION_TIME_TICK`) فما تاكل بطارية.

## التثبيت والإعداد

1. نزّل الـ APK من [Releases](../../releases) ونصّبه (يحتاج Android 14 أو أحدث).
2. افتح التطبيق، اختار صورة، ظبّط الساعة، ودوس **«طبّقها على شاشة القفل»** ← اختار **«شاشة القفل»**.
3. **خفّي ساعة سامسونگ** (مرة وحدة): ضغطة مطوّلة على شاشة القفل ← الساعة ← «الخط واللون»:
   - اختار خط عادي (مو «الساعات المتحرّكة»).
   - طفّي **Blur** (زر القطرة بأول الألوان).
   - منتقي الألوان ← **الشفافية 0%**.
   - صغّر إطار الساعة واسحبه للزاوية العليا (سامسونگ تحط ظل خفيف ثابت تحت ساعتها).
4. إذا الإشعارات تغطي الساعة: الإعدادات ← شاشة القفل ← إشعارات شاشة القفل ← **«نقطة»** أو **«أيقونات»**.

> ⚠️ «إيقاف إجباري» للتطبيق أو مسح بياناته يخلّي أندرويد يشيل الخلفية من شاشة القفل — وهذا سلوك أندرويد لكل الخلفيات الحية.

## البناء

```bash
./gradlew assembleRelease
```

الـ APK يطلع بـ `app/build/outputs/apk/release/`. يحتاج JDK 17+ و Android SDK 36.

## التقنيات

Kotlin · Jetpack Compose · Material 3 · WallpaperService · ML Kit Subject Segmentation · Variable fonts (`wght`/`wdth`)

| المجلد | الوصف |
|---|---|
| `render/` | `LockRenderer` و`ClockRenderer`: رسّام واحد للمعاينة وشاشة القفل |
| `wallpaper/` | الخلفية الحية |
| `data/` | التصميم (`ClockSpec`، `DepthDesign`) والحفظ الذرّي |
| `image/` | فتح الصورة وقص الشخص |
| `ui/` | واجهة التعديل (Compose) |

## الخطوط

الخطوط بمجلد `app/src/main/assets/fonts` مرخّصة بـ [SIL Open Font License 1.1](https://openfontlicense.org) — التفاصيل بـ `OFL-NOTICE.txt`.
خطوط Roboto Flex و One UI Sans وأرقام سامسونگ تنقرا من التلفون نفسه (`/system/fonts`) وما مضمّنة بالمشروع.

</div>

---

### English

**Depth Clock** puts the Android lock-screen clock *behind* the person in your photo — the iOS 16 “depth effect” — on Samsung Galaxy / Android 14+.
It segments the subject on-device (ML Kit), installs a lock-screen-only live wallpaper that draws *photo → clock → subject → date*, and ships a full clock editor: 24 presets, 27 fonts (incl. variable Roboto Flex with weight/width axes), size/height/width/weight/spacing controls, palette colours from the photo, free drag with centre snapping, and a WYSIWYG preview that uses the exact same renderer as the wallpaper.
Samsung’s own clock is hidden by setting it to a plain font, Blur off, and 0% opacity (see the setup steps above).
