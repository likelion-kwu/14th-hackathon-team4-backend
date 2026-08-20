#!/usr/bin/env bash
# 가비아 클라우드 서버(Rocky Linux 8.x)를 CD 배포 대상으로 만드는 초기 설정 스크립트.
#
# 실행 위치 : 가비아 콘솔 [브라우저 터미널]에서 root로 로그인한 뒤 이 서버 위에서 직접.
# 실행 방법 : bash server-bootstrap.sh "<CD에서 쓸 SSH 공개키 한 줄>"
#
# 하는 일
#   1. Docker CE + compose plugin 설치 (Rocky 기본 podman과 파일이 겹치므로 걷어낸다)
#   2. 배포 전용 계정 deploy 생성 + docker 그룹 부여
#   3. 전달받은 공개키를 deploy 계정에 등록 (CD가 이 키로 SSH 접속한다)
#   4. root 원격 로그인 / 비밀번호 로그인 차단 (키 인증만 허용)
#   5. 배포 디렉터리 ~/app 준비
#
# 여러 번 실행해도 같은 결과가 되도록 작성했다.
set -euo pipefail

PUBKEY="${1:-}"
DEPLOY_USER="deploy"

if [ -z "$PUBKEY" ]; then
  echo "사용법: bash server-bootstrap.sh \"ssh-ed25519 AAAA... cd@team4-backend\"" >&2
  exit 1
fi

if [ "$(id -u)" -ne 0 ]; then
  echo "root로 실행해야 한다." >&2
  exit 1
fi

echo "==> 1/5 Docker 설치"
# Rocky에는 podman/buildah가 미리 깔려 있고 docker-ce와 파일이 겹친다.
dnf -y remove podman buildah runc containers-common || true
dnf -y install dnf-plugins-core
dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
dnf -y install --allowerasing \
  docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable --now docker
docker compose version

echo "==> 2/5 배포 계정 준비"
id -u "$DEPLOY_USER" >/dev/null 2>&1 || useradd -m -s /bin/bash "$DEPLOY_USER"
usermod -aG docker "$DEPLOY_USER"

echo "==> 3/5 SSH 공개키 등록"
AUTH_KEYS="/home/${DEPLOY_USER}/.ssh/authorized_keys"
install -d -m 700 -o "$DEPLOY_USER" -g "$DEPLOY_USER" "/home/${DEPLOY_USER}/.ssh"
touch "$AUTH_KEYS"
# 같은 키를 두 번 넣지 않는다.
grep -qxF "$PUBKEY" "$AUTH_KEYS" || echo "$PUBKEY" >> "$AUTH_KEYS"
chmod 600 "$AUTH_KEYS"
# 소유자가 root로 남으면 sshd가 키를 읽지 못해 인증이 실패한다. 디렉터리까지 통째로 넘긴다.
chown -R "${DEPLOY_USER}:${DEPLOY_USER}" "/home/${DEPLOY_USER}/.ssh"
runuser -u "$DEPLOY_USER" -- cat "$AUTH_KEYS" > /dev/null

echo "==> 4/5 SSH 정책 강화"
# 가비아 기본값은 root + 비밀번호 로그인이다. 공인 IP가 붙는 순간 무차별 대입의 대상이 되므로
# 키 인증만 남긴다. 콘솔 [브라우저 터미널]은 SSH를 거치지 않으므로 이 설정과 무관하게 계속 쓸 수 있다.
#
# Rocky 8의 OpenSSH는 /etc/ssh/sshd_config.d/*.conf를 include하지 않는다. (RHEL 9부터 생긴 경로)
# 그래서 본 파일을 직접 고친다. sshd는 같은 키워드가 여러 번 나오면 "첫 번째"가 이기므로,
# 기존 줄을 먼저 주석 처리한 뒤 맨 뒤에 붙여야 한다.
cp -f /etc/ssh/sshd_config "/etc/ssh/sshd_config.bak.$(date +%Y%m%d%H%M%S)"
sed -i '/^# --- team4 CD ---$/,/^# --- team4 CD end ---$/d' /etc/ssh/sshd_config
sed -i -E 's/^[[:space:]]*(PermitRootLogin|PasswordAuthentication|KbdInteractiveAuthentication|ChallengeResponseAuthentication)[[:space:]]/#&/' /etc/ssh/sshd_config
cat >> /etc/ssh/sshd_config <<'SSHEOF'
# --- team4 CD ---
PermitRootLogin no
PasswordAuthentication no
KbdInteractiveAuthentication no
PubkeyAuthentication yes
# --- team4 CD end ---
SSHEOF
sshd -t
systemctl reload sshd
sshd -T | grep -E '^(permitrootlogin|passwordauthentication|kbdinteractiveauthentication|pubkeyauthentication) '

echo "==> 5/5 배포 디렉터리"
install -d -m 755 -o "$DEPLOY_USER" -g "$DEPLOY_USER" "/home/${DEPLOY_USER}/app"

# firewalld는 설치하지 않는다.
# 가비아 이미지에 기본 미설치이기도 하고, Docker가 포트를 publish할 때 iptables에 DNAT를
# 직접 넣어 firewalld의 필터를 우회하므로 컨테이너 포트는 어차피 막히지 않는다.
# 실제 차단은 가비아 콘솔의 보안그룹이 담당한다.
cat <<'MSG'

완료. 다음을 확인한다.

  [가비아 콘솔] 보안 > 보안그룹 > 인바운드 규칙
      SSH   22   0.0.0.0/0
      HTTP  80   0.0.0.0/0
      HTTPS 443  0.0.0.0/0
    * 3389(MS-WBT-SERVER)이 있으면 삭제한다. 리눅스 서버에서 듣는 프로세스가 없다.
    * 3306은 열지 않는다. DB는 compose 내부 네트워크로만 접근한다.

  [GitHub Secrets]
      EC2_HOST     = 이 서버에 연결한 공인 IP
      EC2_USERNAME = deploy
      EC2_KEY      = 위 공개키와 짝인 개인키 전문

MSG
