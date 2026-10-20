const { request, setReLoginHandler } = require('./request')

const TOKEN_KEY = 'wr_token'
const USER_KEY = 'wr_user'

function getToken() {
  try {
    return wx.getStorageSync(TOKEN_KEY) || ''
  } catch (error) {
    return ''
  }
}

function getUser() {
  try {
    return wx.getStorageSync(USER_KEY) || null
  } catch (error) {
    return null
  }
}

function saveUser(user) {
  try {
    wx.setStorageSync(USER_KEY, user)
  } catch (error) {
    // 存不进缓存不影响当前会话
  }
  const app = getApp()
  if (app && app.globalData) {
    app.globalData.user = user
  }
}

function saveSession(token, user) {
  try {
    wx.setStorageSync(TOKEN_KEY, token)
  } catch (error) {
    // ignore
  }
  saveUser(user)
}

function clearSession() {
  try {
    wx.removeStorageSync(TOKEN_KEY)
    wx.removeStorageSync(USER_KEY)
  } catch (error) {
    // ignore
  }
}

function wxLogin() {
  return new Promise((resolve, reject) => {
    wx.login({
      success: (result) => {
        if (result && result.code) {
          resolve(result.code)
        } else {
          reject(new Error('微信登录失败'))
        }
      },
      fail: () => reject(new Error('微信登录失败'))
    })
  })
}

let loginPromise = null

/** wx.login → 后端换 token。并发调用共用同一次登录。 */
function login() {
  if (loginPromise) {
    return loginPromise
  }
  const finish = () => {
    loginPromise = null
  }
  loginPromise = wxLogin()
    .then((code) => request({
      url: '/api/auth/login',
      method: 'POST',
      data: { code },
      auth: false
    }))
    .then((data) => {
      saveSession(data.token, data.user)
      return data.user
    })
  loginPromise.then(finish).catch(finish)
  return loginPromise
}

/**
 * 写操作前调用：没有 token 就登录；force 为 true 时强制换新 token。
 * @returns {Promise<object>} 当前用户
 */
function ensureLogin(force) {
  if (!force && getToken()) {
    return Promise.resolve(getUser())
  }
  return login()
}

// 401 时静默重登一次，由 request 重放原请求
setReLoginHandler(() => ensureLogin(true))

module.exports = {
  getToken,
  getUser,
  saveUser,
  saveSession,
  clearSession,
  ensureLogin
}
