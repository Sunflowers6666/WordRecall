const pako = require('./vendor/pako.min.js')

/**
 * 预置词库数据由 scripts/offline/generate_words.py 生成，
 * 每个模块导出一个 base64(gzip(JSON)) 字符串。
 * 注意：小程序不支持动态 require 路径，必须写成静态字面量。
 */
const MODULES = {
  1: () => require('../data/wb1'),
  2: () => require('../data/wb2'),
  3: () => require('../data/wb3')
}

const BOOKS = [
  { id: 1, name: 'CET-4' },
  { id: 2, name: 'CET-6' },
  { id: 3, name: 'IELTS' }
]

const WORD_ID_BASE = 100000
const cache = {}

function rowsFor(bookId) {
  const loader = MODULES[bookId]
  if (!loader) {
    return []
  }
  const packed = loader()
  const buffer = wx.base64ToArrayBuffer(packed)
  const json = pako.ungzip(new Uint8Array(buffer), { to: 'string' })
  return JSON.parse(json)
}

/** 返回词库单词数组（首次调用解压并缓存）。 */
function words(bookId) {
  if (!cache[bookId]) {
    cache[bookId] = rowsFor(bookId).map((row, index) => ({
      id: bookId * WORD_ID_BASE + index + 1,
      bookId: bookId,
      spelling: row[0],
      phonetic: row[1] || null,
      meaning: row[2],
      exampleSentence: row[3] || null,
      exampleTranslation: row[4] || null,
      sortOrder: index + 1
    }))
  }
  return cache[bookId]
}

function books() {
  return BOOKS.map((book) => ({
    id: book.id,
    name: book.name,
    description: null,
    wordCount: words(book.id).length,
    editable: false,
    source: 'BUILTIN'
  }))
}

module.exports = { BOOKS, words, books }
