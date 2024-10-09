# 베이스 이미지로 OpenJDK 17 사용
FROM openjdk:17-jdk-slim

# 애플리케이션 JAR 파일을 컨테이너의 /app 디렉토리에 복사
WORKDIR /app
COPY build/libs/tamago-0.0.1-SNAPSHOT.jar app.jar

# 애플리케이션 포트 8080 노출
EXPOSE 8087

# 애플리케이션 실행 명령
ENTRYPOINT ["java", "-jar", "app.jar"]