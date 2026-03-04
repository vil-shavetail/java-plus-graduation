# Базовый образ с Java 21
FROM eclipse-temurin:21-jre-jammy
# Копируем JAR-файл
COPY target/*.jar main-service.jar
# Запускаем приложение (исправленная версия)
ENTRYPOINT ["java", "-jar", "/main-service.jar"]