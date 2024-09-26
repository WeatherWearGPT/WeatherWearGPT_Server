package com.weatherweargpt.service;

import com.weatherweargpt.entity.Dialogue;
import com.weatherweargpt.entity.UserEntity;  // 수정된 부분: User → UserEntity
import com.weatherweargpt.repository.DialogueRepository;
import com.weatherweargpt.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GPTService {
    private static final Logger logger = LoggerFactory.getLogger(GPTService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final DialogueRepository dialogueRepository;

    @Autowired
    public GPTService(RestTemplate restTemplate, UserRepository userRepository, DialogueRepository dialogueRepository) {
        this.restTemplate = restTemplate;
        this.userRepository = userRepository;
        this.dialogueRepository = dialogueRepository;
        logger.info("GPTService initialized with API URL: {}", apiUrl);
    }

    // 초기 질문 생성
    public String generateInitialQuestion() {
        logger.info("Generating initial question");
        return "Are you planning to go out?";
    }

    // 사용자 응답 처리 및 목적지 확인
    public String processUserResponse(Long userId, String userResponse) {
        logger.info("Processing user response for userId: {}", userId);
        try {
            UserEntity user = userRepository.findById(userId)  // 수정된 부분: User → UserEntity
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 긍정적 응답 시 목적지를 물어보기
            if (userResponse.toLowerCase().contains("yes") || userResponse.toLowerCase().contains("네")) {
                Dialogue latestDialogue = dialogueRepository.findTopByUserEntityUserIdOrderByConversationDateDesc(userId);  // 수정된 부분: findTopByUserUserId → findTopByUserEntityUserId

                // 이미 옷 추천을 받은 경우, "더 추천을 원하시나요?" 물어보기
                if (latestDialogue != null && latestDialogue.getDestination() != null) {
                    return "Would you like more outfit recommendations?";
                }

                logger.debug("User is planning to go out, asking for destination.");
                return "Where are you going? Please specify the destination.";
            }

            // 사용자가 'no'라고 응답한 경우, 대화 종료 메시지 후 다시 초기 질문으로 돌아가기
            if (userResponse.toLowerCase().contains("no")) {
                return "Thank you! If you need more recommendations, feel free to ask again!\n\n" +
                        generateInitialQuestion();
            }

            // 사용자가 목적지를 입력하면 대화에 저장
            Dialogue latestDialogue = dialogueRepository.findTopByUserEntityUserIdOrderByConversationDateDesc(userId);  // 수정된 부분: findTopByUserUserId → findTopByUserEntityUserId
            if (latestDialogue != null && latestDialogue.getDestination() == null) {
                latestDialogue.setDestination(userResponse); // 목적지 저장
                dialogueRepository.save(latestDialogue);     // 저장된 대화 업데이트
                logger.debug("Saved destination: {}", userResponse);

                // 목적지를 포함하여 옷 추천 프롬포트 생성
                return generateOutfitPrompt(user, userResponse);
            }

            return "Sorry, I couldn't process your request.";
        } catch (Exception e) {
            logger.error("Error processing user response", e);
            return "Sorry, an error occurred while processing your response.";
        }
    }

    // 옷 추천 프롬프트 생성
    private String generateOutfitPrompt(UserEntity user, String destination) {  // 수정된 부분: User → UserEntity
        // 프롬포트 생성: 사용자 성별, 키, 몸무게, 목적지
        String prompt = String.format(
                "User is going to %s. Please suggest appropriate outfits. The user is %s, with a height of %d cm and weight of %d kg.",
                destination, user.getGender(), user.getHeight(), user.getWeight()
        );

        logger.debug("Generated outfit prompt: {}", prompt);

        // GPT API 호출로 옷 추천 받기
        String outfitRecommendation = callGPTAPI(prompt);

        // 옷 추천 후 "더 추천을 원하시나요?" 물어보기
        String followUpQuestion = "Would you like more outfit recommendations?";

        // 추천 결과와 함께 follow-up 질문 반환
        return outfitRecommendation + "\n\n" + followUpQuestion;
    }

    // GPT API 호출 메서드
    private String callGPTAPI(String prompt) {
        logger.info("Calling GPT API");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are a helpful assistant that recommends outfits."),
                Map.of("role", "user", "content", prompt)
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            logger.debug("Sending request to GPT API: {}", request);
            Map response = restTemplate.postForObject(apiUrl, request, Map.class);
            logger.debug("Received response from GPT API: {}", response);

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) choice.get("message");
                    String content = message.get("content");
                    logger.info("Successfully generated response from GPT API");
                    return content;
                }
            }
            logger.warn("Unexpected response structure from GPT API");
        } catch (RestClientException e) {
            logger.error("Error calling GPT API", e);
        } catch (Exception e) {
            logger.error("Unexpected error during API call", e);
        }

        return "Sorry, I couldn't generate a response.";
    }
}
