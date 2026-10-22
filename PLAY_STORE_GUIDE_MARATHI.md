# Time Table Manager — बदलांची माहिती (Fixes) आणि Google Play Store वर Publish करण्याची संपूर्ण माहिती (Marathi Guide)

## १. तुमच्या अ‍ॅपमध्ये केलेले महत्त्वाचे बदल (Fixes Made in Your App)

1. **वेळ झाल्यावर स्क्रीन जागृत होणे आणि ३ सेकंद Vibration (Fixed Alarm & 3-Second Vibration):**

   * **समस्या काय होती:** आधीच्या कोडमध्ये `VibrationHelper.kt` मध्ये `USAGE_ALARM` (Alarm Attributes) आणि `WakeLock` वापरले नव्हते. त्यामुळे Android 10 ते Android 16 (आणि Xiaomi/MIUI, Samsung, Realme, Vivo, Oppo फोनवर) फोन लॉक असताना किंवा बॅकग्राउंडमध्ये असताना सिस्टीम Vibration ब्लॉक करत होती. तसेच `LectureAlarmReceiver` आणि `FullScreenAlertActivity` मधल्या `3500ms` debounce मुळे स्क्रीन उघडल्यावर होणारे Vibration स्किप होत होते.

   * **आता काय सुधारले:**

     * `VibrationHelper.kt` मध्ये **`VibrationAttributes.USAGE_ALARM`**, **`AudioAttributes.USAGE_ALARM`**, आणि **`PowerManager.WakeLock`** जोडले आहे.

     * ३ सेकंदांचा हाय-पॉवर Waveform पॅटर्न (`0ms, 850ms, 125ms, 850ms, 125ms, 1050ms` — Amplitude `255`) सेट केला आहे, ज्यामुळे फोन Silent किंवा लॉक असला तरीही लेक्चरची वेळ झाल्यावर स्क्रीन आपोआप ऑन होईल आणि फोन पूर्ण ३ सेकंद जोरदार Vibrate होईल.

     * `FullScreenAlertActivity.kt` मध्ये पांढरी अलर्ट स्क्रीन ३ सेकंदांऐवजी **१५ सेकंद** चालू राहील (आणि ३ सेकंद Vibrate होईल), जेणेकरून तुम्हाला विषयाचे नाव वाचायला आणि **Snooze (5m / 10m)** किंवा **Dismiss** दाबायला पुरेसा वेळ मिळेल.

2. **अचूक वेळ आणि सर्व Repeat नियमांची सुधारणा (`AlarmScheduler.kt` & `AddEditLectureDialog.kt`):**

   * आधी लेक्चर जोडताना मिनिटे फक्त ५ च्या पटीत (`00, 05, 10...`) निवडता येत होती; आता तुम्ही **१-१ मिनिटाच्या फरकाने (`00` ते `59`)** कोणतीही अचूक वेळ सेट करू शकता.

   * `Once` (एकदाच) आणि `Custom` (`Mon, Wed, Fri` इ.) दिवसांचे अलार्म आता अचूक दिवशी आणि वेळी वाजतील.

3. **खरे Google AdMob SDK इंटिग्रेशन (`AdMobManager.kt` & `AdMobBannerCard.kt`):**

   * आधीच्या प्रोजेक्टमध्ये फक्त खोटे (Mock) डिझाइन होते आणि Google AdMob ची लायब्ररी व `INTERNET` परमिशन नव्हती.

   * आता `gradle/libs.versions.toml` आणि `app/build.gradle.kts` मध्ये अधिकृत **`com.google.android.gms:play-services-ads:23.6.0`** जोडले आहे.

   * `AndroidManifest.xml` मध्ये `INTERNET`, `ACCESS_NETWORK_STATE`, आणि `com.google.android.gms.permission.AD_ID` परमिशन आणि तुमचा AdMob App ID (`ca-app-pub-7527835129661450~4783640462`) जोडला आहे.

   * `TimeTableApplication.kt` मध्ये `MobileAds.initialize()` सुरू केले असून `AdMobBannerCard.kt` मध्ये तुमचा खरा Banner Ad Unit ID (`ca-app-pub-7527835129661450/9591356425`) वापरून खरा `AdView` जोडला आहे.

