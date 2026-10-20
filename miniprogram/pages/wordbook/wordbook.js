const { request } = require('../../utils/request')
const { ensureLogin } = require('../../utils/auth')
const { errorKind, toastError } = require('../../utils/feedback')

Page({
  data: {
    state: 'loading',
    errorKind: '',
    list: []
  },

  onShow() {
    this.load()
  },

  async load() {
    try {
      const list = await request({ url: '/api/wordbooks' })
      this.setData({ state: 'ready', list })
    } catch (error) {
      this.setData({ state: 'error', errorKind: errorKind(error) })
    }
  },

  async onPick(event) {
    const index = Number(event.currentTarget.dataset.index)
    const item = this.data.list[index]
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
      const list = this.data.list.map((row) => Object.assign({}, row, {
        selected: row.id === item.id
      }))
      this.setData({ list })
      wx.showToast({ title: '已切换为 ' + item.name, icon: 'none' })
      setTimeout(() => {
        wx.navigateBack()
      }, 600)
    } catch (error) {
      toastError(error)
    }
  },

  onRetry() {
    this.setData({ state: 'loading' })
    this.load()
  }
})
