package com.weatherweargpt.service;

import com.weatherweargpt.entity.Dialogue;
import com.weatherweargpt.entity.UserEntity;
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
import org.json.JSONObject;

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
    private final WeatherDataService weatherDataService;

    @Autowired
    public GPTService(RestTemplate restTemplate, UserRepository userRepository, DialogueRepository dialogueRepository, WeatherDataService weatherDataService) {
        this.restTemplate = restTemplate;
        this.userRepository = userRepository;
        this.dialogueRepository = dialogueRepository;
        this.weatherDataService = weatherDataService;
        logger.info("GPTService initialized with API URL: {}", apiUrl);
    }

    // 초기 질문 생성
    public String generateInitialQuestion() {
        logger.info("Generating initial question");
        return "외출할 계획이 있으신가요?";
    }

    // 사용자 응답 처리 및 목적지 확인
    public String processUserResponse(Long userId, String userResponse) {
        logger.info("Processing user response for userId: {}", userId);
        try {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 긍정적 응답 시 목적지를 물어보기
            if (userResponse.toLowerCase().contains("yes") || userResponse.toLowerCase().contains("네") || userResponse.contains("응")) {
                Dialogue latestDialogue = dialogueRepository.findTopByUserEntityUserIdOrderByConversationDateDesc(userId);

                if (latestDialogue != null && latestDialogue.getDestination() != null) {
                    return "추가 옷 추천을 원하시나요?";
                }

                logger.debug("사용자가 외출할 계획이 있으며, 목적지를 물어보는 중.");
                return "목적지가 어디인가요?";
            }

            // 사용자가 'no'라고 응답한 경우
            if (userResponse.toLowerCase().contains("no") || userResponse.contains("아니")) {
                return "감사합니다! 더 많은 추천이 필요하면 언제든지 물어보세요!\n\n" +
                        generateInitialQuestion();
            }

            // 사용자 응답에서 지명을 추출하기 위해 GPT 호출
            String extractedDestination = extractDestinationUsingGPT(userResponse);

            if (extractedDestination == null || extractedDestination.isEmpty()) {
                return "죄송합니다, 목적지를 이해하지 못했습니다. 명확하게 입력해주세요.";
            }

            // 사용자가 목적지를 입력하면 대화에 저장
            Dialogue latestDialogue = dialogueRepository.findTopByUserEntityUserIdOrderByConversationDateDesc(userId);
            if (latestDialogue != null && latestDialogue.getDestination() == null) {
                latestDialogue.setDestination(extractedDestination); // 추출된 목적지 저장
                dialogueRepository.save(latestDialogue);     // 저장된 대화 업데이트
                logger.debug("목적지가 저장됨: {}", extractedDestination);

                // 목적지와 날씨 정보를 포함하여 옷 추천 프롬포트 생성
                return generateOutfitPrompt(user, extractedDestination);
            }

            return "죄송합니다, 요청을 처리할 수 없습니다.";
        } catch (Exception e) {
            logger.error("사용자 응답 처리 중 오류 발생", e);
            return "죄송합니다, 응답 처리 중 오류가 발생했습니다.";
        }
    }

    // GPT API를 통한 목적지 추출 메서드
    private String extractDestinationUsingGPT(String userResponse) {
        logger.info("GPT API를 호출하여 목적지를 추출합니다.");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        // GPT에게 보내는 프롬프트 (목적지 추출 요청)
        String prompt = String.format("다음 문장에서 목적지를 추출해 주세요: \"%s\". 오직 지명만 반환해 주세요.", userResponse);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "당신은 문장에서 지명을 추출하는 어시스턴트입니다. 오직 지명만 반환하세요."),
                Map.of("role", "user", "content", prompt)
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            logger.debug("GPT API로 목적지 추출 요청을 보냅니다: {}", prompt);
            Map response = restTemplate.postForObject(apiUrl, request, Map.class);
            logger.debug("GPT API로부터 응답을 받았습니다: {}", response);

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) choice.get("message");
                    String content = message.get("content").trim();
                    logger.info("GPT API로부터 성공적으로 목적지를 추출했습니다: {}", content);
                    return content;  // 추출된 지명 반환
                }
            }
            logger.warn("GPT API의 응답 구조가 예상과 다릅니다.");
        } catch (RestClientException e) {
            logger.error("목적지 추출을 위한 GPT API 호출 중 오류 발생", e);
        } catch (Exception e) {
            logger.error("GPT API 호출 중 예상치 못한 오류 발생", e);
        }

        return null;  // GPT가 응답을 생성하지 못한 경우
    }

    // 옷 추천 프롬프트 생성
    private String generateOutfitPrompt(UserEntity user, String destination) {
        logger.debug("목적지에 따른 옷 추천 프롬프트 생성 중: {}", destination);
        try {
            // 날씨 API를 호출하여 목적지의 날씨 정보 가져오기
            String locationKey = weatherDataService.getLocationKey(destination);
            JSONObject currentWeather = weatherDataService.getCurrentWeather(locationKey);

            // 날씨 정보를 추출 (null 또는 비정상적인 데이터를 대비한 체크 추가)
            if (currentWeather == null || !currentWeather.has("WeatherText") || !currentWeather.has("Temperature")) {
                logger.error("오류: 목적지의 날씨 데이터를 가져올 수 없습니다: {}", destination);
                return "죄송합니다, 해당 목적지의 날씨 정보를 가져올 수 없습니다.";
            }

            String weatherText = currentWeather.getString("WeatherText");
            double temperature = currentWeather.getJSONObject("Temperature").getJSONObject("Metric").getDouble("Value");
            int humidity = currentWeather.getInt("RelativeHumidity");
            double windSpeed = currentWeather.getJSONObject("Wind").getJSONObject("Speed").getJSONObject("Metric").getDouble("Value");

            // 프롬프트 생성: 사용자 성별, 키, 몸무게, 목적지, 날씨 정보 포함
            String prompt = String.format(
                    "사용자는 %s로 갑니다. 그곳의 날씨는 %s이고, 온도는 %.1f°C, 습도는 %d%%, 바람 속도는 %.1f km/h입니다. 적절한 옷차림을 추천해 주세요. 사용자는 %s이며, 키는 %d cm, 몸무게는 %d kg입니다.",
                    destination, weatherText, temperature, humidity, windSpeed, user.getGender(), user.getHeight(), user.getWeight()
            );

            logger.debug("날씨 정보를 포함한 옷차림 프롬프트가 생성되었습니다: {}", prompt);

            // GPT API 호출로 옷 추천 받기
            String outfitRecommendation = callGPTAPI(prompt);

            if (outfitRecommendation == null || outfitRecommendation.isEmpty()) {
                logger.error("오류: GPT API가 옷 추천에 대한 빈 응답을 반환했습니다.");
                return "죄송합니다, 옷 추천을 생성할 수 없습니다.";
            }

            // 옷 추천 후 "더 추천을 원하시나요?" 물어보기
            String followUpQuestion = "추가 옷 추천을 원하시나요?";

            return outfitRecommendation + "\n\n" + followUpQuestion;

        } catch (Exception e) {
            logger.error("날씨 데이터를 사용한 옷 추천 프롬프트 생성 중 오류 발생", e);
            return "죄송합니다, 옷 추천 생성 중 오류가 발생했습니다.";
        }
    }

    // GPT API 호출 메서드
    private String callGPTAPI(String prompt) {
        logger.info("GPT API를 호출하여 옷차림 추천을 생성합니다.");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "당신은 옷차림을 추천하는 어시스턴트입니다."),
                Map.of("role", "user", "content", prompt)
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            logger.debug("GPT API로 옷차림 추천 요청을 보냅니다: {}", prompt);
            Map response = restTemplate.postForObject(apiUrl, request, Map.class);
            logger.debug("GPT API로부터 응답을 받았습니다: {}", response);

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) choice.get("message");
                    String content = message.get("content");
                    logger.info("GPT API로부터 성공적으로 옷차림 추천을 받았습니다.");
                    return content;
                }
            }
            logger.warn("GPT API의 응답 구조가 예상과 다릅니다.");
        } catch (RestClientException e) {
            logger.error("GPT API 호출 중 오류 발생", e);
        } catch (Exception e) {
            logger.error("GPT API 호출 중 예상치 못한 오류 발생", e);
        }

        return "죄송합니다, 응답을 생성할 수 없습니다.";
    }
}
