const config = require('../config/index')

const TOKEN_KEY = 'wr_token'

class AppError extends Error {
  constructor(message, kind, code) {
    super(message)
    this.name = 'AppError'
    /** network | auth | server | biz */
    this.kind = kind || 'biz'
    this.code = code
  }
}

let reLoginHandler = null

/** 由 utils/auth.js 注册：401 时静默重登一次。 */
function setReLoginHandler(handler) {
  reLoginHandler = handler
}

function currentToken() {
  try {
    return wx.getStorageSync(TOKEN_KEY) || ''
  } catch (error) {
    return ''
  }
}

function send(options) {
  return new Promise((resolve, reject) => {
    const header = { 'Content-Type': 'application/json' }
    if (options.auth !== false) {
      const token = currentToken()
      if (token) {
        header.Authorization = 'Bearer ' + token
      }
    }
    wx.request({
      url: config.baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data,
      header,
      timeout: config.requestTimeout,
      success: resolve,
      fail: () => reject(new AppError('网络开小差了', 'network'))
    })
  })
}

function handleResponse(response) {
  const body = response.data
  const isObject = body && typeof body === 'object'

  if (response.statusCode === 401) {
    throw new AppError((isObject && body.message) || '登录已失效', 'auth', isObject ? body.code : 40100)
  }
  if (response.statusCode >= 500) {
    throw new AppError('服务暂时不可用，请稍后再试', 'server')
  }
  if (!isObject) {
    throw new AppError('服务返回格式不正确', 'server')
  }
  if (body.code !== 0) {
    throw new AppError(body.message || '操作失败，请重试', 'biz', body.code)
  }
  return body.data
}

/**
 * 统一请求入口。页面不直接调用 wx.request。
 * @param {{url:string, method?:string, data?:object, auth?:boolean, retried?:boolean}} options
 * @returns {Promise<any>} 解析为 ApiResponse.data
 */
function request(options) {
  return send(options).then((response) => {
    if (response.statusCode === 401 && options.auth !== false && !options.retried && reLoginHandler) {
      return reLoginHandler().then(() => request(Object.assign({}, options, { retried: true })))
    }
    return handleResponse(response)
  })
}

module.exports = {
  request,
  setReLoginHandler,
  AppError
}
