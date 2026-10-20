const { request } = require('../../utils/request')
const { errorKind, toastError } = require('../../utils/feedback')

Page({
  data: {
    bookId: 0,
    wordId: 0,
    isNew: true,
    state: 'ready',
    errorKind: '',
    saving: false,
    form: {
      spelling: '',
      phonetic: '',
      meaning: '',
      exampleSentence: '',
      exampleTranslation: ''
    }
  },

  onLoad(query) {
    const bookId = Number((query && query.bookId) || 0)
    const wordId = Number((query && query.wordId) || 0)
    this.setData({ bookId, wordId, isNew: !wordId, state: wordId ? 'loading' : 'ready' })
    if (wordId) {
      this.load()
    }
  },

  async load() {
    try {
      const word = await request({ url: '/api/words/' + this.data.wordId })
      this.setData({
        state: 'ready',
        form: {
          spelling: word.spelling || '',
          phonetic: word.phonetic || '',
          meaning: word.meaning || '',
          exampleSentence: word.exampleSentence || '',
          exampleTranslation: word.exampleTranslation || ''
        }
      })
    } catch (error) {
      this.setData({ state: 'error', errorKind: errorKind(error) })
    }
  },

  onInput(event) {
    const field = event.currentTarget.dataset.field
    this.setData({ ['form.' + field]: event.detail.value })
  },

  async onSubmit() {
    const form = this.data.form
    if (!form.spelling.trim()) {
      wx.showToast({ title: '请填写单词', icon: 'none' })
      return
    }
    if (!form.meaning.trim()) {
      wx.showToast({ title: '请填写释义', icon: 'none' })
      return
    }
    if (this.data.saving) {
      return
    }
    this.setData({ saving: true })

    const payload = {
      spelling: form.spelling.trim(),
      phonetic: form.phonetic.trim(),
      meaning: form.meaning.trim(),
      exampleSentence: form.exampleSentence.trim(),
      exampleTranslation: form.exampleTranslation.trim()
    }

    try {
      if (this.data.isNew) {
        await request({
          url: '/api/wordbooks/' + this.data.bookId + '/words',
          method: 'POST',
          data: payload
        })
      } else {
        await request({
          url: '/api/words/' + this.data.wordId,
          method: 'PUT',
          data: payload
        })
      }
      wx.showToast({ title: '已保存', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 400)
    } catch (error) {
      this.setData({ saving: false })
      toastError(error)
    }
  },

  onDelete() {
    const that = this
    wx.showModal({
      title: '删除单词',
      content: '确定从词库中删除这个词吗？',
      confirmText: '删除',
      confirmColor: '#FF4B4B',
      success: async (res) => {
        if (!res.confirm) {
          return
        }
        try {
          await request({ url: '/api/words/' + that.data.wordId, method: 'DELETE' })
          wx.showToast({ title: '已删除', icon: 'none' })
          setTimeout(() => wx.navigateBack(), 400)
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
