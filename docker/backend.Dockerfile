FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY . .
RUN mvn -pl ruoyi-admin -am -DskipTests clean package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN useradd --system --uid 10001 ruoyi && mkdir -p /data/uploadPath && chown -R ruoyi:ruoyi /app /data
COPY --from=builder /build/ruoyi-admin/target/ruoyi-admin.jar /app/app.jar
USER ruoyi
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
