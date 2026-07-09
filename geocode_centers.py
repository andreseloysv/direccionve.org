#!/usr/bin/env python3
"""
geocode_centers.py — Voting center → Plus Code converter

Processes the 21.5M-line Venezuelan voter registry to extract 15,748
unique voting centers, geocodes them via a SINGLE Overpass API call
for parish/municipality centroids, and assigns Plus Codes.

Runtime: ~40s total (extraction ~10s, Overpass ~20s, encoding ~1s)
Subsequent runs: ~10s (Overpass cached to disk)

Usage:  python3 geocode_centers.py
Output: data-centers.json
"""

import json, math, os, re, sys, unicodedata
import urllib.request, urllib.parse

RE_FILE   = 're_evento_2025.txt'
OUTPUT    = 'data-centers.json'
CACHE     = 'geocode-cache.json'

# ─── OLC Plus Code Encoder ────────────────────────────────────────
_ALP = '23456789CFGHJMPQRVWX'

def plus_code(lat, lng):
    lat = max(-90.0, min(90.0, float(lat)))
    lng = max(-180.0, min(180.0, float(lng)))
    if lng == 180: lng = -180.0
    lat += 90; lng += 180
    c = ''
    for r in (20, 1, .05, .0025, .000125):
        if len(c) >= 10: break
        a = min(int(lat / r), 19); b = min(int(lng / r), 19)
        lat -= a * r; lng -= b * r
        c += _ALP[a] + _ALP[b]
    return c[:8] + '+' + c[8:]

# ─── Name Normalization ──────────────────────────────────────────
def norm(name):
    """Normalize RE administrative names for matching."""
    s = (name or '').strip()
    for p in ('DTTO. ','EDO. ','MP. ','MCPO. ','PQ. ','CM. ','GRL. '):
        if s.upper().startswith(p):
            s = s[len(p):].strip(); break
    for p in ('BLVNO ','BOLIVARIANO '):
        if s.upper().startswith(p):
            s = s[len(p):].strip(); break
    s = unicodedata.normalize('NFD', s)
    s = ''.join(c for c in s if unicodedata.category(c) != 'Mn')
    return re.sub(r'\s+', ' ', s.lower().strip())

def norm_osm(name):
    """Normalize OSM administrative names for matching."""
    s = (name or '').strip()
    # Strip parenthetical notes: "El Playón (Santa Rosalía)" → "El Playón"
    s = re.sub(r'\s*\([^)]*\)\s*$', '', s)
    for p in ('Municipio ','Parroquia ','Estado ','Distrito '):
        if s.startswith(p):
            s = s[len(p):]; break
    s = unicodedata.normalize('NFD', s)
    s = ''.join(c for c in s if unicodedata.category(c) != 'Mn')
    return re.sub(r'\s+', ' ', s.lower().strip())

# ─── Hardcoded State Centroids (fallback) ────────────────────────
STATES = {
    'capital':(10.4880,-66.8792), 'anzoategui':(8.5916,-63.5887),
    'apure':(7.8894,-69.7500), 'aragua':(10.2310,-67.5947),
    'barinas':(8.6226,-70.2074), 'bolivar':(7.0000,-64.0000),
    'carabobo':(10.1776,-67.9921), 'cojedes':(9.6667,-68.5833),
    'delta amacuro':(9.0500,-62.0500), 'falcon':(11.1812,-69.8596),
    'guarico':(8.7500,-66.2500), 'lara':(10.0678,-69.3293),
    'merida':(8.5897,-71.1561), 'miranda':(10.2506,-66.4167),
    'monagas':(9.7500,-63.2500), 'nueva esparta':(11.0164,-63.9167),
    'portuguesa':(9.0594,-69.7500), 'sucre':(10.4532,-63.2366),
    'tachira':(7.7714,-72.2263), 'trujillo':(9.3658,-70.4270),
    'la guaira':(10.5900,-66.9300), 'vargas':(10.5900,-66.9300),
    'yaracuy':(10.3500,-69.0167), 'zulia':(10.0000,-72.0000),
    'amazonas':(3.4166,-65.8561),
    'dependencias federales':(11.8000,-65.2000),
}

