# ---- Stage 1: Build ----
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Copy local Maven cache nếu có (tăng tốc build)
# COPY .m2 /root/.m2
RUN apk add --no-cache maven && mvn clean package -DskipTests

# ---- Stage 2: Run ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Tạo user không phải root (bảo mật)
RUN addgroup -S petshop && adduser -S petshop -G petshop
USER petshop

COPY --from=builder /app/target/petshop-auth-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]