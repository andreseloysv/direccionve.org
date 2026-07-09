/**
 * DirecciónVE — Motor de resolución de direcciones
 * 
 * API:
 *   DireccionVE.init(housesData, streetsData) — carga los datos
 *   DireccionVE.search(query) — busca por texto libre → [{address, code, lat, lng}]
 *   DireccionVE.lookup({city, street, number}) — búsqueda exacta → {address, code, lat, lng}
 *   DireccionVE.resolve(plusCode) — código → dirección más cercana
 *   DireccionVE.suggest(partial) — autocompletado → [strings]
 *   DireccionVE.encode(lat, lng, len) — coordenadas → Plus Code
 *   DireccionVE.decode(code) — Plus Code → coordenadas
 */
var DireccionVE = (function () {
  'use strict';

  /* =======================================================
     Open Location Code (Plus Codes) — encode / decode
     ======================================================= */
  var A = '23456789CFGHJMPQRVWX';
  var R = [20, 1, .05, .0025, .000125];

  function encode(lat, lng, len) {
    len = len || 10;
    lat = Math.min(90, Math.max(-90, lat));
    while (lng < -180) lng += 360;
    while (lng >= 180) lng -= 360;
    if (lat === 90) lat -= .9 * R[Math.floor((Math.min(len, 10) - 1) / 2)];

    var code = '', aLat = lat + 90, aLng = lng + 180, i = 0;
    while (i < len) {
      if (i < 10) {
        var p = i / 2, r = R[p];
        var dLat = Math.min(Math.floor(aLat / r), 19);
        var dLng = Math.min(Math.floor(aLng / r), 19);
        aLat -= dLat * r; aLng -= dLng * r;
        code += A[dLat] + A[dLng];
        i += 2;
        if (i === 8) code += '+';
      } else {
        var rLat = R[4], rLng = R[4];
        for (var g = 10; g < i; g++) { rLat /= 5; rLng /= 4; }
        var gLatR = rLat / 5, gLngR = rLng / 4;
        var row = Math.min(Math.floor(aLat / gLatR), 4);
        var col = Math.min(Math.floor(aLng / gLngR), 3);
        aLat -= row * gLatR; aLng -= col * gLngR;
        code += A[row * 4 + col];
        i++;
      }
    }
    if (code.indexOf('+') < 0) code += '+';
    return code;
  }

  function decode(code) {
    code = code.toUpperCase().replace(/\s/g, '');
    var clean = code.replace('+', '');
    var lat = -90, lng = -180, latR, lngR;

    var pairLen = Math.min(clean.length, 10);
    for (var i = 0; i < pairLen; i += 2) {
      var p = i / 2;
      latR = R[p]; lngR = R[p];
      lat += A.indexOf(clean[i]) * latR;
      lng += A.indexOf(clean[i + 1]) * lngR;
    }

    if (clean.length <= 10) {
      var p2 = Math.floor((pairLen - 1) / 2);
      latR = R[p2]; lngR = R[p2];
    }

    for (var i = 10; i < clean.length; i++) {
      var d = A.indexOf(clean[i]);
      if (d < 0) break;
      latR /= 5; lngR /= 4;
      lat += Math.floor(d / 4) * latR;
      lng += (d % 4) * lngR;
    }

    return {
      lat: lat + latR / 2, lng: lng + lngR / 2,
      latLo: lat, lngLo: lng,
      latHi: lat + latR, lngHi: lng + lngR,
      latR: latR, lngR: lngR
    };
  }

  function isValidCode(code) {
    if (!code || code.length < 8) return false;
    code = code.toUpperCase().replace(/\s/g, '');
    if (code.indexOf('+') < 0) return false;
    var clean = code.replace('+', '');
    for (var i = 0; i < clean.length; i++) {
      if (A.indexOf(clean[i]) < 0) return false;
    }
    return true;
  }


  /* =======================================================
     Data store & indexing
     ======================================================= */
  var houses = [];    // [{zip, city, street, number, lng, lat}]
  var streets = [];   // [{zip, city, street, xMin, xMax, yMin, yMax, hMin, hMax}]
  var cityIndex = {}; // city (normalized) → [indices into houses]
  var streetIndex = {}; // "city|street" (normalized) → [indices into houses]
  var allStreetNames = []; // unique "City, Street" for autocomplete

  function normalize(str) {
    return (str || '').toLowerCase()
      .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9\s]/g, '')
      .trim();
  }

  function init(housesData, streetsData) {
    // Parse houses: [zip, city, street, number, lng, lat]
    houses = [];
    for (var i = 0; i < housesData.length; i++) {
      var h = housesData[i];
      if (!h[4] || !h[5]) continue;
      houses.push({
        zip: h[0] || '',
        city: h[1] || '',
        street: h[2] || '',
        number: h[3] || '',
        lng: parseFloat(h[4]),
        lat: parseFloat(h[5])
      });
    }

    // Parse streets: [zip, city, street, xMin, xMax, yMin, yMax, hMin, hMax]
    streets = [];
    for (var i = 0; i < streetsData.length; i++) {
      var s = streetsData[i];
      streets.push({
        zip: s[0] || '',
        city: s[1] || '',
        street: s[2] || '',
        xMin: parseFloat(s[3]),
        xMax: parseFloat(s[4]),
        yMin: parseFloat(s[5]),
        yMax: parseFloat(s[6]),
        hMin: parseInt(s[7]) || 0,
        hMax: parseInt(s[8]) || 0
      });
    }

    // Build indexes
    cityIndex = {};
    streetIndex = {};
    var streetSet = {};

    for (var i = 0; i < houses.length; i++) {
      var h = houses[i];
      var nc = normalize(h.city);
      var ns = normalize(h.street);
      var key = nc + '|' + ns;

      if (!cityIndex[nc]) cityIndex[nc] = [];
      cityIndex[nc].push(i);

      if (!streetIndex[key]) streetIndex[key] = [];
      streetIndex[key].push(i);

      var label = h.city + ', ' + h.street;
      if (!streetSet[label]) {
        streetSet[label] = true;
      }
    }

    // Also index streets data
    for (var i = 0; i < streets.length; i++) {
      var s = streets[i];
      var label = s.city + ', ' + s.street;
      if (!streetSet[label]) {
        streetSet[label] = true;
      }
    }

    allStreetNames = Object.keys(streetSet).sort();
  }


  /* =======================================================
     Search: text query → results
     ======================================================= */
  function search(query, limit) {
    limit = limit || 20;
    var q = normalize(query);
    if (!q) return [];

    var tokens = q.split(/\s+/);
    var results = [];
    var seen = {};

    // Try exact code first
    if (isValidCode(query.trim())) {
      return [resolveResult(query.trim())];
    }

    // Score each house by token match
    for (var i = 0; i < houses.length; i++) {
      var h = houses[i];
      var target = normalize(h.city + ' ' + h.street + ' ' + h.number + ' ' + h.zip);
      var score = 0;

      for (var t = 0; t < tokens.length; t++) {
        if (target.indexOf(tokens[t]) >= 0) score++;
      }

      if (score > 0 && score >= Math.ceil(tokens.length * 0.5)) {
        var key = h.lat.toFixed(5) + ',' + h.lng.toFixed(5);
        if (!seen[key]) {
          seen[key] = true;
          results.push({ score: score, index: i });
        }
      }

      if (results.length >= limit * 3) break;
    }

    // Also search streets (bounding box data)
    for (var i = 0; i < streets.length; i++) {
      var s = streets[i];
      var target = normalize(s.city + ' ' + s.street + ' ' + s.zip);
      var score = 0;

      for (var t = 0; t < tokens.length; t++) {
        if (target.indexOf(tokens[t]) >= 0) score++;
      }

      if (score > 0 && score >= Math.ceil(tokens.length * 0.5)) {
        var lat = (s.yMin + s.yMax) / 2;
        var lng = (s.xMin + s.xMax) / 2;
        var key = lat.toFixed(5) + ',' + lng.toFixed(5);
        if (!seen[key]) {
          seen[key] = true;
          results.push({ score: score, streetIdx: i });
        }
      }
    }

    // Sort by score desc
    results.sort(function (a, b) { return b.score - a.score; });
    results = results.slice(0, limit);

    // Format results
    return results.map(function (r) {
      if (r.index !== undefined) {
        var h = houses[r.index];
        return {
          address: formatAddress(h.city, h.street, h.number, h.zip),
          code: encode(h.lat, h.lng, 10),
          lat: h.lat,
          lng: h.lng
        };
      } else {
        var s = streets[r.streetIdx];
        var lat = (s.yMin + s.yMax) / 2;
        var lng = (s.xMin + s.xMax) / 2;
        return {
          address: formatAddress(s.city, s.street, '', s.zip),
          code: encode(lat, lng, 10),
          lat: lat,
          lng: lng
        };
      }
    });
  }


  /* =======================================================
     Lookup: exact match by city + street + number
     ======================================================= */
  function lookup(params) {
    var city = normalize(params.city || '');
    var street = normalize(params.street || '');
    var number = (params.number || '').toString().trim();

    var key = city + '|' + street;
    var indices = streetIndex[key];

    if (!indices || indices.length === 0) {
      // Fallback: try partial street match
      var results = search((params.city || '') + ' ' + (params.street || '') + ' ' + number, 5);
      return results.length > 0 ? results[0] : null;
    }

    // If number specified, find closest match
    if (number) {
      for (var i = 0; i < indices.length; i++) {
        var h = houses[indices[i]];
        if (normalize(h.number) === normalize(number)) {
          return {
            address: formatAddress(h.city, h.street, h.number, h.zip),
            code: encode(h.lat, h.lng, 10),
            lat: h.lat,
            lng: h.lng
          };
        }
      }
    }

    // Return first match on that street
    var h = houses[indices[0]];
    return {
      address: formatAddress(h.city, h.street, h.number, h.zip),
      code: encode(h.lat, h.lng, 10),
      lat: h.lat,
      lng: h.lng
    };
  }


  /* =======================================================
     Resolve: Plus Code → nearest known address
     ======================================================= */
  function resolve(code) {
    if (!isValidCode(code)) return null;
    return resolveResult(code);
  }

  function resolveResult(code) {
    var pos = decode(code);
    var nearest = null;
    var minDist = Infinity;

    // Find nearest house
    for (var i = 0; i < houses.length; i++) {
      var h = houses[i];
      var dlat = h.lat - pos.lat;
      var dlng = h.lng - pos.lng;
      var dist = dlat * dlat + dlng * dlng;
      if (dist < minDist) {
        minDist = dist;
        nearest = i;
      }
    }

    if (nearest === null) {
      return {
        address: null,
        code: code.toUpperCase(),
        lat: pos.lat,
        lng: pos.lng,
        distance: null
      };
    }

    var h = houses[nearest];
    var distMeters = Math.sqrt(minDist) * 111320;

    return {
      address: formatAddress(h.city, h.street, h.number, h.zip),
      code: code.toUpperCase(),
      lat: pos.lat,
      lng: pos.lng,
      nearestLat: h.lat,
      nearestLng: h.lng,
      distance: Math.round(distMeters)
    };
  }


  /* =======================================================
     Suggest: autocomplete for street/city names
     ======================================================= */
  function suggest(partial, limit) {
    limit = limit || 10;
    var q = normalize(partial);
    if (!q || q.length < 2) return [];

    var results = [];
    for (var i = 0; i < allStreetNames.length; i++) {
      if (normalize(allStreetNames[i]).indexOf(q) >= 0) {
        results.push(allStreetNames[i]);
        if (results.length >= limit) break;
      }
    }
    return results;
  }


  /* =======================================================
     Helpers
     ======================================================= */
  function formatAddress(city, street, number, zip) {
    var parts = [];
    if (street) parts.push(street);
    if (number) parts.push('#' + number);
    if (city) parts.push(city);
    if (zip) parts.push('CP ' + zip);
    return parts.join(', ');
  }

  function getStats() {
    return {
      houses: houses.length,
      streets: streets.length,
      cities: Object.keys(cityIndex).length,
      uniqueStreets: allStreetNames.length
    };
  }


  /* =======================================================
     Public API
     ======================================================= */
  return {
    init: init,
    search: search,
    lookup: lookup,
    resolve: resolve,
    suggest: suggest,
    encode: encode,
    decode: decode,
    isValidCode: isValidCode,
    getStats: getStats
  };

})();