# ─── Step 1: Extract unique voting centers ───────────────────────
def extract_centers():
    print('⏳ Extracting centers from RE...', file=sys.stderr, flush=True)
    centers = {}
    n = 0
    with open(RE_FILE, 'r', encoding='utf-8', errors='replace') as f:
        f.readline()  # skip header
        for line in f:
            n += 1
            p = line.split('|')
            if len(p) < 9: continue
            code = p[6]
            if code not in centers:
                centers[code] = [
                    p[1],  # estado
                    p[3],  # municipio
                    p[5],  # parroquia
                    p[7],  # nombre
                    p[8],  # direccion
                    0      # voters
                ]
            centers[code][5] += 1
            if n % 5_000_000 == 0:
                print(f'  ...{n:,} lines', file=sys.stderr, flush=True)
    print(f'✅ {len(centers):,} centers from {n:,} records', file=sys.stderr, flush=True)
    return centers

# ─── Step 2: Fetch parish centroids (single Overpass call) ───────
def fetch_overpass():
    if os.path.exists(CACHE):
        print('📦 Using cached Overpass data', file=sys.stderr, flush=True)
        with open(CACHE) as f:
            return json.load(f)

    print('🌐 Querying Overpass API...', file=sys.stderr, flush=True)
    query = '''
[out:json][timeout:120];
area["ISO3166-1"="VE"]->.ve;
rel["boundary"="administrative"]["admin_level"~"^[468]$"](area.ve);
out center;
'''
    data = urllib.parse.urlencode({'data': query}).encode()
    result = {'elements': []}
    for server in ['https://overpass.kumi.systems/api/interpreter',
                   'https://overpass-api.de/api/interpreter']:
        try:
            req = urllib.request.Request(server, data=data,
                headers={'User-Agent': 'DireccionVE/1.0'})
            with urllib.request.urlopen(req, timeout=300) as resp:
                result = json.loads(resp.read().decode())
            if result.get('elements'):
                break
        except Exception as e:
            print(f'  ⚠️  {server.split("//")[1].split("/")[0]}: {e}',
                  file=sys.stderr)

    if not result.get('elements'):
        print('   Using state-level fallback only', file=sys.stderr)
        return {'elements': []}

    with open(CACHE, 'w') as f:
        json.dump(result, f)

    n = len(result.get('elements', []))
    print(f'✅ {n} admin boundaries retrieved', file=sys.stderr, flush=True)
    return result

# ─── Step 3: Build geocoding index ──────────────────────────────
def build_index(overpass):
    idx = {4: {}, 6: {}, 8: {}}
    for el in overpass.get('elements', []):
        tags = el.get('tags', {})
        level = int(tags.get('admin_level', 0))
        if level not in idx: continue
        name = tags.get('name', tags.get('name:es', ''))
        center = el.get('center', {})
        lat, lng = center.get('lat'), center.get('lon')
        if not name or lat is None or lng is None: continue
        key = norm_osm(name)
        if key and key not in idx[level]:
            idx[level][key] = (lat, lng)

    # Build prefix index for fuzzy matching (parish level only)
    idx['fuzzy8'] = {}
    for key, coords in idx[8].items():
        # Index every word-boundary prefix: "altagracia de orituco" → "altagracia", "altagracia de", "altagracia de orituco"
        words = key.split()
        for i in range(1, len(words)+1):
            prefix = ' '.join(words[:i])
            if prefix not in idx['fuzzy8']:
                idx['fuzzy8'][prefix] = coords

    print(f'📍 Index: {len(idx[4])} states, {len(idx[6])} municipalities, '
          f'{len(idx[8])} parishes ({len(idx["fuzzy8"])} fuzzy keys)',
          file=sys.stderr, flush=True)
    return idx

# ─── Step 4: Geocode + assign Plus Codes ─────────────────────────
def _km(lat1, lon1, lat2, lon2):
    """Quick haversine distance in km."""
    dlat = math.radians(lat2-lat1); dlon = math.radians(lon2-lon1)
    a = math.sin(dlat/2)**2 + math.cos(math.radians(lat1))*math.cos(math.radians(lat2))*math.sin(dlon/2)**2
    return 6371 * 2 * math.asin(min(1, math.sqrt(a)))

