// pages/order-detail/order-detail.js
const api = require('../../api/index.js')
const request = require('../../utils/request')
const { adaptShipment } = require('../../utils/logistics')

Page({
  data: {
    clinical: null, clinicalError: false, clinicalLoading: false,
    orderData: {
      orderId: '',
      userNO: '',
      sampleStatus: 0,
      sampleStatusDesc: '',
      animalName: '',
      animalNameEn: '',
      animalType: '',
      animalTypeEn: '',
      dictSexId: 1,
      genderText: '公',
      age: '',
      animalWeight: '',
      chipNO: '',
      birthday: '',
      collectDt: '',
      receiveSampleDt: '',
      sendSampleDt: '',
      sendReportDt: '',
      sampleArea: '',
      masterName: '',
      masterNameEn: '',
      doctorName: '',
      marketingName: '',
      remark: '',
      testFee: 0,
      createDt: '',
      items: [],
      totalItems: 0
    },
    timeline: [],
    shipment: null,
    shipmentTimeline: [],
    shipmentHistory: [],
    historyOpening: false,
    historyVisible: false,
    historyError: '',
    logisticsError: '',
    labelOpening: false,
    loading: true,
    error: false
  },

  onLoad(options) {
    const orderId = options.id;
    this._orderId = orderId;
    if (orderId) {
      this.loadOrderDetail(orderId);
    } else {
      this.setData({ error: true, loading: false });
      wx.showToast({
        title: '订单ID缺失',
        icon: 'none'
      });
    }
  },

  /**
   * 加载订单详情
   */
  async loadClinical() {
    const id = this._orderId || this.data.orderData.orderId;
    if (!id || this.data.clinicalLoading) return;
    this.setData({clinicalLoading:true,clinicalError:false});
    try {
      const data = await request.get('/orders/'+encodeURIComponent(id)+'/clinical');
      this.setData({clinical:data.fields || {},clinicalError:false});
    } catch (_) { this.setData({clinical:null,clinicalError:true}); }
    finally { this.setData({clinicalLoading:false}); }
  },

  async loadOrderDetail(orderId) {
    if (typeof orderId !== 'string' && typeof orderId !== 'number') orderId = this._orderId || this.data.orderData.orderId;
    if (!orderId) { this.setData({loading:false,error:true}); return; }
    this._orderId = orderId;
    this.setData({ loading: true, error: false });

    try {
      const result = await api.orders.getDetail(orderId);

      this._rawOrder=result;
      // 处理返回的数据
      const orderData = {
        orderId: result.orderId || result.orderId,
        reviewReason:result.reviewReason || '',canResubmit:!!result.canResubmit,reviewHistory:(Array.isArray(result.reviewHistory)?result.reviewHistory:[]).map(row=>({...row,createdAtLabel:this.formatDateTime(row.createdAt) || '\u65f6\u95f4\u672a\u8bb0\u5f55'})),
        refunded:!!result.refunded,refundMock:!!result.refundMock,refundAmount:result.refundAmount,refundReason:String(result.refundReason || '').replace(/^PHP管理员#\d+：/,''),refundedAt:this.formatDateTime(result.refundedAt),
        userNO: result.userNO || '',
        sampleStatus: result.sampleStatus || 0,
        sampleStatusDesc: result.sampleStatusDesc || this.getStatusLabel(result.sampleStatus),
        animalName: result.animalName || '',
        animalNameEn: result.animalNameEn || '',
        animalType: result.animalType || '',
        animalTypeEn: result.animalTypeEn || '',
        dictSexId: result.dictSexId || 1,
        genderText: this.getGenderText(result.dictSexId),
        age: result.age || '',
        animalWeight: result.animalWeight || '',
        chipNO: result.chipNO || '',
        birthday: this.formatDate(result.birthday),
        collectDt: this.formatDate(result.collectDt),
        receiveSampleDt: this.formatDate(result.receiveSampleDt),
        sendSampleDt: this.formatDate(result.sendSampleDt),
        sendReportDt: this.formatDateTime(result.sendReportDt),
        sampleArea: result.sampleArea || '',
        masterName: result.masterName || '',
        masterNameEn: result.masterNameEn || '',
        doctorName: result.doctorName || '',
        marketingName: result.marketingName || '',
        remark: result.remark || '',
        testFee: result.testFee || 0,
        createDt: this.formatDateTime(result.createDt),
        items: this.formatItems(result.items || []),
        totalItems: (result.items || []).length
      };

      // 构建时间线数据
      const timeline = this.buildTimeline(orderData, result);

      this.setData({
        orderData,
        timeline,
        loading: false
      });
      await Promise.all([this.loadLogistics(orderId), this.loadClinical()]);
    } catch (err) {
      console.error('[OrderDetail] 加载订单详情失败:', err);
      this.setData({
        loading: false,
        error: true
      });

      // 网络错误提示
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      });
    }
  },

  copyTrackingNo() {
    const number=this.data.shipment && this.data.shipment.trackingNo;
    if(!number)return;
    wx.setClipboardData({data:String(number)});
  },

  async openShipmentHistory() {
    if(this.data.historyOpening)return;
    if(this.data.historyVisible){this.setData({historyVisible:false});return;}
    const generation=this._logisticsSequence || 0;
    this.setData({historyOpening:true,historyError:''});
    try{const id=Number(this.data.orderData.orderId || this.data.orderId);if(!Number.isInteger(id)||id<=0)throw Error('送检单编号无效');const rows=await request.get(`/logistics/orders/${id}/history`);if(generation!==(this._logisticsSequence||0))return;this.setData({shipmentHistory:rows.map(adaptShipment),historyVisible:true})}catch(e){if(generation===(this._logisticsSequence||0))this.setData({historyError:e.message || '寄件历史加载失败，请重试'})}finally{if(generation===(this._logisticsSequence||0))this.setData({historyOpening:false})}
  },
  copyHistoryTracking(e){const row=this.data.shipmentHistory[Number(e.currentTarget.dataset.index)];if(row && row.trackingNo)wx.setClipboardData({data:String(row.trackingNo)})},

  async openShipmentLabel() {
    if(this.data.labelOpening) return;
    this.setData({labelOpening:true});
    try {
      const id=Number(this.data.orderData.orderId || this.data.orderId);
      if(!Number.isInteger(id) || id<=0) throw new Error('送检单编号无效');
      const label=await request.get(`/logistics/sf/orders/${id}/label`);
      if(!label || !/^\/platform-files\/\d+\/download$/.test(label.url || '')) throw new Error('面单下载信息无效');
      const filePath=await request.downloadFile(label.url);
      await wx.openDocument({filePath,fileType:'pdf',showMenu:true});
    } catch(error) {
      wx.showToast({title:/409/.test(error.message || '')?'面单暂不可用，请刷新物流':error.message || '面单暂时无法打开',icon:'none'});
    } finally {this.setData({labelOpening:false});}
  },

  async loadLogistics(orderId) {
    const generation=this._logisticsSequence=(this._logisticsSequence||0)+1;this.setData({historyOpening:false,historyVisible:false,shipmentHistory:[],historyError:''});
    try {
      const shipment = adaptShipment(await request.get(`/logistics/orders/${orderId}`))
      if(generation!==this._logisticsSequence)return;
      this.setData({ shipment, shipmentTimeline: shipment ? shipment.timeline : [], logisticsError: '',shipmentHistory:[],historyVisible:false,historyError:'' })
    } catch (error) {
      if(generation!==this._logisticsSequence)return;
      const missing = /不存在|404/.test(error.message || '')
      this.setData({ shipment: null, shipmentTimeline: [], logisticsError: missing ? '' : '物流信息暂时无法加载' })
    }
  },

  /**
   * 解析日期为时间戳（iOS兼容）
   */
  parseDate(dateStr) {
    if (!dateStr) return 0;

    // 处理时间戳（数字）
    if (typeof dateStr === 'number') {
      return dateStr;
    }
    // 处理时间戳字符串
    if (typeof dateStr === 'string' && /^\d+$/.test(dateStr)) {
      return parseInt(dateStr);
    }
    // 处理字符串日期
    const isoStr = dateStr.replace(' ', 'T');
    const date = new Date(isoStr);
    return isNaN(date.getTime()) ? 0 : date.getTime();
  },

  /**
   * 构建时间线数据
   */
  buildTimeline(orderData, rawData) {
    const timeline = []
    const status = orderData.sampleStatus

    // 判断日期是否有效（不是1899年的默认值）
    const isValidDate = (dateStr) => {
      if (!dateStr) return false
      const year = this.parseDate(dateStr)
      return year > 0 && new Date(year).getFullYear() > 1900
    }

    // 1. 订单创建 - 总是显示（已完成）
    timeline.push({
      title: '订单创建',
      time: orderData.createDt,
      operator: '系统',
      completed: true,
      current: false,
      pending: false
    })

    // 2. 样本采集 - 始终显示
    const hasCollectDt = isValidDate(rawData.collectDt)
    timeline.push({
      title: '样本采集',
      time: hasCollectDt ? orderData.collectDt : '',
      operator: hasCollectDt ? (orderData.doctorName || '未记录') : '',
      completed: hasCollectDt,
      current: false,
      pending: !hasCollectDt
    })

    // 3. 收到样本 - 始终显示
    const hasReceiveSampleDt = isValidDate(rawData.receiveSampleDt)
    timeline.push({
      title: '收到样本',
      time: hasReceiveSampleDt ? orderData.receiveSampleDt : '',
      operator: hasReceiveSampleDt ? '实验室' : '',
      completed: hasReceiveSampleDt,
      current: false,
      pending: !hasReceiveSampleDt
    })

    // 4. 寄出样本 - 可选显示
    const hasSendSampleDt = isValidDate(rawData.sendSampleDt)
    if (hasSendSampleDt) {
      timeline.push({
        title: '寄出样本',
        time: orderData.sendSampleDt,
        operator: '实验室',
        completed: true,
        current: false,
        pending: false
      })
    }

    // 5. 报告发出 - 已完成时显示
    const hasSendReportDt = isValidDate(rawData.sendReportDt)
    if (hasSendReportDt) {
      timeline.push({
        title: '报告发出',
        time: orderData.sendReportDt,
        operator: '实验室',
        completed: true,
        current: false,
        pending: false
      })
    }

    // 6. 检测中/已完成 - 根据状态判断
    const isTesting = status == 2 // 等待报告
    const isCompleted = status == 3 // 已完成

    if (isTesting && !hasSendReportDt) {
      timeline.push({
        title: '检测中',
        time: '',
        operator: '实验室',
        completed: false,
        current: true,
        pending: false,
        status: '检测进行中...'
      })
    } else if (isCompleted && !hasSendReportDt) {
      timeline.push({
        title: '检测完成',
        time: '',
        operator: '实验室',
        completed: true,
        current: false,
        pending: false,
        status: '所有检测项目已完成'
      })
    } else if (!hasSendReportDt) {
      // 其他状态（草稿、待审核等）显示为pending
      timeline.push({
        title: '检测完成',
        time: '',
        operator: '实验室',
        completed: false,
        current: false,
        pending: true
      })
    }

    return timeline
  },

  /**
   * 格式化检测项目列表
   */
  formatItems(items) {
    return items.map(item => ({
      itemId: item.itemId,
      name: item.testTypeName || `检测项目 ${item.infoTestTypeId}`,
      price: item.testPrice || 0,
      realFee: item.realFee || 0,
      diagnosisStatus: item.diagnosisStatus,
      diagnosisStatusLabel: this.getDiagnosisStatusLabel(item.diagnosisStatus),
      detailRemark: item.detailRemark || '',
      // PDF相关字段
      pdfHttpUrl: item.pdfHttpUrl || '',
      pdfFullUrl: item.pdfFullUrl || '',
      securePdfUrl: item.securePdfUrl || '', // 安全访问URL（带token验证）
      isSendMail: item.isSendMail || 0,
      sendMailDt: item.sendMailDt || null
    }));
  },

  /**
   * 获取诊断状态标签
   */
  getDiagnosisStatusLabel(status) {
    const labels = {
      0: '待检测',
      1: '检测中',
      2: '已完成',
      3: '已取消'
    };
    return labels[status] || '未知';
  },

  /**
   * 获取状态标签
   */
  async editAndResubmit(){
    const o=this._rawOrder;if(!o || !o.canResubmit)return;
    let speciesName='';try{const species=await api.dictionary.getAnimals();const selected=species.find(x=>x.infoAnimalId===o.infoAnimalId);speciesName=selected?selected.animalName:''}catch(e){}
    const date=value=>value?String(value).slice(0,10):'';
    const form={petId:o.petId || 'order-'+o.orderId,resubmitOrderId:o.orderId,laboratoryId:o.laboratoryId,laboratoryName:o.laboratoryName,userCouponId:o.userCouponId,
      petInfo:{animalName:o.animalName,animalNameEn:o.animalNameEn,infoAnimalId:o.infoAnimalId,infoAnimalName:speciesName,infoVarietyId:o.animalTypeId,infoVarietyName:o.animalType,infoVarietyNameEn:o.animalTypeEn,dictSexId:o.dictSexId,chipNO:o.chipNO,birthday:date(o.birthday)},
      sampleInfo:{infoSampleTypeId:o.infoSampleTypeId,sampleArea:o.sampleArea,collectDt:date(o.collectDt),sampleTypeName:''},deliveryInfo:{masterName:o.masterName,masterNameEn:o.masterNameEn,doctorName:o.doctorName},otherInfo:{remark:o.remark || ''},animalInfo:{age:o.age,animalWeight:o.animalWeight || ''},clinicalInfo:{medicalHistory:o.medicalHistory || '',clinicalSymptoms:o.clinicalSymptoms || '',diagnosis:o.diagnosis || '',medication:o.medication || ''},
      selectedTestItems:(o.items || []).map(x=>({infoTestTypeId:x.infoTestTypeId,testName:x.testTypeName,price:x.testPrice}))};
    wx.setStorageSync('order_form',form);wx.navigateTo({url:'/pages/order-add-pet/order-add-pet?draft=1&petId='+form.petId});
  },
  getStatusLabel(status) {
    const labels = {
      '-3': '待支付',
      '-2': '草稿',
      '0': '待审核',
      '1': '审核不通过',
      '2': '等待报告',
      '3': '已完成',
      '4': '已取消'
    };
    return labels[String(status)] || '未知';
  },

  /**
   * 获取性别文本
   */
  getGenderText(genderId) {
    const map = { 0: '未知', 1: '公', 2: '母', 3: '其他' };
    return map[genderId] || '未知';
  },

  /**
   * 格式化日期（iOS兼容格式）
   * 支持多种输入格式：时间戳、"yyyy-MM-dd"、"yyyy-MM-dd HH:mm:ss"等
   */
  formatDate(dateStr) {
    if (!dateStr) return '';

    let date;

    // 处理时间戳（数字）
    if (typeof dateStr === 'number') {
      date = new Date(dateStr);
    }
    // 处理时间戳字符串
    else if (typeof dateStr === 'string' && /^\d+$/.test(dateStr)) {
      date = new Date(parseInt(dateStr));
    }
    // 处理字符串日期
    else {
      // 替换空格为T，使其符合ISO格式 "yyyy-MM-ddTHH:mm:ss"
      const isoStr = dateStr.replace(' ', 'T');
      date = new Date(isoStr);
    }

    if (isNaN(date.getTime())) return '';

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  },

  /**
   * 格式化日期时间（iOS兼容格式）
   * 支持多种输入格式：时间戳、"yyyy-MM-dd"、"yyyy-MM-dd HH:mm:ss"等
   */
  formatDateTime(dateStr) {
    if (!dateStr) return '';

    let date;

    // 处理时间戳（数字）
    if (typeof dateStr === 'number') {
      date = new Date(dateStr);
    }
    // 处理时间戳字符串
    else if (typeof dateStr === 'string' && /^\d+$/.test(dateStr)) {
      date = new Date(parseInt(dateStr));
    }
    // 处理字符串日期
    else {
      // 替换空格为T，使其符合ISO格式 "yyyy-MM-ddTHH:mm:ss"
      const isoStr = dateStr.replace(' ', 'T');
      date = new Date(isoStr);
    }

    if (isNaN(date.getTime())) return '';

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}`;
  },

  /**
   * 重新预约
   */
  onRebook() {
    wx.switchTab({
      url: '/pages/order-select-pet/order-select-pet'
    });
  },

  /**
   * 预览PDF报告
   */
  onPreviewPDF(e) {
    const pdfUrl = e.currentTarget.dataset.url;
    if (!pdfUrl) {
      wx.showToast({
        title: 'PDF地址不存在',
        icon: 'none'
      });
      return;
    }

    console.log('[OrderDetail] 预览PDF:', pdfUrl);

    // 跳转到PDF预览页面
    wx.navigateTo({
      url: `/pages/pdf-view/pdf-view?pdf_url=${encodeURIComponent(pdfUrl)}`
    });
  },

  /**
   * 下载PDF报告
   */
  async onDownloadPDF(e) {
    wx.showLoading({title:'下载中...',mask:true});
    try{await require('../../utils/reports').openReportFromURL(e.currentTarget.dataset.url)}catch(error){wx.showToast({title:error.message || '下载失败，请重试',icon:'none'})}finally{wx.hideLoading()}
  }
});
