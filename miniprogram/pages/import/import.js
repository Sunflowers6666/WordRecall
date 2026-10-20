const { request } = require('../../utils/request')
const { ensureLogin, getToken } = require('../../utils/auth')
const { errorKind, toastError } = require('../../utils/feedback')
const config = require('../../config/index')

function uploadFile(path, formData) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: config.baseUrl + path,
      filePath: formData.filePath,
      name: 'file',
      header: { Authorization: 'Bearer ' + getToken() },
      formData: formData.fields || {},
      success: (res) => {
        let body = null
        try {
          body = JSON.parse(res.data)
        } catch (error) {
          reject(new Error('服务返回格式不正确'))
          return
        }
        if (res.statusCode >= 400 || body.code !== 0) {
          reject(new Error(body.message || '上传失败'))
          return
        }
        resolve(body.data)
      },
      fail: () => reject(new Error('上传失败，请检查网络'))
    })
  })
}

Page({
  data: {
    state: 'idle',
    errorKind: '',
    file: null,
    preview: null,
    target: 'new',
    newName: '',
    books: [],
    bookIndex: 0,
    result: null
  },

  onLoad() {
    this.loadBooks()
  },

  async loadBooks() {
    try {
      const list = await request({ url: '/api/wordbooks' })
      this.setData({ books: list.filter((item) => item.editable) })
    } catch (error) {
      // 词库列表失败不阻塞选文件
    }
  },

  onChooseFile() {
    const that = this
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['csv', 'xlsx', 'xls'],
      success: (res) => {
        const file = res.tempFiles && res.tempFiles[0]
        if (!file) {
          return
        }
        const name = file.name || 'words.csv'
        that.setData({
          state: 'parsing',
          file: { name, path: file.path, size: file.size },
          newName: name.replace(/\.[^.]+$/, '')
        })
        that.preview(file.path)
      },
      fail: () => {}
    })
  },

  async preview(path) {
    try {
      await ensureLogin()
      const data = await uploadFile('/api/imports/preview', { filePath: path })
      this.setData({ state: 'preview', preview: data })
    } catch (error) {
      this.setData({ state: 'error', errorKind: 'server' })
      wx.showToast({ title: error.message || '解析失败', icon: 'none' })
    }
  },

  onTargetChange(event) {
    this.setData({ target: event.currentTarget.dataset.target })
  },

  onNewNameInput(event) {
    this.setData({ newName: event.detail.value })
  },

  onBookChange(event) {
    this.setData({ bookIndex: Number(event.detail.value) })
  },

  async onImport() {
    if (this.data.state === 'importing') {
      return
    }
    this.setData({ state: 'importing' })
    try {
      await ensureLogin()
      let path = '/api/imports'
      const fields = {}
      if (this.data.target === 'new') {
        const name = (this.data.newName || '').trim()
        if (!name) {
          wx.showToast({ title: '请填写词库名称', icon: 'none' })
          this.setData({ state: 'preview' })
          return
        }
        fields.newBookName = name
      } else {
        const book = this.data.books[this.data.bookIndex]
        if (!book) {
          wx.showToast({ title: '请选择词库', icon: 'none' })
          this.setData({ state: 'preview' })
          return
        }
        fields.bookId = book.id
      }
      const query = Object.keys(fields)
        .map((key) => key + '=' + encodeURIComponent(fields[key]))
        .join('&')
      if (query) {
        path += '?' + query
      }
      const result = await uploadFile(path, { filePath: this.data.file.path })
      this.setData({ state: 'done', result })
    } catch (error) {
      this.setData({ state: 'preview' })
      toastError(error)
    }
  },

  onReset() {
    this.setData({
      state: 'idle',
      file: null,
      preview: null,
      result: null,
      target: 'new',
      newName: '',
      bookIndex: 0
    })
  },

  onDone() {
    wx.navigateBack()
  },

  onRetry() {
    this.setData({ state: 'idle' })
  }
})
