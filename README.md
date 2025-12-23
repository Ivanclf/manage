# 高校学生活动全流程管理系统

## 项目简介

本系统是高校学生活动管理的全流程解决方案，提供活动创建、报名管理、签到确认等核心功能，帮助学校高效组织和管理各类学生活动。

## 核心功能

### 活动管理
- 活动全生命周期管理（创建/查询/更新/删除）
- 活动状态自动转换（未发布→报名中→进行中→已结束）
- 地理位置信息管理（经纬度、位置描述）
- 活动二维码生成与分享

### 报名系统
- 用户在线报名与信息管理
- 报名名额实时控制与防重复报名
- 高并发报名处理（Redis + Lua脚本）
- 异步数据落库（RabbitMQ）

### 签到管理
- 二维码签到机制
- 地理位置验证签到
- 签到状态实时更新
- 签到数据异步处理

### 海报管理
- 海报模板管理
- 二维码与模板合成海报
- 海报存储与分享

### 权限控制
- 管理员登录认证
- Token身份验证
- 账号密码管理

## 技术架构

- 后端框架: Spring Boot 3.5.6 + MyBatis 3.0.5
- 数据库: MySQL (mysql-connector-j 8.x)
- 缓存: Redis (Spring Data Redis)
- 消息队列: RabbitMQ (Spring AMQP)
- 对象存储: 阿里云OSS (aliyun-sdk-oss 3.17.4)
- 分页插件: PageHelper 1.4.7
- API文档: Knife4j 3.0.2 (Swagger增强)
- 二维码生成: Google ZXing 3.3.0
- 工具库: Hutool 5.8.16
- 其他技术:
  - Lombok
  - WebSocket
  - JAXB (用于XML处理)
  - commons-pool2 (Redis连接池)
  - Actuator (应用监控)

## 部署说明

1. 确保已安装Java 21环境
2. 配置MySQL、Redis、RabbitMQ环境并修改application.yml配置
3. 执行sql目录下的数据库脚本创建表结构
4. 使用Maven构建项目：`mvn clean package`
5. 运行jar包：`java -jar manage-1.0.0.jar`

## 注意事项

- 项目使用Java 21版本开发，请确保运行环境兼容
- 需要配置阿里云OSS服务用于文件存储
- 需要正确配置Redis和RabbitMQ服务地址
- 系统使用JWT进行身份验证，请妥善保管密钥信息

## 项目地址

- 管理员端：[admin.manage.ivanclf.com](https://admin.manage.ivanclf.com)
- 用户端：[user.manage.ivanclf.com](https://user.manage.ivanclf.com)

## 前端源码

[https://github.com/564562333/xd-](https://github.com/564562333/xd-)
