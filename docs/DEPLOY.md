# 배포 런북 (가비아 클라우드 + GitHub Actions CD)

`main`에 push되면 CD가 이미지를 빌드해 Docker Hub에 올리고, SSH로 서버에 접속해
`docker compose`로 교체한다.

구성: `nginx(80/443) -> app(8080) -> db(3306, 내부 전용)`

| 항목 | 값 |
|---|---|
| 서버 | Rocky Linux 8.10, High CPU 2vCore / 4GB / 100GB |
| 공인 IP | `1.201.116.100` |
| 배포 계정 | `deploy` |
| 배포 경로 | `/home/deploy/app` |
| 이용 기한 | **2026-08-28(금) 23:59 일괄 삭제** |

---

## 0. 기한

8/28 23:59에 서버가 **일괄 삭제**된다. 그 전에 데이터를 백업하고 6장대로 직접 정리한다.
무상 제공 사양(2vCore/4GB/100GB/공인IP 1개)을 넘기면 연결된 결제 수단으로 과금된다.

---

## 1. 서버 생성 (가비아 콘솔)

`[컴퓨팅 → 서버 → 서버 생성]`

| 항목 | 값 |
|---|---|
| 운영체제 | **Rocky Linux 8.10** 또는 Ubuntu (Windows 불가) |
| 서버 타입 | **High CPU / 2vCore / 4GB / 무료 트래픽 1TB** |
| 서버 개수 | **1개** |
| 루트 스토리지 | **100GB** |
| 데이터 스토리지 | 사용 안 함 |
| 로그인 방식 | **비밀번호 접속 방식** |
| 네트워크 | VPC 생성 → 사설 IP 자동 할당 |
| 사용자 스크립트 | 지정하지 않음 |

> 로그인 방식을 비밀번호로 고르는 이유: 콘솔의 [브라우저 터미널]은 SSH 키페어 로그인을
> 지원하지 않는다. 키페어로 만들면 터미널을 쓰려고 별도로 [관리자 비밀번호 발급]을 받아야
> 한다. 어차피 2장에서 CD 전용 키를 새로 심으므로 생성 시점 방식은 비밀번호가 편하다.

생성 후 `[네트워크 → 공인 IP]`에서 IP를 만들어 서버에 연결한다. 이 값이 `EC2_HOST`가 된다.

---

## 2. 서버 초기 설정

### 2-1. 터미널 접속

`[컴퓨팅 → 서버 → (서버 이름) → 브라우저 터미널로 접속 → 터미널 접속하기]`

- 아이디 `root`, 비밀번호는 **`[가비아 클라우드] 서버가 생성되었습니다`** 메일 본문에 있다.

> - 터미널 창을 닫아도 세션은 안 끊긴다. 끝나면 반드시 `exit`으로 로그아웃한다.
> - `[Ctrl + Alt + Del]`은 로그아웃이 아니라 재부팅/종료다.
> - noVNC 콘솔이라 `Ctrl+V`가 안 먹는다. 화면 왼쪽 가장자리 탭을 열어 클립보드 칸을 쓴다.
> - **여러 줄을 한꺼번에 붙여넣지 않는다.** 앞 명령이 뭔가 물으면 뒷줄이 그대로 답변으로
>   들어간다. `cp`는 root에서 `cp -i` 별칭이라 덮어쓸 때 반드시 묻는다. 별칭을 피하려면
>   `/bin/cp -f`로 직접 부른다.

### 2-2. CD용 SSH 키페어

키는 이미 생성되어 있다.

- 개인키 `~/.ssh/team4_deploy` → GitHub Secret `EC2_KEY`에 등록 완료
- 공개키 `~/.ssh/team4_deploy.pub` → 아래 2-3에서 서버에 심는다

다시 만들어야 한다면:

```bash
ssh-keygen -t ed25519 -C "cd@team4-backend" -f ~/.ssh/team4_deploy -N ""
gh secret set EC2_KEY < ~/.ssh/team4_deploy
```

### 2-3. 부트스트랩 실행 (서버에서 root로)

레포가 public이라 그대로 내려받아 실행할 수 있다.

```bash
curl -sL https://raw.githubusercontent.com/likelion-kwu/14th-hackathon-team4-backend/main/scripts/server-bootstrap.sh | bash -s "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAID8wlGNBTryu1VVHz/WggsK9CTfk4zj1vznzcHxq8slG cd@team4-backend"
```

[`scripts/server-bootstrap.sh`](../scripts/server-bootstrap.sh)가 하는 일: Docker CE + compose
plugin 설치(podman 제거 포함), `deploy` 계정 생성, 공개키 등록, root/비밀번호 SSH 차단,
`~/app` 생성.

**Rocky 8에서 걸리는 것 두 가지** — 스크립트는 이미 처리하지만, 수동으로 할 때 알아야 한다.

- `/etc/ssh/sshd_config.d/`를 **읽지 않는다.** 그 경로는 RHEL 9부터다. 본 파일을 직접
  고쳐야 하고, sshd는 같은 키워드가 여러 번 나오면 **첫 번째가 이기므로** 기존 줄을 주석
  처리한 뒤 맨 뒤에 붙여야 한다.
- `firewalld`가 **설치되어 있지 않다.** 설치할 필요도 없다. Docker가 포트를 publish할 때
  iptables에 DNAT를 직접 넣어 firewalld 필터를 우회하므로 컨테이너 포트는 어차피 막히지
  않는다. 실제 차단은 보안그룹이 한다.

