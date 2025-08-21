#!/usr/bin/env python3
"""
SparkGod MCP Server - Simplified Version
두 개의 핵심 도구만 제공:
1. Spark 설정 분석
2. 중요한 로그 수집 및 LLM 분석 요청
"""

import json
import os
import requests
import glob
import logging
import re
from datetime import datetime
from typing import Dict, Any, List, Optional
from mcp.server.fastmcp import FastMCP

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler('spark_god_mcp_debug.log')
    ]
)
logger = logging.getLogger('SparkGodMCP')

# MCP 서버 초기화
mcp = FastMCP("SparkGod MCP Server")

# Spark UI 기본 URL
SPARK_UI_URL = os.getenv('SPARK_UI_URL', 'http://localhost:4040')

def safe_parse_timestamp(timestamp_value) -> Optional[datetime]:
    """안전하게 타임스탬프를 파싱하는 유틸리티 함수"""
    if not timestamp_value:
        return None
    
    try:
        if isinstance(timestamp_value, str):
            if 'T' in timestamp_value:
                iso_string = timestamp_value
                if iso_string.endswith('GMT'):
                    iso_string = iso_string.replace('GMT', '+00:00')
                elif iso_string.endswith('Z'):
                    iso_string = iso_string.replace('Z', '+00:00')
                elif iso_string.endswith('UTC'):
                    iso_string = iso_string.replace('UTC', '+00:00')
                
                try:
                    return datetime.fromisoformat(iso_string)
                except ValueError:
                    return parse_timestamp_with_strptime(timestamp_value)
            else:
                try:
                    timestamp_ms = int(timestamp_value)
                    return datetime.fromtimestamp(timestamp_ms / 1000)
                except ValueError:
                    return None
        elif isinstance(timestamp_value, (int, float)):
            return datetime.fromtimestamp(timestamp_value / 1000)
        else:
            return None
    except (ValueError, TypeError, OSError) as e:
        logger.warning(f"⚠️ 타임스탬프 파싱 실패: {timestamp_value}, 오류: {e}")
        return None

def parse_timestamp_with_strptime(timestamp_str: str) -> Optional[datetime]:
    """strptime을 사용한 추가 타임스탬프 파싱"""
    patterns = [
        '%Y-%m-%dT%H:%M:%S.%fGMT',
        '%Y-%m-%dT%H:%M:%S.%fUTC',
        '%Y-%m-%dT%H:%M:%SGMT',
        '%Y-%m-%dT%H:%M:%SUTC',
        '%Y-%m-%dT%H:%M:%S.%f',
        '%Y-%m-%dT%H:%M:%S',
    ]
    
    for pattern in patterns:
        try:
            return datetime.strptime(timestamp_str, pattern)
        except ValueError:
            continue
    return None

@mcp.tool(description="현재 Spark 애플리케이션의 설정을 분석하여 성능 최적화 권장사항을 제시합니다")
def analyze_spark_configuration() -> str:
    """
    Spark 설정 분석 도구
    현재 실행 중인 Spark 애플리케이션의 설정을 조회하고 분석하여 최적화 권장사항을 제시합니다.
    """
    try:
        logger.info("🔧 Spark 설정 분석 시작")
        
        # SparkGod 플러그인의 config API 호출
        config_url = f"{SPARK_UI_URL}/spark-god/config/json/"
        
        logger.info(f"🌐 Spark 설정 조회: {config_url}")
        response = requests.get(config_url, timeout=10)
        
        if response.status_code == 200:
            config_data = response.json()
            spark_props = config_data.get('sparkProperties', {})
            
            if not spark_props:
                return "❌ Spark 설정 정보를 찾을 수 없습니다."
            
            logger.info(f"📊 설정 항목 수집: {len(spark_props)}개")
            
            # 설정 분석 및 권장사항 생성
            analysis_result = analyze_spark_settings(spark_props)
            
            logger.info("✅ Spark 설정 분석 완료")
            return analysis_result
            
        else:
            # History Server를 통한 대체 시도
            logger.warning(f"⚠️ SparkGod API 호출 실패: HTTP {response.status_code}")
            return try_history_server_config()
            
    except requests.exceptions.ConnectionError:
        logger.warning("⚠️ Spark UI 연결 실패, History Server 시도")
        return try_history_server_config()
    except Exception as e:
        logger.error(f"❌ Spark 설정 분석 중 오류: {e}", exc_info=True)
        return f"❌ Spark 설정 분석 중 오류 발생: {str(e)}"

