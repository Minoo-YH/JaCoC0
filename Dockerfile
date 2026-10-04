FROM eclipse-temurin:17-jdk

RUN apt-get update && \
    apt-get install -y \
    maven \
    libgtk-3-0 \
    libx11-6 \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libgl1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src

CMD ["mvn", "javafx:run"]
