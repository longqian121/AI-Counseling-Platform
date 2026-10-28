package org.example.aispringboot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {
	@Bean
	public ChatMemory chatMemory() {
		return MessageWindowChatMemory.builder()
				.maxMessages(30)
				.build();
	}

	@Bean
	public ChatClient openAiChatClient(OpenAiChatModel openAiChatModel) {
		return ChatClient.builder(openAiChatModel)
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory()).build())
				.defaultSystem("你是一个专业的心理疏导师，温和耐心，善于倾听，能够提供专业的心理支持和建议")
				.build();
	}

	@Bean
	public ChatClient deepseekChatClient(OpenAiChatModel openAiChatModel) {
		return ChatClient.builder(openAiChatModel)
				.defaultOptions(OpenAiChatOptions.builder().model("deepseek-chat").build())
				.build();
	}

	@Bean
	public ChatClient deepseekReasonerClient(OpenAiChatModel openAiChatModel) {
		return ChatClient.builder(openAiChatModel)
				.defaultOptions(OpenAiChatOptions.builder().model("deepseek-reasoner").build())
				.build();
	}
}
