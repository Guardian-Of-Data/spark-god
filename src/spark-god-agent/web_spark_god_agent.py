#!/usr/bin/env python3

from strands import Agent
from strands.tools.mcp.mcp_client import MCPClient
from mcp.client.streamable_http import streamablehttp_client
from mcp import stdio_client, StdioServerParameters
from flask import Flask, request, jsonify
from flask_cors import CORS
import os
import sys
from dotenv import load_dotenv

# 환경 변수 로드
load_dotenv()

app = Flask(__name__)
CORS(app)  # React 앱에서 접근할 수 있도록 CORS 설정

# JSON 응답에서 한국어가 제대로 표시되도록 설정
app.config['JSON_AS_ASCII'] = False
app.config['JSONIFY_PRETTYPRINT_REGULAR'] = True

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

# 전역 변수
spark_god_agent = None
mcp_client = None

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

def initialize_spark_god_agent():
    """SparkGod AI 어시스턴트 초기화"""
    global spark_god_agent, mcp_client
    
    try:
        print("🚀 SparkGod AI 어시스턴트를 초기화합니다...")
        
        # MCP 전송 방식 선택 (환경 변수로 제어)
        use_http = os.getenv('USE_HTTP_MCP', 'false').lower() == 'true'
        
        if use_http:
            print("📡 HTTP MCP 클라이언트를 사용합니다...")
            mcp_client = MCPClient(create_streamable_http_transport)
        else:
            print("📡 STDIO MCP 클라이언트를 사용합니다...")
            mcp_client = MCPClient(create_stdio_transport)
        
        # MCP 클라이언트를 컨텍스트 매니저로 사용하여 초기화
        with mcp_client:
            # MCP 서버에서 사용 가능한 도구 목록 가져오기
            available_tools = mcp_client.list_tools_sync()
            print(f"✅ 사용 가능한 MCP 도구: {len(available_tools)}개")
            
            # 도구 목록 출력 (속성 접근 방식 수정)
            for i, tool in enumerate(available_tools):
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
                tools=available_tools,
                model=model_name
            )
        
        print("✅ SparkGod AI 어시스턴트가 준비되었습니다!")
        return True
        
    except Exception as e:
        print(f"❌ SparkGod AI 어시스턴트 초기화 실패: {e}")
        import traceback
        print("상세 오류 정보:")
        traceback.print_exc()
        return False

@app.route('/chat', methods=['POST'])
def chat():
    """채팅 API 엔드포인트 - Strands Agent를 통해 MCP 도구를 사용하여 프롬프트 처리"""
    global spark_god_agent, mcp_client
    
    try:
        data = request.get_json()
        user_message = data.get('message', '')
        
        if not user_message:
            import json
            from flask import Response
            result = {
                'success': False,
                'error': '메시지가 비어있습니다.'
            }
            return Response(
                json.dumps(result, ensure_ascii=False, indent=2),
                mimetype='application/json; charset=utf-8',
                status=400
            )
        
        if not spark_god_agent or not mcp_client:
            import json
            from flask import Response
            result = {
                'success': False,
                'error': 'SparkGod AI 어시스턴트가 초기화되지 않았습니다.'
            }
            return Response(
                json.dumps(result, ensure_ascii=False, indent=2),
                mimetype='application/json; charset=utf-8',
                status=500
            )
        
        print(f"📝 질문 받음: {user_message}")
        
        # Strands Agent 실행 - MCP 도구를 자동으로 사용하여 답변 생성
        with mcp_client:
            print("🤖 SparkGod AI가 MCP 도구를 사용하여 답변을 생성 중입니다...")
            try:
                response = spark_god_agent(user_message)
                print("✅ 답변 생성 완료")
                
                # 응답이 문자열이 아닌 경우 처리
                if not isinstance(response, str):
                    print(f"⚠️ 응답 타입: {type(response)}")
                    response = str(response)
                    
            except Exception as agent_error:
                print(f"❌ Agent 실행 오류: {agent_error}")
                import traceback
                traceback.print_exc()
                response = f"죄송합니다. 답변 생성 중 오류가 발생했습니다: {str(agent_error)}"
        
        # 응답을 안전하게 JSON으로 직렬화
        try:
            # 응답이 문자열인지 확인하고, 아니면 문자열로 변환
            if hasattr(response, 'content'):
                response_text = response.content
            elif hasattr(response, 'text'):
                response_text = response.text
            else:
                response_text = str(response)
                
            # 한국어가 제대로 표시되도록 Response 객체 직접 생성
            import json
            from flask import Response
            
            result = {
                'success': True,
                'response': response_text
            }
            
            return Response(
                json.dumps(result, ensure_ascii=False, indent=2),
                mimetype='application/json; charset=utf-8'
            )
            
        except Exception as json_error:
            print(f"❌ JSON 직렬화 오류: {json_error}")
            import json
            from flask import Response
            
            result = {
                'success': True,
                'response': str(response)  # 강제로 문자열 변환
            }
            
            return Response(
                json.dumps(result, ensure_ascii=False, indent=2),
                mimetype='application/json; charset=utf-8'
            )
        
    except Exception as e:
        print(f"❌ 채팅 처리 중 오류: {e}")
        import traceback
        print("상세 오류 정보:")
        traceback.print_exc()
        
        # JSON 직렬화 오류인 경우 특별 처리
        import json
        from flask import Response
        
        if "not JSON serializable" in str(e):
            result = {
                'success': False,
                'error': 'Agent 응답을 처리하는 중 오류가 발생했습니다. 응답 형식을 확인해주세요.'
            }
        else:
            result = {
                'success': False,
                'error': f'오류가 발생했습니다: {str(e)}'
            }
            
        return Response(
            json.dumps(result, ensure_ascii=False, indent=2),
            mimetype='application/json; charset=utf-8',
            status=500
        )

def main():
    """메인 실행 함수"""
    print("🌐 SparkGod AI 어시스턴트 웹 서버를 시작합니다...")
    
    # 에이전트 초기화
    if not initialize_spark_god_agent():
        print("❌ 에이전트 초기화에 실패했습니다.")
        print("다음 사항을 확인해주세요:")
        print("1. MCP 서버 경로가 올바른지 확인")
        print("2. Python 의존성이 설치되었는지 확인")
        print("3. 인터넷 연결 상태 확인")
        sys.exit(1)
    
    print("🌐 웹 서버를 시작합니다...")
    print("📱 React 앱에서 http://localhost:8001/chat 으로 접속 가능합니다!")
    
    # Flask 앱 실행
    port = int(os.getenv('AGENT_PORT', 8001))
    print(f"🌐 서버 포트: {port}")
    app.run(host='0.0.0.0', port=port, debug=False, threaded=True)

if __name__ == "__main__":
    main()