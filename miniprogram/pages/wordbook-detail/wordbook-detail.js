const { request } = require('../../utils/request')
const { errorKind, toastError } = require('../../utils/feedback')

Page({
  data: {
    id: 0,
    book: null,
    words: [],
    total: 0,
    state: 'loading',
    errorKind: '',
    isCurrent: false
  },

  onLoad(query) {
    this.setData({ id: Number((query && query.id) || 0) })
  },

  onShow() {
    if (this.data.id) {
      this.load()
    }
  },

  async load() {
    try {
      const books = await request({ url: '/api/wordbooks' })
      const book = books.find((item) => item.id === this.data.id)
      if (!book) {
        this.setData({ state: 'error', errorKind: 'server' })
        return
      }
      if (!book.editable) {
        wx.showToast({ title: '预置词库不可编辑', icon: 'none' })
        wx.navigateBack()
        return
      }
      const page = await request({
        url: '/api/wordbooks/' + this.data.id + '/words?limit=200'
      })
      this.setData({
        state: 'ready',
        book,
        words: page.words || [],
        total: page.total || 0,
        isCurrent: !!book.selected
      })
    } catch (error) {
      this.setData({ state: 'error', errorKind: errorKind(error) })
    }
  },

  async onSetCurrent() {
    if (this.data.isCurrent) {
      return
    }
    try {
      await request({
        url: '/api/users/me/wordbook',
        method: 'PUT',
        data: { wordbookId: this.data.id }
      })
      this.setData({ isCurrent: true })
      wx.showToast({ title: '已设为当前词库', icon: 'none' })
    } catch (error) {
      toastError(error)
    }
  },

  onRename() {
    const that = this
    wx.showModal({
      title: '词库改名',
      editable: true,
      placeholderText: '新的词库名称',
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
          await request({
            url: '/api/wordbooks/' + that.data.id,
            method: 'PATCH',
            data: { name }
          })
          wx.showToast({ title: '已改名', icon: 'none' })
          that.load()
        } catch (error) {
          toastError(error)
        }
      }
    })
  },

  onDelete() {
    const that = this
    wx.showModal({
      title: '删除词库',
      content: '词库里的单词和对应的学习记录都会一起删除，确定吗？',
      confirmText: '删除',
      confirmColor: '#FF4B4B',
      success: async (res) => {
        if (!res.confirm) {
          return
        }
        try {
          await request({ url: '/api/wordbooks/' + that.data.id, method: 'DELETE' })
          wx.showToast({ title: '已删除', icon: 'none' })
          setTimeout(() => wx.navigateBack(), 500)
        } catch (error) {
          toastError(error)
        }
      }
    })
  },

  onAdd() {
    wx.navigateTo({ url: '/pages/word-edit/word-edit?bookId=' + this.data.id })
  },

  onWordTap(event) {
    const index = Number(event.currentTarget.dataset.index)
    const word = this.data.words[index]
    if (!word) {
      return
    }
    wx.navigateTo({
      url: '/pages/word-edit/word-edit?bookId=' + this.data.id + '&wordId=' + word.id
    })
  },

  onRetry() {
    this.setData({ state: 'loading' })
    this.load()
  }
})
