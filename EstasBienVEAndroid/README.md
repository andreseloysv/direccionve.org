# EstasBienVE — App de emergencia sísmica para Venezuela

## Concepto
Después de un sismo, la app pregunta "¿Estás bien?" a todos los usuarios registrados.
Si no responden en 30 minutos, se marcan como **sin respuesta** y su zona como posiblemente afectada.

## Arquitectura

```
┌─────────────────┐     ┌──────────────┐     ┌─────────────────┐
│  USGS Earthquake │────▶│  WorkManager  │────▶│  Push / Local   │
│      API         │     │  (cada 15m)   │     │  Notification   │
└─────────────────┘     └──────────────┘     └────────┬────────┘
                                                       │
                                              ┌────────▼────────┐
                                              │  ¿Estás bien?   │
                                              │  [Sí]  [Ayuda]  │
                                              └────────┬────────┘
                                                       │
                                              ┌────────▼────────┐
                                              │  Mapa en vivo   │
                                              │  🟢 🔴 🟡       │
                                              └─────────────────┘
```

## Stack
- **Kotlin** + Jetpack Compose + Material 3
- **USGS Earthquake API** para detección de sismos
- **Firebase Cloud Messaging** para push notifications
- **WorkManager** para verificación periódica (cada 15 min)
- **DataStore** para perfil y estado local
- **osmdroid** para mapas OpenStreetMap
- **Plus Codes** (mismo algoritmo de DirecciónVE)

## Requisitos
- Android Studio Hedgehog (2023.1+) o superior
- JDK 17
- Gradle 8.9

## Setup

1. Abrir en Android Studio:
   ```
   File → Open → seleccionar carpeta EstasBienVEAndroid
   ```

2. Agregar Firebase:
   - Crear proyecto en [Firebase Console](https://console.firebase.google.com)
   - Descargar `google-services.json` → copiar a `app/`
   - Descomentar `google-services` plugin en `app/build.gradle.kts`

3. Build & Run:
   ```
   ./gradlew assembleDebug
   ```

## Pantallas
1. **Registro** — Nombre, teléfono, Plus Code de tu casa
2. **Home** — Estado actual + sismos recientes (USGS)
3. **Alerta** — "¿Estás bien?" con botones Sí/Necesito ayuda
4. **Mapa** — Mapa con estado de respuestas por zona

## Estructura
```
app/src/main/java/com/direccionve/estasbien/
├── MainActivity.kt          # Entry point
├── EstasBienApp.kt          # Application class
├── data/
│   ├── model/
│   │   ├── Earthquake.kt    # USGS API models
│   │   └── UserStatus.kt    # User response models
│   ├── EarthquakeApi.kt     # Retrofit interface
│   ├── EarthquakeRepository.kt # Data layer
│   ├── PlusCodeUtils.kt     # Plus Code encoder/decoder
│   └── UserPreferences.kt   # DataStore preferences
├── service/
│   ├── EstasBienFirebaseService.kt  # FCM handler
│   └── EarthquakeCheckWorker.kt     # Periodic earthquake check
└── ui/
    ├── theme/
    │   ├── Color.kt          # VE flag + dark theme colors
    │   └── Theme.kt          # Material 3 dark theme
    ├── screens/
    │   ├── RegisterScreen.kt # User registration
    │   ├── HomeScreen.kt     # Dashboard
    │   ├── AlertScreen.kt    # Emergency alert
    │   └── MapScreen.kt      # Response map
    └── navigation/
        └── NavGraph.kt       # Navigation routes
```

## TODO (Backend)
- [ ] Cloud Function que escuche USGS y envíe FCM push
- [ ] Base de datos de usuarios registrados (Supabase/Firebase)
- [ ] Timer de 30 min → marcar como sin respuesta
- [ ] API de agregación para el mapa de respuestas
- [ ] Notificación a contactos de emergencia

## Licencia
MIT