def try_history_server_config() -> str:
    """History Server를 통한 설정 조회"""
    try:
        # 현재 애플리케이션 정보 가져오기
        app_info = get_current_spark_application()
        if not app_info:
            return "❌ 현재 실행 중인 Spark 애플리케이션을 찾을 수 없습니다."
        
        app_id = app_info['id']
        env_url = f"{SPARK_UI_URL}/api/v1/applications/{app_id}/environment"
        
        response = requests.get(env_url, timeout=10)
        if response.status_code == 200:
            env_data = response.json()
            spark_props = {}
            
            # sparkProperties 추출
            for prop in env_data.get('sparkProperties', []):
                if len(prop) >= 2:
                    spark_props[prop[0]] = prop[1]
            
            if spark_props:
                return analyze_spark_settings(spark_props)
            else:
                return "❌ Spark 설정 정보를 찾을 수 없습니다."
        else:
            return f"❌ History Server API 호출 실패: HTTP {response.status_code}"
            
    except Exception as e:
        return f"❌ History Server를 통한 설정 조회 실패: {str(e)}"

def analyze_spark_settings(spark_props: Dict[str, str]) -> str:
    """Spark 설정 분석 및 권장사항 생성"""
    analysis_sections = []
    
    # 1. 메모리 설정 분석
    memory_analysis = analyze_memory_settings(spark_props)
    if memory_analysis:
        analysis_sections.append(f"💾 **메모리 설정 분석**:\n{memory_analysis}")
    
    # 2. 성능 설정 분석
    performance_analysis = analyze_performance_settings(spark_props)
    if performance_analysis:
        analysis_sections.append(f"⚡ **성능 설정 분석**:\n{performance_analysis}")
    
    # 3. 리소스 관리 분석
    resource_analysis = analyze_resource_settings(spark_props)
    if resource_analysis:
        analysis_sections.append(f"🎯 **리소스 관리 분석**:\n{resource_analysis}")
    
    # 4. 최적화 권장사항
    recommendations = generate_optimization_recommendations(spark_props)
    if recommendations:
        analysis_sections.append(f"💡 **최적화 권장사항**:\n{recommendations}")
    
    return "\n\n".join(analysis_sections) if analysis_sections else "✅ 현재 설정이 적절합니다."

def analyze_memory_settings(spark_props: Dict[str, str]) -> str:
    """메모리 관련 설정 분석"""
    issues = []
    
    executor_memory = spark_props.get('spark.executor.memory', 'N/A')
    driver_memory = spark_props.get('spark.driver.memory', 'N/A')
    
    issues.append(f"  📊 Executor 메모리: {executor_memory}")
    issues.append(f"  📊 Driver 메모리: {driver_memory}")
    
    # 메모리 크기 검증
    if executor_memory != 'N/A' and 'g' in executor_memory.lower():
        try:
            exec_mem_gb = float(executor_memory.lower().replace('g', ''))
            if exec_mem_gb < 1:
                issues.append(f"  ⚠️ Executor 메모리가 너무 작습니다 ({executor_memory}). 최소 1GB 권장")
        except ValueError:
            pass
    
    # Off-heap 메모리
    offheap_enabled = spark_props.get('spark.memory.offHeap.enabled', 'false')
    if offheap_enabled.lower() == 'true':
        offheap_size = spark_props.get('spark.memory.offHeap.size', 'N/A')
        issues.append(f"  ✅ Off-heap 메모리 활성화: {offheap_size}")
    else:
        issues.append(f"  💡 Off-heap 메모리 비활성화 (대용량 데이터 처리 시 활성화 고려)")
    
    return "\n".join(issues)

def analyze_performance_settings(spark_props: Dict[str, str]) -> str:
    """성능 관련 설정 분석"""
    issues = []
    
    # 병렬 처리 설정
    executor_cores = spark_props.get('spark.executor.cores', 'N/A')
    default_parallelism = spark_props.get('spark.default.parallelism', 'N/A')
    
    issues.append(f"  ⚡ Executor 코어: {executor_cores}개")
    issues.append(f"  🔄 기본 병렬도: {default_parallelism}")
    
    # SQL 최적화 설정
    adaptive_enabled = spark_props.get('spark.sql.adaptive.enabled', 'false')
    if adaptive_enabled.lower() == 'true':
        issues.append(f"  ✅ Adaptive Query Execution 활성화")
    else:
        issues.append(f"  💡 Adaptive Query Execution 비활성화 (성능 향상을 위해 활성화 권장)")
    
    # 동적 할당
    dynamic_allocation = spark_props.get('spark.dynamicAllocation.enabled', 'false')
    if dynamic_allocation.lower() == 'true':
        min_executors = spark_props.get('spark.dynamicAllocation.minExecutors', '0')
        max_executors = spark_props.get('spark.dynamicAllocation.maxExecutors', 'infinity')
        issues.append(f"  🔄 동적 할당 활성화: Min={min_executors}, Max={max_executors}")
    
    return "\n".join(issues)

