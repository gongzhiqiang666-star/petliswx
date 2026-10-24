// pages/mine/mine.js
const api = require('../../api/index.js');
const app = getApp();

Page({
  data: {
    userInfo: {
      id: '',
      nickname: '',
      mobile: '',
      avatarUrl: ''
    },
    hospitalInfo: {
      infoHospitalId: '',
      hospitalName: '',
      hospitalNameEn: '',
      place: '',
      tel: ''
    },
    unreadCount: 0,
    fadeInUp: null,
    fadeInUpDelay: null,
    fadeInUpDelay2: null,
    fadeInUpDelay3: null,
    fadeInUpDelay4: null
  },

  onLoad() {
    // 创建动画效果
    this.createAnimations();

    this.loadUserInfo();
  },

  onShow() {
    // 每次显示页面时刷新未读通知数量
    this.loadUnreadCount();

    // 重新播放动画
    this.playAnimations();
  },

  /**
   * 创建动画效果
   */
  createAnimations() {
    const fadeInUp = wx.createAnimation({
      duration: 600,
      timingFunction: 'cubic-bezier(0.4, 0, 0.2, 1)',
      delay: 0
    });

    const fadeInUpDelay = wx.createAnimation({
      duration: 600,
      timingFunction: 'cubic-bezier(0.4, 0, 0.2, 1)',
      delay: 100
    });

    const fadeInUpDelay2 = wx.createAnimation({
      duration: 600,
      timingFunction: 'cubic-bezier(0.4, 0, 0.2, 1)',
      delay: 200
    });

    const fadeInUpDelay3 = wx.createAnimation({
      duration: 600,
      timingFunction: 'cubic-bezier(0.4, 0, 0.2, 1)',
      delay: 300
    });

    const fadeInUpDelay4 = wx.createAnimation({
      duration: 600,
      timingFunction: 'cubic-bezier(0.4, 0, 0.2, 1)',
      delay: 400
    });

    this.setData({
      fadeInUp,
      fadeInUpDelay,
      fadeInUpDelay2,
      fadeInUpDelay3,
      fadeInUpDelay4
    });
  },

  /**
   * 播放动画
   */
  playAnimations() {
    const { fadeInUp, fadeInUpDelay, fadeInUpDelay2, fadeInUpDelay3, fadeInUpDelay4 } = this.data;

    if (fadeInUp) {
      fadeInUp.opacity(1).translateY(0).step();
      fadeInUpDelay.opacity(1).translateY(0).step();
      fadeInUpDelay2.opacity(1).translateY(0).step();
      fadeInUpDelay3.opacity(1).translateY(0).step();
      fadeInUpDelay4.opacity(1).translateY(0).step();

      this.setData({
        fadeInUp,
        fadeInUpDelay,
        fadeInUpDelay2,
        fadeInUpDelay3,
        fadeInUpDelay4
      });
    }
  },

  /**
   * 加载用户信息
   */
  async loadUserInfo() {
    try {
      wx.showNavigationBarLoading();

      const userInfo = await api.user.getProfile();

      this.setData({ userInfo });

      // 加载用户的医院信息
      if (userInfo.hospitalId) {
        this.loadHospitalInfo(userInfo.hospitalId);
      }
    } catch (error) {
      console.error('[Mine] 加载用户信息失败:', error);
      wx.showToast({
        title: '加载失败',
        icon: 'none',
        duration: 2000
      });
    } finally {
      wx.hideNavigationBarLoading();
    }
  },

  /**
   * 加载医院信息
   * @param {Number} hospitalId - 医院ID
   */
  async loadHospitalInfo(hospitalId) {
    try {
      // ✅ 按需获取：只查询当前用户所属的医院信息
      const hospital = await api.dictionary.getHospitalById(hospitalId);

      if (hospital) {
        this.setData({ hospitalInfo: hospital });
      } else {
        console.warn('[Mine] 未找到医院信息, hospitalId:', hospitalId);
        this.setData({
          hospitalInfo: {
            hospitalName: '未知医院',
            place: '',
            tel: ''
          }
        });
      }
    } catch (error) {
      console.error('[Mine] 加载医院信息失败:', error);
      // 不显示toast，避免干扰用户体验
      this.setData({
        hospitalInfo: {
          hospitalName: '未知医院',
          place: '',
          tel: ''
        }
      });
    }
  },

  /**
   * 加载未读通知数量
   */
  async loadUnreadCount() {
    try {
      const result = await api.notifications.getUnreadCount();
      const count = result.data || result || 0;

      this.setData({ unreadCount: count });
    } catch (error) {
      console.error('[Mine] 加载未读通知数量失败:', error);
    }
  },

  /**
   * 快捷操作处理
   */
  onQuickAction(e) {
    const { action } = e.currentTarget.dataset;

    // 添加点击反馈
    wx.vibrateShort({ type: 'light' });

    switch (action) {
      case 'notifications':
        wx.navigateTo({
          url: '/pages/notifications/notifications'
        });
        break;

      default:
        console.warn('[Mine] 未知的快捷操作:', action);
    }
  },

  /**
   * 医院卡片点击
   */
  onHospitalTap() {
    const { hospitalInfo } = this.data;

    if (!hospitalInfo.place) {
      wx.showToast({
        title: '医院地址信息不完整',
        icon: 'none'
      });
      return;
    }

    // 显示医院信息详情
    wx.showModal({
      title: hospitalInfo.hospitalName,
      content: `地址：${hospitalInfo.place}\n电话：${hospitalInfo.tel || '暂无'}`,
      showCancel: true,
      cancelText: '关闭',
      confirmText: '复制地址',
      success: (res) => {
        if (res.confirm && hospitalInfo.place) {
          wx.setClipboardData({
            data: hospitalInfo.place,
            success: () => {
              wx.showToast({
                title: '地址已复制',
                icon: 'success'
              });
            }
          });
        }
      }
    });
  },

  /**
   * 拨打医院电话
   */
  onCallHospital(e) {
    const { tel } = e.currentTarget.dataset;

    if (!tel) {
      wx.showToast({
        title: '暂无联系电话',
        icon: 'none'
      });
      return;
    }

    wx.showModal({
      title: '拨打电话',
      content: `确定要拨打 ${tel} 吗？`,
      confirmText: '拨打',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          wx.makePhoneCall({
            phoneNumber: tel,
            fail: (err) => {
              console.error('[Mine] 拨打电话失败:', err);
              wx.showToast({
                title: '拨打失败',
                icon: 'none'
              });
            }
          });
        }
      }
    });
  },

  /**
   * 设置项点击
   */
  onSettingTap(e) {
    const { setting } = e.currentTarget.dataset;

    // 添加点击反馈
    wx.vibrateShort({ type: 'light' });

    switch (setting) {
      case 'profile':
        wx.showToast({
          title: '个人资料功能开发中',
          icon: 'none'
        });
        break;

      case 'help':
        wx.showToast({
          title: '帮助与反馈功能开发中',
          icon: 'none'
        });
        break;

      case 'about':
        wx.showModal({
          title: '关于我们',
          content: '宠物检测管理系统\n版本：v1.0.0\n\n专业的宠物医学检验管理平台，为宠物医院提供高效的检验服务。',
          showCancel: false,
          confirmText: '我知道了'
        });
        break;

      default:
        console.warn('[Mine] 未知的设置项:', setting);
    }
  },

  /**
   * 跳转到通知页面
   */
  onNotificationTap() {
    wx.navigateTo({
      url: '/pages/notifications/notifications'
    });
  },

  /**
   * 退出登录
   */
  onLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确定要退出当前账号吗？',
      confirmText: '退出',
      confirmColor: '#ef4444',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          // 添加震动反馈
          wx.vibrateShort({ type: 'medium' });

          // 清理本地存储
          wx.removeStorageSync('user_info');
          wx.removeStorageSync('hospital_info');
          wx.removeStorageSync('token');

          // 清理全局数据
          app.globalData.userInfo = null;
          app.globalData.token = null;
          app.globalData.hospitalInfo = null;

          wx.showToast({
            title: '已退出登录',
            icon: 'success',
            duration: 1500
          });

          // 跳转到登录页面
          setTimeout(() => {
            wx.reLaunch({
              url: '/pages/login/login'
            });
          }, 1500);
        }
      }
    });
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.loadUserInfo().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  /**
   * 页面分享
   */
  onShareAppMessage() {
    return {
      title: '宠物检测管理系统',
      path: '/pages/home/home',
      imageUrl: ''
    };
  }
});
