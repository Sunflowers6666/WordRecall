/**
 * 单机版本地 API：把后端的接口语义在本地实现，页面代码不用改。
 * 记录模型：records[wordId] = { first: 'YYYY-MM-DD', wrong: 0|1 }
 *   - first：第一次学这个词的日期（与后端 learning_record 含义一致）
 *   - wrong：该词最近一次复习是否没记住
 */
const store = require('./offline-store')
const data = require('./offline-data')

function fail(message) {
  const error = new Error(message)
  error.kind = 'biz'
  throw error
}

function todayIso() {
  const now = new Date()
  return now.getFullYear() + '-' +
    String(now.getMonth() + 1).padStart(2, '0') + '-' +
    String(now.getDate()).padStart(2, '0')
}

function toDto(word, wrong) {
  return {
    id: word.id,
    spelling: word.spelling,
    phonetic: word.phonetic,
    meaning: word.meaning,
    exampleSentence: word.exampleSentence,
    exampleTranslation: word.exampleTranslation,
    isWrong: !!wrong
  }
}

function recordOf(state, wordId) {
  return state.records[wordId] || null
}

function learnedOn(state, date) {
  return Object.keys(state.records)
    .map((key) => Number(key))
    .filter((id) => state.records[id].first === date)
}

function wrongIds(state) {
  return Object.keys(state.records)
    .map((key) => Number(key))
    .filter((id) => state.records[id].wrong)
}

function currentWords(state) {
  return data.words(state.wordbookId)
}

function stats(state) {
  const ids = Object.keys(state.records)
  const dates = {}
  ids.forEach((id) => {
    dates[state.records[id].first] = true
  })
  const learnedDays = Object.keys(dates).length
  const today = todayIso()
  const todayWords = ids.filter((id) => state.records[id].first === today).length
  const wrongWords = wrongIds(state).length

  let streakDays = 0
  const cursor = new Date()
  const iso = (value) => value.getFullYear() + '-' +
    String(value.getMonth() + 1).padStart(2, '0') + '-' +
    String(value.getDate()).padStart(2, '0')
  if (!dates[iso(cursor)]) {
    cursor.setDate(cursor.getDate() - 1)
  }
  while (dates[iso(cursor)]) {
    streakDays += 1
    cursor.setDate(cursor.getDate() - 1)
  }

  return { learnedDays, learnedWords: ids.length, wrongWords, streakDays, todayWords }
}

function calendar(state, year, month) {
  const prefix = year + '-' + String(month).padStart(2, '0')
  const map = {}
  Object.keys(state.records).forEach((id) => {
    const record = state.records[id]
    if (record.first.indexOf(prefix) !== 0) {
      return
    }
    if (!map[record.first]) {
      map[record.first] = { date: record.first, wordCount: 0, wrongCount: 0 }
    }
    map[record.first].wordCount += 1
    if (record.wrong) {
      map[record.first].wrongCount += 1
    }
  })
  const days = Object.keys(map).sort().map((key) => map[key])
  const summary = days.reduce((acc, day) => ({
    days: acc.days + 1,
    words: acc.words + day.wordCount,
    wrong: acc.wrong + day.wrongCount
  }), { days: 0, words: 0, wrong: 0 })
  return { month: prefix, days: days, summary: summary }
}

function sessions(state, size) {
  const batch = size || state.batchSize
  const words = currentWords(state)
  const unlearned = words.filter((word) => !recordOf(state, word.id))
  const picked = unlearned.slice(0, batch)
  return {
    words: picked.map((word) => toDto(word, false)),
    todayWordCount: stats(state).todayWords,
    remaining: unlearned.length - picked.length
  }
}

function recordWords(state, wordIds) {
  const today = todayIso()
  let inserted = 0
  ;(wordIds || []).forEach((wordId) => {
    if (!state.records[wordId]) {
      state.records[wordId] = { first: today, wrong: 0 }
      inserted += 1
    }
  })
  store.save(state)
  return { recorded: inserted, todayWordCount: stats(state).todayWords }
}

function reviewOf(state, date) {
  const target = date || todayIso()
  const words = currentWords(state)
  const ids = learnedOn(state, target)
  return {
    date: target,
    words: ids
      .map((id) => words.find((word) => word.id === id))
      .filter(Boolean)
      .map((word) => toDto(word, state.records[word.id].wrong))
  }
}

