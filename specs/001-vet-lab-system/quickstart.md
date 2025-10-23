# 快速开始指南

## 项目概述

PETLISWX（宠物医院检验系统）是一个基于微信小程序的宠物检验管理平台，支持检验申请提交、进度跟踪、报告查看等完整业务流程。

## 技术栈

- **前端**: 微信小程序 + WeUI组件库
- **后端**: SpringBoot 1.5.22 + Java 1.8
- **数据库**: MySQL 5.7 + Redis 6.0
- **部署**: Docker + Nginx + 阿里云

## 开发环境搭建

### 1. 前置要求

- Java 1.8+
- Node.js 12+
- MySQL 5.7+
- Redis 6.0+
- 微信开发者工具

### 2. 克隆项目

```bash
git clone https://github.com/your-org/petliswx.git
cd petliswx
```

### 3. 后端开发环境

#### 配置数据库
```sql
-- 创建数据库
CREATE DATABASE petliswx CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户
CREATE USER 'petliswx'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON petliswx.* TO 'petliswx'@'localhost';
FLUSH PRIVILEGES;
```

#### 配置Redis
```bash
# 启动Redis
redis-server

# 测试连接
redis-cli ping
```

#### 修改配置文件
```yaml
# backend/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/petliswx?useUnicode=true&characterEncoding=utf8
    username: petliswx
    password: your_password
    driver-class-name: com.mysql.jdbc.Driver

  redis:
    host: localhost
    port: 6379
    database: 0

# 微信小程序配置
wechat:
  appid: your_wechat_appid
  secret: your_wechat_secret

# 支付配置
payment:
  wechat:
    appid: your_wechat_pay_appid
    mchid: your_mchid
    key: your_wechat_pay_key
  alipay:
    appid: your_alipay_appid
    private_key: your_private_key
    public_key: your_alipay_public_key
```

#### 启动后端服务
```bash
cd backend
./mvnw spring-boot:run
```

### 4. 前端开发环境

#### 安装依赖
```bash
cd miniprogram
npm install
```

#### 配置小程序信息
```json
// miniprogram/project.config.json
{
  "appid": "your_wechat_appid",
  "projectname": "宠物医院检验系统",
  "libVersion": "2.19.4"
}
```

#### 配置API地址
```javascript
// miniprogram/utils/config.js
const config = {
  apiBaseUrl: 'http://localhost:8080/api/v1',
  env: 'development'
}

module.exports = config
```

#### 启动小程序
1. 打开微信开发者工具
2. 导入项目文件夹 `miniprogram`
3. 填入AppID
4. 点击编译

## 核心功能使用

### 1. 用户登录

#### 手机号登录
```javascript
// 发送验证码
wx.request({
  url: `${config.apiBaseUrl}/auth/sms/send`,
  method: 'POST',
  data: { phone: '13812345678' },
  success: (res) => {
    console.log('验证码已发送');
  }
});

// 登录
wx.request({
  url: `${config.apiBaseUrl}/auth/login`,
  method: 'POST',
  data: {
    loginType: 'PHONE',
    phone: '13812345678',
    verifyCode: '123456'
  },
  success: (res) => {
    // 保存token
    wx.setStorageSync('token', res.data.data.token);
  }
});
```

#### 微信登录
```javascript
wx.login({
  success: (res) => {
    wx.request({
      url: `${config.apiBaseUrl}/auth/login`,
      method: 'POST',
      data: {
        loginType: 'WECHAT',
        wechatCode: res.code
      },
      success: (res) => {
        wx.setStorageSync('token', res.data.data.token);
      }
    });
  }
});
```

### 2. 检验申请

#### 获取检验项目
```javascript
wx.request({
  url: `${config.apiBaseUrl}/test-items`,
  method: 'GET',
  header: {
    'Authorization': `Bearer ${wx.getStorageSync('token')}`
  },
  success: (res) => {
    this.setData({
      testItems: res.data.data
    });
  }
});
```

