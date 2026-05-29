# 第一步：创建 SpringBoot 后端项目结构

## 一、项目概述

我们将创建一个 AI 聊天平台的后端项目，使用 **SpringBoot 3.2 + Maven + Java 17**。

## 二、项目目录结构

```
ai-chat/                          # 项目根目录（当前工作区）
├── pom.xml                       # Maven 项目配置文件
├── src/
│   ├── main/
│   │   ├── java/com/al/aichat/   # Java 源代码根目录
│   │   │   ├── AiChatApplication.java    # SpringBoot 启动类
│   │   │   ├── controller/       # 控制器层 - 接收 HTTP 请求
│   │   │   ├── service/          # 服务层 - 业务逻辑
│   │   │   ├── mapper/           # 数据访问层 - 操作数据库
│   │   │   ├── entity/           # 实体类 - 对应数据库表
│   │   │   ├── config/           # 配置类 - 项目配置
│   │   │   ├── dto/              # 数据传输对象 - 前后端交互
│   │   │   ├── common/           # 公共工具类
│   │   │   └── utils/            # 工具类
│   │   └── resources/
│   │       ├── application.yml   # 主配置文件
│   │       └── application-dev.yml # 开发环境配置
│   └── test/java/com/al/aichat/  # 测试代码
```

## 三、各目录作用说明

### 1. controller（控制器层）
- **作用**：接收前端发来的 HTTP 请求，调用 service 层处理业务，返回结果给前端
- **类比**：餐厅的服务员，负责接收顾客（前端）的点单，传给厨房（service），再把菜端给顾客
- **典型类**：`UserController.java`、`ChatController.java`

### 2. service（服务层）
- **作用**：编写业务逻辑代码，调用 mapper 层操作数据库
- **类比**：餐厅的厨房，负责实际处理业务
- **典型类**：`UserService.java`、`ChatService.java`

### 3. mapper（数据访问层）
- **作用**：操作数据库，执行 SQL 语句（MyBatis Plus 帮我们简化了很多 SQL 编写）
- **类比**：仓库管理员，负责存取数据
- **典型类**：`UserMapper.java`、`ChatMessageMapper.java`

### 4. entity（实体类）
- **作用**：定义数据模型，每个实体类对应数据库中的一张表
- **类比**：仓库的货架模板，规定了每种货物怎么摆放
- **典型类**：`User.java`、`ChatMessage.java`

### 5. config（配置类）
- **作用**：配置第三方工具（如 JWT、跨域等）
- **典型类**：`JwtConfig.java`、`WebMvcConfig.java`

### 6. dto（数据传输对象）
- **作用**：定义前后端交互的数据格式，不同于 entity（entity 对应数据库，dto 对应接口）
- **典型类**：`LoginRequest.java`、`LoginResponse.java`

### 7. common（公共模块）
- **作用**：存放通用返回结果、异常处理等
- **典型类**：`Result.java`（统一响应格式）、`GlobalExceptionHandler.java`

### 8. utils（工具类）
- **作用**：存放通用工具方法
- **典型类**：`JwtUtils.java`

## 四、需要创建的文件清单

### 文件 1：pom.xml
Maven 项目配置文件，声明项目依赖（SpringBoot、MyBatis Plus、JWT、MySQL 驱动等）。

### 文件 2：application.yml
SpringBoot 主配置文件，配置：
- 服务器端口（8080）
- 数据库连接信息
- MyBatis Plus 配置
- JWT 密钥配置

### 文件 3：application-dev.yml
开发环境配置（可选），用于区分开发/生产环境。

### 文件 4：AiChatApplication.java
SpringBoot 启动类，项目的入口。

## 五、pom.xml 关键依赖说明

| 依赖 | 作用 |
|------|------|
| spring-boot-starter-web | 提供 Web 开发能力，内嵌 Tomcat |
| mysql-connector-j | MySQL 数据库驱动 |
| mybatis-plus-spring-boot3-starter | MyBatis Plus，简化数据库操作 |
| jjwt-api / jjwt-impl / jjwt-jackson | JWT 令牌生成和验证 |
| lombok | 自动生成 getter/setter/构造方法 |
| spring-boot-starter-test | 单元测试 |

## 六、如何启动项目

### 方式一：IDE 启动（推荐）
1. 在 IDEA 或 VS Code 中打开项目
2. 找到 `AiChatApplication.java`
3. 点击运行按钮（绿色三角形）

### 方式二：命令行启动
```bash
# 先编译打包
mvn clean package -DskipTests

# 运行 jar 包
java -jar target/ai-chat-1.0.0.jar
```

### 方式三：Maven 插件直接启动
```bash
mvn spring-boot:run
```

启动成功后，控制台会显示：
```
Tomcat started on port(s): 8080 (http)
Started AiChatApplication in 2.345 seconds
```

然后访问：http://localhost:8080

## 七、注意事项

1. **Java 版本**：确保本地安装了 JDK 17+
2. **Maven**：确保配置了 Maven 环境变量
3. **MySQL**：暂时不需要启动数据库，后续步骤会用到
4. **Lombok**：IDE 需要安装 Lombok 插件（VS Code 默认支持）
