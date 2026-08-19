#!/usr/bin/env bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly CHECK_SCRIPT="${SCRIPT_DIR}/ci-gradle-check.sh"
readonly TEST_DIR="$(mktemp -d "${TMPDIR:-/tmp}/ci-gradle-check-test.XXXXXX")"
readonly FAKE_GRADLE="${TEST_DIR}/gradlew"
readonly STATE_FILE="${TEST_DIR}/attempts"
readonly OUTPUT_FILE="${TEST_DIR}/output"

trap 'rm -rf "$TEST_DIR"' EXIT

cat > "$FAKE_GRADLE" <<'FAKE_GRADLE_SCRIPT'
#!/usr/bin/env bash

set -euo pipefail

attempt=0
if [[ -f "$FAKE_STATE_FILE" ]]; then
	attempt="$(<"$FAKE_STATE_FILE")"
fi
attempt=$(( attempt + 1 ))
echo "$attempt" > "$FAKE_STATE_FILE"

case "$FAKE_SCENARIO" in
	success)
		echo "BUILD SUCCESSFUL"
		exit 0
		;;
	non_rate_limit)
		echo "Compilation failed"
		exit 7
		;;
	rate_limit_then_success)
		if (( attempt == 1 )); then
			echo "Could not GET artifact: HTTP 429"
			exit 42
		fi
		echo "BUILD SUCCESSFUL"
		exit 0
		;;
	always_rate_limited)
		echo "Server returned Too Many Requests"
		exit 29
		;;
	numeric_boundary)
		echo "Dependency resolution failed with internal code 1429"
		exit 8
		;;
	*)
		echo "Unknown fake scenario: $FAKE_SCENARIO" >&2
		exit 99
		;;
esac
FAKE_GRADLE_SCRIPT
chmod +x "$FAKE_GRADLE"

fail() {
	echo "FAIL: $1" >&2
	exit 1
}

run_scenario() {
	local scenario="$1"
	local expected_status="$2"
	local expected_attempts="$3"
	local status

	rm -f "$STATE_FILE"
	set +e
	FAKE_SCENARIO="$scenario" \
		FAKE_STATE_FILE="$STATE_FILE" \
		CI_GRADLE_EXECUTABLE="$FAKE_GRADLE" \
		CI_GRADLE_BACKOFF_SECONDS=0 \
		"$CHECK_SCRIPT" > "$OUTPUT_FILE" 2>&1
	status=$?
	set -e

	[[ "$status" -eq "$expected_status" ]] || fail "$scenario returned $status, expected $expected_status"
	[[ "$(<"$STATE_FILE")" -eq "$expected_attempts" ]] || fail "$scenario ran $(<"$STATE_FILE") times, expected $expected_attempts"
}

run_scenario success 0 1
run_scenario non_rate_limit 7 1
run_scenario rate_limit_then_success 0 2
run_scenario always_rate_limited 29 3
run_scenario numeric_boundary 8 1

echo "All ci-gradle-check tests passed."
