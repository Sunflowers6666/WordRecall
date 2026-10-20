/**
 * 运行环境与接口地址。
 *
 * develop：开发者工具里的本地后端。真机调试时把 baseUrl 换成电脑的局域网 IP，
 *          例如 http://192.168.1.10:8080，并在开发者工具关闭域名校验。
 * trial / release：必须是已备案的 HTTPS 域名（阶段 6 上线前替换 CHANGE_ME）。
 */
function envVersion() {
  try {
    const info = wx.getAccountInfoSync()
    return (info && info.miniProgram && info.miniProgram.envVersion) || 'develop'
  } catch (error) {
    return 'develop'
  }
}

const BASE_URL = {
  develop: 'http://127.0.0.1:8080',
  trial: 'https://CHANGE_ME.example.com',
  release: 'https://CHANGE_ME.example.com'
}

const env = envVersion()

module.exports = {
  env,
  baseUrl: BASE_URL[env] || BASE_URL.develop,
  /** 每批新学词数，阶段 2 固定 20（见 UI 规范 §11） */
  batchSize: 20,
  /** 新学卡片默认遮住释义，点击展开 */
  revealMeaning: true,
  requestTimeout: 15000
}
