/**
 * 页面逻辑冒烟：用假的 wx/Page 运行时跑页面 JS（不需要微信开发者工具）。
 * 覆盖：日历网格与选中、月份切换、新学翻页、复习提交、结果页数据传递。
 */
const path = require('path')
const assert = require('assert')

const failures = []
const passed = []

function check(name, fn) {
  try {
    fn()
    passed.push(name)
    console.log('PASS  ' + name)
  } catch (error) {
    failures.push(name)
    console.log('FAIL  ' + name + ' -> ' + error.message)
  }
}

async function checkAsync(name, fn) {
  try {
    await fn()
    passed.push(name)
    console.log('PASS  ' + name)
  } catch (error) {
    failures.push(name)
    console.log('FAIL  ' + name + ' -> ' + error.message)
  }
}

const storage = {}
const calls = { navigateTo: [], redirectTo: [], switchTab: [], toast: [], requests: [], uploads: [] }

// 页面按「在线版」跑冒烟；单机版逻辑在文件末尾单独测
require('../config/index').offline = false

let todayIso = new Date().getFullYear() + '-' +
  String(new Date().getMonth() + 1).padStart(2, '0') + '-' +
  String(new Date().getDate()).padStart(2, '0')

const words = [
  { id: 1, spelling: 'abandon', phonetic: '/əˈbændən/', meaning: 'v. 放弃', exampleSentence: null, exampleTranslation: null, isWrong: false },
  { id: 2, spelling: 'benefit', phonetic: '/ˈbenɪfɪt/', meaning: 'n. 好处', exampleSentence: null, exampleTranslation: null, isWrong: false }
]

const routes = {
  '/api/wordbooks': () => [
    { id: 1, name: 'CET-4', description: null, wordCount: 3849, selected: true, editable: false, source: 'BUILTIN' },
    { id: 2, name: 'CET-6', description: null, wordCount: 5407, selected: false, editable: false, source: 'BUILTIN' },
    { id: 3, name: 'IELTS', description: null, wordCount: 5040, selected: false, editable: false, source: 'BUILTIN' },
    { id: 9, name: '我的词表', description: null, wordCount: 2, selected: false, editable: true, source: 'USER' }
  ],
  '/api/calendar': () => ({
    month: '2026-10',
    days: [{ date: todayIso, wordCount: 20, wrongCount: 2 }],
    summary: { days: 1, words: 20, wrong: 2 }
  }),
  '/api/study/sessions': () => ({ words: words, todayWordCount: 2, remaining: 3827 }),
  '/api/study/records': () => ({ recorded: 1, todayWordCount: 3 }),
  '/api/review': () => ({ date: todayIso, words: words }),
  '/api/review/submit': () => ({ date: todayIso, total: 2, wrong: 1 }),
  '/api/users/me': () => ({ id: 1, nickname: '小明', avatarUrl: null, currentWordbookId: 1, batchSize: 30 }),
  '/api/users/me/wordbook': () => ({ id: 1, nickname: '小明', avatarUrl: null, currentWordbookId: 2, batchSize: 30 }),
  '/api/users/me/stats': () => ({ learnedDays: 4, learnedWords: 41, wrongWords: 2, streakDays: 3, todayWords: 5 }),
  '/api/wrongbook': () => ({
    total: 2,
    words: [
      { id: 1, spelling: 'abandon', meaning: '放弃', isWrong: true },
      { id: 2, spelling: 'benefit', meaning: '好处', isWrong: true }
    ]
  }),
  '/api/wrongbook/submit': () => 1,
  '/api/auth/login': () => ({ token: 'fake-token', user: { id: 1, nickname: '小明', avatarUrl: null, currentWordbookId: 1 } })
}

const storedUsersBook = { id: 9, name: '我的词表', description: null, wordCount: 2, selected: false, editable: true, source: 'USER' }

/** 带路径参数的路由（自建词库相关）。 */
function dynamicRoute(pathOnly, method) {
  let match = pathOnly.match(/^\/api\/wordbooks\/(\d+)\/words$/)
  if (match) {
    if (method === 'POST') {
      return { id: 101, spelling: 'alpha', meaning: '阿尔法', isWrong: false }
    }
    return {
      total: 2,
      words: [
        { id: 101, spelling: 'alpha', meaning: '阿尔法', isWrong: false },
        { id: 102, spelling: 'beta', meaning: '贝塔', isWrong: false }
      ]
    }
  }
  match = pathOnly.match(/^\/api\/wordbooks\/(\d+)$/)
  if (match) {
    return storedUsersBook
  }
  match = pathOnly.match(/^\/api\/words\/(\d+)$/)
  if (match) {
    return { id: 101, spelling: 'alpha', phonetic: '/ˈælfə/', meaning: '阿尔法', exampleSentence: null, exampleTranslation: null, isWrong: false }
  }
  return null
}

