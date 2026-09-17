FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src src
RUN mvn package -q -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/classes /app/classes
COPY --from=build /app/target/dependency /app/dependency
ENV PORT=8080
CMD ["sh", "-c", "java -cp '/app/classes:/app/dependency/*' api.ApiServer"]