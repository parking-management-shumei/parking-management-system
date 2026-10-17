const path = require('path')
const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  pages: { index: { entry: 'main.js', template: 'public/index.html' } },
  transpileDependencies: true,
  productionSourceMap: false,
  configureWebpack: { resolve: { alias: { '@': path.resolve(__dirname) } } },
  devServer: {
    host: '127.0.0.1',
    port: 8080,
    proxy: Object.fromEntries(
      ['/api', '/user', '/menu'].map((prefix) => [
        prefix,
        {
          target: process.env.DEV_API_TARGET || 'http://127.0.0.1:8090',
          changeOrigin: true,
        },
      ]),
    ),
  },
})
