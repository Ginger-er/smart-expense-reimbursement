# CLAUDE.md

本文件为 Claude Code 在本项目中工作时提供指导。

## 项目用途

「智报」——企业差旅报销管理平台。覆盖「出差申请 → 发票上传识别 → 报销单提交 → 多级审批 → 财务打款」全流程,支持数据报表与员工/领导/财务/管理员四级角色权限隔离。

## 技术架构

- 后端:Spring Boot 3.2.5 + MyBatis-Plus 3.5.5 + Sa-Token 1.37(JWT + Redis 会话)+ MySQL 8 + Redis 7 + 百度 OCR
- 前端:Vue 3.4 + TypeScript + Element Plus + Vite + Pinia
- 报销审批为**手写状态机**(状态码 0草稿/1待审批/2审批中/3已通过/4已驳回/5已打款,≥5000 元走两级审批),未使用工作流引擎
- Redis 用途:Sa-Token 会话存储、分布式锁(审批/打款防并发)、幂等提交标记、统计缓存(5 分钟 TTL)

## 常用命令

```bash
# 一键启动(Windows):Docker 拉起 MySQL/Redis + 后端 + 前端
scripts\start.bat

# 仅启动基础设施
cd deploy && docker-compose up -d mysql redis

# 后端构建 / 运行(端口 8080,Swagger: http://localhost:8080/swagger-ui/index.html)
cd server && mvn clean package
cd server && mvn spring-boot:run

# 单元测试(纯 Mockito 单测,不需要 MySQL/Redis)
cd server && mvn test

# 前端(端口 3001)
cd web-admin && npm install && npm run dev
```

测试账号:admin / zhangsan / lisi / wangwu,密码均 123456(首次启动由 DataInitializer 自动创建)。

## 注意事项

- `server/src/main/resources/application-dev.yml` 含真实百度 OCR 密钥,**已被 .gitignore 忽略,严禁提交**
- 数据库由 `server/src/main/resources/db/init.sql` 建表;`DataInitializer` 仅在表为空时插入演示数据
- MyBatis-Plus 分页插件全局 maxLimit=100,查询全量数据(如导出)必须循环分页,不可用大 pageSize 一次取
- 修改 Service 构造函数依赖时,同步更新对应 `*Test.java` 的 @Mock 字段与透传 stub
- 锁/缓存组件(redis 包)保持纯组件,不调用 StpUtil,便于单测
