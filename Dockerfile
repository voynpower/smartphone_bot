# 1. Rasm (image) sifatida openjdk:17 foydalanamiz
FROM openjdk:17-jdk-slim

# 1. Rasm (image) sifatida openjdk:17 foydalanamiz
FROM openjdk:17-jdk-slim

# 2. Loyiha uchun ishchi katalog yaratamiz
WORKDIR /app

# 3. build/libs/ dan JAR faylni loyihaga ko'chiramiz
COPY build/libs/*.jar app.jar

# 4. Spring Boot ilovasini portini o'rnatamiz (Docker konteyner ichidagi port)
EXPOSE 8080

# 5. JAR faylni ishga tushiramiz
ENTRYPOINT ["java", "-jar", "app.jar"]
