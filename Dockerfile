# ===== 多阶段构建 =====

# 阶段1: 构建前端
FROM node:20-alpine AS frontend-build

WORKDIR /app/vue-code

# 先复制依赖文件，利用缓存
COPY vue-code/package.json vue-code/package-lock.json ./
RUN npm ci

# 复制前端源码并构建
COPY vue-code/ ./
RUN npm run build:spring

# 阶段2: 构建后端 JAR
FROM eclipse-temurin:21-jdk-alpine AS backend-build

WORKDIR /app

# 先复制 Maven 配置和 pom.xml，利用缓存
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./
RUN chmod +x mvnw

# 复制前端构建产物到 static 目录
COPY --from=frontend-build /app/vue-code/../src/main/resources/static src/main/resources/static/

# 复制后端源码
COPY src/ src/

# 构建 JAR（跳过测试）
RUN ./mvnw clean package -DskipTests

# 阶段3: 运行时镜像
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="IAMLZY"
LABEL description="XianYuAssistant - 闲鱼自动化管理系统"

WORKDIR /app

# 创建数据目录
RUN mkdir -p /app/dbdata /app/logs

# 从构建阶段复制 JAR
COPY --from=backend-build /app/target/XianYuAssistant-1.0.7.jar app.jar

# 暴露端口
EXPOSE 12400

# 环境变量
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SERVER_PORT=12400
ENV ALI_API_KEY=""

# 启动命令
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Dserver.port=${SERVER_PORT} -jar app.jar"]
