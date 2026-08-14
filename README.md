# 14th Hackathon Team4 — Backend

Spring Boot 백엔드 배포 구성입니다. `main`에 push하면 GitHub Actions가 빌드해서
Docker Hub에 올리고, EC2에 접속해 자동으로 재배포합니다.

> ⚠️ 현재 애플리케이션 코드는 아직 없습니다. 배포 인프라 파일만 먼저 올려둔 상태입니다.
> Spring Boot 프로젝트(`build.gradle` 포함)를 push하면 CD가 자동으로 동작하기 시작합니다.
> 그전까지 워크플로우는 "skip" 처리되어 실패로 뜨지 않습니다.

## 구조

```
[개발자] --push--> [GitHub] --Actions--> [Docker Hub]
                       |                      |
                       +---- ssh ----> [ EC2 ] pull & compose up
                                          |
                              nginx(80) -> app(8080) -> mysql(3306)
```

- **nginx** — 리버스 프록시. 외부에 열린 유일한 컨테이너(80/443)
- **app** — Spring Boot. 외부에 직접 노출하지 않음
- **db** — MySQL 8. 내부 네트워크에서만 접근 가능
- 컨테이너끼리는 IP 대신 **이름**(`app`, `db`)으로 통신합니다.

## 파일

| 파일 | 역할 |
|---|---|
| `Dockerfile` | 멀티스테이지 빌드. gradle로 jar 만들고 JRE 이미지에 얹음 |
| `docker-compose.yml` | nginx + app + db 한 번에 실행 |
| `nginx/conf.d/default.conf` | 리버스 프록시 설정 |
| `.github/workflows/cd.yml` | 빌드 → Docker Hub push → EC2 배포 |
| `.env.example` | 환경변수 템플릿 (`.env`는 커밋 금지) |

---

## 1. 준비물

- Docker Hub 계정 + **Access Token** (비밀번호 말고 토큰)
- AWS EC2 (Ubuntu) 인스턴스 + `.pem` 키
- 보안 그룹 인바운드: `22`, `80`, `443` 오픈

## 2. EC2 최초 세팅 (한 번만)

```bash
sudo apt-get update && sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker $USER   # 이후 재접속 필요
mkdir -p ~/app
```

재접속 후 `docker ps`가 sudo 없이 되면 성공입니다.

## 3. GitHub Secrets 등록

`Settings → Secrets and variables → Actions → New repository secret`

| 이름 | 값 |
|---|---|
| `DOCKERHUB_USERNAME` | Docker Hub 아이디 |
| `DOCKERHUB_TOKEN` | Docker Hub Access Token |
| `EC2_HOST` | EC2 퍼블릭 IP 또는 도메인 |
| `EC2_USER` | `ubuntu` |
| `EC2_SSH_KEY` | `.pem` 파일 **전체 내용** (`-----BEGIN` 줄부터 끝까지) |
| `MYSQL_ROOT_PASSWORD` | root 비밀번호 |
| `MYSQL_DATABASE` | `team4` |
| `MYSQL_USER` | DB 유저명 |
| `MYSQL_PASSWORD` | DB 비밀번호 |
| `JWT_SECRET` | 32자 이상 랜덤 문자열 |

## 4. 애플리케이션 설정

앱은 DB 정보를 **환경변수**로 받습니다. `application.yml`에 하드코딩하지 마세요.

```yaml
# src/main/resources/application-prod.yml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
server:
  port: 8080
```

## 5. 배포

`main`에 push하면 끝입니다.

```bash
git push origin main
```

Actions 탭에서 진행 상황을 볼 수 있습니다. 완료 후 `http://<EC2-IP>` 로 접속합니다.

---

## 로컬에서 확인하기

```bash
cp .env.example .env
```

`.env`를 채우고, 로컬 소스로 이미지를 직접 빌드하려면:

```bash
docker compose build app && docker compose up -d
```

> `docker-compose.yml`의 `app.image`는 Docker Hub 이미지를 가리킵니다.
> 로컬 빌드를 쓰려면 `app` 서비스에 `build: .` 를 임시로 추가하세요.

유용한 명령어:

```bash
docker compose logs -f app
```

```bash
docker compose ps
```

```bash
docker compose down
```

---

## HTTPS 붙이기 (도메인 필요)

도메인을 EC2 IP로 연결한 뒤, 서버에서:

```bash
cd ~/app && mkdir -p nginx/certbot/www nginx/certbot/conf
docker run --rm -v $PWD/nginx/certbot/conf:/etc/letsencrypt -v $PWD/nginx/certbot/www:/var/www/certbot certbot/certbot certonly --webroot -w /var/www/certbot -d 도메인 --email 이메일 --agree-tos --no-eff-email
```

발급되면 `nginx/conf.d/default.conf`의 HTTPS 블록 주석을 해제하고 `example.com`을 실제 도메인으로 바꾼 뒤:

```bash
docker compose restart nginx
```

## CORS

CORS는 nginx가 아니라 **Spring에서** 처리하는 걸 권장합니다. 중복 설정되면
`Access-Control-Allow-Origin` 헤더가 두 번 붙어 브라우저가 요청을 거부합니다.

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("https://프론트도메인", "http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

`allowCredentials(true)`일 때는 `allowedOrigins("*")`를 쓸 수 없습니다.
와일드카드가 필요하면 `allowedOriginPatterns`를 쓰세요.

## 트러블슈팅

| 증상 | 확인 |
|---|---|
| 502 Bad Gateway | `docker compose logs app` — 앱이 뜨다 죽었을 가능성 |
| DB 연결 실패 | `.env`의 DB 값과 compose 환경변수가 맞는지 |
| Actions에서 SSH 실패 | `EC2_SSH_KEY`에 `-----BEGIN/END-----` 줄까지 넣었는지 |
| 이미지 pull 실패 | Docker Hub 저장소가 private이면 서버에서도 로그인 필요 |
