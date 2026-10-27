const fs = require('node:fs')
const path = require('node:path')
const Module = require('node:module')

const root = path.resolve(__dirname, '..')

function loadPage(relative, requestHandlers = {}, userInfo = {}) {
  const file = path.join(root, 'pages', `${relative}.js`)
  const originalLoad = Module._load
  const state = { definition: null, wx: { calls: [] }, app: null }
  const storage = { token: 'stored-token', userInfo }
  const request = {
    get(url, params) { if (requestHandlers.get) return requestHandlers.get(url, params); return Promise.resolve({}) },
    post(url, data) { if (requestHandlers.post) return requestHandlers.post(url, data); return Promise.resolve({}) },
    put(url, data) { if (requestHandlers.put) return requestHandlers.put(url, data); return Promise.resolve({}) }
  }
  state.app = {
    globalData: { token: 'token', userInfo, hospitalId: 1 },
    getHospitalId() { return 1 },
    getUserInfo() { return userInfo },
    logout() { storage.token = null; state.wx.calls.push(['logout']); return Promise.resolve() },
    clearCachedOrderForm() {}
  }
  const wx = new Proxy({
    getStorageSync(key) { return storage[key] },
    removeStorageSync(key) { delete storage[key] },
    setStorageSync(key, value) { storage[key] = value },
    createAnimation() { return { opacity() { return this }, translateY() { return this }, step() { return this } } },
    getAccountInfoSync() { return { miniProgram: { envVersion: 'develop' } } }
  }, {
    get(target, key) {
      if (key in target) return target[key]
      return (...args) => { state.wx.calls.push([key, ...args]) }
    }
  })
  global.wx = wx
  global.getApp = () => state.app
  global.getCurrentPages = () => []
  global.Page = definition => { state.definition = definition }
  Module._load = function(requestName, parent, isMain) {
    if (requestName.endsWith('utils/request') || requestName.endsWith('utils/request.js')) return request
    if (requestName.endsWith('api/index.js')) return {
      orders: { getDetail: id => request.get(`/orders/${id}`), getList: params => request.get('/orders', params) },
      pets: { getDetail: id => request.get(`/pets/${id}`) },
      reports: { getList: params => request.get('/reports', params) },
      user: { getProfile: () => request.get('/user/profile') },
      dictionary: { getHospitalById: id => request.get(`/dictionary/hospitals/${id}`) },
      notifications: { getUnreadCount: () => request.get('/inbox/unread-count') }
    }
    return originalLoad.call(this, requestName, parent, isMain)
  }
  try {
    delete require.cache[file]
    require(file)
  } finally {
    Module._load = originalLoad
  }
  const instance = { ...state.definition, data: JSON.parse(JSON.stringify(state.definition.data || {})) }
  instance.setData = patch => { Object.assign(instance.data, patch) }
  return { instance, wx, app: state.app, storage, request }
}

function freshRequire(relative) {
  const file = path.join(root, relative)
  delete require.cache[file]
  return require(file)
}

function loadApp(post, events) {
  const originalLoad = Module._load
  let definition
  global.App = value => { definition = value }
  global.wx = {
    removeStorageSync(key) { events.push(['remove', key]) },
    reLaunch(options) { events.push(['reLaunch', options.url]) },
    getStorageSync() { return null }
  }
  const requestPath = require.resolve(path.join(root, 'utils/request.js'))
  require.cache[requestPath] = { id: requestPath, filename: requestPath, loaded: true, exports: { post: (url, data) => { events.push(['post', url]); return post(url, data) } } }
  Module._load = function(name, parent, main) {
    if (name === './utils/updates') return { installUpdates() {} }
    if (name === './utils/diagnostics') return { capture() {} }
    return originalLoad.call(this, name, parent, main)
  }
  try {
    freshRequire('app.js')
    Object.assign(definition.globalData, { token: 'token', userInfo: {id:1}, hospitalId: 1, cityId: 1 })
    return definition
  } finally { Module._load = originalLoad }
}

module.exports = { root, loadPage, loadApp, freshRequire }
