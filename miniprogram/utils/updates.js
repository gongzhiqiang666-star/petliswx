// Only apply a downloaded release after the user agrees to restart.
function installUpdates() {
  if (typeof wx.getUpdateManager !== 'function') return
  const manager = wx.getUpdateManager()
  let prompting = false
  manager.onUpdateReady(async () => {
    if (prompting) return
    prompting = true
    try {
      const answer = await wx.showModal({
        title: '新版本已准备好',
        content: '重启后使用新版本。正在填写的送检信息请先保存为草稿；也可以稍后重新打开小程序更新。',
        confirmText: '重启更新', cancelText: '稍后'
      })
      if (answer.confirm) manager.applyUpdate()
    } catch (_) {
      // A dismissed prompt must never force a restart.
    } finally { prompting = false }
  })
  manager.onUpdateFailed(() => {
    wx.showToast({title: '更新下载失败，请稍后重新打开', icon: 'none'})
  })
}
module.exports = {installUpdates}