### 2-4. 보안그룹

`[보안 → 보안그룹]` 인바운드 규칙:

| 타입 | 프로토콜 | 포트 | IP/CIDR | 비고 |
|---|---|---|---|---|
| SSH | TCP | 22 | 0.0.0.0/0 | GitHub Actions 러너 IP가 고정이 아니라 좁힐 수 없다. 대신 2-3에서 키 인증만 남겨 막는다. |
| HTTP | TCP | 80 | 0.0.0.0/0 | nginx. Let's Encrypt ACME 챌린지도 여기로 온다. |
| HTTPS | TCP | 443 | 0.0.0.0/0 | 인증서 발급 후 사용 |

- **`MS-WBT-SERVER`(3389)가 기본으로 들어 있으면 삭제한다.** 윈도우 원격 데스크톱 포트라
  리눅스 서버에서는 듣는 프로세스가 없다.
- **3306은 추가하지 않는다.** compose에서 `expose`만 해 호스트 포트에 올라오지 않는다.
  여기에 열면 그때부터 실제로 외부에 노출된다.

### 2-5. 접속 확인 (로컬에서)

```bash
ssh -i ~/.ssh/team4_deploy deploy@1.201.116.100 "id; docker compose version"
```

비밀번호 로그인이 실제로 막혔는지도 같이 본다. `Permission denied`가 정상이다.

```bash
ssh -o PubkeyAuthentication=no -o PreferredAuthentications=password root@1.201.116.100 true
```

---

## 3. GitHub Secrets

| Secret | 값 |
|---|---|
| `EC2_HOST` | `1.201.116.100` |
| `EC2_USERNAME` | `deploy` |
| `EC2_KEY` | `~/.ssh/team4_deploy` 개인키 전문 |
| `DOCKER_USERNAME` / `DOCKER_PASSWORD` | Docker Hub 계정 / Access Token |
| `MYSQL_ROOT_PASSWORD` / `MYSQL_DATABASE` / `MYSQL_USER` / `MYSQL_PASSWORD` | DB 접속 정보 |
| `JWT_SECRET` | 32바이트 이상 랜덤 문자열 |
| `CORS_ALLOWED_ORIGINS` | `http://1.201.116.100,http://localhost:5173` |

값은 stdin으로 넣어 셸 히스토리에 남기지 않는다.

```bash
gh secret set EC2_KEY < ~/.ssh/team4_deploy
```

> `CORS_ALLOWED_ORIGINS`가 비면 **앱이 시작되지 않는다.** scheme/host/port가 정확히 일치하는
> 값만 허용하고 path·query가 붙으면 기동 시점에 거부한다. wildcard(`*`)는 쓰지 않는다.
> 프론트 배포 도메인이 정해지면 여기에 추가한다.

---

## 4. 배포

CD는 `main` push에서만 돈다. 개발 흐름은 `feature → develop → main`이다.

```bash
gh pr create --base main --head develop --title "release: ..."
# 머지되면 CD가 자동으로 돈다. 수동 재실행은 아래.
gh workflow run CD --ref main
gh run watch
```

CD가 하는 일:

1. 테스트 → 이미지 빌드 → Docker Hub push (**tag는 commit SHA**, `latest`는 쓰지 않는다)
2. `docker-compose.yml` + `nginx/`를 서버 `~/app`으로 복사
3. 서버에서 `.env` 생성 → `docker compose pull && up -d`
4. `/actuator/health` 확인 + **실행 중인 컨테이너의 이미지 ID가 이번 SHA인지 대조**

4번의 이미지 대조가 중요하다. 헬스체크만으로는 `pull`/`up`이 실패해 **이전 컨테이너가 그대로
응답하는 경우**를 걸러내지 못한다.

배포 확인:

```bash
curl -i http://1.201.116.100/actuator/health
```

---

## 5. 롤백

```bash
gh run list --workflow=CD --limit 10        # 성공한 직전 배포의 commit 확인

ssh -i ~/.ssh/team4_deploy deploy@1.201.116.100
cd ~/app
sed -i "s|team4-backend:.*|team4-backend:<직전-SHA>|" .env
docker compose pull && docker compose up -d
curl -fsS http://localhost/actuator/health
```

CD의 `docker image prune -f`는 dangling 이미지만 지우므로 직전 SHA 이미지는 서버에 남아 있다.

---

## 6. 종료 시 정리 (8/28 전)

백업 후 **생성의 역순**으로 지운다. 하나라도 남으면 과금될 수 있다.

```bash
ssh -i ~/.ssh/team4_deploy deploy@1.201.116.100 \
  "docker exec team4-db mysqldump -u root -p'<비번>' --all-databases" > backup.sql
```

1. `[컴퓨팅 → 서버]` 서버 삭제
2. `[스토리지 → 블록 스토리지]` 스토리지 삭제
3. `[네트워크 → 공인 IP]` 공인 IP 삭제
4. `[네트워크 → 서브넷]` 서브넷 삭제
5. `[네트워크 → VPC]` VPC 삭제

---

## 문의

- 기술 지원: `gajet@gabia.com` (8/18~8/21 18:00, 영업시간 외 익일 답변)
- 메일 제목 예시: `[ID : likelion] 프로젝트명_서버명 기술지원 문의드립니다.`
