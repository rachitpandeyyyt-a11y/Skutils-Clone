# SkUtils — Sketchware Companion

**Made by Rachit Pandey**

Sketchware ke projects ko manage karne wali ek chhoti si utility app. Ye app
`/sdcard/.sketchware` (yaani `.sketchware`) folder ko padhti hai, usme project
backup banati hai, restore karti hai, aur projects / activities ko clone ya
copy karti hai.

> Important: ye environment APK build nahi kar sakta (JDK, Gradle aur Android
> SDK nahi hai). Neeche diye steps se apne phone/PC par khud build karna hai.

---

## Features (jo SkUtils jaisa chahiye tha)

| Feature | Screen / Class |
|---|---|
| Project backup (single project → `.swb`) | `BackupActivity` + `util/BackupManager.java` |
| Backup browse / view / share | `RestoreActivity`, `ViewBackupedActivity` |
| Restore (pura ya chuni hui activities) | `SelectToRestoreActivity` + `BackupManager` |
| Activity clone (same project me copy) | `CloneActivity` + `util/ProjectCloner.java` |
| Activity copy (ek project se dusre me) | `CloneActivity` + `ProjectCloner` |
| Pura project clone (naya sc_id) | `CloneActivity` → `ProjectCloner.cloneProject()` |
| Edit Files (activity list add/rename/clone/delete) | `EditorActivity` |
| Project To Top (project list me sabse upar) | `MainActivity` → `SketchwareUtil.touchProject()` |
| Debug (paths + decrypt status) | `DebugActivity` |

---

## Sketchware ka data layout (jise app use karti hai)

```
.sketchware/
  data/<sc_id>/                          project ka asli data
    file, view, logic, library, resource, project_config, permission,
    view_root, custom_blocks, local_library, build_config, ...
    files/{java,assets,resource,native_libs}
    Injection/{androidmanifest, app_components.txt, ...}
  mysc/list/<sc_id>/project              project metadata (JSON, encrypted)
  mysc/<sc_id>/                          Sketchware ka generated gradle project
  backups/<ws_name>/<name>.swb           Sketchware ka backup screen bhi yahi dekhta hai
  resources/{images,fonts,sounds}/<sc_id>/
  resources/icons/<sc_id>.png
  libs/local_libs/<libname>/
```

### Encryption

Sketchware `file`, `view`, `logic`, `library`, `resource` aur `project` ko
**AES-128 / CBC / PKCS#5** se encrypt karta hai, jisme
**key = IV = `sketchwaresecure`**. Ye logic `util/CryptoUtil.java` me hai.
`project_config` aur `permission` plain hote hain, isliye unhe as-is read kiya
jaata hai.

---

## Project structure

```
app banao/
├── settings.gradle
├── build.gradle
├── gradle.properties
└── app/
    ├── build.gradle                     namespace / applicationId = rachit.skutils
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml          9 activities + FileProvider
        ├── java/rachit/skutils/
        │   ├── activity/                Splash, Main, Backup, Restore,
        │   │                            SelectToRestore, ViewBackuped,
        │   │                            Clone, Editor, Debug
        │   ├── adapter/                 Project, Activity, Backup adapters
        │   ├── model/                   Project, ActivityItem
        │   └── util/                    CryptoUtil, FileUtil, SketchwareUtil,
        │                                BackupManager, ProjectCloner,
        │                                ProgressListener, RuntimeUtil
        └── res/                         layouts, drawables, values, menu, xml
```

---

## GitHub par upload (mobile se, ek hi baar me)

Zyada files (58) hain aur `.github` hidden folder hai, isliye GitHub website se
ek-ek file karna mushkil hai. Sabse aasan:

### Tarika A — Termux (recommended, ek command)

1. Play Store / F-Droid se **Termux** install karo.
2. Termux kholo aur chalao:
   ```sh
   pkg update -y && pkg install git -y
   ```
3. GitHub par ek **khaali repo** banao (README/gitignore ke bina).
4. GitHub → Settings → Developer settings → **Personal access tokens
   (classic)** → Generate new token → `repo` scope tick → copy.
