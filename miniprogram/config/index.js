/**
 * 运行环境与接口地址。
 *
 * develop：电脑上的本地后端。
 *   - 开发者工具里也能用局域网 IP，所以这里直接写死电脑的局域网地址，
 *     手机和电脑连同一个 WiFi 时都能访问。
 *   - IP 变了（DHCP 重新分配）就改这里：命令行 `ipconfig` 看「IPv4 地址」。
 *   - 只在自己电脑上调试的话，也可以换成 http://127.0.0.1:8080。
 *
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
  // 2026-10-20 本机 WLAN（SanXiaU）地址
  develop: 'http://172.20.242.224:8080',
  trial: 'https://CHANGE_ME.example.com',
  release: 'https://CHANGE_ME.example.com'
}

const env = envVersion()

module.exports = {
  env,
  baseUrl: BASE_URL[env] || BASE_URL.develop,
  /** 每批新学词数默认值（服务端按 user.batch_size 返回，这里只作兜底展示） */
  batchSize: 20,
  /** 新学卡片默认遮住释义，点击展开 */
  revealMeaning: true,
  requestTimeout: 15000
}