def analyze_resource_settings(spark_props: Dict[str, str]) -> str:
    """리소스 관리 설정 분석"""
    issues = []
    
    # 마스터 모드
    master = spark_props.get('spark.master', 'N/A')
    issues.append(f"  🎯 마스터 모드: {master}")
    
    # Executor 인스턴스
    executor_instances = spark_props.get('spark.executor.instances', 'N/A')
    if executor_instances != 'N/A':
        issues.append(f"  📊 Executor 인스턴스: {executor_instances}개")
    
    return "\n".join(issues)

def generate_optimization_recommendations(spark_props: Dict[str, str]) -> str:
    """최적화 권장사항 생성"""
    recommendations = []
    
    # 메모리 관련 권장사항
    executor_memory = spark_props.get('spark.executor.memory', '')
    if 'g' in executor_memory.lower():
        try:
            exec_mem_gb = float(executor_memory.lower().replace('g', ''))
            if exec_mem_gb < 2:
                recommendations.append("💾 Executor 메모리를 최소 2GB 이상으로 증가 권장")
        except ValueError:
            pass
    
    # AQE 권장사항
    adaptive_enabled = spark_props.get('spark.sql.adaptive.enabled', 'false')
    if adaptive_enabled.lower() != 'true':
        recommendations.append("⚡ Adaptive Query Execution 활성화 권장 (spark.sql.adaptive.enabled=true)")
    
    # Off-heap 권장사항
    offheap_enabled = spark_props.get('spark.memory.offHeap.enabled', 'false')
    if offheap_enabled.lower() != 'true':
        recommendations.append("💾 대용량 데이터 처리 시 Off-heap 메모리 활성화 고려")
    
    # 동적 할당 권장사항
    dynamic_allocation = spark_props.get('spark.dynamicAllocation.enabled', 'false')
    if dynamic_allocation.lower() != 'true':
        recommendations.append("🔄 리소스 효율성을 위해 동적 할당 활성화 고려")
    
    return "\n".join([f"  {rec}" for rec in recommendations]) if recommendations else "✅ 현재 설정이 적절합니다"

@mcp.tool(description="Spark 로그에서 중요한 오류와 경고만 수집하여 LLM 분석을 위한 구조화된 데이터를 제공합니다")
def collect_critical_spark_logs() -> str:
    """
    중요한 Spark 로그 수집 도구
    INFO, DEBUG 등을 제외하고 ERROR, WARN, FATAL 등 중요한 로그만 수집하여
    LLM이 분석할 수 있는 구조화된 형태로 제공합니다.
    """
    try:
        logger.info("🚨 중요한 Spark 로그 수집 시작")
        
        # 현재 애플리케이션 정보 가져오기
        app_info = get_current_spark_application()
        if not app_info:
            return "❌ 현재 실행 중인 Spark 애플리케이션을 찾을 수 없습니다."
        
        app_id = app_info['id']
        app_name = app_info['name']
        
        logger.info(f"🎯 로그 수집 대상: {app_name} (ID: {app_id})")
        
        # Spark UI에서 실시간 로그 수집
        critical_logs = collect_critical_logs_from_spark_ui(app_id)
        
        # Spark UI 메트릭 수집
        ui_metrics = collect_basic_ui_metrics(app_id)
        
        # LLM 분석용 구조화된 출력 생성
        structured_output = format_critical_logs_for_llm(
            app_id, app_name, critical_logs, ui_metrics
        )
        
        logger.info("✅ 중요한 Spark 로그 수집 완료")
        return structured_output
        
    except Exception as e:
        logger.error(f"❌ 중요한 로그 수집 중 오류: {e}", exc_info=True)
        return f"❌ 중요한 로그 수집 중 오류 발생: {str(e)}"

