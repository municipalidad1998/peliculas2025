@echo off
echo Construyendo APK de Backup a Telegram...
echo.

REM Verificar si Java está instalado
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java no está instalado o no está en el PATH
    echo Instala Java JDK desde: https://adoptium.net/
    echo Luego configura JAVA_HOME en las variables de entorno
    pause
    exit /b 1
)

REM Verificar si Gradle está disponible
cd streamflix
if exist gradlew (
    echo Ejecutando Gradle Wrapper...
    call gradlew assembleDebug
    echo.
    echo APK generado en: streamflix\app\build\outputs\apk\debug\
) else (
    echo ERROR: Gradle Wrapper no encontrado
    echo Ejecuta: gradle wrapper --gradle-version 8.5
)

echo.
echo Compilación completada!
pause