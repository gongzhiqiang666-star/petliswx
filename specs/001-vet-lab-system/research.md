# Research Phase: 宠物医院检验系统

## Research Findings

### 微信小程序开发最佳实践

**Decision**: 使用原生微信小程序开发框架，结合WeUI组件库
**Rationale**:
- 原生开发性能最佳，符合微信生态规范
- WeUI提供统一的设计语言，符合医疗行业专业形象
- 避免跨平台框架的复杂性和性能损耗
**Alternatives considered**:
- Taro/uni-app (跨平台但性能损耗大)
- H5嵌入 (体验不如原生)

### SpringBoot 1.8 技术栈选择

**Decision**: SpringBoot 1.5.x + Spring Security + MyBatis
**Rationale**:
- SpringBoot 1.5.x是Java 1.8兼容的最后稳定版本
- Spring Security提供完善的认证授权机制
- MyBatis比JPA更灵活，适合复杂查询场景
**Alternatives considered**:
- SpringBoot 2.x (需要Java 11+)
- JPA/Hibernate (查询复杂度高，不适合医疗数据)

### 数据库架构设计

**Decision**: MySQL 5.7+ 主库 + Redis缓存
**Rationale**:
- MySQL 5.7+支持JSON字段，适合存储检验报告数据
- Redis缓存热点数据，提升系统响应速度
- 主从复制架构保证数据安全性
**Alternatives considered**:
- PostgreSQL (JSON支持更好但运维复杂度高)
- MongoDB (NoSQL不适合关系型医疗数据)

### 支付集成方案

**Decision**: 微信支付 + 支付宝SDK集成
**Rationale**:
- 用户已经在微信生态内，微信支付转化率最高
- 支付宝作为补充，覆盖更多用户群体
- 两种支付方式都提供完善的退款和对账功能
**Alternatives considered**:
- 只用微信支付 (用户覆盖不全)
- 银联支付 (集成复杂度高，用户体验差)

### 微信通知方案

**Decision**: 微信模板消息 + 服务号通知
**Rationale**:
- 模板消息适合状态更新通知
- 服务号通知支持更丰富的消息格式
- 两种方式结合确保重要消息不遗漏
**Alternatives considered**:
- 短信通知 (成本高，体验不如微信通知)
- App推送 (需要用户安装独立App)

### 文件存储方案

**Decision**: 阿里云OSS + CDN加速
**Rationale**:
- OSS存储成本低，扩展性强
- CDN加速提升文件下载速度
- 支持PDF和图片格式的直接访问
**Alternatives considered**:
- 本地存储 (扩展性差，备份困难)
- 腾讯云COS (与微信生态集成度更高)

### 安全性考虑

**Decision**: HTTPS + JWT + 数据加密
**Rationale**:
- HTTPS确保传输安全
- JWT token管理用户会话
- 敏感数据加密存储
**Alternatives considered**:
- Session管理 (扩展性差)
- 明文存储 (不安全)

### 性能优化策略

**Decision**: 缓存 + 分页 + 异步处理
**Rationale**:
- Redis缓存减少数据库压力
- 分页查询避免大数据量问题
- 异步处理提升用户体验
**Alternatives considered**:
- 全量查询 (性能差)
- 同步处理 (用户体验差)

## 技术架构决策

### 前端架构
- 微信小程序原生开发
- WeUI组件库 + 自定义组件
- Promise/async-await处理异步请求

### 后端架构
- SpringBoot 1.5.22 + Java 1.8
- Spring Security + JWT认证
- MyBatis + MySQL 5.7
- Redis缓存 + 阿里云OSS

### 部署架构
- 阿里云ECS + RDS
- Nginx反向代理
- Docker容器化部署

## 第三方服务集成

1. **微信服务**
   - 微信登录授权
   - 微信支付
   - 模板消息通知

2. **支付服务**
   - 微信支付API
   - 支付宝支付API

3. **云服务**
   - 阿里云OSS文件存储
   - 阿里云CDN加速
   - 阿里云短信服务(备用)

4. **物流服务**
   - 快递100 API (物流查询)
   - 顺丰/京东物流API (直接对接)