def get_current_spark_application() -> Optional[Dict[str, Any]]:
    """현재 실행 중인 Spark 애플리케이션 정보 가져오기"""
    try:
        apps_url = f"{SPARK_UI_URL}/api/v1/applications"
        response = requests.get(apps_url, timeout=10)
        
        if response.status_code == 200:
            apps = response.json()
            if apps:
                current_app = apps[0]
                app_id = current_app['id']
                app_name = current_app['name']
                
                logger.info(f"✅ 현재 애플리케이션: {app_name} (ID: {app_id})")
                
                return {
                    'id': app_id,
                    'name': app_name,
                    'attempts': current_app.get('attempts', [])
                }
        
        return None
        
    except Exception as e:
        logger.error(f"❌ 애플리케이션 정보 조회 실패: {e}")
        return None

def find_application_log_file(app_id: str, app_name: str) -> Optional[str]:
    """애플리케이션 로그 파일 찾기"""
    try:
        # Event log 디렉토리에서 검색
        search_patterns = [
            f"/tmp/spark-events/{app_id}*",
            f"/tmp/spark-events/app-*{app_id[-8:]}*",  # 마지막 8자리로 검색
            f"/var/log/spark/{app_id}*",
            f"./spark-events/{app_id}*"
        ]
        
        for pattern in search_patterns:
            files = glob.glob(pattern)
            if files:
                # 가장 최근 파일 선택
                latest_file = max(files, key=os.path.getmtime)
                logger.info(f"📁 로그 파일 발견: {latest_file}")
                return latest_file
        
        logger.warning("⚠️ 로그 파일을 찾을 수 없습니다")
        return None
        
    except Exception as e:
        logger.error(f"❌ 로그 파일 검색 실패: {e}")
        return None

def collect_critical_logs_from_spark_ui(app_id: str) -> Dict[str, Any]:
    """Spark UI에서 실시간 중요한 로그 수집"""
    try:
        logger.info(f"🌐 Spark UI에서 실시간 로그 수집: {app_id}")
        
        critical_entries = {
            'ERROR': [],
            'WARN': [],
            'FATAL': [],
            'summary': {
                'total_lines_scanned': 0,
                'critical_lines_found': 0,
                'sources_checked': []
            }
        }
        
        # Spark UI는 8081 포트 사용 (실제 환경에 맞춤)
        port = 8081
        
        # API 대신 직접 알려진 Executor들의 로그 페이지에 접근
        # 일반적으로 Executor 0 (driver)와 다른 Executor들이 있음
        known_executors = ['0', 'driver']  # 일반적인 Executor ID들
        
        logger.info(f"🔍 알려진 Executor들의 로그 직접 수집: {known_executors}")
        
        for executor_id in known_executors:
            logger.info(f"🔍 Executor {executor_id} 로그 수집 시작")
            
            # stderr 로그 수집 (ERROR, WARN이 주로 여기에)
            logger.info(f"📋 Executor {executor_id} stderr 로그 수집 중...")
            stderr_logs = collect_executor_logs(port, app_id, executor_id, 'stderr')
            if stderr_logs:
                critical_entries = merge_log_entries(critical_entries, stderr_logs)
                critical_entries['summary']['sources_checked'].append(f"executor-{executor_id}-stderr")
                logger.info(f"✅ Executor {executor_id} stderr 로그 수집 완료")
            else:
                logger.info(f"❌ Executor {executor_id} stderr 로그 수집 실패")
            
            # stdout 로그도 확인 (일부 WARN이 여기에 있을 수 있음)
            logger.info(f"📋 Executor {executor_id} stdout 로그 수집 중...")
            stdout_logs = collect_executor_logs(port, app_id, executor_id, 'stdout')
            if stdout_logs:
                critical_entries = merge_log_entries(critical_entries, stdout_logs)
                critical_entries['summary']['sources_checked'].append(f"executor-{executor_id}-stdout")
                logger.info(f"✅ Executor {executor_id} stdout 로그 수집 완료")
            else:
                logger.info(f"❌ Executor {executor_id} stdout 로그 수집 실패")
        
        if not critical_entries['summary']['sources_checked']:
            logger.warning("⚠️ Spark UI에서 로그 수집 실패, 로컬 파일 시도")
            logger.info(f"📊 수집 시도한 소스: {critical_entries['summary']['sources_checked']}")
            return collect_critical_log_entries_fallback(app_id)
        else:
            logger.info(f"📊 성공적으로 수집한 소스: {critical_entries['summary']['sources_checked']}")
        
        logger.info(f"📊 UI 로그 수집 완료: ERROR {len(critical_entries['ERROR'])}개, WARN {len(critical_entries['WARN'])}개, FATAL {len(critical_entries['FATAL'])}개")
        
        # 수집된 로그 요약 출력
        total_critical = len(critical_entries['ERROR']) + len(critical_entries['WARN']) + len(critical_entries['FATAL'])
        if total_critical > 0:
            logger.info(f"📋 수집된 중요 로그 요약 (총 {total_critical}개):")
            
            # ERROR 로그 요약
            if critical_entries['ERROR']:
                logger.info(f"  🚨 ERROR 로그 {len(critical_entries['ERROR'])}개:")
                for i, error in enumerate(critical_entries['ERROR'][:3], 1):
                    logger.info(f"    {i}. [{error['source']}] {error['content'][:60]}...")
                if len(critical_entries['ERROR']) > 3:
                    logger.info(f"    ... 및 {len(critical_entries['ERROR']) - 3}개 더")
            
            # WARN 로그 요약
            if critical_entries['WARN']:
                logger.info(f"  ⚠️ WARN 로그 {len(critical_entries['WARN'])}개:")
                for i, warn in enumerate(critical_entries['WARN'][:3], 1):
                    logger.info(f"    {i}. [{warn['source']}] {warn['content'][:60]}...")
                if len(critical_entries['WARN']) > 3:
                    logger.info(f"    ... 및 {len(critical_entries['WARN']) - 3}개 더")
            
            # FATAL 로그 요약
            if critical_entries['FATAL']:
                logger.info(f"  💀 FATAL 로그 {len(critical_entries['FATAL'])}개:")
                for i, fatal in enumerate(critical_entries['FATAL'][:3], 1):
                    logger.info(f"    {i}. [{fatal['source']}] {fatal['content'][:60]}...")
                if len(critical_entries['FATAL']) > 3:
                    logger.info(f"    ... 및 {len(critical_entries['FATAL']) - 3}개 더")
        else:
            logger.info("✅ 중요한 로그(ERROR/WARN/FATAL)가 발견되지 않았습니다")
        
        return critical_entries
        
    except Exception as e:
        logger.error(f"❌ Spark UI 로그 수집 실패: {e}")
        return {'error': str(e)}

