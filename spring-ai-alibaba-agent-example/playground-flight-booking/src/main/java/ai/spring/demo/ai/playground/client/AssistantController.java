package ai.spring.demo.ai.playground.client;

import ai.spring.demo.ai.playground.services.CustomerSupportAssistant;
import com.alibaba.cloud.ai.dashscope.agent.DashScopeAgent;
import com.alibaba.cloud.ai.dashscope.agent.DashScopeAgentOptions;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;


@RequestMapping("/api/assistant")
@RestController
public class AssistantController {

//	private final CustomerSupportAssistant agent;

	private DashScopeAgent agent;

	@Value("${spring.ai.dashscope.agent.app-id}")
	private String appId;

	public AssistantController(DashScopeAgentApi dashscopeAgentApi) {
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectNode bizParams = objectMapper.createObjectNode();
		bizParams.put("name", "Alice");
		bizParams.put("age", 30);

		this.agent = new DashScopeAgent(dashscopeAgentApi,
				DashScopeAgentOptions.builder()
						.withSessionId("current_session_id")
						.withIncrementalOutput(true)
						.withHasThoughts(true)
						.withBizParams(bizParams)
						.build());
	}

	@RequestMapping(path="/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<String> chat(@RequestParam(name = "chatId") String chatId,
							 @RequestParam(name = "userMessage") String userMessage) {
		return agent.stream(new Prompt(userMessage, DashScopeAgentOptions.builder().withAppId(appId).build())).map(response -> {
			if (response == null || response.getResult() == null) {
				System.err.println("chat response is null");
				return "chat response is null";
			}

			AssistantMessage app_output = response.getResult().getOutput();
			String content = app_output.getText();

			DashScopeAgentApi.DashScopeAgentResponse.DashScopeAgentResponseOutput output = (DashScopeAgentApi.DashScopeAgentResponse.DashScopeAgentResponseOutput) app_output.getMetadata().get("output");
			List<DashScopeAgentApi.DashScopeAgentResponse.DashScopeAgentResponseOutput.DashScopeAgentResponseOutputDocReference> docReferences = output.docReferences();
			List<DashScopeAgentApi.DashScopeAgentResponse.DashScopeAgentResponseOutput.DashScopeAgentResponseOutputThoughts> thoughts = output.thoughts();

//			logger.info("content:\n{}\n\n", content);
			System.out.print(content);

			return content;
		});
	}

}
