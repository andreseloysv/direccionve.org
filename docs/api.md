# DirecciónVE — API Reference

Motor de resolución de direcciones para Venezuela.  
Cada metro cuadrado del país tiene un código único ([Plus Code](https://plus.codes)).

## Instalación

```html
<script src="https://direccionve.org/direccionve.js"></script>
```

O descarga el archivo y sírvelo localmente:

```html
<script src="/direccionve.js"></script>
```

## Quick Start

```js
// 1. Cargar datos (fetch desde tu servidor o CDN)
const [houses, streets] = await Promise.all([
  fetch('/data-houses.json').then(r => r.json()),
  fetch('/data-streets.json').then(r => r.json())
]);

// 2. Inicializar el motor
DireccionVE.init(houses, streets);

// 3. Buscar
const results = DireccionVE.search('Altamira Caracas');
console.log(results[0]);
// → { address: "Av. Luis Roche, Altamira", code: "67P8GRJ5+XX", lat: 10.5, lng: -66.85 }

// 4. Resolver un Plus Code
const place = DireccionVE.resolve('67P8GRJ5+XX');
console.log(place.address); // → "Av. Luis Roche, #12, Altamira"
```

---

## API

### `DireccionVE.init(housesData, streetsData)`

Inicializa el motor con los datos descargados. Debe llamarse antes de cualquier otra función.

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `housesData` | `Array` | Array de `[zip, city, street, number, lng, lat]` |
| `streetsData` | `Array` | Array de `[city, name, lng, lat]` |

**Retorna:** `undefined`

---

### `DireccionVE.search(query, limit?)`

Búsqueda de texto libre. Tokeniza la query y puntúa cada dirección por coincidencia de tokens.

| Parámetro | Tipo | Default | Descripción |
|-----------|------|---------|-------------|
| `query` | `string` | — | Texto libre: ciudad, calle, número, código postal |
| `limit` | `number` | `20` | Máximo de resultados |

**Retorna:** `Array<Result>`

```js
DireccionVE.search('los palos grandes', 5);
// → [{ address, code, lat, lng, type }, ...]
```

---

### `DireccionVE.lookup({ city, street, number? })`

Búsqueda exacta por ciudad + calle + número opcional.

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `city` | `string` | Nombre de la ciudad/localidad |
| `street` | `string` | Nombre de la calle |
| `number` | `string` | (Opcional) Número de casa |

**Retorna:** `Result | null`

```js
DireccionVE.lookup({ city: 'Caracas', street: 'Av Libertador', number: '123' });
// → { address: "Av Libertador, #123, Caracas", code: "67P8...", lat, lng }
```

---

### `DireccionVE.resolve(plusCode)`

Dado un Plus Code, encuentra la dirección conocida más cercana.

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `plusCode` | `string` | Plus Code válido (ej: `67P8GRJ5+XX`) |

**Retorna:** `ResolveResult | null`

```js
DireccionVE.resolve('67P8GRJ5+XX');
// → { address, code, lat, lng, nearestLat, nearestLng, distance }
```

El campo `distance` indica metros entre el código y la dirección más cercana.

---

### `DireccionVE.suggest(partial, limit?)`

Autocompletado de calles/ciudades.

| Parámetro | Tipo | Default | Descripción |
|-----------|------|---------|-------------|
| `partial` | `string` | — | Texto parcial (mín. 2 caracteres) |
| `limit` | `number` | `10` | Máximo sugerencias |

**Retorna:** `Array<string>`

```js
DireccionVE.suggest('altam');
// → ["Altamira, Av. Luis Roche", "Altamira, Calle Madrid", ...]
```

---

### `DireccionVE.encode(lat, lng, length?)`

Convierte coordenadas a Plus Code.

| Parámetro | Tipo | Default | Descripción |
|-----------|------|---------|-------------|
| `lat` | `number` | — | Latitud (-90 a 90) |
| `lng` | `number` | — | Longitud (-180 a 180) |
| `length` | `number` | `10` | Longitud del código (8, 10, 11, 12...) |

**Retorna:** `string`

```js
DireccionVE.encode(10.4961, -66.8509);
// → "67P8GRJ5+XX"
```

---

### `DireccionVE.decode(plusCode)`

Decodifica un Plus Code a coordenadas.

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `plusCode` | `string` | Plus Code válido |

**Retorna:** `{ lat, lng, latLo, lngLo, latHi, lngHi, latR, lngR }`

```js
const pos = DireccionVE.decode('67P8GRJ5+XX');
console.log(pos.lat, pos.lng); // 10.4961, -66.8509
```

---

### `DireccionVE.isValidCode(code)`

Valida si un string es un Plus Code válido.

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `code` | `string` | String a validar |

**Retorna:** `boolean`

---

### `DireccionVE.getStats()`

Estadísticas del dataset cargado.

**Retorna:** `{ houses, streets, cities, uniqueStreets }`

```js
DireccionVE.getStats();
// → { houses: 3249, streets: 17351, cities: 412, uniqueStreets: 8920 }
```

---

## Tipos

### `Result`

```ts
interface Result {
  address: string;   // Dirección formateada
  code: string;      // Plus Code de 10 dígitos
  lat: number;       // Latitud
  lng: number;       // Longitud
  type?: string;     // Tipo de lugar (opcional)
}
```

### `ResolveResult`

```ts
interface ResolveResult {
  address: string | null;  // Dirección más cercana (null si no hay datos)
  code: string;            // El Plus Code consultado
  lat: number;             // Latitud del código
  lng: number;             // Longitud del código
  nearestLat?: number;     // Latitud de la dirección más cercana
  nearestLng?: number;     // Longitud de la dirección más cercana
  distance?: number;       // Distancia en metros al punto más cercano
}
```

---

## Datos

Los archivos JSON se sirven desde `https://direccionve.org/`:

| Archivo | Registros | Tamaño | Descripción |
|---------|-----------|--------|-------------|
| `data-houses.json` | 3,249 | ~200 KB | Direcciones con número |
| `data-streets.json` | 17,351 | ~600 KB | Calles sin número específico |
| `data-all-addresses.json` | 145,123 | ~9 MB | Todos los POI de OSM Venezuela |
| `data-centers.json` | 15,748 | ~1.5 MB | Centros de votación geocodificados |

### Formato `data-houses.json`
```json
[["1010","Caracas","Av Libertador","123",-66.85,10.49], ...]
```
`[zip, city, street, number, lng, lat]`

### Formato `data-streets.json`
```json
[["Caracas","Av Libertador",-66.85,10.49], ...]
```
`[city, name, lng, lat]`

### Formato `data-centers.json`
```json
[["010101001","U.E. Simón Bolívar","Distrito Capital","Libertador","Catedral",1250,10.51,-66.91,"67P8CR5F+2X"], ...]
```
`[code, name, state, municipality, parish, voters, lat, lng, plusCode]`

---

## Licencia

MIT — Datos de OpenStreetMap (ODbL) y CNE (público).