def collect_executor_logs(port: int, app_id: str, executor_id: str, log_type: str) -> Dict[str, Any]:
    """특정 Executor의 로그 수집"""
    try:
        # Spark UI 로그 페이지 URL
        log_url = f"http://localhost:{port}/logPage/?appId={app_id}&executorId={executor_id}&logType={log_type}"
        
        logger.info(f"📋 로그 수집: {log_url}")
        response = requests.get(log_url, timeout=10)
        
        if response.status_code == 200:
            log_content = response.text
            
            # HTML에서 실제 로그 내용 추출
            log_lines = extract_log_lines_from_html(log_content)
            
            if log_lines:
                return parse_critical_logs_from_lines(log_lines, f"{executor_id}-{log_type}")
            else:
                logger.warning(f"⚠️ {executor_id} {log_type} 로그에서 내용을 추출할 수 없음")
                return None
        else:
            logger.warning(f"⚠️ {executor_id} {log_type} 로그 페이지 접근 실패: HTTP {response.status_code}")
            return None
            
    except Exception as e:
        logger.warning(f"⚠️ {executor_id} {log_type} 로그 수집 실패: {e}")
        return None

def extract_log_lines_from_html(html_content: str) -> List[str]:
    """HTML에서 실제 로그 라인 추출"""
    try:
        # Spark UI 로그 페이지는 보통 <pre> 태그 안에 로그가 있음
        import re
        
        # <pre> 태그 내용 추출
        pre_pattern = r'<pre[^>]*>(.*?)</pre>'
        pre_matches = re.findall(pre_pattern, html_content, re.DOTALL | re.IGNORECASE)
        
        if pre_matches:
            # 가장 큰 <pre> 블록을 로그로 간주
            log_content = max(pre_matches, key=len)
            
            # HTML 엔티티 디코딩
            import html
            log_content = html.unescape(log_content)
            
            # 라인별로 분할
            lines = log_content.strip().split('\n')
            
            # 빈 라인 제거
            lines = [line.strip() for line in lines if line.strip()]
            
            logger.info(f"📄 HTML에서 {len(lines)}줄 추출")
            
            # 추출된 로그 라인들을 출력 (처음 10줄만)
            logger.info("📋 추출된 로그 라인 샘플:")
            for i, line in enumerate(lines[:10], 1):
                logger.info(f"  {i:2d}. {line[:80]}{'...' if len(line) > 80 else ''}")
            if len(lines) > 10:
                logger.info(f"  ... 및 {len(lines) - 10}줄 더")
            
            return lines
        else:
            # <pre> 태그가 없으면 전체 텍스트에서 로그 패턴 찾기
            lines = html_content.split('\n')
            log_lines = []
            
            for line in lines:
                # 일반적인 로그 패턴 (타임스탬프 포함)
                if re.search(r'\d{2}/\d{2}/\d{2} \d{2}:\d{2}:\d{2}', line) or \
                   re.search(r'\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}', line) or \
                   any(level in line for level in ['ERROR', 'WARN', 'FATAL', 'INFO']):
                    log_lines.append(line.strip())
            
            logger.info(f"📄 HTML에서 로그 패턴으로 {len(log_lines)}줄 추출")
            
            # 추출된 로그 라인들을 출력 (처음 10줄만)
            if log_lines:
                logger.info("📋 패턴 매칭된 로그 라인 샘플:")
                for i, line in enumerate(log_lines[:10], 1):
                    logger.info(f"  {i:2d}. {line[:80]}{'...' if len(line) > 80 else ''}")
                if len(log_lines) > 10:
                    logger.info(f"  ... 및 {len(log_lines) - 10}줄 더")
            
            return log_lines
            
    except Exception as e:
        logger.error(f"❌ HTML 로그 추출 실패: {e}")
        return []

