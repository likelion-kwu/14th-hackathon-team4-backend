# ---------- build stage ----------
# 소스코드 -> 컴파일 -> 패키징(.jar)
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# build.gradle / build.gradle.kts 어느 쪽이든 동작하도록 통째로 복사한다.
# 레이어 캐싱은 CI(buildx gha cache)가 담당한다.
COPY . .
# 테스트는 CI에서 따로 돌리므로 이미지 빌드에서는 제외
RUN ./gradlew clean bootJar -x test --no-daemon

# Spring Boot는 실행 가능한 jar과 -plain.jar 두 개를 만든다.
# 실행 가능한 쪽만 골라서 고정된 이름으로 옮긴다.
RUN set -eux; \
    jar="$(find build/libs -name '*.jar' ! -name '*-plain.jar' | head -n 1)"; \
    test -n "$jar"; \
    cp "$jar" /app/app.jar

# ---------- runtime stage ----------
# 실행에 필요한 건 jar + JRE 뿐
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /app/app.jar /app/app.jar

EXPOSE 8080

ENV TZ=Asia/Seoul
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
