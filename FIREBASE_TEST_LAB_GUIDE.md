# Option B: Cloud Build with GitHub Actions & Firebase Test Lab

This turnkey setup allows you to compile **`app-debug.apk`** in the cloud (no local Android Studio or JDK needed) and test it on real Android tablets in **Firebase Test Lab**.

---

### Step 1: Download the Project or Push to GitHub
1. Click **"Export Android Project ZIP"** in the top bar.
2. Unzip the project folder, initialize a git repo, and push it to GitHub:
   ```bash
   git init
   git add .
   git commit -m "feat: initial OnAir Studio commit with GitHub Actions CI"
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/onair-studio.git
   git push -u origin main
   ```

---

### Step 2: GitHub Actions Automated Cloud Build
- The included `.github/workflows/build.yml` runs automatically on every commit.
- Ubuntu cloud runners will:
  1. Set up JDK 17 & Android SDK 34
  2. Run `./gradlew assembleDebug`
  3. Generate and verify `app/build/outputs/apk/debug/app-debug.apk`
  4. Attach the compiled `app-debug.apk` under the **"Artifacts"** section of the run.

**To download your compiled binary:**
1. Navigate to your GitHub repository -> **Actions** tab.
2. Click the latest workflow run: **"Build & Assemble Debug APK"**.
3. Under **Artifacts** at the bottom, click **`app-debug-apk`** to download your ready-to-test APK!

---

### Step 3: Run Firebase Test Lab
Once you download `app-debug.apk`:

#### Method A: Via Firebase Console (Zero-CLI)
1. Open [Firebase Console](https://console.firebase.google.com/) and select your Firebase Project.
2. In the left navigation, open **Quality > Test Lab**.
3. Click **"Run a test"** -> Select **"Robo"** or **"Game Loop / Custom"**.
4. Drag and drop your downloaded **`app-debug.apk`**.
5. Choose device matrix: Select **Google Pixel Tablet**, **Samsung Galaxy Tab S9**, or modern Android 14 phones.
6. Click **Start Test**! Firebase will install the app, execute video streaming permission prompts, test OpenGL frame rendering, and generate test videos, logs, and performance metrics.

#### Method B: Via Google Cloud CLI (gcloud)
```bash
gcloud firebase test android run \
  --type robo \
  --app app-debug.apk \
  --device model=tangorpro,version=34,locale=en,orientation=landscape \
  --timeout 5m
```
