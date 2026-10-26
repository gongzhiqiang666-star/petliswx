const request=require('./request')
async function openReportById(id) {
 if(!Number.isInteger(Number(id)) || Number(id)<=0)throw new Error('报告编号无效')
 const path=await request.downloadFile('/pdf/view/'+Number(id))
 await wx.openDocument({filePath:path,fileType:'pdf',showMenu:true})
}
function openReportFromURL(url){const m=String(url || '').match(/\/pdf\/view\/(\d+)(?:[?#]|$)/);if(!m)return Promise.reject(new Error('报告尚未生成，请刷新后重试'));return openReportById(Number(m[1]))}
module.exports={openReportById,openReportFromURL}
