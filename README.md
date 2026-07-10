# DirecciónVE

Infraestructura de direccionamiento digital, abierta y soberana para Venezuela.

> "Esquina los angelitos con av. guasdualito, frente al portón rojo" no es una dirección.

Venezuela no tiene un sistema estandarizado de direcciones. DirecciónVE es un proyecto open source para darle a cada metro cuadrado del país un código único, verificable y que funcione 100% offline.

**Website:** [direccionve.org](https://direccionve.org)
**App:** [direccionve.org/app](https://direccionve.org/app.html)

---

## ¿Por qué si ya existe Google Maps?

Google Maps ya geocodifica direcciones y Plus Codes existen desde 2015. Pero nada de eso ha resuelto el problema de Venezuela. Esta es la razón:

**Google no entiende las direcciones venezolanas.** Escribe "frente al portón rojo, Petare" en Google Maps — no devuelve nada. El 80% de las direcciones que los venezolanos usan día a día son referencias orales que ningún geocodificador existente puede resolver.

**Nadie usa Plus Codes.** No hay adopción institucional, ninguna alcaldía los reconoce, los sistemas de delivery no los integran y no hay una capa local que los traduzca a lenguaje que la gente entienda.

**Dependencia y riesgo de bloqueo.** Google puede restringir servicios en Venezuela en cualquier momento (ya ha limitado Google Pay, YouTube Premium, etc.). Los datos de ubicación de los ciudadanos pasan por servidores en EE.UU.

**Sin internet no hay nada.** Google Maps necesita conexión. En zonas con conectividad limitada (que en Venezuela son muchas), el sistema simplemente no sirve.

### DirecciónVE vs. Google Maps

| | Google Maps | DirecciónVE |
|---|---|---|
| Entiende "al lado del Locatel" | ❌ | ✅ Traductor Criollo con IA |
| Funciona offline | ❌ | ✅ Datos locales + PostGIS |
| Datos en servidores del país | ❌ | ✅ On-premise |
| Inmune a sanciones/bloqueos | ❌ | ✅ Stack 100% open source |
| Integrable con sistemas del Estado | ❌ API privada y pagada | ✅ API abierta |
| Catastro para alcaldías | ❌ | ✅ |

**DirecciónVE no compite con Google Maps.** Google Maps es una app de navegación para consumidores. DirecciónVE es infraestructura pública: la capa de adopción que falta para integrar Plus Codes en sistemas reales del país, el traductor que convierte el lenguaje informal en coordenadas, y la soberanía de que los datos geográficos del país no dependan de una empresa extranjera.

La tecnología base existe (OLC es open source). Lo que falta es la implementación local, adaptada y soberana.

---

## Stack Técnico

| Capa | Tecnología | Licencia |
|---|---|---|
| Mapas base | OpenStreetMap (vectores descargados) | ODbL |
| Renderizado | MapLibre GL JS | BSD-3 |
| Motor de coordenadas | Open Location Code (Plus Codes) | Apache 2.0 |
| Base de datos geoespacial | PostgreSQL + PostGIS | PostgreSQL License |
| Traductor Criollo (IA) | Llama 3 / Mistral (local) | Meta Community / Apache 2.0 |
| API | Python (FastAPI) o Node.js | MIT |
| Landing page | Vanilla HTML/CSS/JS | — |

---

## Roadmap

### Fase 0 — Validación (actual)
- [x] Landing page con captura de emails
- [x] Registro a Google Sheets para medir interés
- [ ] Campaña de difusión en redes (Twitter/X, Instagram)
- [ ] Alcanzar 1,000 registros en la lista de espera
- [ ] Publicar el repositorio en GitHub con licencia abierta

### Fase 1 — Prototipo (MVP)
**Objetivo:** Demostrar que una dirección venezolana se puede convertir en un código métrico único.

- [ ] Descargar y procesar los datos OSM de Venezuela (archivos `.pbf`)
- [ ] Implementar el algoritmo Open Location Code (extraer la librería, adaptarla al contexto local)
- [ ] Montar una instancia PostgreSQL + PostGIS con los datos geoespaciales del país
- [ ] Construir una API REST básica:
  - `POST /geocode` — recibe una dirección informal → devuelve Plus Code + coordenadas
  - `GET /reverse/{code}` — recibe Plus Code → devuelve dirección legible
  - `GET /validate/{code}` — verifica si un código existe dentro del territorio venezolano
- [ ] App web mínima: mapa interactivo (MapLibre) donde el usuario toca un punto y obtiene su código
- [ ] Probar con un municipio piloto (ej. un municipio de Carabobo o Miranda)

### Fase 2 — Traductor Criollo
**Objetivo:** Entender direcciones informales venezolanas y convertirlas a coordenadas.

- [ ] Construir el dataset de entrenamiento:
  - Recopilar direcciones informales reales por estado/municipio
  - Mapear jerga regional (bloques, veredas, sectores, urbanizaciones, puntos de referencia)
  - Formato: JSON con estructura `{ "input": "frente al portón rojo, Petare", "output": { "lat": ..., "lng": ..., "plus_code": "..." } }`
- [ ] Fine-tunear un modelo de lenguaje ligero (Llama 3 8B o Mistral 7B) con el dataset
- [ ] Empaquetar el modelo para inferencia local (llama.cpp / Ollama)
- [ ] Integrar con la API: `POST /translate` — recibe texto libre → devuelve código + coordenadas
- [ ] Benchmark de precisión: medir hit rate por estado

### Fase 3 — Casos de Uso Gobierno
**Objetivo:** Despliegue piloto con una alcaldía o ente público.

#### Opción A: Catastro Digital para Alcaldías
- [ ] Generar códigos únicos para cada parcela/local comercial de un municipio
- [ ] Dashboard de administración: mapa con parcelas coloreadas por estado fiscal
- [ ] Integración con sistema de patentes/impuestos: el comercio necesita su código para renovar licencia
- [ ] Módulo de reportes de recaudación

#### Opción B: Geolocalización para Servicios Públicos
- [ ] Módulo que se integra a sistemas de gestión de reportes (tipo VenApp)
- [ ] Cuando un ciudadano reporta una avería, el sistema autogenera el Plus Code del punto
- [ ] Las cuadrillas de Corpoelec/Cantv reciben el código exacto en vez de "frente a la casa de la señora María"
- [ ] Panel de tracking de cuadrillas (precisión configurable: ~14m default, hasta ~3m)

### Fase 4 — Infraestructura Soberana
**Objetivo:** Despliegue en servidores nacionales, independiente de servicios extranjeros.

- [ ] Empaquetar todo el stack en contenedores Docker para despliegue on-premise
- [ ] Documentar instalación en servidores del Estado (Cantv datacenter o equivalente)
- [ ] Modo 100% offline: tile server local con vectores OSM pre-renderizados
- [ ] Pruebas de carga: garantizar que el sistema aguante el volumen de un estado completo
- [ ] Auditoría de seguridad y privacidad de datos geográficos ciudadanos
- [ ] Certificación bajo estándares de software libre exigidos por ley venezolana

### Fase 5 — Escala Nacional
- [ ] Expandir el dataset del Traductor Criollo a todos los estados
- [ ] API pública documentada para que terceros integren (delivery, logística, fintech)
- [ ] SDK para Android/iOS (apps de delivery, encomiendas, transporte)
- [ ] Programa de capacitación para ingenieros del Estado
- [ ] Contrato de soporte y actualización anual con entes públicos

---

## Cómo Contribuir

El proyecto está en fase de validación. Por ahora:

1. **Regístrate** en [direccionve.org](https://direccionve.org) para apoyar la iniciativa
2. **Comparte** el proyecto en redes
3. **Abre un issue** si tienes ideas, datos geográficos de Venezuela, o experiencia con OSM/PostGIS

---

## Licencia

MIT
