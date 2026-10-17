const express = require('express')
const path = require('path')
const { createProxyMiddleware } = require('http-proxy-middleware')
const app = express()
app.disable('x-powered-by')
app.use(
  ['/api', '/user', '/menu'],
  createProxyMiddleware({
    target: process.env.API_TARGET || 'http://127.0.0.1:8090',
    changeOrigin: true,
  }),
)
app.use(express.static(path.join(__dirname, 'dist')))
app.get('*', (req, res) => res.sendFile(path.join(__dirname, 'dist/index.html')))
app.listen(Number(process.env.PORT || 8080), process.env.HOST || '127.0.0.1', () =>
  console.log('Parking UI server is ready'),
)
