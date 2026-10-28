# 第一階段：Maven 編譯
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# 第二階段：執行 Spring Boot
FROM eclipse-temurin:17-jre
WORKDIR /app

ENV TZ=Asia/Taipei
ENV DB_TYPE=h2
ENV JAVA_TOOL_OPTIONS="-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xms64m -Xmx256m -XX:ReservedCodeCacheSize=64m -XX:+ExitOnOutOfMemoryError -Dspring.jmx.enabled=false"

COPY --from=build /app/target/*.war app.war
EXPOSE 8080
CMD ["sh", "-c", "exec java -Dserver.address=0.0.0.0 -Dserver.port=${PORT:-10000} -jar app.war"]