4. **Upload Keystore (`my-upload-key.jks`) तयार केला आहे:**

   * प्रोजेक्टमध्ये `my-upload-key.jks`, `debug.keystore`, आणि `keystore.properties` आधीच तयार करून जोडले आहेत, जेणेकरून `.aab` फाईल तयार करताना कोणतीही Signing Error येणार नाही.

## २. `.aab` (Android App Bundle) फाईल कशी मिळवायची? (दोन सोपे मार्ग)

### पर्याय A: Google AI Studio मधून थेट `.aab` डाउनलोड करणे (सर्वात सोपा मार्ग)

तुमचा प्रोजेक्ट Google AI Studio चा आहे (`https://ai.studio/apps/a5c01ba6-de7a-4ffb-b932-6518b00cb32a`):

1. आमच्याकडून दिलेली **`time-table-manager-fixed-playstore-ready.zip`** फाईल तुमच्या कॉम्प्युटर/मोबाईलवर अनझिप (Extract) करा.

2. AI Studio मध्ये तुमचे अ‍ॅप उघडा आणि सुधारलेल्या फाईल्स तिथे अपलोड/रिप्लेस करा.

3. वरच्या उजव्या कोपऱ्यातील **Download / Export / Build** पर्यायावर क्लिक करून **Download Android App Bundle (`.aab`)** निवडा.

### पर्याय B: Android Studio मधून १ मिनिटात `.aab` तयार करणे

1. तुमच्या कॉम्प्युटरवर **Android Studio** उघडा आणि अनझिप केलेला प्रोजेक्ट फोल्डर **Open** करा.

2. Gradle Sync पूर्ण होण्यासाठी १-२ मिनिटे थांबा.

3. वरच्या मेनूमधून **Build ➔ Generate Signed App Bundle / APK...** वर क्लिक करा (किंवा खालच्या **Terminal** मध्ये `./gradlew bundleRelease` ही कमांड चालवा).

4. जर विझार्ड वापरत असाल तर:

   * **Android App Bundle** निवडा आणि **Next** दाबा.

   * **Key store path:** प्रोजेक्ट फोल्डरमधील `my-upload-key.jks` निवडा.

   * **Key store password:** `timetable123`

   * **Key alias:** `upload`

   * **Key password:** `timetable123`

   * **Next** दाबून **release** निवडा आणि **Create** वर क्लिक करा.

5. तुमची तयार `.aab` फाईल या फोल्डरमध्ये मिळेल:
   `app/build/outputs/bundle/release/app-release.aab`

## ३. Google Play Store वर अ‍ॅप Publish करण्याची संपूर्ण पद्धत (Step-by-Step)

### पायरी १: Google Play Console अकाउंट तयार करणे

