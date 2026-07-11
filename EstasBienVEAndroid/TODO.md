# EstasBien — TODO

> Estado actual: funciona como rastreador local de sismos.
> Falta: todo el backend, push notifications, mapa, y la red de emergencia.

---

## 🔴 P0 — Backend / Servidor API

- [ ] Crear proyecto backend (Node.js/FastAPI) en carpeta `estasbien-server/`
- [ ] Definir esquema de base de datos (usuarios, respuestas, sismos)
- [ ] Endpoint `POST /api/users/register` — registrar usuario (nombre, teléfono, Plus Code, FCM token)
- [ ] Endpoint `DELETE /api/users/:id` — eliminar datos del usuario (derecho al olvido)
- [ ] Endpoint `GET /api/users/:id/export` — exportar datos del usuario en CSV
- [ ] Endpoint `POST /api/alerts/:earthquakeId/respond` — recibir respuesta del usuario (safe/help)
- [ ] Endpoint `GET /api/alerts/:earthquakeId/responses` — obtener respuestas agregadas por zona para el mapa
- [ ] Endpoint `GET /api/earthquakes/recent` — proxy de USGS con cache
- [ ] Cron job / webhook que consulte USGS cada 5 min y dispare alertas FCM
- [ ] Lógica de timer 30 minutos: marcar usuarios sin respuesta como `NO_RESPONSE`
- [ ] Notificar contactos de emergencia cuando el timer expira o el usuario pide ayuda
- [ ] Rate limiting y validación de input en todos los endpoints
- [ ] Configurar HTTPS / TLS

## 🔴 P0 — Base de datos

- [ ] Elegir base de datos (Supabase / Firebase Realtime DB / PostgreSQL)
- [ ] Tabla `users` — id, nombre, teléfono, plus_code, lat, lng, fcm_token, created_at
- [ ] Tabla `emergency_contacts` — user_id, nombre, teléfono
- [ ] Tabla `earthquake_alerts` — id, usgs_id, magnitud, lugar, timestamp
- [ ] Tabla `responses` — user_id, alert_id, status (SAFE/HELP/NO_RESPONSE), responded_at
- [ ] Índices geoespaciales para consultas por zona
- [ ] Migraciones / seed data

## 🔴 P0 — Firebase & Push Notifications

- [ ] Crear proyecto en Firebase Console
- [ ] Agregar `google-services.json` a `app/`
- [ ] Agregar dependencias Firebase en `build.gradle.kts` (firebase-messaging, firebase-analytics)
- [ ] Descomentar plugin `google-services` en `app/build.gradle.kts`
- [ ] Crear `EstasBienFirebaseService.kt` — recibir push, mostrar notificación, guardar estado
- [ ] Manejar FCM token refresh y enviarlo al backend
- [ ] Cloud Function: escuchar USGS → enviar FCM push a usuarios en radio del sismo
- [ ] Manejar notificaciones en foreground vs background
- [ ] Testear que las notificaciones lleguen con app cerrada

---

## 🟠 P1 — Android: Pantallas faltantes

### MapScreen (no existe)
- [ ] Crear `MapScreen.kt` con osmdroid (OpenStreetMap)
- [ ] Agregar dependencia osmdroid en `build.gradle.kts` (ya definida en `libs.versions.toml`)
- [ ] Mostrar marcadores por zona: 🟢 safe, 🔴 help, 🟡 sin respuesta
- [ ] Consultar endpoint de respuestas agregadas del backend
- [ ] Centrar mapa en la ubicación del usuario
- [ ] Registrar ruta `MapScreen` en `NavGraph.kt`
- [ ] Agregar navegación al mapa desde `HomeScreen`

### Contactos de emergencia
- [ ] Agregar UI de contactos de emergencia en `RegisterScreen.kt` (nombre + teléfono)
- [ ] Guardar contactos en `UserPreferences` / DataStore
- [ ] Sincronizar contactos con el backend
- [ ] Enviar notificación (SMS/push) a contactos cuando el usuario pide ayuda o no responde

### Pantalla de detalle de sismo
- [ ] Crear `EarthquakeDetailScreen.kt` — info completa del sismo + mapa
- [ ] Navegar desde las tarjetas de sismo en `HomeScreen`

## 🟠 P1 — Android: Features incompletos

