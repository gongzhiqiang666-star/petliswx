import http from 'node:http';
import { createReadStream } from 'node:fs';
import { stat } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const mime = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8', '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml', '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg',
  '.ico': 'image/x-icon', '.woff2': 'font/woff2'
};

export function contentType(file) {
  return mime[path.extname(file).toLowerCase()] || 'application/octet-stream';
}

export function resolveStaticPath(root, requestPath) {
  let decoded;
  try { decoded = decodeURIComponent(requestPath.split('?')[0]); } catch { return null; }
  if (decoded.includes('\0') || decoded.split('/').includes('..')) return null;
  const relative = decoded.replace(/^\/+/, '');
  const candidate = path.resolve(root, relative || 'index.html');
  const normalizedRoot = path.resolve(root);
  if (candidate !== normalizedRoot && !candidate.startsWith(`${normalizedRoot}${path.sep}`)) return null;
  return path.extname(candidate) ? candidate : path.join(normalizedRoot, 'index.html');
}

function proxy(request, response, origin) {
  const target = new URL(request.url, origin);
  const transport = target.protocol === 'https:' ? import('node:https') : Promise.resolve({ default: http });
  transport.then(({ default: client }) => {
    const upstream = client.request(target, { method: request.method, headers: { ...request.headers, host: target.host } }, (result) => {
      response.writeHead(result.statusCode || 502, result.headers);
      result.pipe(response);
    });
    upstream.on('error', () => {
      if (!response.headersSent) response.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' });
      response.end(JSON.stringify({ code: 50200, message: '后端服务暂不可用，请确认 Petlis backend 已启动' }));
    });
    request.pipe(upstream);
  }).catch(() => { response.writeHead(502); response.end(); });
}

export function createStaticServer({ root = path.dirname(fileURLToPath(import.meta.url)), apiOrigin = process.env.API_ORIGIN || 'http://localhost:8080' } = {}) {
  return http.createServer(async (request, response) => {
    if (request.url.startsWith('/api/')) { proxy(request, response, apiOrigin); return; }
    const file = resolveStaticPath(root, request.url);
    if (!file) { response.writeHead(400); response.end('Bad request'); return; }
    try {
      const info = await stat(file);
      if (!info.isFile()) throw new Error('Not a file');
      response.writeHead(200, {
        'Content-Type': contentType(file),
        'Content-Length': info.size,
        'X-Content-Type-Options': 'nosniff',
        'Referrer-Policy': 'same-origin',
        'Content-Security-Policy': "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'"
      });
      if (request.method === 'HEAD') response.end(); else createReadStream(file).pipe(response);
    } catch {
      response.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      response.end('Not found');
    }
  });
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const port = Number(process.env.PORT || 4173);
  const host = process.env.HOST || '0.0.0.0';
  const server = createStaticServer();
  server.listen(port, host, () => console.log(`Petlis admin: http://127.0.0.1:${port} (API: ${process.env.API_ORIGIN || 'http://localhost:8080'})`));
}