def parse_critical_logs_from_lines(log_lines: List[str], source: str) -> Dict[str, Any]:
    """로그 라인에서 중요한 로그만 파싱"""
    critical_entries = {
        'ERROR': [],
        'WARN': [],
        'FATAL': [],
        'summary': {
            'total_lines_scanned': len(log_lines),
            'critical_lines_found': 0,
            'source': source
        }
    }
    
    for i, line in enumerate(log_lines):
        line = line.strip()
        if not line:
            continue
        
        # ERROR 로그
        if ' ERROR ' in line or line.startswith('ERROR'):
            critical_entries['ERROR'].append({
                'line_number': i + 1,
                'content': line,
                'timestamp': extract_timestamp(line),
                'source': source
            })
            critical_entries['summary']['critical_lines_found'] += 1
            logger.info(f"🚨 ERROR 로그 수집 [{source}:{i+1}]: {line[:100]}{'...' if len(line) > 100 else ''}")
        
        # WARN 로그
        elif ' WARN ' in line or line.startswith('WARN'):
            critical_entries['WARN'].append({
                'line_number': i + 1,
                'content': line,
                'timestamp': extract_timestamp(line),
                'source': source
            })
            critical_entries['summary']['critical_lines_found'] += 1
            logger.info(f"⚠️ WARN 로그 수집 [{source}:{i+1}]: {line[:100]}{'...' if len(line) > 100 else ''}")
        
        # FATAL 로그
        elif ' FATAL ' in line or line.startswith('FATAL'):
            critical_entries['FATAL'].append({
                'line_number': i + 1,
                'content': line,
                'timestamp': extract_timestamp(line),
                'source': source
            })
            critical_entries['summary']['critical_lines_found'] += 1
            logger.info(f"💀 FATAL 로그 수집 [{source}:{i+1}]: {line[:100]}{'...' if len(line) > 100 else ''}")
    
    logger.info(f"📊 {source}에서 중요한 로그 파싱: ERROR {len(critical_entries['ERROR'])}개, WARN {len(critical_entries['WARN'])}개, FATAL {len(critical_entries['FATAL'])}개")
    
    return critical_entries

def merge_log_entries(main_entries: Dict[str, Any], new_entries: Dict[str, Any]) -> Dict[str, Any]:
    """여러 소스의 로그 엔트리 병합"""
    if not new_entries:
        return main_entries
    
    for log_level in ['ERROR', 'WARN', 'FATAL']:
        if log_level in new_entries:
            main_entries[log_level].extend(new_entries[log_level])
    
    # 요약 정보 업데이트
    if 'summary' in new_entries:
        main_entries['summary']['total_lines_scanned'] += new_entries['summary'].get('total_lines_scanned', 0)
        main_entries['summary']['critical_lines_found'] += new_entries['summary'].get('critical_lines_found', 0)
    
    return main_entries

def collect_critical_log_entries_fallback(app_id: str) -> Dict[str, Any]:
    """로컬 파일에서 로그 수집 (fallback)"""
    try:
        log_file = find_application_log_file(app_id, "fallback")
        if log_file:
            return collect_critical_log_entries(log_file)
        else:
            return {
                'ERROR': [],
                'WARN': [],
                'FATAL': [],
                'summary': {
                    'total_lines_scanned': 0,
                    'critical_lines_found': 0,
                    'error': 'No log sources available'
                }
            }
    except Exception as e:
        logger.error(f"❌ Fallback 로그 수집 실패: {e}")
        return {'error': str(e)}

