FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN ./gradlew clean build -x test

FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=build /app/build/libs/prova-estapar-1.1-all.jar app.jar

EXPOSE 3003

ENTRYPOINT ["java", "-jar", "app.jar"]