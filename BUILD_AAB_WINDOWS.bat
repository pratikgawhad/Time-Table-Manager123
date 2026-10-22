@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"

echo =====================================================================
echo   Time Table Manager v2.1 - Automatic APK ^& Play Store AAB Builder
echo =====================================================================
echo.

:: 1. Auto-detect JAVA_HOME from Android Studio if not already set
if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
        set "PATH=!JAVA_HOME!\bin;!PATH!"
        echo [OK] Found Android Studio Java at: !JAVA_HOME!
    ) else if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" (
        set "JAVA_HOME=%LOCALAPPDATA%\Programs\Android Studio\jbr"
        set "PATH=!JAVA_HOME!\bin;!PATH!"
        echo [OK] Found Android Studio Java at: !JAVA_HOME!
    )
)

:: 2. Auto-detect ANDROID_HOME if not set
if "%ANDROID_HOME%"=="" (
    if exist "%LOCALAPPDATA%\Android\Sdk" (
        set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
        echo [OK] Found Android SDK at: !ANDROID_HOME!
    )
)

:: 3. Find Gradle executable (gradlew.bat, installed gradle, or cached Android Studio gradle)
set "GRADLE_CMD="
if exist "gradlew.bat" (
    set "GRADLE_CMD=gradlew.bat"
) else (
    where gradle >nul 2>nul
    if !ERRORLEVEL! EQU 0 (
        set "GRADLE_CMD=gradle"
    ) else (
        for /d %%G in ("%USERPROFILE%\.gradle\wrapper\dists\gradle-*") do (
            for /d %%H in ("%%G\*") do (
                if exist "%%H\gradle-*\bin\gradle.bat" (
                    for %%B in ("%%H\gradle-*\bin\gradle.bat") do set "GRADLE_CMD=%%~fB"
                )
            )
        )
    )
)

if "!GRADLE_CMD!"=="" (
    echo [INFO] Downloading Gradle 9.3.1 for automatic build...
    powershell -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-9.3.1-bin.zip' -OutFile 'gradle-bin.zip'; Expand-Archive -Path 'gradle-bin.zip' -DestinationPath '.gradle_local' -Force; Remove-Item 'gradle-bin.zip'"
    if exist ".gradle_local\gradle-9.3.1\bin\gradle.bat" (
        set "GRADLE_CMD=.gradle_local\gradle-9.3.1\bin\gradle.bat"
    )
)

if "!GRADLE_CMD!"=="" (
    echo [ERROR] Could not locate Gradle. Please open this folder in Android Studio
    echo         and click Build -^> Generate Signed App Bundle / APK.
    pause
    exit /b 1
)

echo [BUILDING] Running: "!GRADLE_CMD!" assembleDebug assembleRelease bundleRelease
call "!GRADLE_CMD!" assembleDebug assembleRelease bundleRelease

echo.
echo =====================================================================
if exist "app\build\outputs\bundle\release\app-release.aab" (
    echo [SUCCESS] Build Complete!
    echo.
    echo 1. Mobile Test APK ^(Install on your phone^):
    echo    %~dp0app\build\outputs\apk\debug\app-debug.apk
    echo    %~dp0app\build\outputs\apk\release\app-release.apk
    echo.
    echo 2. Play Store AAB ^(Upload to Google Play Console^):
    echo    %~dp0app\build\outputs\bundle\release\app-release.aab
    echo =====================================================================
    explorer.exe "%~dp0app\build\outputs"
) else (
    echo [NOTE] Build finished or requires Android Studio SDK sync.
    echo        If opening for the first time, open this folder in Android Studio.
)
echo.
pause