def collect_critical_log_entries(log_file_path: str) -> Dict[str, Any]:
    """중요한 로그 엔트리만 수집 (ERROR, WARN, FATAL)"""
    try:
        critical_entries = {
            'ERROR': [],
            'WARN': [],
            'FATAL': [],
            'summary': {
                'total_lines_scanned': 0,
                'critical_lines_found': 0,
                'file_size_mb': 0
            }
        }
        
        file_size = os.path.getsize(log_file_path)
        critical_entries['summary']['file_size_mb'] = file_size / (1024 * 1024)
        
        with open(log_file_path, 'r', encoding='utf-8', errors='ignore') as f:
            lines = f.readlines()
        
        critical_entries['summary']['total_lines_scanned'] = len(lines)
        
        # 중요한 로그 레벨만 필터링
        for i, line in enumerate(lines):
            line = line.strip()
            if not line:
                continue
            
            # ERROR 로그
            if ' ERROR ' in line:
                critical_entries['ERROR'].append({
                    'line_number': i + 1,
                    'content': line,
                    'timestamp': extract_timestamp(line)
                })
                critical_entries['summary']['critical_lines_found'] += 1
            
            # WARN 로그
            elif ' WARN ' in line:
                critical_entries['WARN'].append({
                    'line_number': i + 1,
                    'content': line,
                    'timestamp': extract_timestamp(line)
                })
                critical_entries['summary']['critical_lines_found'] += 1
            
            # FATAL 로그
            elif ' FATAL ' in line:
                critical_entries['FATAL'].append({
                    'line_number': i + 1,
                    'content': line,
                    'timestamp': extract_timestamp(line)
                })
                critical_entries['summary']['critical_lines_found'] += 1
        
        logger.info(f"📊 중요한 로그 수집 완료: ERROR {len(critical_entries['ERROR'])}개, WARN {len(critical_entries['WARN'])}개, FATAL {len(critical_entries['FATAL'])}개")
        
        return critical_entries
        
    except Exception as e:
        logger.error(f"❌ 중요한 로그 수집 실패: {e}")
        return {'error': str(e)}

def extract_timestamp(log_line: str) -> Optional[str]:
    """로그 라인에서 타임스탬프 추출"""
    # 일반적인 Spark 로그 타임스탬프 패턴
    patterns = [
        r'(\d{2}/\d{2}/\d{2} \d{2}:\d{2}:\d{2})',  # 24/08/17 17:50:03
        r'(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})',  # 2024-08-17 17:50:03
        r'(\d{2}:\d{2}:\d{2})'  # 17:50:03
    ]
    
    for pattern in patterns:
        match = re.search(pattern, log_line)
        if match:
            return match.group(1)
    
    return None

def collect_basic_ui_metrics(app_id: str) -> Dict[str, Any]:
    """기본적인 Spark UI 메트릭 수집"""
    metrics = {}
    
    endpoints = {
        'jobs': f"/api/v1/applications/{app_id}/jobs",
        'stages': f"/api/v1/applications/{app_id}/stages",
        'executors': f"/api/v1/applications/{app_id}/executors"
    }
    
    for metric_name, endpoint in endpoints.items():
        try:
            url = f"{SPARK_UI_URL}{endpoint}"
            response = requests.get(url, timeout=5)
            
            if response.status_code == 200:
                data = response.json()
                
                if metric_name == 'jobs':
                    failed_jobs = [j for j in data if j.get('status') == 'FAILED']
                    metrics['failed_jobs_count'] = len(failed_jobs)
                    metrics['total_jobs_count'] = len(data)
                
                elif metric_name == 'stages':
                    failed_stages = [s for s in data if s.get('status') == 'FAILED']
                    metrics['failed_stages_count'] = len(failed_stages)
                    metrics['total_stages_count'] = len(data)
                
                elif metric_name == 'executors':
                    active_executors = [e for e in data if e.get('isActive', True)]
                    metrics['active_executors_count'] = len(active_executors)
                    metrics['total_executors_count'] = len(data)
            
        except Exception as e:
            logger.warning(f"⚠️ {metric_name} 메트릭 수집 실패: {e}")
    
    return metrics

