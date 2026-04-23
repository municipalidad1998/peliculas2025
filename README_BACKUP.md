# Backup a Telegram - Aplicación Android

Esta aplicación permite realizar backups automáticos de archivos del teléfono y subirlos a Telegram utilizando solo el token del bot y el ID del chat.

## Características

- 📱 Interfaz simple para ingresar token y chat ID
- 🔄 Backup automático de archivos
- ☁️ Subida a Telegram mediante API
- 🔐 Manejo de permisos de almacenamiento
- ⚡ Ejecución en segundo plano

## Configuración

1. **Crear un bot de Telegram**:
   - Busca @BotFather en Telegram
   - Crea un nuevo bot con `/newbot`
   - Obtén el token del bot

2. **Obtener el Chat ID**:
   - Envía un mensaje al bot
   - Visita `https://api.telegram.org/bot<TU_TOKEN>/getUpdates`
   - Encuentra el chat_id en la respuesta JSON

3. **Configurar la aplicación**:
   - Abre la app
   - Ingresa el token y chat ID
   - Permite los permisos de almacenamiento
   - Presiona "Iniciar Backup"

## Permisos Requeridos

- `READ_EXTERNAL_STORAGE`: Para leer archivos del dispositivo
- `WRITE_EXTERNAL_STORAGE`: Para acceder al almacenamiento
- `INTERNET`: Para conectarse a la API de Telegram

## Estructura del Proyecto

- `BackupActivity.kt`: Actividad principal con interfaz de usuario
- `BackupService.kt`: Servicio que maneja el backup y subida
- `activity_backup.xml`: Layout de la interfaz de usuario

## Cómo Compilar

1. Asegúrate de tener Java JDK y Android SDK instalados
2. Ejecuta: `./gradlew assembleDebug`
3. El APK se generará en `app/build/outputs/apk/debug/`

## Uso

1. Instala la aplicación en tu dispositivo Android
2. Abre la app e ingresa tu token de bot y chat ID
3. Concede los permisos necesarios
4. Presiona "Iniciar Backup"
5. La aplicación buscará y subirá archivos automáticamente

## Notas

- La aplicación respalda archivos de directorios comunes (Descargas, Documentos, Imágenes, etc.)
- Se limita a los primeros 100 archivos para evitar saturación
- Los archivos se suben uno por uno con intervalos de 1 segundo
- Requiere conexión a Internet activa