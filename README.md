# 🐰 AiCutePets - AI可爱宠物小程序后端
> 一款专为智能玩具设计的AI陪伴小程序后端服务，为"多尼兔"等智能玩具提供完整的后端支持。

---

## 📖 项目简介

AiCutePets 是一个基于 **Spring Boot** 构建的微信小程序后端服务，用于管理智能AI玩具设备。系统支持设备绑定、远程管理、OTA固件升级、成长日记等功能，致力于为儿童和家长提供安全、有趣的智能陪伴体验。

## ✨ 核心功能

| 功能模块 | 描述 |
|---------|------|
| 🔐 **微信登录** | 支持微信小程序一键登录，手机号授权绑定 |
| 📱 **设备管理** | 蓝牙配网、设备绑定/解绑、多设备切换 |
| 🏠 **首页聚合** | 设备状态、亲密度等级、成长数据、今日心情 |
| 📅 **记忆日历** | AI生成日记、情绪标签、月度日历视图 |
| 🏅 **徽章系统** | 成就徽章墙、徽章确认机制 |
| ⬆️ **OTA升级** | 固件版本检测、远程升级、升级日志 |
| 📝 **内容管理** | 玩伴指南、陪伴约定等富文本内容 |

## 🛠️ 技术栈

- **框架**: Spring Boot 2.7.18
- **持久层**: MyBatis 2.3.2
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **认证**: JWT (jjwt 0.11.5)
- **文档**: Knife4j (OpenAPI 3)
- **工具库**: Lombok

## 📁 项目结构

```
src/main/java/com/aiqutepets/
├── AiQutePetsApplication.java    # 启动类
├── common/                       # 通用组件 (Result响应封装)
├── config/                       # 配置类 (JWT、Redis、Swagger等)
├── controller/                   # 控制器层
│   ├── AuthController           # 认证接口
│   ├── DeviceController         # 设备管理接口
│   ├── HomeController           # 首页聚合接口
│   ├── MemoryController         # 记忆日历接口
│   ├── OtaController            # OTA升级接口
│   └── ContentController        # 内容管理接口
├── dto/                         # 数据传输对象
├── entity/                      # 实体类
│   ├── MpUser                   # 小程序用户
│   ├── DeviceInfo               # 设备信息
│   ├── UserDeviceRel            # 用户设备关系
│   ├── DeviceDiary              # 设备日记
│   ├── DeviceOtaLog             # OTA日志
│   └── AppRichContent           # 富文本内容
├── enums/                       # 枚举类
├── interceptor/                 # 拦截器 (JWT认证)
├── mapper/                      # MyBatis Mapper接口
├── service/                     # 服务层
├── util/                        # 工具类
└── vo/                          # 视图对象
```

## 🚀 快速开始

### 环境要求

- JDK 11+
- Maven 3.6+
- MySQL 8.0
- Redis

### 1. 克隆项目

```bash
git clone https://github.com/HuTu1016/AiCutePets.git
cd AiCutePets
```

### 2. 配置数据库

创建数据库并导入初始数据：

```bash
mysql -u root -p
CREATE DATABASE aiqutepets DEFAULT CHARACTER SET utf8mb4;
USE aiqutepets;
SOURCE aiqutepets.sql;
```

### 3. 修改配置

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/aiqutepets
    username: your_username
    password: your_password
  redis:
    host: localhost
    port: 6379

wx:
  miniapp:
    app-id: your_wx_app_id
    app-secret: your_wx_app_secret
```

### 4. 启动项目

```bash
mvn spring-boot:run
```

服务启动后访问：
- 服务地址: `http://localhost:8888`
- API文档: `http://localhost:8888/doc.html`

## 📚 API 接口概览

### 认证接口 `/api/auth`

| 方法 | 路径 | 描述 | 鉴权 |
|-----|------|------|-----|
| POST | `/login` | 微信登录 | ❌ |
| POST | `/phone` | 绑定手机号 | ✅ |

### 设备接口 `/api/device`

| 方法 | 路径 | 描述 | 鉴权 |
|-----|------|------|-----|
| GET | `/check` | 校验设备合法性 | ❌ |
| POST | `/bind` | 绑定设备 | ✅ |
| GET | `/detail` | 获取设备详情 | ✅ |
| PUT | `/update` | 更新设备信息 | ✅ |
| DELETE | `/unbind` | 解绑设备 | ✅ |
| GET | `/list` | 获取设备列表 | ✅ |
| POST | `/switch` | 切换当前设备 | ✅ |

### 首页接口 `/api/home`

| 方法 | 路径 | 描述 | 鉴权 |
|-----|------|------|-----|
| GET | `/index` | 获取首页聚合数据 | ✅ |

### 记忆接口 `/api/memory`

| 方法 | 路径 | 描述 | 鉴权 |
|-----|------|------|-----|
| GET | `/calendar` | 获取记忆日历 | ✅ |
| GET | `/diary` | 获取日记详情 | ✅ |
| GET | `/badges` | 获取徽章列表 | ✅ |
| POST | `/badge/ack` | 确认徽章展示 | ✅ |

### OTA接口 `/api/device/ota`

| 方法 | 路径 | 描述 | 鉴权 |
|-----|------|------|-----|
| GET | `/check` | 检查固件更新 | ✅ |
| POST | `/upgrade` | 执行固件升级 | ✅ (Owner Only) |

### 内容接口 `/api/content`

| 方法 | 路径 | 描述 | 鉴权 |
|-----|------|------|-----|
| GET | `/detail` | 获取富文本内容 | ❌ |
| POST | `/cache/clear` | 清除内容缓存 | ❌ |

## 📊 数据库设计

### 核心表结构

- **mp_user**: 小程序用户表
- **device_info**: 设备白名单表
- **user_device_rel**: 用户设备绑定关系
- **device_diary**: 设备日记历史表
- **device_ota_log**: OTA操作日志
- **app_rich_content**: 富文本内容表

## 🔧 配置说明

| 配置项 | 说明 | 默认值 |
|-------|------|-------|
| `server.port` | 服务端口 | 8888 |
| `jwt.expiration` | JWT过期时间 | 7天 |
| `thirdparty.timeout` | 第三方接口超时 | 10000ms |