global.wx = {
  getAccountInfoSync: () => ({ miniProgram: { envVersion: 'develop' } }),
  getStorageSync: (key) => storage[key],
  setStorageSync: (key, value) => { storage[key] = value },
  removeStorageSync: (key) => { delete storage[key] },
  base64ToArrayBuffer: (value) => Uint8Array.from(Buffer.from(value, 'base64')).buffer,
  showToast: (opts) => calls.toast.push(opts),
  showModal: () => {},
  showActionSheet: () => {},
  navigateTo: (opts) => calls.navigateTo.push(opts.url),
  redirectTo: (opts) => calls.redirectTo.push(opts.url),
  switchTab: (opts) => calls.switchTab.push(opts.url),
  navigateBack: () => calls.navigateTo.push('back'),
  stopPullDownRefresh: () => {},
  login: (opts) => opts.success({ code: 'fake-code' }),
  chooseMessageFile: (opts) => {
    opts.success({ tempFiles: [{ name: 'demo.csv', path: '/tmp/demo.csv', size: 120 }] })
  },
  uploadFile: (opts) => {
    calls.uploads.push({ url: opts.url, fields: opts.formData })
    setTimeout(() => {
      const isPreview = opts.url.indexOf('/api/imports/preview') >= 0
      const data = isPreview
        ? {
            fileName: 'demo.csv',
            total: 3,
            valid: 2,
            invalid: 1,
            rows: [
              { row: 1, spelling: 'alpha', meaning: '阿尔法', ok: true, reason: '' },
              { row: 3, spelling: 'broken', meaning: '', ok: false, reason: '缺少释义' }
            ]
          }
        : {
            bookId: 11,
            bookName: 'demo',
            total: 3,
            imported: 2,
            skippedDuplicate: 0,
            skippedInvalid: 1,
            problems: [{ row: 3, spelling: 'broken', meaning: '', ok: false, reason: '缺少释义' }]
          }
      opts.success({ statusCode: 200, data: JSON.stringify({ code: 0, message: 'ok', data }) })
    }, 0)
  },
  request: (opts) => {
    const pathOnly = opts.url.replace(/^https?:\/\/[^/]+/, '').split('?')[0]
    calls.requests.push({ method: opts.method, path: pathOnly, body: opts.data })
    setTimeout(() => {
      const factory = routes[pathOnly]
      const dynamic = factory ? null : dynamicRoute(pathOnly, opts.method)
      if (!factory && !dynamic) {
        opts.fail({ errMsg: 'no fake route: ' + pathOnly })
        return
      }
      opts.success({ statusCode: 200, data: { code: 0, message: 'ok', data: factory ? factory() : dynamic } })
    }, 0)
  }
}

const appGlobal = { globalData: { reviewResult: null } }
global.getApp = () => appGlobal
global.App = () => {}

let captured = null
global.Page = (definition) => { captured = definition }
global.Component = (definition) => { captured = definition }
global.getCurrentPages = () => [{ route: 'pages/index/index' }]

function instantiate(definition) {
  const page = Object.create(definition)
  page.data = JSON.parse(JSON.stringify(definition.data || {}))
  page.setData = function setData(patch) {
    Object.assign(this.data, patch)
  }
  return page
}

function loadPage(relative) {
  captured = null
  delete require.cache[require.resolve(relative)]
  require(relative)
  return instantiate(captured)
}

const flush = () => new Promise((resolve) => setTimeout(resolve, 20))

const dateUtil = require('../utils/date')

