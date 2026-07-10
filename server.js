const http = require('http');
const fs = require('fs');
const path = require('path');

const MIME = {
  '.html': 'text/html',
  '.js': 'application/javascript',
  '.json': 'application/json',
  '.css': 'text/css',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.ico': 'image/x-icon'
};

http.createServer((req, res) => {
  let url = req.url.split('?')[0];
  if (url === '/') url = '/index.html';
  if (url === '/app') url = '/app.html';
  const file = path.join(__dirname, url);
  fs.readFile(file, (err, data) => {
    if (err) {
      // Serve 404 page
      fs.readFile(path.join(__dirname, '404.html'), (e2, d2) => {
        res.writeHead(404, { 'Content-Type': 'text/html' });
        res.end(d2 || 'Not Found');
      });
      return;
    }
    res.writeHead(200, {
      'Content-Type': MIME[path.extname(file)] || 'text/plain',
      'Cache-Control': path.extname(file) === '.json' ? 'public, max-age=3600' : 'public, max-age=600'
    });
    res.end(data);
  });
}).listen(8000, () => console.log('http://localhost:8000'));