function submitReview(state, date, wrongWordIds) {
  const target = date || todayIso()
  const ids = learnedOn(state, target)
  if (!ids.length) {
    fail('这天没有学习记录')
  }
  const wrong = {}
  ;(wrongWordIds || []).forEach((id) => {
    wrong[id] = true
  })
  ids.forEach((id) => {
    state.records[id].wrong = wrong[id] ? 1 : 0
  })
  store.save(state)
  return { date: target, total: ids.length, wrong: Object.keys(wrong).length }
}

function wrongbook(state) {
  const words = []
  ;[1, 2, 3].forEach((bookId) => {
    const ids = wrongIds(state)
    data.words(bookId).forEach((word) => {
      if (ids.indexOf(word.id) >= 0) {
        words.push(toDto(word, true))
      }
    })
  })
  return { total: words.length, words: words }
}

function submitWrongbook(state, remembered) {
  let cleared = 0
  ;(remembered || []).forEach((id) => {
    if (state.records[id] && state.records[id].wrong) {
      state.records[id].wrong = 0
      cleared += 1
    }
  })
  store.save(state)
  return cleared
}

/** 入口：与后端 REST 路径对齐。 */
function request(url, method, body) {
  const state = store.load()
  const questionMark = url.indexOf('?')
  const path = questionMark >= 0 ? url.slice(0, questionMark) : url
  const query = {}
  if (questionMark >= 0) {
    url.slice(questionMark + 1).split('&').forEach((pair) => {
      if (!pair) {
        return
      }
      const index = pair.indexOf('=')
      query[decodeURIComponent(pair.slice(0, index))] = decodeURIComponent(pair.slice(index + 1))
    })
  }
  const verb = (method || 'GET').toUpperCase()

  if (path === '/api/wordbooks' && verb === 'GET') {
    return data.books().map((book) => Object.assign({}, book, { selected: book.id === state.wordbookId }))
  }
  if (path === '/api/users/me' && verb === 'GET') {
    return {
      id: 0,
      nickname: '本机',
      avatarUrl: null,
      currentWordbookId: state.wordbookId,
      batchSize: state.batchSize
    }
  }
  if (path === '/api/users/me' && verb === 'PATCH') {
    if (body && body.batchSize) {
      state.batchSize = body.batchSize
      store.save(state)
    }
    return {
      id: 0,
      nickname: '本机',
      avatarUrl: null,
      currentWordbookId: state.wordbookId,
      batchSize: state.batchSize
    }
  }
  if (path === '/api/users/me/wordbook' && verb === 'PUT') {
    const bookId = Number(body && body.wordbookId)
    if (!data.BOOKS.some((book) => book.id === bookId)) {
      fail('词库不存在')
    }
    state.wordbookId = bookId
    store.save(state)
    return {
      id: 0,
      nickname: '本机',
      avatarUrl: null,
      currentWordbookId: bookId,
      batchSize: state.batchSize
    }
  }
  if (path === '/api/users/me/stats' && verb === 'GET') {
    return stats(state)
  }
  if (path === '/api/calendar' && verb === 'GET') {
    return calendar(state, Number(query.year), Number(query.month))
  }
  if (path === '/api/study/sessions' && verb === 'POST') {
    return sessions(state, body && body.size)
  }
  if (path === '/api/study/records' && verb === 'POST') {
    return recordWords(state, body && body.wordIds)
  }
  if (path === '/api/review' && verb === 'GET') {
    return reviewOf(state, query.date)
  }
  if (path === '/api/review/submit' && verb === 'POST') {
    return submitReview(state, body && body.date, body && body.wrongWordIds)
  }
  if (path === '/api/wrongbook' && verb === 'GET') {
    return wrongbook(state)
  }
  if (path === '/api/wrongbook/submit' && verb === 'POST') {
    return submitWrongbook(state, body && body.rememberedWordIds)
  }
  if (path.indexOf('/audio') >= 0) {
    fail('单机版没有发音')
  }
  fail('单机版不支持这个接口：' + path)
}

module.exports = { request }
