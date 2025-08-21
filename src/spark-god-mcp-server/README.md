# SparkGod MCP Server (Simplified)

Python 기반 Model Context Protocol (MCP) 서버 - 두 개의 핵심 도구로 Spark 분석을 제공합니다.

## 핵심 기능 (2개 도구만)

1. **Spark 설정 분석**: 성능 최적화 권장사항 제시
2. **중요한 로그 수집**: ERROR/WARN/FATAL 로그만 수집하여 LLM 분석 요청

## 특징

- 🎯 **집중된 기능**: 꼭 필요한 2개 도구만 제공
- 🧠 **LLM 기반 분석**: 중요한 로그를 LLM에게 전달하여 정확한 분석
- ⚡ **효율적인 수집**: INFO/DEBUG 제외, 중요한 로그만 선별 수집
- 🔧 **설정 최적화**: 실시간 Spark 설정 분석 및 권장사항

## 설치

```bash
pip install -r requirements.txt
```

## 환경 변수

```bash
# Spark UI URL (기본값: http://localhost:4040)
export SPARK_UI_URL=http://localhost:4040
```

## 사용법

SparkGod Agent가 자동으로 이 MCP 서버를 실행합니다:

```bash
python3 spark_god_mcp_server.py
```

## 도구 목록 (2개)

### 1. `analyze_spark_configuration()`
- **기능**: 현재 Spark 애플리케이션의 설정을 분석하여 성능 최적화 권장사항을 제시
- **데이터 소스**: 
  - SparkGod 플러그인의 `/spark-god/config/json/` API (우선)
  - Spark UI API `/api/v1/applications/{app_id}/environment` (대체)
- **분석 영역**:
  - 💾 **메모리 설정**: Executor/Driver 메모리, Off-heap 설정
  - ⚡ **성능 설정**: 병렬 처리, Adaptive Query Execution
  - 🎯 **리소스 관리**: 마스터 모드, 동적 할당
  - 💡 **최적화 권장사항**: 구체적인 설정 개선 방안

### 2. `collect_critical_spark_logs()`
- **기능**: Spark UI에서 실시간으로 중요한 오류와 경고만 수집하여 LLM 분석을 위한 구조화된 데이터를 제공
- **수집 소스**: 
  - 🌐 **Spark UI 로그 페이지**: `http://localhost:4040/logPage/?appId={app_id}&executorId={executor_id}&logType=stderr`
  - 📊 **다중 Executor**: 모든 Executor의 stderr/stdout 로그 수집
  - 🎯 **4040 포트**: Spark UI 표준 포트 사용
- **수집 대상**: 
  - 🚨 **ERROR 로그**: 심각한 오류 및 예외
  - ⚠️ **WARN 로그**: 경고 및 잠재적 문제
  - 💀 **FATAL 로그**: 치명적인 오류
  - ❌ **INFO/DEBUG 제외**: 불필요한 로그는 필터링
- **출력 형태**:
  - 📋 구조화된 로그 엔트리 (타임스탬프, 라인 번호, 소스 포함)
  - 📊 기본 애플리케이션 메트릭 (Jobs/Stages/Executors 상태)
  - 🎯 LLM 분석을 위한 컨텍스트 및 가이드라인
- **장점**:
  - 🌐 **실시간**: Spark UI에서 최신 로그 직접 수집
  - 🎯 **효율적**: 중요한 로그만 선별하여 빠른 분석
  - 🧠 **LLM 최적화**: LLM이 분석하기 좋은 구조화된 형태
  - 📈 **정확한 분석**: 노이즈 제거로 핵심 이슈에 집중

## Agent 연동

SparkGod Agent는 상황에 따라 적절한 도구를 자동 선택합니다:

- **설정 관련 질문** → `analyze_spark_configuration()` 호출
- **로그 분석 요청** → `collect_critical_spark_logs()` 호출 → LLM이 분석

### 새로운 LLM 기반 분석 워크플로우

1. **사용자 요청**: "Spark 로그를 분석해줘"
2. **MCP 서버**: 중요한 로그만 수집 + 구조화 (`collect_critical_spark_logs`)
3. **LLM**: 중요한 로그 맥락 분석 + 인사이트 도출
4. **결과**: 정확하고 상세한 분석 보고서

## 사용 예시

### Spark 설정 분석
```
사용자: "현재 Spark 설정에 문제가 있나요?"
→ analyze_spark_configuration() 호출
→ 메모리, 성능, 리소스 등 핵심 설정 분석 및 권장사항 제공
```

### 중요한 로그 분석
```
사용자: "Spark Job에 어떤 오류가 있었나요?"
→ collect_critical_spark_logs() 호출
→ ERROR/WARN/FATAL 로그만 수집하여 LLM에게 전달
→ LLM이 근본 원인 분석 및 해결방안 제시
```

## 로그 수집 예시

### 수집되는 로그 형태
```markdown
# Spark 애플리케이션 중요 로그 분석
**애플리케이션**: SparkGod Test (ID: app-123)

## 🚨 ERROR 로그 (3개)
**라인 1234** (17:50:05):
```
24/08/17 17:50:05 ERROR TaskScheduler: Task 1.0 in stage 1.0 (TID 3) failed: java.lang.OutOfMemoryError: Java heap space
```

## ⚠️ WARN 로그 (5개)
**라인 1456** (17:50:06):
```
24/08/17 17:50:06 WARN MemoryManager: Total allocation exceeds 95% of heap: 512.0 MB
```
```

## 디버깅 팁

1. **로그가 수집되지 않는 경우**: 
   - Spark UI 포트 확인 (4040)
   - `http://localhost:4040/logPage/?appId={app_id}&executorId=0&logType=stderr` 직접 접근 테스트
   - 애플리케이션이 실행 중인지 확인

2. **중요한 로그가 누락되는 경우**:
   - 모든 Executor의 stderr/stdout 로그를 수집하는지 확인
   - 로그 레벨이 ERROR/WARN/FATAL인지 확인
   - HTML 파싱이 올바르게 되는지 확인

3. **포트 접근 실패하는 경우**:
   - 방화벽 설정 확인
   - Spark UI가 4040 포트에서 실행 중인지 확인 (`http://localhost:4040`)
   - 네트워크 연결 상태 확인

4. **성능이 느린 경우**:
   - 중요한 로그만 수집하므로 기본적으로 빠름
   - 4040 포트 단일 접근으로 최적화됨

## 요구사항

1. **Spark 애플리케이션 실행 중**: `http://localhost:4040`에서 Spark UI 접근 가능
2. **SparkGod 플러그인 설치**: 설정 정보 조회를 위해 필요 (선택사항)
3. **로그 파일 접근 권한**: 로그 분석을 위해 필요

## 장점

### 기존 복잡한 버전 대비
- ❌ **기존**: 6개 도구, 복잡한 분석 로직, 패턴 매칭 한계
- ✅ **현재**: 2개 도구, 간단명확, LLM 기반 정확한 분석

### 효율성
- 🎯 **집중**: 꼭 필요한 기능만 제공
- ⚡ **빠름**: 중요한 로그만 선별 수집
- 🧠 **정확**: LLM의 자연어 이해 능력 활용
- 🔧 **실용**: 실제 문제 해결에 집중