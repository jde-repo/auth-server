FROM openjdk:11-jdk-slim
WORKDIR /app

# netcat 설치
RUN apt-get update && apt-get install -y netcat

# wait-for-it.sh 복사
COPY wait-for-it.sh /app/wait-for-it.sh
RUN chmod +x /app/wait-for-it.sh

# JAR 복사
COPY build/libs/*.jar app.jar

ENTRYPOINT ["./wait-for-it.sh", "mysql", "3306", "--", "java", "-jar", "app.jar"]

ENV SPRING_PROFILES_ACTIVE=docker
