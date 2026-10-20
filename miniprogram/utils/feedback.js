function toastError(error) {
  const message = (error && error.message) || '操作失败，请重试'
  wx.showToast({ title: message, icon: 'none' })
}

function errorKind(error) {
  return (error && error.kind) || 'server'
}

function toastSuccess(title) {
  wx.showToast({ title: title || '已完成', icon: 'none' })
}

module.exports = {
  toastError,
  errorKind,
  toastSuccess
}
