# 1. Java 17 obrazini tanlaymiz
FROM openjdk:17-jdk-alpine AS builder

# 2. Ilova fayllarini konteynerga ko'chiramiz
COPY . /app

# 3. Ilovani quramiz (testlarni o'tkazmay)
WORKDIR /app
RUN ./gradlew build -x test

# 4. Yaratilgan JAR faylini konteynerga ko'chiramiz
FROM openjdk:17-jdk-alpine

COPY --from=builder /app/build/libs/*.jar app.jar

# 5. Spring Boot ilovasini ishga tushiramiz
ENTRYPOINT ["java", "-jar", "/app.jar"]
