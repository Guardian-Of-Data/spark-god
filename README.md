# Spark UI Plugin Development Environment

이 프로젝트는 Spark Web UI에 커스텀 탭을 추가하는 플러그인을 개발하기 위한 로컬 환경입니다.

## 목표

- Spark History Server의 정보를 바탕으로 사용자에게 피드백을 제공하는 Spark 플러그인 개발
- Spark Web UI의 상단 네비게이션 바에 새로운 탭 추가
- 사용자가 탭을 클릭하면 플러그인에서 제공하는 정보를 보여주는 React 앱 렌더링

## 빠른 시작

### 1. 환경 설정

```bash
# Spark 다운로드 및 설치
make setup

# 환경 변수 설정
source .env
```

### 2. Spark 클러스터 시작

```bash
# Spark Master 시작
make start-master

# Spark Worker 시작 (새 터미널에서)
make start-worker

# Spark History Server 시작 (새 터미널에서)
make start-history
```

### 3. 플러그인 빌드 및 테스트

```bash
# 플러그인 빌드
make plugin-build

# 플러그인을 Spark jars 디렉토리로 복사
make plugin-copy

# 플러그인 테스트
make plugin-test
```

### 4. 웹 UI 접근

- **Spark Master UI**: http://localhost:8080
- **Spark History Server**: http://localhost:18080
- **애플리케이션 UI**: http://localhost:4040 (플러그인 테스트 실행 후)

## 프로젝트 구조

```
spark/
├── Makefile                    # 로컬 개발 환경 관리
├── README.md                   # 이 파일
├── spark-3.5.0-bin-hadoop3/   # Spark 설치 디렉토리 (설치 후 생성)
├── src/
│   └── spark-ui-plugin/       # Spark UI 플러그인 프로젝트
│       ├── build.sbt          # SBT 빌드 설정
│       └── src/main/
│           ├── scala/         # Scala 소스 코드
│           ├── resources/     # 리소스 파일
│           └── webapp/        # 웹 애플리케이션 파일
└── spark-events/              # Spark 이벤트 로그 (실행 후 생성)
```

## 주요 명령어

### 환경 관리
- `make setup` - Spark 다운로드 및 설치
- `make clean` - 모든 파일 정리
- `source .env` - 환경 변수 설정

### Spark 관리
- `make start-master` - Spark Master 시작
- `make start-worker` - Spark Worker 시작
- `make start-history` - Spark History Server 시작
- `make stop-spark` - 모든 Spark 프로세스 중지
- `make spark-status` - Spark 프로세스 상태 확인

### 플러그인 개발
- `make plugin-build` - 플러그인 빌드
- `make plugin-copy` - 플러그인을 Spark jars로 복사
- `make plugin-test` - 플러그인 테스트
- `make plugin-clean` - 플러그인 빌드 파일 정리

### 개발 도구
- `make sample-job` - 샘플 Spark 작업 실행
- `make logs` - Spark 로그 확인

## 플러그인 기능

현재 플러그인은 다음 기능을 제공합니다:

1. **커스텀 탭 추가**: Spark Web UI에 "Custom Plugin" 탭 추가
2. **기본 정보 표시**: 애플리케이션 이름, ID, 시작 시간 등 표시
3. **확장 가능한 구조**: 향후 History Server 정보 통합을 위한 기반 제공

## 다음 단계

1. **History Server 연동**: Spark History Server API를 사용하여 과거 애플리케이션 정보 수집
2. **React UI 개발**: 더 풍부한 사용자 인터페이스를 위한 React 컴포넌트 개발
3. **실시간 데이터**: 실시간 애플리케이션 메트릭 및 로그 표시
4. **사용자 피드백**: 사용자 정의 알림 및 경고 시스템

## 문제 해결

### SBT가 설치되지 않은 경우
```bash
# macOS
brew install sbt

# Ubuntu/Debian
echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt_old.list
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add
sudo apt-get update
sudo apt-get install sbt
```

### 포트 충돌 문제
- Spark Master UI: 8080
- Spark History Server: 18080
- 애플리케이션 UI: 4040

포트가 사용 중인 경우 해당 프로세스를 중지하거나 다른 포트를 사용하세요.

## 참고 자료

- [Spark Plugin API Documentation](https://spark.apache.org/docs/latest/api/java/org/apache/spark/api/plugin/package-summary.html)
- [Spark Web UI Customization](https://spark.apache.org/docs/latest/monitoring.html#web-interfaces)
- [Spark History Server](https://spark.apache.org/docs/latest/monitoring.html#viewing-after-the-fact)
