# 构建阶段
FROM maven:3.8-eclipse-temurin-8 AS build
WORKDIR /app
COPY pom.xml .
# 预拉取依赖（利用 Docker 层缓存，源码变更时无需重复下载）
RUN mvn -q -B dependency:go-offline || true
COPY src ./src
RUN mvn -q -B package -DskipTests

# 运行阶段
FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=build /app/target/storeiq-api-1.0.0.jar app.jar
EXPOSE 8002
ENTRYPOINT ["java", "-jar", "app.jar"]