def _near(lat, lng, ref, max_km=300):
    """Check if (lat,lng) is within max_km of ref centroid."""
    if not ref: return True
    return _km(lat, lng, ref[0], ref[1]) < max_km

def geocode(centers, idx):
    print('🔗 Matching centers to coordinates...', file=sys.stderr, flush=True)
    stats = {'parish': 0, 'parish_fuzzy': 0, 'muni': 0, 'state_osm': 0,
             'state_hc': 0, 'fallback': 0}
    results = []

    for code, (estado, muni, parish, nombre, direccion, voters) in centers.items():
        lat = lng = None

        # Get state centroid for cross-state validation
        sk = norm(estado)
        state_ref = idx[4].get(sk) or STATES.get(sk)

        # Try exact parish match (admin_level=8) — validate near state
        k = norm(parish)
        if k and k in idx[8]:
            plat, plng = idx[8][k]
            if _near(plat, plng, state_ref):
                lat, lng = plat, plng; stats['parish'] += 1
        # Try fuzzy parish match (prefix) — validate near state
        if lat is None and k and k in idx['fuzzy8']:
            plat, plng = idx['fuzzy8'][k]
            if _near(plat, plng, state_ref):
                lat, lng = plat, plng; stats['parish_fuzzy'] += 1
        # Try municipality (admin_level=6)
        if lat is None:
            k = norm(muni)
            if k and k in idx[6]:
                plat, plng = idx[6][k]
                if _near(plat, plng, state_ref):
                    lat, lng = plat, plng; stats['muni'] += 1
            elif k and k in idx['fuzzy8']:
                plat, plng = idx['fuzzy8'][k]
                if _near(plat, plng, state_ref):
                    lat, lng = plat, plng; stats['muni'] += 1
        # Try state from Overpass (admin_level=4)
        if lat is None:
            k = norm(estado)
            if k and k in idx[4]:
                lat, lng = idx[4][k]; stats['state_osm'] += 1
        # Hardcoded state centroids
        if lat is None:
            k = norm(estado)
            if k in STATES:
                lat, lng = STATES[k]; stats['state_hc'] += 1
            else:
                lat, lng = 8.0, -66.0; stats['fallback'] += 1

        results.append([
            code, nombre, estado, muni, parish,
            voters, round(lat, 6), round(lng, 6), plus_code(lat, lng)
        ])

    results.sort(key=lambda r: -r[5])  # sort by voters desc

    total = sum(stats.values())
    print(f'✅ Geocoded {total:,}: {stats["parish"]+stats["parish_fuzzy"]} parish '
          f'({stats["parish"]} exact + {stats["parish_fuzzy"]} fuzzy), '
          f'{stats["muni"]} muni, '
          f'{stats["state_osm"]+stats["state_hc"]} state, {stats["fallback"]} fallback',
          file=sys.stderr, flush=True)
    return results

# ─── Step 5: Write output ───────────────────────────────────────
def write_output(results):
    with open(OUTPUT, 'w') as f:
        json.dump(results, f, ensure_ascii=False)
    size = os.path.getsize(OUTPUT)
    total_voters = sum(r[5] for r in results)
    print(f'💾 {OUTPUT}: {len(results):,} centers, {size/1024:.0f}KB', file=sys.stderr)
    print(f'📊 {total_voters:,} voters total', file=sys.stderr)
    print(f'\n   Top 5 centers:', file=sys.stderr)
    for r in results[:5]:
        print(f'   {r[1][:45]:45s} {r[5]:>7,} voters  {r[8]}', file=sys.stderr)

# ─── Main ────────────────────────────────────────────────────────
if __name__ == '__main__':
    centers  = extract_centers()
    overpass = fetch_overpass()
    idx      = build_index(overpass)
    results  = geocode(centers, idx)
    write_output(results)
    print('\n✨ Done!', file=sys.stderr)