5. Ye project folder Termux me le jao aur script chalao:
   ```sh
   cd "/storage/emulated/0/app banao"
   sh push-to-github.sh <github-username> <repo-name> <token>
   ```
   (script is repo me already hai: `push-to-github.sh`)

### Tarika B — MGit / Pocket Git (git app)

1. Play Store se **MGit** install karo.
2. Khaali GitHub repo clone karo (local folder ban jayega).
3. Ek file manager se `app banao` ka poora content us cloned folder me copy karo.
4. MGit me commit + push.

### Tarika C — Chrome (desktop mode) se manual

`github.com/<user>/<repo>` kholo → **Add file → Upload files**. Android file
picker foldler select nahi karta, isliye har sub-folder (`app/src/main/java/...`)
me jaakar files select karni padengi. Bas `Create new file` se
`.github/workflows/build.yml` zaroor add karo, warna Actions build nahi chalega.

---

## How to build (khud banana padega)

### Option 1 — GitHub Actions (phone se, recommended)

Isi project me workflow ready hai: `.github/workflows/build.yml`.

1. GitHub par ek naya (private bhi chalega) repo banao.
2. Is folder (`app banao`) ka **poora content** repo me upload/push karo
   (phone se GitHub app / website se bhi upload ho jata hai).
   Dhyan rakho: `.github/`, `app/`, `build.gradle`, `settings.gradle`,
   `gradle.properties` — sab upload karo.
3. Repo ke **Actions** tab me jao → "Build SkUtils APK" → **Run workflow**.
   (Push karte hi apne aap bhi chal jayega.)
4. 2-4 minute me build ho jayega. Us run ke neeche **Artifacts** me
   `SkUtils-debug-apk` milega — download karke install kar lo.

> Note: workflow me `gradle/actions/setup-gradle` use hota hai jo Gradle
> 8.2 install karta hai; isliye repo me wrapper commit karne ki zaroorat nahi.

### Option 2 — Android Studio (PC)

1. Android Studio me `File → Open` karke ye folder (`app banao`) kholo.
2. Gradle sync hone do (AGP 8.1.4, Java 17 chahiye).
3. `Build → Build Bundle(s) / APK(s) → Build APK(s)`.
4. APK milega: `app/build/outputs/apk/debug/app-debug.apk`.

### Option 3 — Command line (JDK 17 + Android SDK)

`local.properties.sample` ko `local.properties` naam se copy karke usme apna
SDK path daalo, phir:

```
gradle assembleDebug
```

### Option 4 — Termux (phone par hi build)

```sh
pkg install openjdk-17 gradle wget unzip
# Android command line tools install karo, phir:
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
echo "sdk.dir=$HOME/android-sdk" > local.properties
gradle assembleDebug
```

---

## Pehli baar chalane par (testing steps)

1. App kholo — storage permission maangega (`MANAGE_EXTERNAL_STORAGE`,
   Android 11+ ke liye).
2. **Debug** screen kholo (kisi project par tap → *Project info*) aur check
   karo ki `file`, `view`, `logic` sab `[decrypted]` dikh rahe hain.
   Agar `[could not decrypt]` aaye to encryption format match nahi hua.
3. Fir Backup banao aur Restore screen se wapas restore karke test karo.

> Backup **banane se pehle** ek project ka copy rakh lena, safety ke liye.

---

## Notes / limitations

- Sirf source code hai; yahan build/test nahi hua.
- Restore karte waqt Sketchware ka generated cache (`mysc/<sc_id>`) delete kar
  diya jaata hai taaki Sketchware project dobara generate kare. Isliye restore
  ke baad **Sketchware ko ek baar band karke kholna** zaroori hai.
- Activity copy karte waqt `view` aur `logic` containers (jo ek screen ke
  hisse hain) ek saath copy hote hain — warna Sketchware screen drop kar deta
  hai.

---

Made with care by **Rachit Pandey**.
