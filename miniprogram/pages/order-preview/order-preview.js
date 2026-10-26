// pages/order-preview/order-preview.js
const request = require('../../utils/request');
const api = require('../../api/index.js');
const app = getApp();
const logger = require('../../utils/logger.js');
const notificationConfig = require('../../config/notification.js');
const ageCalculator = require('../../utils/age-calculator.js');

Page({
  data: {
    formData: {},
    subscriptionTemplates:[],subscribe:false,
    totalPrice: 0, coupons: [], couponIndex:0, quote:null, quoteError:'', quoting:false,
    submitting: false
  },

  onLoad() {
    try {
      // 清除全局缓存和storage缓存，确保只使用最新数据
      app.globalData.cache.orderForm = null;
      app.globalData.cache.orderFormTimestamp = 0;

      // 从storage读取最新数据
      let formData = null;
      try {
        formData = wx.getStorageSync('order_form');
        logger.log('OrderPreview', '从storage读取订单表单', formData ? '成功' : '失败（无数据）');
      } catch (error) {
        logger.error('OrderPreview', '读取storage失败', error);
      }

      if (!formData || !formData.petInfo) {
        logger.error('OrderPreview', '订单表单数据无效', formData);
        wx.showToast({
          title: '数据错误，请重新填写',
          icon: 'none'
        });
        setTimeout(() => {
          wx.navigateBack();
        }, 1500);
        return;
      }

      // 记录数据来源，便于调试
      logger.log('OrderPreview', '订单表单数据', {
        petId: formData.petId,
        animalName: formData.petInfo?.animalName,
        sampleType: formData.sampleInfo?.sampleTypeName,
        selectedItemsCount: formData.selectedTestItems?.length
      });

      // 计算总价 - 使用price字段
      const testItems = formData.selectedTestItems || [];
      const totalPrice = testItems.reduce((sum, item) => {
        const price = parseFloat(item.price) || 0;
        return sum + price;
      }, 0);

      // 添加性别文本映射
      if (formData.petInfo && !formData.petInfo.genderText) {
        const genderMap = { 0: '未知', 1: '公', 2: '母', 3: '其他' };
        formData.petInfo.genderText = genderMap[formData.petInfo.dictSexId] || '未知';
      }

      // 添加采样类型名称
      if (formData.sampleInfo) {
        formData.sampleInfo.sampleTypeName = this.getSampleTypeName(formData.sampleInfo.infoSampleTypeId);
        // 格式化采样日期，只显示日期部分
        if (formData.sampleInfo.collectDt) {
          formData.sampleInfo.collectDt = this.formatDate(formData.sampleInfo.collectDt);
        }
      }

      // 处理animalInfo中的年龄和体重
      if (formData.animalInfo) {
        formData.animalInfo.ageDisplay = ageCalculator.formatAgeDisplay(formData.animalInfo.age) || '未填写';
        formData.animalInfo.weightDisplay = this.formatFieldValue(formData.animalInfo.animalWeight);
      } else {
        // 如果没有animalInfo，创建一个空对象
        formData.animalInfo = {
          ageDisplay: '未填写',
          weightDisplay: '未填写'
        };
      }

      this.setData({
        formData,
        totalPrice: totalPrice.toFixed(2)
      });
      this.loadCoupons();
      request.get('/inbox/templates').then(r=>this.setData({subscriptionTemplates:r.filter(x=>['order','report'].includes(x.eventType)).slice(0,3)})).catch(()=>{});
    } catch (error) {
      logger.error('OrderPreview', '加载数据失败', error);
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      });
    }
  },

  subscribeChange(e){this.setData({subscribe:e.detail.value.length>0})},
  async saveDraft(){if(this._savingDraft)return;this._savingDraft=true;try{this.setData({formData:await require('../../utils/drafts').save(this.data.formData)});wx.showToast({title:'草稿已保存'})}catch(e){wx.showToast({title:e.message,icon:'none'})}finally{this._savingDraft=false}},
  async loadCoupons(){
    try{const coupons=await request.get('/coupons',{category:'available'});this.setData({coupons:[{id:null,name:'不使用优惠券'},...coupons]});await this.requote(null)}catch(e){this.setData({quoteError:e.message})}
  },
  async couponChange(e){const index=Number(e.detail.value);this.setData({couponIndex:index});await this.requote(this.data.coupons[index].id)},
  async requote(userCouponId){
    const requestId=(this._quoteRequestId || 0)+1;this._quoteRequestId=requestId;
    this.setData({quoting:true,quote:null,quoteError:''});const form=this.data.formData;
    try{
      const quote=await request.post('/coupons/quote',{laboratoryId:form.laboratoryId,testTypeIds:form.selectedTestItems.map(x=>x.infoTestTypeId),userCouponId});
      if(requestId!==this._quoteRequestId)return;
      form.userCouponId=userCouponId;form.selectedTestItems=form.selectedTestItems.map(x=>({...x,price:quote.prices[x.infoTestTypeId]}));wx.setStorageSync('order_form',form);this.setData({quote,formData:form,totalPrice:Number(quote.payableAmount).toFixed(2)});
    }catch(e){if(requestId===this._quoteRequestId)this.setData({quoteError:e.message})}finally{if(requestId===this._quoteRequestId)this.setData({quoting:false})}
  },
  /**
   * 格式化字段值，空值显示"未填写"
   * @param {String} value - 字段值
   * @param {Boolean} isEmpty - 是否为空
   * @returns {Object} { value, isEmpty }
   */
  formatFieldValue(value) {
    if (!value || (typeof value === 'string' && !value.trim())) {
      return '未填写';
    }
    return value;
  },

  getSampleTypeName(typeId) {
    // 从 storage 中获取选择表单，提取样本类型名称
    const formData = wx.getStorageSync('order_form');
    if (formData && formData.sampleInfo && formData.sampleInfo.sampleTypeName) {
      return formData.sampleInfo.sampleTypeName;
    }
    return '未知';
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

  onBack() {
    wx.navigateBack();
  },

  async onSubmit() {
    const { formData, submitting } = this.data;

    if (submitting) {
      return;
    }

    // 验证是否选择了检测项目
    if (!formData.selectedTestItems || formData.selectedTestItems.length === 0) {
      wx.showToast({
        title: '请选择检测项目',
        icon: 'none'
      });
      return;
    }

    try {
      this.setData({ submitting: true });

      // 检查是否包含狂犬检测
      const hasRabies = formData.selectedTestItems.some(
        item => item.infoTestTypeId === 1225
      );

      if (hasRabies) {
        if (!formData.petInfo.chipNO) {
          wx.showToast({
            title: '狂犬检测必须填写芯片号',
            icon: 'none'
          });
          this.setData({ submitting: false });
          return;
        }
        if (!formData.petInfo.birthday) {
          wx.showToast({
            title: '狂犬检测必须填写出生日期',
            icon: 'none'
          });
          this.setData({ submitting: false });
          return;
        }
        if (!formData.otherInfo.remark || !formData.otherInfo.remark.trim()) {
          wx.showToast({
            title: '狂犬检测必须在备注中填写快递单号',
            icon: 'none'
          });
          this.setData({ submitting: false });
          return;
        }
      }

      // 步骤1: 检查账单状态（月结客户验证）
      logger.log('OrderPreview', '开始检查账单状态');
      const billingResult = await api.billing.checkStatus();
      const billingInfo = billingResult.data || billingResult;

      logger.log('OrderPreview', '账单检查结果', billingInfo);

      // 如果是月结客户且有未支付账单，阻止提交
      if (billingInfo.isMonthlyCustomer && billingInfo.hasUnpaidBills) {
        wx.showModal({
          title: '月结账户提示',
          content: billingInfo.message || `您有${billingInfo.unpaidCount}笔未支付账单，共计¥${billingInfo.unpaidAmount}，请先结清账单`,
          showCancel: false,
          confirmText: '我知道了'
        });
        this.setData({ submitting: false });
        return;
      }

      // 步骤2: 构造订单数据
      if(!this.data.quote || this.data.quoting)throw new Error('请先完成价格核对');
      const orderData = {
        draftId:formData._draftId,
        draftVersion:formData._draftVersion,
        laboratoryId:formData.laboratoryId,
        userCouponId:formData.userCouponId,
        ...formData.petInfo,
        ...formData.sampleInfo,
        ...formData.deliveryInfo,
        // 添加年龄和体重
        age: formData.animalInfo?.age || '',
        animalWeight: formData.animalInfo?.animalWeight || '',
        // 修复品种字段映射: infoVarietyId -> animalTypeId
        animalTypeId: formData.petInfo.infoVarietyId,
        animalType: formData.petInfo.infoVarietyName,
        animalTypeEn: formData.petInfo.infoVarietyNameEn,
        remark: (formData.otherInfo || {}).remark || '',
        // sampleStatus 由后端设置为 0（待审核）
        diagnosisDetails: formData.selectedTestItems.map(item => ({
          infoTestTypeId: item.infoTestTypeId
          // 价格从后端价格表获取，不再从前端传递
        }))
      };

      // 转换日期为时间戳（毫秒），以便后端Date类型正确反序列化
      if (orderData.birthday) {
        orderData.birthday = this.convertDateToTimestamp(orderData.birthday);
      }
      if (orderData.collectDt) {
        orderData.collectDt = this.convertDateToTimestamp(orderData.collectDt);
      }

      // 步骤3: 创建订单
      logger.log('OrderPreview', '开始创建订单');
      if(this.data.subscribe && this.data.subscriptionTemplates.length){try{await wx.requestSubscribeMessage({tmplIds:[...new Set(this.data.subscriptionTemplates.map(x=>x.templateId))]})}catch(e){/* A declined subscription never blocks submitting an order. */}}
      const orderResult = formData.resubmitOrderId ? (await request.post('/orders/'+formData.resubmitOrderId+'/resubmit',orderData),{orderId:formData.resubmitOrderId,needPayment:false}) : await api.orders.create(orderData);
      const orderResponse = orderResult.data || orderResult;
      const orderId = orderResponse.orderId;  // 提取订单ID
      logger.log('OrderPreview', '订单创建成功', { orderId, needPayment: orderResponse.needPayment });

      const reportNO = orderData.reportNO || '生成中...';

      // 提交后等待审核；支付入口仅在服务端确认审核通过后显示。
      // 步骤5: 显示成功提示
      wx.showModal({
        title: '送检申请已提交',
        content: `订单号：${orderId}\n审核通过后，现结客户可支付，月结客户按协议挂账。`,
        showCancel: false,
        confirmText: '查看订单',
        success: () => {
          // 清理本地存储
          wx.removeStorageSync('order_form');

          // 跳转到订单列表（订单页为非 Tab 页面，替换当前预览页）
          wx.redirectTo({
            url: '/pages/orders/orders'
          });
        }
      });
    } catch (error) {
      logger.error('OrderPreview', '提交订单失败', error);
      this.setData({ submitting: false });

      wx.showToast({
        title: error.message || '提交失败，请重试',
        icon: 'none',
        duration: 2000
      });
    }
  },

  /**
   * 调起微信支付
   * @param {Number} orderId 订单ID
   * @param {String} totalFee 支付金额（字符串格式，如 "150.00"）
   * @returns {Boolean} 支付是否成功
   */
  async invokeWeChatPayment(orderId, totalFee) {
    try {
      // 1. 创建支付订单，获取支付参数
      logger.log('OrderPreview', '创建支付订单', { orderId, totalFee });

      // 获取用户openid
      const app = getApp();
      const userInfo = app.globalData.userInfo;

      if (!userInfo || !userInfo.openid) {
        wx.showModal({
          title: '支付失败',
          content: '用户信息异常，请重新登录',
          showCancel: false
        });
        return false;
      }

      const paymentData = {
        orderId: orderId,
        amount: totalFee,
        openid: userInfo.openid
      };

      const paymentResult = await api.payment.create(paymentData);
      const paymentParams = paymentResult.data || paymentResult;

      logger.log('OrderPreview', '支付参数', paymentParams);

      // 2. 调起微信支付
      return new Promise((resolve) => {
        wx.requestPayment({
          timeStamp: paymentParams.timeStamp,
          nonceStr: paymentParams.nonceStr,
          package: paymentParams.package,
          signType: paymentParams.signType || 'MD5',
          paySign: paymentParams.paySign,
          success: (res) => {
            logger.log('OrderPreview', '微信支付成功', res);
            wx.showToast({
              title: '支付成功',
              icon: 'success',
              duration: 1500
            });
            resolve(true);
          },
          fail: (err) => {
            logger.error('OrderPreview', '微信支付失败', err);
            // 用户取消支付
            if (err.errMsg.includes('cancel')) {
              wx.showToast({
                title: '已取消支付',
                icon: 'none',
                duration: 1500
              });
            } else {
              wx.showModal({
                title: '支付失败',
                content: err.errMsg || '支付失败，请重试',
                showCancel: false
              });
            }
            resolve(false);
          }
        });
      });
    } catch (error) {
      logger.error('OrderPreview', '创建支付订单失败', error);
      wx.showModal({
        title: '支付失败',
        content: error.message || '创建支付订单失败，请重试',
        showCancel: false
      });
      return false;
    }
  },

  /**
   * 请求订阅消息授权
   * @returns {Promise<Boolean>} 用户是否同意订阅
   */
  requestMessageSubscription() {
    return new Promise((resolve) => {
      wx.requestSubscribeMessage({
        tmplIds: [notificationConfig.TEMPLATE_ID],
        success: (res) => {
          logger.log('OrderPreview', '订阅消息授权结果', res);

          // 检查授权结果
          const templateId = notificationConfig.TEMPLATE_ID;
          if (res[templateId] === 'accept') {
            // 用户同意订阅
            wx.showToast({
              title: '已开启消息通知',
              icon: 'success',
              duration: 1500
            });
            resolve(true);
          } else if (res[templateId] === 'reject') {
            // 用户拒绝订阅
            wx.showToast({
              title: '已拒绝消息通知',
              icon: 'none',
              duration: 1500
            });
            resolve(false);
          } else if (res[templateId] === 'ban') {
            // 用户已永久拒绝，需要引导用户到设置页面开启
            wx.showModal({
              title: '消息通知被关闭',
              content: '您已关闭订阅消息通知，如需接收订单状态通知，请在设置中开启',
              confirmText: '去设置',
              success: (modalRes) => {
                if (modalRes.confirm) {
                  wx.openSetting({
                    success: (settingRes) => {
                      logger.log('OrderPreview', '设置页面返回', settingRes);
                    }
                  });
                }
              }
            });
            resolve(false);
          } else {
            // 其他情况
            resolve(false);
          }
        },
        fail: (err) => {
          logger.error('OrderPreview', '订阅消息授权失败', err);

          // 用户取消授权或其他错误
          if (err.errCode === 20004) {
            // 用户关闭了订阅消息主开关
            wx.showToast({
              title: '订阅消息功能已关闭',
              icon: 'none',
              duration: 2000
            });
          } else {
            wx.showToast({
              title: '授权失败',
              icon: 'none',
              duration: 1500
            });
          }
          resolve(false);
        }
      });
    });
  },

  /**
   * 将日期字符串转换为Unix时间戳（毫秒）（iOS兼容格式）
   * @param {String} dateStr - 日期字符串，支持多种格式
   * @returns {Number} Unix时间戳（毫秒）
   */
  convertDateToTimestamp(dateStr) {
    if (!dateStr) return null;

    // 如果已经是数字（时间戳），直接返回
    if (typeof dateStr === 'number') {
      return dateStr;
    }

    // 如果已经是 ISO 格式（包含T），直接转换为时间戳
    if (dateStr.includes('T')) {
      return new Date(dateStr).getTime();
    }

    // 处理时间戳字符串
    if (/^\d+$/.test(dateStr)) {
      return parseInt(dateStr);
    }

    // 将日期字符串转换为ISO兼容格式
    // 替换空格为T，使其符合 "yyyy-MM-ddTHH:mm:ss"
    const isoStr = dateStr.replace(' ', 'T');
    const date = new Date(isoStr);
    return date.getTime();
  }
});
