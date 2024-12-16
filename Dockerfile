# Первый этап: сборка с Gradle
FROM gradle:8.3.0-jdk17-alpine AS builder
WORKDIR /app

# Копируем файлы Gradle и конфигурацию
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .

# Загружаем зависимости для кэширования
RUN ./gradlew --no-daemon dependencies

# Копируем исходный код
COPY src ./src

# Собираем проект (без тестов, если необходимо)
RUN ./gradlew --no-daemon clean build -x test

# Второй этап: образ для запуска
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Копируем собранный jar-файл из предыдущего шага
COPY --from=builder /app/build/libs/themeExperiment-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
