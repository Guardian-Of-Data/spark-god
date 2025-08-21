// ===== BEDROCK SERVICE (COMMENTED OUT) =====
// This service has been replaced with Strand Agents Service
// Keeping the code for reference

/*
import { BedrockRuntimeClient, ConverseCommand } from '@aws-sdk/client-bedrock-runtime';
import { sparkConfigService } from './sparkConfigService';

// AWS Bedrock 클라이언트 설정
const bedrockClient = new BedrockRuntimeClient({
    region: import.meta.env.VITE_AWS_REGION || 'us-east-1',
    credentials: {
        accessKeyId: import.meta.env.VITE_AWS_ACCESS_KEY_ID || '',
        secretAccessKey: import.meta.env.VITE_AWS_SECRET_ACCESS_KEY || '',
    },
});

export interface ChatMessage {
    role: 'user' | 'assistant';
    content: string;
}

export class BedrockService {
    private modelId: string;

    constructor(modelId: string = 'us.anthropic.claude-3-5-sonnet-20241022-v2:0') {
        this.modelId = modelId;
    }

    async sendMessage(messages: ChatMessage[]): Promise<string> {
        try {
            // Spark configuration 정보 가져오기
            const sparkConfig = await sparkConfigService.getSparkConfiguration();
            
            // 기본 system prompt
            let systemPrompt = `당신은 Apache Spark 전문가인 SparkGod AI 어시스턴트입니다. 
            
주요 역할:
- Apache Spark 관련 모든 질문에 전문적이고 정확한 답변 제공
- 성능 최적화, 오류 해결, 아키텍처 설계 등 실무적인 조언
- 한국어로 친근하고 이해��기 쉽게 설명
- 코드 예제와 구체적인 해결책 제시

답변 스타일:
- 전문적이지만 친근한 톤
- 구체적인 예시와 코드 포함
- 단계별 해결 방법 제시
- 이모지 적절히 사용하여 가독성 향상`;

            // Spark configuration이 있으면 system prompt에 추가
            if (sparkConfig) {
                const configContext = sparkConfigService.generateSystemPromptContext(sparkConfig);
                systemPrompt += `\n\n${configContext}`;
            }

            // Converse API용 메시지 포맷
            const converseMessages = messages.map(msg => ({
                role: msg.role,
                content: [{ text: msg.content }]
            }));

            const command = new ConverseCommand({
                modelId: this.modelId,
                messages: converseMessages,
                inferenceConfig: {
                    maxTokens: 2000,
                    temperature: 0.7,
                    topP: 0.9
                },
                system: [
                    {
                        text: systemPrompt
                    }
                ]
            });

            const response = await bedrockClient.send(command);

            if (response.output?.message?.content?.[0]?.text) {
                return response.output.message.content[0].text;
            } else {
                return '죄송합니다. 응답을 생성할 수 없습니다.';
            }
        } catch (error) {
            console.error('Bedrock API Error:', error);
            return 'AWS Bedrock 연결에 문제가 있습니다. AWS 자격증명과 권한을 확인해주세요.';
        }
    }
}

// 싱글톤 인스턴스
export const bedrockService = new BedrockService();
*/

// ===== TEMPORARY EXPORT FOR COMPATIBILITY =====
// Export types and dummy service to maintain compatibility
// export interface ChatMessage {
//     role: 'user' | 'assistant';
//     content: string;
// }

// // Dummy service that throws error if accidentally used
// export const bedrockService = {
//     sendMessage: async (messages: ChatMessage[]): Promise<string> => {
//         throw new Error('Bedrock service is disabled. Use Strand Agents service instead.');
//     }
// };