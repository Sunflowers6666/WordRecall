const { request } = require('../../utils/request')
const { ensureLogin, getUser, saveUser } = require('../../utils/auth')
const { toastError } = require('../../utils/feedback')

const BATCH_OPTIONS = [10, 20, 30]

Page({
  data: {
    user: null,
    initial: '单',
    wordbookName: '',
    version: '1.0.0',
    stats: null,
    batchSize: 20
  },

  onShow() {
    this.syncUser()
    this.loadWordbook()
    this.loadStats()
  },

  syncUser() {
    const user = getUser()
    this.setData({
      user,
      initial: user && user.nickname ? user.nickname.slice(0, 1) : '单',
      batchSize: (user && user.batchSize) || 20
    })
  },

  async loadStats() {
    if (!getUser()) {
      this.setData({ stats: null })
      return
    }
    try {
      const stats = await request({ url: '/api/users/me/stats' })
      this.setData({ stats })
    } catch (error) {
      this.setData({ stats: null })
    }
  },

  async loadWordbook() {
    try {
      const list = await request({ url: '/api/wordbooks' })
      const current = list.find((item) => item.selected) || list[0]
      this.setData({ wordbookName: current ? current.name : '' })
    } catch (error) {
      // 词库名不是关键信息，失败时保留旧值
    }
  },

  async onLogin() {
    try {
      const user = await ensureLogin()
      saveUser(user)
      this.syncUser()
      this.loadStats()
      wx.showToast({ title: '已登录', icon: 'none' })
    } catch (error) {
      toastError(error)
    }
  },

  onEditNickname() {
    if (!this.data.user) {
      this.onLogin()
      return
    }
    const that = this
    wx.showModal({
      title: '修改昵称',
      editable: true,
      placeholderText: '输入昵称',
      success: async (res) => {
        if (!res.confirm) {
          return
        }
        const nickname = (res.content || '').trim()
        if (!nickname) {
          wx.showToast({ title: '昵称不能为空', icon: 'none' })
          return
        }
        try {
          const user = await request({
            url: '/api/users/me',
            method: 'PATCH',
            data: { nickname }
          })
          saveUser(user)
          that.syncUser()
          wx.showToast({ title: '已保存', icon: 'none' })
        } catch (error) {
          toastError(error)
        }
      }
    })
  },

  onBatchSize() {
    if (!this.data.user) {
      this.onLogin()
      return
    }
    const that = this
    const labels = BATCH_OPTIONS.map((size) => size + ' 词')
    wx.showActionSheet({
      itemList: labels,
      success: async (res) => {
        const batchSize = BATCH_OPTIONS[res.tapIndex]
        if (!batchSize || batchSize === that.data.batchSize) {
          return
        }
        try {
          const user = await request({
            url: '/api/users/me',
            method: 'PATCH',
            data: { batchSize }
          })
          saveUser(user)
          that.syncUser()
          wx.showToast({ title: '每批 ' + batchSize + ' 词', icon: 'none' })
        } catch (error) {
          toastError(error)
        }
      }
    })
  },

  onWordbook() {
    wx.navigateTo({ url: '/pages/wordbook/wordbook' })
  },

  onAbout() {
    wx.showModal({
      title: '关于',
      content: '单词日历 v' + this.data.version + '\n词库来源 ECDICT（MIT）',
      showCancel: false
    })
  }
})
