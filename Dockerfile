# 本番用: Maven で JAR をビルドし、JREのみで実行
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app

COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw

COPY src src
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd --system stepflow && useradd --system --gid stepflow stepflow
USER stepflow

COPY --from=build /app/target/stepflow-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
