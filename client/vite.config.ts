import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import type { ProxyOptions } from 'vite'
import { defineConfig } from 'vitest/config'

const apiProxy: ProxyOptions = {
  target: 'http://localhost:8080',
  // Browsers send an Origin header on POST/PUT/DELETE. Forwarded as-is, Spring would see
  // "Origin: localhost:5173" arriving at localhost:8080, treat it as cross-origin and
  // answer 403. The browser->Vite hop really is same-origin, so drop the header here.
  configure: (proxy) => {
    proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'))
  },
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // In development the browser only talks to Vite (localhost:5173). Vite forwards /api to
    // Spring Boot, so the browser sees one origin and CORS never comes into play.
    proxy: { '/api': apiProxy },
  },
  preview: {
    // `npm run preview` serves the production build with the same CSP as render.yaml
    // (API on the same origin here), so a policy that breaks the app shows up locally.
    proxy: { '/api': apiProxy },
    headers: {
      'Content-Security-Policy':
        "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'",
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
})