1. [Google Play Console](https://play.google.com/console) वर जा आणि तुमच्या Google अकाउंटने लॉगिन करा.

2. नवीन Developer Account साठी **\$25 (अंदाजे ₹२,१००) एकदाच (One-time) नोंदणी फी** भरावी लागते (Debit/Credit Card जिथे International पेमेंट चालू आहे त्याद्वारे).

3. तुमची ओळख पडताळणी (Identity Verification - PAN Card / Passport / Driving License) पूर्ण करा.
   *(टीप: जर तुमचे नवीन Personal Developer Account असेल, तर Google च्या नियमानुसार Production वर जाण्यापूर्वी किमान १२ टेस्टर्सकडून १४ दिवस Closed Testing पूर्ण करावे लागते.)*

### पायरी २: नवीन अ‍ॅप तयार करणे (Create App)

1. Play Console डॅशबोर्डवर **Create app** बटनावर क्लिक करा.

2. खालील माहिती भरा:

   * **App name:** `Time Table Manager`

   * **Default language:** `English (United States)`

   * **App or game:** `App`

   * **Free or paid:** `Free`

3. खालील Declarations वर टिक (Check) करा आणि **Create app** वर क्लिक करा.

### पायरी ३: Dashboard वरील आवश्यक माहिती भरणे (App Content / Policy)

डाव्या बाजूच्या मेनूमध्ये **Policy and programs ➔ App content** (किंवा **Dashboard** वरील सेक्शन्स) एक-एक करून पूर्ण करा:

1. **Privacy Policy:**

   * तुमच्या अ‍ॅपमध्ये Settings मध्ये Privacy Policy स्क्रीन आधीच आहे. Play Store साठी तीच माहिती एका पब्लिक लिंकवर (उदा. Google Sites किंवा GitHub Pages वर फ्रीमध्ये) पेस्ट करून तिची URL येथे टाका.

2. **App access:**

   * `"All functionality in my app is available without any access restrictions"` निवडा (कारण तुमच्या अ‍ॅपमध्ये लॉगिन पासवर्डची गरज नाही) आणि **Save** करा.

3. **Ads (जाहिराती):**

   * `"Yes, my app contains ads"` निवडा (कारण तुम्ही AdMob इंटिग्रेट केले आहे) आणि **Save** करा.

4. **Content rating:**

   * **Start questionnaire** वर क्लिक करा ➔ तुमचा ईमेल टाका ➔ Category मध्ये **Utility, Productivity, Communication, or other** निवडा ➔ सर्व प्रश्नांना **No** निवडा ➔ **Save** आणि **Submit** करा.

5. **Target audience and content:**

   * वयोगट **13-15, 16-17, 18 and over** निवडा (१३ वर्षांखालील लहान मुलांचा गट निवडू नका, म्हणजे Families Policy चा त्रास होणार नाही).

6. **Data safety:**

   * **Next** दाबा ➔ `"Does your app collect or share any of the required user data types?"` यावर **Yes** निवडा (कारण Google AdMob जाहिरातींसाठी Device ID वापरतो).

   * **Device or other IDs** वर टिक करा ➔ हे डेटा **Collected** आणि **Shared** (Advertising or marketing साठी) निवडा आणि **Save** करा.

7. **Government apps & Financial features:**

   * दोन्ही ठिकाणी **No** निवडा.

### पायरी ४: Store Listing (अ‍ॅपची माहिती आणि फोटो)

डाव्या मेनूमधून **Grow users ➔ Store presence ➔ Main store listing** वर जा:

1. **App name:** `Time Table Manager`

2. **Short description (८० अक्षरांपर्यंत):**
   `Smart class timetable manager with exact lecture alarms & 3s vibration alerts.`

3. **Full description:**
   अ‍ॅपच्या वैशिष्ट्यांची माहिती लिहा (Daily/Weekly lecture schedule, exact wake-up alarm, 3-second vibration, full-screen white alert with snooze, dark mode, room numbers & subject emojis).

4. **Graphics (फोटो आणि आयकॉन):**

   * **App icon:** `512 x 512 px` (PNG)

   * **Feature graphic:** `1024 x 500 px` (PNG/JPEG)

   * **Phone screenshots:** तुमच्या मोबाईलवर अ‍ॅपचे किमान २ ते ४ स्क्रीनशॉट्स (Home Screen, Add Lecture Dialog, Weekly Schedule, Settings) अपलोड करा.

### पायरी ५: `.aab` फाईल अपलोड करणे आणि Release करणे

1. डाव्या मेनूमधून **Test and release ➔ Production** (किंवा नवीन अकाउंट असल्यास **Testing ➔ Closed testing**) वर जा.

2. **Countries / regions** टॅबमध्ये जाऊन **Add countries / regions** वर क्लिक करा आणि **India** (किंवा सर्व देश) निवडा.

3. **Create new release** वर क्लिक करा.

4. **App bundles** विभागात **Upload** वर क्लिक करून तुमची तयार केलेली **`app-release.aab`** फाईल अपलोड करा.

5. **Release name:** `2.1.0 (2)` आपोआप येईल.

6. **Release notes** मध्ये लिहा:
   `Initial release of Time Table Manager with exact lecture alarms, 3-second vibration wake screen, and weekly schedule management.`

7. **Next** दाबा, काही Warnings असल्यास तपासा आणि शेवटी **Save and publish / Send for review** वर क्लिक करा.

8. Google ची टीम २ ते ५ दिवसांत तुमचे अ‍ॅप तपासून (Review करून) Play Store वर Live करेल!