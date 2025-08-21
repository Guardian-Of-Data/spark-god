export interface ChatMessage {
    role: 'user' | 'assistant';
    content: string;
}

export class StrandAgentsService {
    private serverUrl: string;

    constructor() {
        // 환경 변수에서 서버 URL 가져오기
        this.serverUrl = import.meta.env.VITE_SPARK_GOD_AGENT_SERVER || 'http://localhost:8001';
        console.log('🔗 SparkGod Agent 서버 URL:', this.serverUrl);
    }

    async sendMessage(messages: ChatMessage[]): Promise<string> {
        console.log('🚀 StrandAgentsService.sendMessage 호출됨!');
        console.log('📝 받은 메시지들:', messages);

        try {
            // 마지막 사용자 메시지만 사용 (Strands Agent는 단일 질문 처리)
            const lastUserMessage = messages.filter(msg => msg.role === 'user').pop();
            console.log("🔍 마지막 사용자 메시지:", lastUserMessage);
            if (!lastUserMessage) {
                return '질문을 입력해주세요.';
            }

            const userQuestion = lastUserMessage.content;

            console.log('📤 SparkGod Agent 요청:', {
                url: `${this.serverUrl}/chat`,
                message: userQuestion
            });

            // SparkGod Agent 서버에 요청 전송
            const response = await fetch(`${this.serverUrl}/chat`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    message: userQuestion
                })
            });

            if (!response.ok) {
                throw new Error(`Server error: ${response.status} ${response.statusText}`);
            }

            const data = await response.json();

            console.log('📥 SparkGod Agent 응답:', data);

            if (data.success && data.response) {
                return data.response;
            } else {
                console.error('❌ SparkGod Agent 오류:', data.error);
                return data.error || '죄송합니다. 응답을 생성할 수 없습니다.';
            }

        } catch (error) {
            console.error('Strand Agents Service Error:', error);
            return `SparkGod Agent 서버에 연결할 수 없습니다.

다음을 확인해주세요:
1. SparkGod Agent 서버가 실행 중인지 확인: ${this.serverUrl}
2. Spark 애플리케이션이 실행 중인지 확인: http://localhost:4040

서버 시작 방법:
make agent-server`;
        }
    }
}

export const strandAgentsService = new StrandAgentsService();