FROM openjdk:17-slim

LABEL maintainer="PETLIS Team <support@petlis.com>"
LABEL description="PETLIS Backend API Service"

# 设置工作目录
WORKDIR /app

# 复制Maven配置文件
COPY pom.xml .
COPY src ./src

# 构建应用
RUN apt-get update && \
    apt-get install -y maven && \
    mvn clean package -DskipTests && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 运行应用
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "target/petlis-backend-1.0.0.jar"]

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1
