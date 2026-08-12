# 天空外卖 (SkyTakeOut)

基于 Spring Boot + Vue 3 + UniApp 的全栈外卖系统，集成 DeepSeek 大模型智能美食助手。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.5 |
| ORM | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.0 |
| 管理后台 | Vue 3 + Element Plus + Vite | — |
| 小程序/移动端 | UniApp (Vue 3) | 3.0 |
| AI | DeepSeek Chat API | deepseek-chat |

## 环境要求

请先安装以下软件：

| 工具 | 最低版本 | 验证命令 |
|------|----------|----------|
| JDK | 17 | `java -version` |
| MySQL | 8.0 | `mysql -u root -p` |
| Node.js | 18+ | `node -v` |

> Maven 不需要单独安装，项目已内置 Maven Wrapper（`mvnw`），首次运行会自动下载。

## 项目结构

```
PBL/
├── backend/                  # 后端 (Spring Boot + Maven)
│   ├── common/               #   公共模块 (工具类、异常)
│   ├── pojo/                 #   数据模块 (Entity、DTO、VO)
│   ├── server/               #   主服务 (Controller、Service、Mapper)
│   └── pom.xml               #   父 POM
├── admin/                    # 管理后台 (Vue 3 + Element Plus)
│   └── src/views/            #   页面: 仪表盘/用户/菜品/订单管理
├── mobile/                   # 移动端 (UniApp)
│   └── src/pages/            #   页面: 点餐/购物车/订单/AI聊天/个人中心
└── README.md
```

## 1. 数据库准备

### 1.1 启动 MySQL 服务

确保 MySQL 服务正在运行（端口默认 3306）。

### 1.2 创建数据库

登录 MySQL 执行：

```sql
CREATE DATABASE takeaway DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

### 1.3 检查数据库连接配置

打开 `backend/server/src/main/resources/application.yml`，确认数据库连接信息与你的环境一致：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/takeaway?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root        # ← 改成你的 MySQL 用户名
    password: 123456      # ← 改成你的 MySQL 密码
```

### 1.4 导入建表 SQL

```bash
mysql -u root -p takeaway < backend/init.sql
```

> 或者用 Navicat/SQLyog 等工具打开 `backend/init.sql` 执行。

### 1.5 修改文件上传路径

打开 `backend/server/src/main/resources/application.yml`，将 `upload.path` 改成你电脑上的一个目录：

```yaml
upload:
  path: D:/your-path/uploads   # ← 改成你电脑上存在的目录
```

### 1.6 修改 DeepSeek API Key（如需使用 AI 助手）

```yaml
deepseek:
  api-key: 你的API密钥   # ← 在 https://platform.deepseek.com 免费获取
```

## 2. 启动后端

```bash
cd backend
mvnw spring-boot:run -pl server
```

或者用 IDEA 打开 `backend/` 目录，右键运行 `SkyTakeOutApplication.main()`。

启动成功后，后端运行在 **http://localhost:8080**，所有 API 前缀为 `/api`。

验证：访问 http://localhost:8080/api/hello，看到返回 JSON 即成功。

## 3. 启动管理后台

```bash
cd admin
npm install
npm run dev
```

管理后台运行在 **http://localhost:3001**，会自动将 `/api` 请求代理到后端 8080。

## 4. 启动移动端

```bash
cd mobile
npm install
npm run dev:h5
```

移动端运行在 **http://localhost:3000**，同样会自动代理 `/api` 到后端。

## 5. 端口一览

| 服务 | 地址 | 说明 |
|------|------|------|
| 后端 API | http://localhost:8080 | `/api` 前缀 |
| 管理后台 | http://localhost:3001 | 管理员界面 |
| 移动端/H5 | http://localhost:3000 | 用户端 |

## 6. 快速体验流程

1. 启动 MySQL → 创建 `takeaway` 库 → 执行 `init.sql` → 修改 `application.yml` 里的密码和路径
2. 启动后端 → 表已通过 SQL 建好
3. 启动移动端 → 打开 http://localhost:3000 → 注册账号 → 浏览菜品 → 下单
4. 启动管理后台 → 打开 http://localhost:3001 → 管理菜品/用户/订单
5. 在移动端"美食问问"页面体验 DeepSeek 智能推荐

## 7. 常见问题

**Q: 启动报 "Unknown database 'takeaway'"?**
→ 没创建数据库，先执行第 1 步的 `CREATE DATABASE`

**Q: 启动报 "Access denied for user"?**
→ `application.yml` 里的用户名或密码不对，改成你的 MySQL 账号

**Q: 页面请求全是 404?**
→ 后端没启动，或端口被占用。确认 8080 端口上有 Java 进程

**Q: npm install 很慢?**
→ 设置国内镜像：`npm config set registry https://registry.npmmirror.com`

**Q: Java 版本不对?**
→ 检查 `java -version` 输出，必须是 17 或更高。如果装了多版本，设置 `JAVA_HOME` 指向 JDK 17
