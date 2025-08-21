#!/usr/bin/env python3

from strands import Agent
from strands.tools.mcp.mcp_client import MCPClient
from mcp.client.streamable_http import streamablehttp_client
from mcp import stdio_client, StdioServerParameters
import os
import sys
from dotenv import load_dotenv

# 환경 변수 로드
load_dotenv()

# SparkGod AI 어시스턴트를 위한 시스템 프롬프트
SPARK_GOD_SYSTEM_PROMPT = """
You are SparkGod AI Assistant, an expert in Apache Spark configuration and troubleshooting.

Your main roles:
- Provide professional and accurate answers to all Apache Spark related questions
- Offer practical advice on performance optimization, error resolution, and architecture design
- Analyze current Spark configurations and logs to provide specific recommendations
- Explain in Korean in a friendly and easy-to-understand manner
- Provide code examples and specific solutions

Available MCP Tools:
1. get_spark_troubleshooting_config(): 트러블슈팅에 중요한 Spark 설정 정보를 조회하고 분석
2. analyze_spark_job_logs(): 현재 실행 중인 Spark Job의 모든 로그를 종합 분석

Tool Usage Guidelines:
- 사용자가 Spark 설정이나 구성에 대해 질문하면 get_spark_troubleshooting_config()를 호출하여 핵심 설정 분석
- 성능 문제, 오류, 실패 원인을 찾아달라고 하면 analyze_spark_job_logs()를 사용하여 종합 로그 분석
- 두 도구 모두 트러블슈팅에 특화되어 있으므로 문제 해결에 집중된 답변 제공
- 도구 결과를 바탕으로 구체적인 문제점과 실행 가능한 해결방안 제시

Response style:
- Professional but friendly tone in Korean
- Include specific examples and code when relevant
- Present step-by-step solutions
- Use emojis appropriately to improve readability
- Always reference actual configuration values when available
- Provide actionable recommendations based on real data
"""

def create_streamable_http_transport():
    """Streamable HTTP 전송 생성"""
    return streamablehttp_client("http://localhost:8001/mcp/")

def create_stdio_transport():
    """STDIO 전송 생성 - Python MCP 서버 사용"""
    current_dir = os.path.dirname(os.path.abspath(__file__))
    mcp_server_path = os.path.join(current_dir, "..", "spark-god-mcp-server", "spark_god_mcp_server.py")
    
    return stdio_client(
        StdioServerParameters(
            command="python3", 
            args=[mcp_server_path],
            env={"SPARK_UI_URL": "http://localhost:4040"}
        )
    )

def main():
    """메인 실행 함수"""
    if len(sys.argv) < 2:
        print("사용법: python spark_god_agent.py '질문 내용'")
        print("예시: python spark_god_agent.py 'Spark 애플리케이션의 메모리 설정을 최적화하는 방법을 알려주세요'")
        sys.exit(1)
    
    user_question = " ".join(sys.argv[1:])
    
    print("🚀 SparkGod AI 어시스턴트를 시작합니다...")
    print(f"📝 질문: {user_question}")
    print("=" * 80)
    
    # MCP 전송 방식 선택 (환경 변수로 제어)
    use_http = os.getenv('USE_HTTP_MCP', 'false').lower() == 'true'
    
    if use_http:
        print("📡 HTTP MCP 클라이언트를 사용합니다...")
        mcp_client = MCPClient(create_streamable_http_transport)
    else:
        print("📡 STDIO MCP 클라이언트를 사용합니다...")
        mcp_client = MCPClient(create_stdio_transport)
    
    try:
        # MCP 클라이언트를 컨텍스트 매니저로 사용
        with mcp_client:
            # MCP 서버에서 사용 가능한 도구 목록 가져오기
            tools = mcp_client.list_tools_sync()
            print(f"✅ 사용 가능한 MCP 도구: {len(tools)}개")
            
            # 도구 목록 출력 (속성 접근 방식 수정)
            for i, tool in enumerate(tools):
                try:
                    tool_name = getattr(tool, 'name', f'tool_{i}')
                    tool_desc = getattr(tool, 'description', 'No description')
                    print(f"  - {tool_name}: {tool_desc}")
                except Exception as e:
                    print(f"  - Tool {i}: {type(tool)} (속성 접근 오류: {e})")
            
            # SparkGod 에이전트 생성 - Bedrock 모델 사용
            # 환경 변수로 모델 선택 가능
            model_name = os.getenv('STRANDS_MODEL', 'us.anthropic.claude-3-7-sonnet-20250219-v1:0')
            print(f"🤖 사용 모델: {model_name}")
            
            spark_god_agent = Agent(
                system_prompt=SPARK_GOD_SYSTEM_PROMPT,
                tools=tools,
                model=model_name
            )
            
            # 사용자 질문에 대한 답변 생성
            print("\n🤖 SparkGod AI가 MCP 도구를 사용하여 답변을 생성 중입니다...")
            response = spark_god_agent(user_question)
            
            print("\n🤖 SparkGod AI 답변:")
            print("=" * 80)
            print(response)
            
    except Exception as e:
        print(f"❌ 오류가 발생했습니다: {e}")
        print("다음 사항을 확인해주세요:")
        print("1. Spark 애플리케이션이 실행 중인지 확인 (http://localhost:4040)")
        print("2. MCP 서버가 빌드되었는지 확인 (cd src/spark-god-mcp-server && npm run build)")
        print("3. AWS 자격증명이 설정되었는지 확인")
        sys.exit(1)

if __name__ == "__main__":
    main()