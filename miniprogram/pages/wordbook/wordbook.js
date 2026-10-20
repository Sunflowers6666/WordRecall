const { request } = require('../../utils/request')
const { ensureLogin } = require('../../utils/auth')
const { errorKind, toastError } = require('../../utils/feedback')
const feature = require('../../config/feature')

Page({
  data: {
    state: 'loading',
    errorKind: '',
    builtin: [],
    mine: [],
    canCreate: feature.enableUserWordbook
  },

  onShow() {
    this.load()
  },

  async load() {
    try {
      const list = await request({ url: '/api/wordbooks' })
      const builtin = list.filter((item) => !item.editable)
      const mine = list.filter((item) => item.editable)
      this.setData({ state: 'ready', builtin, mine })
    } catch (error) {
      this.setData({ state: 'error', errorKind: errorKind(error) })
    }
  },

  async onPickBuiltin(event) {
    const item = this.data.builtin[Number(event.currentTarget.dataset.index)]
    if (!item || item.selected) {
      return
    }
    try {
      await ensureLogin()
      await request({
        url: '/api/users/me/wordbook',
        method: 'PUT',
        data: { wordbookId: item.id }
      })
      this.setData({
        builtin: this.data.builtin.map((book) => Object.assign({}, book, {
          selected: book.id === item.id
        }))
      })
      wx.showToast({ title: '已切换为 ' + item.name, icon: 'none' })
    } catch (error) {
      toastError(error)
    }
  },

  onPickMine(event) {
    const item = this.data.mine[Number(event.currentTarget.dataset.index)]
    if (!item) {
      return
    }
    wx.navigateTo({ url: '/pages/wordbook-detail/wordbook-detail?id=' + item.id })
  },

  onCreate() {
    if (!this.data.canCreate) {
      return
    }
    const that = this
    wx.showModal({
      title: '新建词库',
      editable: true,
      placeholderText: '例如：考研核心词',
      success: async (res) => {
        if (!res.confirm) {
          return
        }
        const name = (res.content || '').trim()
        if (!name) {
          wx.showToast({ title: '名称不能为空', icon: 'none' })
          return
        }
        try {
          await ensureLogin()
          await request({ url: '/api/wordbooks', method: 'POST', data: { name } })
          wx.showToast({ title: '已创建', icon: 'none' })
          that.load()
        } catch (error) {
          toastError(error)
        }
      }
    })
  },

  onRetry() {
    this.setData({ state: 'loading' })
    this.load()
  }
})
