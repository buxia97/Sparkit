// 菜单配置 - 定义所有侧边栏菜单
export const menuConfig = [
  {
    title: '仪表盘',
    icon: 'Odometer',
    path: '/',
    perm: 'dashboard'
  },
  {
    title: '系统管理',
    icon: 'Setting',
    perm: 'system',
    children: [
      { title: '管理员管理', icon: 'User', path: '/system/admin', perm: 'system:admin:list' },
      { title: '角色管理', icon: 'Avatar', path: '/system/role', perm: 'system:role:list' },
      { title: '菜单管理', icon: 'Menu', path: '/system/menu', perm: 'system:menu:list' },
      { title: '部门管理', icon: 'OfficeBuilding', path: '/system/dept', perm: 'system:dept:list' },
      { title: '岗位管理', icon: 'Briefcase', path: '/system/post', perm: 'system:post:list' },
      { title: '系统配置', icon: 'Tools', path: '/system/config', perm: 'system:config:list' },
      { title: '字典管理', icon: 'Collection', path: '/system/dict', perm: 'system:dict:list' },
      { title: '地区管理', icon: 'Location', path: '/system/region', perm: 'system:region:list' },
      { title: '国际化管理', icon: 'ChatLineSquare', path: '/system/i18n', perm: 'system:i18n:list' },
      { title: '导入导出', icon: 'Upload', path: '/system/import-export', perm: 'system:import-export:list' },
      { title: '主题皮肤', icon: 'Brush', path: '/system/theme', perm: 'system:theme:list' }
    ]
  },
  {
    title: '用户管理',
    icon: 'UserFilled',
    path: '/user',
    perm: 'user:list'
  },
  {
    title: '新闻管理',
    icon: 'Document',
    perm: 'news',
    children: [
      { title: '新闻分类', icon: 'CollectionTag', path: '/news/category', perm: 'news:category:list' },
      { title: '新闻文章', icon: 'Notebook', path: '/news/article', perm: 'news:article:list' }
    ]
  },
  {
    title: '存储管理',
    icon: 'FolderOpened',
    path: '/storage',
    perm: 'storage:list'
  },
  {
    title: '支付管理',
    icon: 'Money',
    path: '/payment',
    perm: 'payment:list'
  },
  {
    title: '通知管理',
    icon: 'Bell',
    path: '/notification',
    perm: 'notification:list'
  },
  {
    title: 'AI 管理',
    icon: 'Cpu',
    path: '/ai',
    perm: 'ai:list'
  },
  {
    title: '定时任务',
    icon: 'Clock',
    path: '/job',
    perm: 'job:list'
  },
  {
    title: '代码生成器',
    icon: 'EditPen',
    path: '/generator',
    perm: 'generator:list'
  },
  {
    title: '多租户管理',
    icon: 'Connection',
    path: '/tenant',
    perm: 'tenant:list'
  },
  {
    title: '数据备份',
    icon: 'FolderOpened',
    path: '/backup',
    perm: 'backup:list'
  },
  {
    title: '日志审计',
    icon: 'Tickets',
    perm: 'monitor',
    children: [
      { title: '操作日志', icon: 'List', path: '/monitor/operlog', perm: 'monitor:operlog:list' },
      { title: '登录日志', icon: 'UserFilled', path: '/monitor/loginlog', perm: 'monitor:loginlog:list' }
    ]
  }
]

// 全局搜索数据源
export const searchData = [
  { title: '仪表盘', path: '/', icon: 'Odometer', keywords: ['首页', 'dashboard', '统计'] },
  { title: '管理员管理', path: '/system/admin', icon: 'User', keywords: ['管理员', 'admin', '后台用户'] },
  { title: '角色管理', path: '/system/role', icon: 'Avatar', keywords: ['角色', 'role'] },
  { title: '菜单管理', path: '/system/menu', icon: 'Menu', keywords: ['菜单', 'menu', '权限'] },
  { title: '部门管理', path: '/system/dept', icon: 'OfficeBuilding', keywords: ['部门', 'dept'] },
  { title: '岗位管理', path: '/system/post', icon: 'Briefcase', keywords: ['岗位', 'post'] },
  { title: '系统配置', path: '/system/config', icon: 'Tools', keywords: ['配置', 'config', '参数'] },
  { title: '字典管理', path: '/system/dict', icon: 'Collection', keywords: ['字典', 'dict'] },
  { title: '地区管理', path: '/system/region', icon: 'Location', keywords: ['地区', 'region'] },
  { title: '国际化管理', path: '/system/i18n', icon: 'ChatLineSquare', keywords: ['国际化', 'i18n', '翻译', '多语言'] },
  { title: '导入导出', path: '/system/import-export', icon: 'Upload', keywords: ['导入', '导出', 'import', 'export'] },
  { title: '主题皮肤', path: '/system/theme', icon: 'Brush', keywords: ['主题', '皮肤', 'theme', '配色', '暗黑'] },
  { title: '用户管理', path: '/user', icon: 'UserFilled', keywords: ['用户', 'user', 'C端用户'] },
  { title: '新闻分类', path: '/news/category', icon: 'CollectionTag', keywords: ['新闻分类', 'category'] },
  { title: '新闻文章', path: '/news/article', icon: 'Notebook', keywords: ['新闻', 'article', '文章'] },
  { title: '存储管理', path: '/storage', icon: 'FolderOpened', keywords: ['存储', 'storage', '文件'] },
  { title: '支付管理', path: '/payment', icon: 'Money', keywords: ['支付', 'payment'] },
  { title: '通知管理', path: '/notification', icon: 'Bell', keywords: ['通知', 'notification', '消息'] },
  { title: 'AI 管理', path: '/ai', icon: 'Cpu', keywords: ['AI', 'ai', '大模型'] },
  { title: '定时任务', path: '/job', icon: 'Clock', keywords: ['任务', 'job', '定时'] },
  { title: '代码生成器', path: '/generator', icon: 'EditPen', keywords: ['代码生成', 'generator', 'gen'] },
  { title: '多租户管理', path: '/tenant', icon: 'Connection', keywords: ['多租户', 'tenant', '租户'] },
  { title: '数据备份', path: '/backup', icon: 'FolderOpened', keywords: ['备份', 'backup', '恢复', '数据库'] },
  { title: '操作日志', path: '/monitor/operlog', icon: 'List', keywords: ['操作日志', 'operlog', '审计'] },
  { title: '登录日志', path: '/monitor/loginlog', icon: 'UserFilled', keywords: ['登录日志', 'loginlog'] }
]