#### 提交申请
```javascript
wx.request({
  url: `${config.apiBaseUrl}/applications`,
  method: 'POST',
  header: {
    'Authorization': `Bearer ${wx.getStorageSync('token')}`
  },
  data: {
    petId: 1,
    itemIds: [1, 2, 3],
    urgency: 'NORMAL',
    remarks: '例行检查'
  },
  success: (res) => {
    wx.showToast({
      title: '申请提交成功',
      icon: 'success'
    });
  }
});
```

### 3. 查看进度

#### 获取申请列表
```javascript
wx.request({
  url: `${config.apiBaseUrl}/applications`,
  method: 'GET',
  header: {
    'Authorization': `Bearer ${wx.getStorageSync('token')}`
  },
  data: {
    status: 'SUBMITTED',
    page: 1,
    size: 10
  },
  success: (res) => {
    this.setData({
      applications: res.data.data.list
    });
  }
});
```

#### 获取申请详情
```javascript
wx.request({
  url: `${config.apiBaseUrl}/applications/${applicationId}`,
  method: 'GET',
  header: {
    'Authorization': `Bearer ${wx.getStorageSync('token')}`
  },
  success: (res) => {
    this.setData({
      application: res.data.data
    });
  }
});
```

### 4. 查看报告

#### 获取报告
```javascript
wx.request({
  url: `${config.apiBaseUrl}/reports/${applicationId}`,
  method: 'GET',
  header: {
    'Authorization': `Bearer ${wx.getStorageSync('token')}`
  },
  success: (res) => {
    const report = res.data.data;
    if (report.fileType === 'PDF') {
      // 下载PDF
      wx.downloadFile({
        url: report.filePath,
        success: (downloadRes) => {
          wx.openDocument({
            filePath: downloadRes.tempFilePath,
            fileType: 'pdf'
          });
        }
      });
    } else {
      // 查看图片
      wx.previewImage({
        urls: [report.filePath]
      });
    }
  }
});
```

## 测试

### 单元测试
```bash
# 后端测试
cd backend
./mvnw test

# 前端测试
cd miniprogram
npm test
```

### API测试
```bash
# 使用Postman导入API文档
# 导入 contracts/api.yaml
```

## 部署

### Docker部署
```bash
# 构建后端镜像
cd backend
./mvnw package docker:build

# 启动服务
docker-compose up -d
```

### 生产环境部署
```bash
# 打包后端
./mvnw clean package -Pprod

# 上传到服务器
scp target/petliswx.jar user@server:/opt/petliswx/

# 启动服务
java -jar -Dspring.profiles.active=prod petliswx.jar
```

## 常见问题

### 1. 数据库连接失败
- 检查MySQL服务是否启动
- 验证数据库连接配置
- 确认用户权限

### 2. Redis连接失败
- 检查Redis服务状态
- 验证连接配置
- 确认防火墙设置

### 3. 微信登录失败
- 检查AppID和Secret配置
- 确认域名白名单设置
- 验证HTTPS证书

### 4. 支付接口调用失败
- 检查商户号和API密钥
- 确认支付参数格式
- 验证回调URL配置

## 开发规范

### 代码规范
- 后端遵循阿里巴巴Java开发手册
- 前端遵循微信小程序开发规范
- 使用ESLint和Prettier格式化代码

### 提交规范
```bash
# 提交前运行测试
./mvnw test

# 提交代码
git add .
git commit -m "feat: 添加检验申请功能"
git push origin feature/xxx
```

### 分支管理
- `main`: 生产环境分支
- `develop`: 开发环境分支
- `feature/*`: 功能开发分支
- `hotfix/*`: 热修复分支

## 联系方式

- 技术支持: tech@petliswx.com
- 项目地址: https://github.com/your-org/petliswx
- 文档地址: https://docs.petliswx.com