### Timer de 30 minutos
- [ ] Implementar countdown en `AlertScreen.kt` (barra de progreso + texto)
- [ ] WorkManager job que verifique si la respuesta llegó al backend en 30 min
- [ ] Si no respondió: disparar notificación local de escalamiento
- [ ] Notificar al backend para que alerte a contactos de emergencia

### Sincronización con backend
- [ ] Enviar perfil al backend al registrarse
- [ ] Enviar FCM token al backend
- [ ] Enviar respuesta de alerta al backend (reemplazar el TODO en `MainActivity.kt`)
- [ ] Manejar errores de red y reintentos
- [ ] Sincronizar cuando se recupere la conexión

### Historial de alertas
- [ ] Guardar historial de respuestas localmente
- [ ] Mostrar historial en `HomeScreen` o pantalla nueva
- [ ] Incluir historial en el export CSV

---

## 🟡 P2 — Testing

### Unit tests
- [ ] Tests para `PlusCodeUtils` (encode/decode, edge cases)
- [ ] Tests para `EarthquakeRepository` (Haversine, filtrado por distancia)
- [ ] Tests para `UserPreferences` (save/load/clear/export)
- [ ] Tests para lógica de timer 30 min

### Instrumentation tests
- [ ] Test de flujo de registro completo
- [ ] Test de navegación (Register → Home → Privacy → back)
- [ ] Test de permisos (GPS, teléfono, notificaciones)
- [ ] Test de export CSV (verificar contenido del archivo)
- [ ] Test de borrado de datos (verificar que DataStore queda vacío)

### Integration tests
- [ ] Test de conexión con USGS API
- [ ] Test de conexión con backend (cuando exista)
- [ ] Test de notificaciones push end-to-end

---

## 🟡 P2 — Seguridad

- [ ] Validar Plus Code format en el cliente antes de enviar
- [ ] Sanitizar inputs en el backend (SQL injection, XSS)
- [ ] Cifrar datos sensibles en DataStore (teléfono)
- [ ] Implementar autenticación para el API (JWT o Firebase Auth)
- [ ] Verificar que el export CSV no incluya datos de otros usuarios
- [ ] Agregar certificate pinning para conexiones al backend
- [ ] Revisar permisos en AndroidManifest (¿READ_PHONE_STATE es necesario post-registro?)

---

## 🟡 P2 — Deployment & Release

- [ ] Configurar versionado semántico en `build.gradle.kts`
- [ ] Generar APK firmado para release
- [ ] Crear cuenta de Google Play Developer
- [ ] Preparar listado de Play Store (screenshots, descripción, categoría)
- [ ] Subir política de privacidad a direccionve.org/estasbien/privacidad
- [ ] Configurar Firebase Crashlytics para reportes de crashes
- [ ] Configurar ProGuard/R8 rules
- [ ] CI/CD: GitHub Actions para build + test automático
- [ ] Beta testing con grupo cerrado antes de release público

---

## 🟢 P3 — Mejoras futuras

- [ ] Soporte offline mejorado: cache de mapa tiles para áreas sin internet
- [ ] Modo SOS: botón de pánico que envía ubicación GPS a contactos
- [ ] Integración con Protección Civil Venezuela
- [ ] Notificaciones SMS como fallback cuando no hay internet
- [ ] Widget de Android para estado rápido
- [ ] Soporte multi-idioma (inglés, portugués)
- [ ] App para iOS (Kotlin Multiplatform o SwiftUI)
- [ ] Dashboard web para organismos de emergencia (ver mapa de respuestas)
- [ ] Integración con DirecciónVE web para búsqueda de Plus Codes
- [ ] Accesibilidad: TalkBack, contraste alto, tamaños de fuente

---

## Referencia: qué ya funciona ✓

- ✅ Registro local (nombre, teléfono, Plus Code, GPS)
- ✅ Consulta de sismos recientes (USGS API)
- ✅ Cálculo de distancia Haversine (radio 500km, M≥4.0)
- ✅ Notificaciones locales con botones de acción
- ✅ WorkManager cada 15 min para chequeo en background
- ✅ Plus Code encoder/decoder
- ✅ Dark theme con colores de bandera venezolana
- ✅ Privacidad: export CSV + borrado de datos
- ✅ Edición de nombre en HomeScreen
