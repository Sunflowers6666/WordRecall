App({
  globalData: {
    // 复习页提交后由结果页读取，避免把列表塞进 URL
    reviewResult: null
  },

  onLaunch() {
    // 首屏（日历/词库）允许匿名浏览，登录在写操作时按需触发，
    // 因此这里不做阻塞式登录。
  }
})
