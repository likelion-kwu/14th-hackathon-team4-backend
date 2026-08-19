#!/usr/bin/env bash

set -euo pipefail

readonly MAX_ATTEMPTS=3
readonly BACKOFF_SECONDS="${CI_GRADLE_BACKOFF_SECONDS:-5}"
readonly GRADLE_EXECUTABLE="${CI_GRADLE_EXECUTABLE:-./gradlew}"

if [[ ! "$BACKOFF_SECONDS" =~ ^[0-9]+$ ]]; then
	echo "CI_GRADLE_BACKOFF_SECONDS must be a non-negative integer." >&2
	exit 2
fi

attempt_log="$(mktemp "${TMPDIR:-/tmp}/ci-gradle-check.XXXXXX")"
trap 'rm -f "$attempt_log"' EXIT

is_maven_rate_limit_failure() {
	grep -Eiq '(^|[^[:digit:]])429([^[:digit:]]|$)|too[[:space:]-]+many[[:space:]-]+requests' "$attempt_log"
}

attempt=1
while (( attempt <= MAX_ATTEMPTS )); do
	: > "$attempt_log"

	set +e
	"$GRADLE_EXECUTABLE" clean check --no-daemon 2>&1 | tee "$attempt_log"
	exit_code=${PIPESTATUS[0]}
	set -e

	if (( exit_code == 0 )); then
		exit 0
	fi

	if ! is_maven_rate_limit_failure; then
		exit "$exit_code"
	fi

	if (( attempt == MAX_ATTEMPTS )); then
		echo "Gradle check still failed due to Maven rate limiting after ${MAX_ATTEMPTS} attempts." >&2
		exit "$exit_code"
	fi

	delay=$(( BACKOFF_SECONDS * attempt ))
	echo "Maven rate limit detected. Retrying Gradle check in ${delay}s (attempt $(( attempt + 1 ))/${MAX_ATTEMPTS})." >&2
	sleep "$delay"
	(( attempt += 1 ))
done
