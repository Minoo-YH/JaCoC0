# Estefade az Java 17 baraye ejraye application
FROM eclipse-temurin:17-jdk-alpine

# Sakhtane working directory dakhele container
WORKDIR /app
# Copy kardane JAR file dakhele container
COPY target/tempConverter-1.0-SNAPSHOT.jar app.jar
# Ejraye application vaghti container start mishe
CMD ["java", "-jar", "app.jar"]