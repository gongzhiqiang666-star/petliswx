// pages/order-detail/order-detail.js
const api = require('../../api/index.js')

Page({
  data: {
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
    loading: true,
    error: false
  },

  onLoad(options) {
    const orderId = options.id;
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
  async loadOrderDetail(orderId) {
    this.setData({ loading: true, error: false });

    try {
      const result = await api.orders.getDetail(orderId);

      this._rawOrder=result;
      // 处理返回的数据
      const orderData = {
        orderId: result.orderId || result.orderId,
        reviewReason:result.reviewReason || '',canResubmit:!!result.canResubmit,reviewHistory:result.reviewHistory || [],
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
    const form={petId:'order-'+o.orderId,resubmitOrderId:o.orderId,laboratoryId:o.laboratoryId,laboratoryName:o.laboratoryName,userCouponId:o.userCouponId,
      petInfo:{animalName:o.animalName,animalNameEn:o.animalNameEn,infoAnimalId:o.infoAnimalId,infoAnimalName:speciesName,infoVarietyId:o.animalTypeId,infoVarietyName:o.animalType,infoVarietyNameEn:o.animalTypeEn,dictSexId:o.dictSexId,chipNO:o.chipNO,birthday:date(o.birthday)},
      sampleInfo:{infoSampleTypeId:o.infoSampleTypeId,sampleArea:o.sampleArea,collectDt:date(o.collectDt),sampleTypeName:''},deliveryInfo:{masterName:o.masterName,masterNameEn:o.masterNameEn,doctorName:o.doctorName},otherInfo:{remark:o.remark || ''},animalInfo:{age:o.age,animalWeight:o.animalWeight || ''},
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
