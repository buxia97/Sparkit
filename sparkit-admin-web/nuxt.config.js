// Nuxt 3 配置文件 - 静态SPA模式
export default defineNuxtConfig({
  ssr: false,

  devtools: { enabled: false },

  app: {
    baseURL: '/',
    head: {
      title: 'Sparkit 管理中心',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: 'Sparkit 企业级开发框架管理中心' }
      ],
      link: [
        { rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' }
      ]
    }
  },

  css: [
    'element-plus/dist/index.css',
    '~/assets/css/global.css'
  ],

  modules: [
    '@pinia/nuxt',
    '@element-plus/nuxt'
  ],

  elementPlus: {
    importStyle: 'css'
  },

  // 静态编译配置
  nitro: {
    preset: 'static',
    prerender: {
      routes: ['/'],
      crawlLinks: false
    },
    devProxy: {
      '/api': {
        target: 'http://localhost:8083',
        changeOrigin: true
      }
    }
  },

  // 开发代理
  vite: {
    server: {
      proxy: {
        '/api': {
          target: 'http://localhost:8083',
          changeOrigin: true
        }
      }
    }
  },

  // 路由配置：所有路由使用SPA模式
  router: {
    options: {
      strict: false
    }
  }
})