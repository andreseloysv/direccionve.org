const USGS_BASE = 'https://earthquake.usgs.gov/fdsnws/event/1/query';

// Venezuela extended bounding box
const BOUNDS = {
  minLat: 0, maxLat: 15,
  minLng: -76, maxLng: -58,
};

export async function fetchUsgsQuakes({ hoursBack = 24, minMagnitude = 3.0, limit = 20 } = {}) {
  const startTime = new Date(Date.now() - hoursBack * 3600_000).toISOString().split('T')[0];

  const params = new URLSearchParams({
    format: 'geojson',
    minlatitude: BOUNDS.minLat,
    maxlatitude: BOUNDS.maxLat,
    minlongitude: BOUNDS.minLng,
    maxlongitude: BOUNDS.maxLng,
    minmagnitude: minMagnitude,
    orderby: 'time',
    limit,
    starttime: startTime,
  });

  const res = await fetch(`${USGS_BASE}?${params}`);
  if (!res.ok) throw new Error(`USGS API error: ${res.status}`);

  const data = await res.json();
  return data.features.map(f => ({
    id: f.id,
    magnitude: f.properties.mag,
    place: f.properties.place,
    time: f.properties.time,
    latitude: f.geometry.coordinates[1],
    longitude: f.geometry.coordinates[0],
    depthKm: f.geometry.coordinates[2],
  }));
}