;(async () => {
  check('月历固定 6×7 且周一起始', () => {
    const cells = dateUtil.monthCells(2026, 10, todayIso)
    assert.strictEqual(cells.length, 42)
    // 2026-10-01 是周四，周一起始时前面空 3 格
    assert.strictEqual(cells[3].day, 1)
    assert.strictEqual(cells[3].iso, '2026-10-01')
    assert.strictEqual(cells[2].inMonth, false)
    assert.strictEqual(cells[33].day, 31)
  })

  check('未来日期与今天标记正确', () => {
    const cells = dateUtil.monthCells(2026, 10, '2026-10-20')
    const today = cells.find((cell) => cell.iso === '2026-10-20')
    const future = cells.find((cell) => cell.iso === '2026-10-21')
    const past = cells.find((cell) => cell.iso === '2026-10-19')
    assert.strictEqual(today.isToday, true)
    assert.strictEqual(today.isFuture, false)
    assert.strictEqual(future.isFuture, true)
    assert.strictEqual(past.isFuture, false)
  })

  check('日期标签与月份标题', () => {
    assert.strictEqual(dateUtil.dateLabel('2026-10-16'), '10月16日 周五')
    assert.strictEqual(dateUtil.monthTitle(2026, 10), '2026年10月')
    assert.deepStrictEqual(dateUtil.shiftMonth(2026, 1, -1), { year: 2025, month: 12 })
  })

  const indexPage = loadPage('../pages/index/index')

  await checkAsync('首页加载日历并选中今天', async () => {
    indexPage.onLoad()
    await flush()
    assert.strictEqual(indexPage.data.state, 'ready')
    assert.strictEqual(indexPage.data.selected, todayIso)
    assert.strictEqual(indexPage.data.cells.length, 42)
    assert.strictEqual(indexPage.data.daySummary.wordCount, 20)
    assert.strictEqual(indexPage.data.daySummary.wrongCount, 2)
    assert.strictEqual(indexPage.data.monthEmpty, false)
    assert.strictEqual(indexPage.data.wordbookName, 'CET-4')
    const learned = indexPage.data.cells.filter((cell) => cell.learned)
    assert.strictEqual(learned.length, 1)
  })

  await checkAsync('点未来日期不改变选中', async () => {
    const before = indexPage.data.selected
    const futureIndex = indexPage.data.cells.findIndex((cell) => cell.isFuture)
    indexPage.onDayTap({ currentTarget: { dataset: { index: futureIndex } } })
    assert.strictEqual(indexPage.data.selected, before)
  })

  await checkAsync('切到上个月后选中月内日期', async () => {
    indexPage.onPrevMonth()
    await flush()
    assert.strictEqual(indexPage.data.state, 'ready')
    assert.strictEqual(indexPage.data.isCurrentMonth, false)
    assert.ok(indexPage.data.selected.indexOf('-') > 0)
  })

  await checkAsync('不允许浏览未来月份', async () => {
    indexPage.setData({ year: new Date().getFullYear(), month: new Date().getMonth() + 1 })
    const toastCount = calls.toast.length
    indexPage.onNextMonth()
    await flush()
    assert.strictEqual(calls.toast.length, toastCount + 1)
    assert.strictEqual(calls.toast[calls.toast.length - 1].title, '已经是本月了')
  })

  const learnPage = loadPage('../pages/learn/learn')

  await checkAsync('新学翻过才记录，打开页面不写任何记录', async () => {
    calls.requests.length = 0
    learnPage.onLoad()
    await flush()
    assert.strictEqual(learnPage.data.state, 'ready')
    assert.strictEqual(learnPage.data.batchSize, 2)
    assert.strictEqual(learnPage.data.current.spelling, 'abandon')
    assert.strictEqual(learnPage.data.revealed, false)
    assert.strictEqual(
      calls.requests.filter((item) => item.path === '/api/study/records').length,
      0,
      '打开页面不应写学习记录'
    )

    learnPage.onReveal()
    assert.strictEqual(learnPage.data.revealed, true)

    await learnPage.onNext()
    await flush()
    assert.strictEqual(learnPage.data.index, 1)
    assert.strictEqual(learnPage.data.current.spelling, 'benefit')
    const afterFirst = calls.requests.filter((item) => item.path === '/api/study/records')
    assert.strictEqual(afterFirst.length, 1)
    assert.deepStrictEqual(afterFirst[0].body.wordIds, [1])

    await learnPage.onNext()
    await flush()
    assert.strictEqual(learnPage.data.state, 'finished')
    assert.strictEqual(learnPage.data.finishedTitle, '本批 2 词已学完')
    const afterSecond = calls.requests.filter((item) => item.path === '/api/study/records')
    assert.strictEqual(afterSecond.length, 2)
    assert.deepStrictEqual(afterSecond[1].body.wordIds, [2])
    assert.strictEqual(learnPage.data.todayWordCount, 3)
  })

  const reviewPage = loadPage('../pages/review/review')

  await checkAsync('复习提交只回传没记住的词', async () => {
    appGlobal.globalData.reviewResult = null
    reviewPage.onLoad({ date: todayIso })
    await flush()
    assert.strictEqual(reviewPage.data.state, 'ready')
    assert.strictEqual(reviewPage.data.batchSize, 2)
    reviewPage.onForgot()
    assert.deepStrictEqual(reviewPage.data.wrongIds, [1])
    reviewPage.onRemember()
    await flush()
    assert.strictEqual(reviewPage.data.state, 'submitting')
    assert.ok(appGlobal.globalData.reviewResult)
    assert.strictEqual(appGlobal.globalData.reviewResult.wrong, 1)
    assert.strictEqual(appGlobal.globalData.reviewResult.wrongWords[0].id, 1)
    assert.ok(calls.redirectTo[calls.redirectTo.length - 1].indexOf('/pages/result/result') === 0)
  })

  const resultPage = loadPage('../pages/result/result')

  await checkAsync('结果页读取提交结果并清空全局数据', async () => {
    appGlobal.globalData.reviewResult = {
      date: todayIso,
      total: 2,
      wrong: 1,
      wrongWords: [words[0]]
    }
    const page = instantiate(captured)
    page.onLoad()
    assert.strictEqual(page.data.loaded, true)
    assert.strictEqual(page.data.allRight, false)
    assert.strictEqual(page.data.wrongWords.length, 1)
    assert.strictEqual(appGlobal.globalData.reviewResult, null)
  })

  const wordbookPage = loadPage('../pages/wordbook/wordbook')

  await checkAsync('我的页展示统计与每批词数', async () => {
    storage.wr_token = 'fake-token'
    storage.wr_user = { id: 1, nickname: '小明', batchSize: 30 }
    const profilePage = loadPage('../pages/profile/profile')
    profilePage.onShow()
    await flush()
    assert.strictEqual(profilePage.data.batchSize, 30)
    assert.ok(profilePage.data.stats, '应有统计数据')
    assert.strictEqual(profilePage.data.stats.streakDays, 3)
    assert.strictEqual(profilePage.data.stats.learnedDays, 4)
    assert.strictEqual(profilePage.data.wordbookName, 'CET-4')
  })

  await checkAsync('词库页分组：预置 3 个 + 我的 1 个', async () => {
    wordbookPage.onShow()
    await flush()
    assert.strictEqual(wordbookPage.data.state, 'ready')
    assert.strictEqual(wordbookPage.data.builtin.length, 3)
    assert.strictEqual(wordbookPage.data.mine.length, 1)
    assert.strictEqual(wordbookPage.data.mine[0].name, '我的词表')
  })

  await checkAsync('切换预置词库更新选中态', async () => {
    storage.wr_token = 'fake-token'
    storage.wr_user = { id: 1, nickname: '小明' }
    await wordbookPage.onPickBuiltin({ currentTarget: { dataset: { index: 1 } } })
    await flush()
    assert.strictEqual(wordbookPage.data.builtin[1].selected, true)
    assert.strictEqual(wordbookPage.data.builtin[0].selected, false)
  })

  const detailPage = loadPage('../pages/wordbook-detail/wordbook-detail')

  await checkAsync('词库详情加载词条', async () => {
    detailPage.onLoad({ id: '9' })
    detailPage.onShow()
    await flush()
    assert.strictEqual(detailPage.data.state, 'ready')
    assert.strictEqual(detailPage.data.total, 2)
    assert.strictEqual(detailPage.data.words.length, 2)
    assert.strictEqual(detailPage.data.book.name, '我的词表')
    assert.strictEqual(detailPage.data.isCurrent, false)
  })

  const wordEditPage = loadPage('../pages/word-edit/word-edit')

  await checkAsync('新增词条提交到对应词库', async () => {
    calls.requests.length = 0
    wordEditPage.onLoad({ bookId: '9' })
    wordEditPage.setData({
      form: { spelling: 'gamma', phonetic: '', meaning: '伽马', exampleSentence: '', exampleTranslation: '' }
    })
    await wordEditPage.onSubmit()
    await flush()
    const posts = calls.requests.filter(
      (item) => item.path === '/api/wordbooks/9/words' && item.method === 'POST')
    assert.strictEqual(posts.length, 1)
    assert.strictEqual(posts[0].body.spelling, 'gamma')
  })

  const wordEditEdit = loadPage('../pages/word-edit/word-edit')

  await checkAsync('编辑词条会回填表单', async () => {
    wordEditEdit.onLoad({ bookId: '9', wordId: '101' })
    await flush()
    assert.strictEqual(wordEditEdit.data.isNew, false)
    assert.strictEqual(wordEditEdit.data.form.spelling, 'alpha')
  })

  const importPage = loadPage('../pages/import/import')

  await checkAsync('导入页：解析预览后导入到新词库', async () => {
    storage.wr_token = 'fake-token'
    importPage.onLoad()
    await flush()
    importPage.onChooseFile()
    await flush()
    assert.strictEqual(importPage.data.state, 'preview')
    assert.strictEqual(importPage.data.preview.total, 3)
    assert.strictEqual(importPage.data.preview.invalid, 1)
    assert.strictEqual(importPage.data.newName, 'demo')

    await importPage.onImport()
    await flush()
    assert.strictEqual(importPage.data.state, 'done')
    assert.strictEqual(importPage.data.result.imported, 2)
    assert.strictEqual(calls.uploads.length, 2)
    assert.ok(calls.uploads[1].url.indexOf('newBookName=demo') > 0, calls.uploads[1].url)
  })

  const wrongbookPage = loadPage('../pages/wrongbook/wrongbook')

  await checkAsync('错词本：复习后提交记住的词', async () => {
    storage.wr_token = 'fake-token'
    calls.requests.length = 0
    wrongbookPage.onShow()
    await flush()
    assert.strictEqual(wrongbookPage.data.state, 'ready')
    assert.strictEqual(wrongbookPage.data.total, 2)

    wrongbookPage.onStart()
    assert.strictEqual(wrongbookPage.data.mode, 'review')
    assert.strictEqual(wrongbookPage.data.current.id, 1)

    wrongbookPage.onRemember()
    await flush()
    assert.strictEqual(wrongbookPage.data.index, 1)
    assert.deepStrictEqual(wrongbookPage.data.remembered, [1])

    wrongbookPage.onForgot()
    await flush()
    const submits = calls.requests.filter(
      (item) => item.path === '/api/wrongbook/submit' && item.method === 'POST')
    assert.strictEqual(submits.length, 1)
    assert.deepStrictEqual(submits[0].body.rememberedWordIds, [1])
    assert.strictEqual(wrongbookPage.data.mode, 'list')
  })

  // ---- 单机版：本地 API 全流程 ----
  const offlineApi = require('../utils/offline-api')
  const offlineStore = require('../utils/offline-store')

  await checkAsync('单机版：学习 → 统计 → 复习 → 错词本全走本地', async () => {
    offlineStore.reset()
    const books = offlineApi.request('/api/wordbooks', 'GET')
    assert.strictEqual(books.length, 3)
    assert.strictEqual(books[0].selected, true)

    const session = offlineApi.request('/api/study/sessions', 'POST', { size: 5 })
    assert.strictEqual(session.words.length, 5)
    assert.ok(session.words[0].spelling)
    assert.strictEqual(session.words[0].exampleSentence !== undefined, true)

    offlineApi.request('/api/study/records', 'POST', {
      wordIds: session.words.map((word) => word.id)
    })
    const stats = offlineApi.request('/api/users/me/stats', 'GET')
    assert.strictEqual(stats.todayWords, 5)
    assert.strictEqual(stats.learnedWords, 5)
    assert.strictEqual(stats.learnedDays, 1)

    const calendar = offlineApi.request('/api/calendar?year=' + todayIso.slice(0, 4) +
      '&month=' + Number(todayIso.slice(5, 7)), 'GET')
    assert.strictEqual(calendar.summary.words, 5)

    const review = offlineApi.request('/api/review?date=' + todayIso, 'GET')
    assert.strictEqual(review.words.length, 5)

    offlineApi.request('/api/review/submit', 'POST', {
      date: todayIso,
      wrongWordIds: [review.words[0].id]
    })
    const wrong = offlineApi.request('/api/wrongbook', 'GET')
    assert.strictEqual(wrong.total, 1)

    const cleared = offlineApi.request('/api/wrongbook/submit', 'POST', {
      rememberedWordIds: [review.words[0].id]
    })
    assert.strictEqual(cleared, 1)
    assert.strictEqual(offlineApi.request('/api/wrongbook', 'GET').total, 0)

    // 已学过的词不会再次出现在新学批次里
    const next = offlineApi.request('/api/study/sessions', 'POST', { size: 5 })
    assert.strictEqual(next.words.length, 5)
    assert.ok(next.words[0].id !== review.words[0].id)
  })

  console.log('')
  console.log(passed.length + ' passed, ' + failures.length + ' failed')
  if (failures.length) {
    process.exit(1)
  }
})()
