# API契约: 用户模块 (User API)

**Feature**: 004-bugfixes-and-enhancements
**Version**: 1.0.0
**Date**: 2025-12-28

---

## 1. 用户医院信息

### 1.1 获取用户所属医院信息

**接口**: `GET /api/user/hospital`

**功能说明**:
- 获取当前登录用户所属的医院信息（FR-015, FR-016）
- 从JWT token中获取用户ID，查询hospital_id
- 关联查询医院详细信息

#### 请求头

```
Authorization: Bearer <jwt_token>
```

#### 请求参数

无

#### 响应数据

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "infoHospitalId": 789,
    "hospitalName": "北京宠物医院",
    "hospitalNameEn": "Beijing Pet Hospital",
    "adress": "北京市朝阳区xxx路xxx号",
    "tel": "010-12345678",
    "fax": "010-12345679",
    "cityName": "北京",
    "payType": 0,
    "payTypeLabel": "月结客户",
    "amount": 5000.00
  }
}
```

#### 字段说明

- `infoHospitalId`: 医院ID
- `hospitalName`: 医院名称（中文）✅ **必显示**
- `hospitalNameEn`: 医院名称（英文）
- `adress`: 医院地址 ✅ **必显示**
- `tel`: 联系电话 ✅ **必显示**
- `fax`: 传真号码
- `cityName`: 城市名称
- `payType`: 支付类型（0=月结, 1=次结）
- `payTypeLabel`: 支付类型标签
- `amount`: 账户余额

#### 错误响应

```json
{
  "code": 404,
  "message": "用户未归属医院",
  "data": null
}
```

---

## 2. 前端调用示例

### 2.1 我的页面显示医院信息（FR-015, FR-016）

```javascript
// pages/mine/mine.js

Page({
  data: {
    userInfo: null,
    hospitalInfo: null,
    loading: true
  },

  onLoad() {
    this.loadUserInfo()
  },

  async loadUserInfo() {
    this.setData({ loading: true })

    try {
      // 并行加载用户信息和医院信息
      const [userInfo, hospitalInfo] = await Promise.all([
        api.user.getInfo(),
        api.user.getHospital()
      ])

      this.setData({
        userInfo,
        hospitalInfo
      })
    } catch (err) {
      console.error('[Mine] 加载失败:', err)

      if (err.code === 404) {
        // 用户未归属医院（FR-017）
        wx.showModal({
          title: '提示',
          content: '您还未归属医院，请联系管理员',
          showCancel: false
        })
      } else {
        wx.showToast({ title: '加载失败', icon: 'none' })
      }
    } finally {
      this.setData({ loading: false })
    }
  },

  onRetry() {
    this.loadUserInfo()
  }
})
```

**WXML显示**:
```xml
<view class="mine-container">
  <!-- 用户信息 -->
  <view class="user-card">
    <image class="avatar" src="{{userInfo.avatar}}" />
    <text class="nickname">{{userInfo.nickname}}</text>
  </view>

  <!-- 医院信息 (FR-015) -->
  <view class="hospital-card" wx:if="{{hospitalInfo}}">
    <view class="card-title">我的医院</view>
    <view class="hospital-name">{{hospitalInfo.hospitalName}}</view>
    <view class="hospital-info">
      <text>📍 {{hospitalInfo.adress}}</text>
    </view>
    <view class="hospital-info">
      <text>📞 {{hospitalInfo.tel}}</text>
    </view>
    <view class="hospital-info">
      <text>💳 {{hospitalInfo.payTypeLabel}}</text>
    </view>
  </view>

  <!-- 加载中 -->
  <view wx:if="{{loading}}">
    <text>加载中...</text>
  </view>

  <!-- 加载失败 -->
  <view wx:if="{{!loading && !hospitalInfo}}">
    <button bindtap="onRetry">重试</button>
  </view>
</view>
```

---

## 3. 数据验证

### 3.1 必填字段（FR-015）

- ✅ `hospitalName`: 医院名称（必须显示）
- ✅ `adress`: 医院地址（必须显示）
- ✅ `tel`: 联系电话（必须显示）

### 3.2 性能要求（SC-006）

- API响应时间 < 1秒
- 数据缓存策略: 5分钟

---

**文档版本**: 1.0.0
**最后更新**: 2025-12-28
