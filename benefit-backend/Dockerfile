# 第一階段：使用 Maven 編譯
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests


# 第二階段：執行 Spring Boot
FROM eclipse-temurin:17-jre

WORKDIR /app

ENV TZ=Asia/Taipei
ENV DB_TYPE=h2
ENV JAVA_TOOL_OPTIONS="-Xms128m -Xmx384m"

COPY --from=build /app/target/*.war app.war

EXPOSE 8080

CMD ["sh", "-c", "exec java -Dserver.port=${PORT:-8080} -jar app.war"]