def format_critical_logs_for_llm(app_id: str, app_name: str, critical_logs: Dict[str, Any], ui_metrics: Dict[str, Any]) -> str:
    """LLM 분석을 위한 중요한 로그 포맷팅"""
    output_parts = []
    
    # 헤더
    output_parts.append("# Spark 애플리케이션 중요 로그 분석")
    output_parts.append(f"**애플리케이션**: {app_name} (ID: {app_id})")
    output_parts.append(f"**분석 시간**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    output_parts.append("")
    
    # 분석 요청
    output_parts.append("## 🎯 분석 요청")
    output_parts.append("다음 Spark 애플리케이션의 중요한 로그를 분석하여 다음을 제공해주세요:")
    output_parts.append("1. **오류 및 경고의 근본 원인** 분석")
    output_parts.append("2. **성능에 미치는 영향** 평가")
    output_parts.append("3. **구체적인 해결 방안** 제시")
    output_parts.append("4. **예방을 위한 설정 권장사항** 제공")
    output_parts.append("")
    
    # 요약 정보
    if 'summary' in critical_logs:
        summary = critical_logs['summary']
        output_parts.append("## 📊 로그 요약")
        output_parts.append(f"- **파일 크기**: {summary.get('file_size_mb', 0):.1f} MB")
        output_parts.append(f"- **전체 스캔 라인**: {summary.get('total_lines_scanned', 0):,}줄")
        output_parts.append(f"- **중요 로그 발견**: {summary.get('critical_lines_found', 0):,}줄")
        output_parts.append("")
    
    # UI 메트릭
    if ui_metrics:
        output_parts.append("## 📈 애플리케이션 메트릭")
        if 'total_jobs_count' in ui_metrics:
            output_parts.append(f"- **Jobs**: 총 {ui_metrics['total_jobs_count']}개 (실패: {ui_metrics.get('failed_jobs_count', 0)}개)")
        if 'total_stages_count' in ui_metrics:
            output_parts.append(f"- **Stages**: 총 {ui_metrics['total_stages_count']}개 (실패: {ui_metrics.get('failed_stages_count', 0)}개)")
        if 'total_executors_count' in ui_metrics:
            output_parts.append(f"- **Executors**: 총 {ui_metrics['total_executors_count']}개 (활성: {ui_metrics.get('active_executors_count', 0)}개)")
        output_parts.append("")
    
    # 중요한 로그들
    for log_level in ['FATAL', 'ERROR', 'WARN']:
        if log_level in critical_logs and critical_logs[log_level]:
            entries = critical_logs[log_level]
            output_parts.append(f"## 🚨 {log_level} 로그 ({len(entries)}개)")
            
            # 최대 20개까지만 표시
            for entry in entries[:20]:
                timestamp = entry.get('timestamp', 'N/A')
                line_num = entry.get('line_number', 'N/A')
                content = entry.get('content', '')
                
                output_parts.append(f"**라인 {line_num}** ({timestamp}):")
                output_parts.append(f"```")
                output_parts.append(content)
                output_parts.append(f"```")
                output_parts.append("")
            
            if len(entries) > 20:
                output_parts.append(f"... 및 {len(entries) - 20}개 추가 {log_level} 로그")
                output_parts.append("")
    
    # 분석 가이드
    output_parts.append("## 💡 분석 가이드")
    output_parts.append("### 주요 확인 사항")
    output_parts.append("- OutOfMemoryError, GC 관련 이슈")
    output_parts.append("- Task 실패 및 재시도 패턴")
    output_parts.append("- 네트워크 및 셔플 관련 오류")
    output_parts.append("- 직렬화 및 클래스 로딩 문제")
    output_parts.append("- 리소스 부족 및 할당 이슈")
    output_parts.append("")
    output_parts.append("---")
    output_parts.append("**참고**: 위 중요한 로그들을 바탕으로 Spark 애플리케이션의 문제점을 분석하고 구체적인 해결 방안을 제시해주세요.")
    
    return "\n".join(output_parts)

if __name__ == "__main__":
    logger.info("🚀 SparkGod MCP Server (Simplified) 시작")
    logger.info(f"🌐 Spark UI URL: {SPARK_UI_URL}")
    logger.info("🔧 사용 가능한 도구 (2개):")
    logger.info("  1. analyze_spark_configuration() - Spark 설정 분석 및 최적화 권장사항")
    logger.info("  2. collect_critical_spark_logs() - 중요한 로그 수집 및 LLM 분석 요청")
    logger.info("📝 디버그 로그 파일: spark_god_mcp_debug.log")
    logger.info("=" * 50)
    
    try:
        mcp.run()
    except Exception as e:
        logger.error(f"❌ MCP 서버 실행 중 오류: {e}", exc_info=True)
        raise