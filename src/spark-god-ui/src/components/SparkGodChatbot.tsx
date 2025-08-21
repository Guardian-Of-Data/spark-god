import React, { useState } from 'react';
import ChatBot from 'react-chatbotify';
import { strandAgentsService } from '../services/strandAgentsService';
import type { ChatMessage } from '../services/strandAgentsService';

// ===== BEDROCK SERVICE IMPORT (COMMENTED OUT) =====
// import { bedrockService } from '../services/bedrockService';
// import type { ChatMessage } from '../services/bedrockService';

const SparkGodChatbot: React.FC = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);

  const flow = {
    start: {
      message: "안녕하세요! SparkGod AI 어시스턴트입니다. Spark 관련 질문이나 도움이 필요한 것이 있으시면 언제든 말씀해주세요! 🚀",
      path: "loop"
    },
    loop: {
      message: async (params: any) => {
        const userMessage = params.userInput;

        if (!userMessage) {
          return "질문을 입력해주세요!";
        }

        // 사용자 메시지를 히스토리에 추가
        const newMessages: ChatMessage[] = [
          ...messages,
          { role: 'user', content: userMessage }
        ];

        try {
          // Strand Agents API 사용
          const response = await strandAgentsService.sendMessage(newMessages);

          // ===== BEDROCK API 호출 (주석 처리) =====
          // const response = await bedrockService.sendMessage(newMessages);

          // 응답을 히스토리에 추가
          const updatedMessages: ChatMessage[] = [
            ...newMessages,
            { role: 'assistant', content: response }
          ];

          setMessages(updatedMessages);
          return response;
        } catch (error) {
          console.error('Chat error:', error);
          return "죄송합니다. 현재 서비스에 문제가 있습니다. 잠시 후 다시 시도해주세요.";
        }
      },
      path: "loop"
    }
  };

  return (
    <div style={{ position: 'fixed', bottom: '20px', right: '20px', zIndex: 1000 }}>
      <ChatBot
        flow={flow}
        settings={{
          general: {
            primaryColor: "#667eea"
          },
          header: {
            title: "SparkGod AI"
          },
          chatInput: {
            enabledPlaceholderText: "Spark에 대해 질문해보세요..."
          },
          botBubble: {
            simulateStream: false
          }
        }}
      />
    </div>
  );
};

export default SparkGodChatbot;