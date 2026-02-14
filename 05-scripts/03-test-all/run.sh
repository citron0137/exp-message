#!/bin/sh
# =============================================================================
# Run all checks: clean, detekt, ktlintCheck, test (or unitTest with --except-integration)
# Run from repository root, or any directory (script resolves repo root).
# 메시지·색상 출력은 이 스크립트에서 처리 (pre-push, CI, 수동 실행 공통).
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
MONOLITIC="${REPO_ROOT}/02-backend/00-monolitic"

EXCEPT_INTEGRATION=0
for arg in "$@"; do
  case "$arg" in
    --except-integration) EXCEPT_INTEGRATION=1 ;;
  esac
done

# 색상: TTY이고 Windows(Git Bash 등)가 아닐 때만 ANSI 사용
use_color=0
if [ -t 1 ] && [ "${TERM:-dumb}" != "dumb" ]; then
  case "$(uname -s 2>/dev/null)" in
    MINGW*|MSYS*|CYGWIN*) ;;
    *) use_color=1 ;;
  esac
fi

if [ "$use_color" = 1 ]; then
  RED='\033[0;31m'
  GREEN='\033[0;32m'
  YELLOW='\033[1;33m'
  BOLD='\033[1m'
  RESET='\033[0m'
else
  RED=''
  GREEN=''
  YELLOW=''
  BOLD=''
  RESET=''
fi

cd "${MONOLITIC}"

# gradlew 실행 권한 (CI/체크아웃 후 권한이 없을 수 있음)
chmod +x ./gradlew 2>/dev/null || true

echo ""
echo "${BOLD}━━━ Pre-push checks (02-backend/00-monolitic) ━━━${RESET}"
echo ""

# [1/4] clean
echo "${BOLD}[1/4] clean${RESET}"
./gradlew clean
echo ""

# [2/4] detekt
echo "${BOLD}[2/4] detekt${RESET}"
if ! ./gradlew detekt; then
  echo ""
  echo "${RED}  ✗ detekt failed${RESET}"
  echo ""
  echo "${YELLOW}  위 로그의 파일 경로와 규칙명을 보고 코드를 수정한 뒤,${RESET}"
  echo "  ./gradlew detekt  로 확인 후 다시 실행하세요."
  echo ""
  exit 1
fi
echo "${GREEN}  ✓ detekt passed${RESET}"
echo ""

# [3/4] ktlintCheck
echo "${BOLD}[3/4] ktlintCheck${RESET}"
if ! ./gradlew ktlintCheck; then
  echo ""
  echo "${RED}  ✗ ktlintCheck failed${RESET}"
  echo ""
  echo "${YELLOW}  포맷 자동 수정: ./gradlew ktlintFormat${RESET}"
  echo "  변경 파일 스테이징 후 다시 실행하세요."
  echo ""
  exit 1
fi
echo "${GREEN}  ✓ ktlintCheck passed${RESET}"
echo ""

# [4/4] test / unitTest
if [ "$EXCEPT_INTEGRATION" = 1 ]; then
  echo "${BOLD}[4/4] unitTest${RESET}"
  if ! ./gradlew unitTest; then
    echo ""
    echo "${RED}  ✗ unitTest failed${RESET}"
    echo ""
    echo "${YELLOW}  위 로그에서 실패한 테스트를 확인한 뒤 수정하세요.${RESET}"
    echo "  다시 실행: 05-scripts/03-test-all/run.sh --except-integration"
    echo ""
    exit 1
  fi
else
  echo "${BOLD}[4/4] test${RESET}"
  if ! ./gradlew test; then
    echo ""
    echo "${RED}  ✗ test failed${RESET}"
    echo ""
    echo "${YELLOW}  1. 현재 환경에 도커가 설치되어 있는지 확인하세요 (테스트 시 필요).${RESET}"
    echo ""
    echo "${YELLOW}  2. 다음 경로의 index.html을 브라우저로 열어 에러 리포트를 확인하세요:${RESET}"
    echo "     ${MONOLITIC}/build/reports/tests/test/index.html"
    echo ""
    echo "${YELLOW}  3. 05-scripts/03-test-all/run.sh 로 다시 결과를 확인한 뒤, 수정 후 push 하세요.${RESET}"
    echo "     단위만: --except-integration 옵션 추가"
    echo ""
    exit 1
  fi
fi

echo "${GREEN}  ✓ test passed${RESET}"
echo ""
echo "${GREEN}✓ All checks passed. Good to push.${RESET}"
echo ""
exit 0
