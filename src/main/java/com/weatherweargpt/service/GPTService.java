package com.weatherweargpt.service;

import com.weatherweargpt.entity.Dialogue;
import com.weatherweargpt.entity.UserEntity;
import com.weatherweargpt.entity.WeatherEntity;
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

import java.time.LocalDateTime;
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
    private final WeatherService weatherService;

    @Autowired
    public GPTService(RestTemplate restTemplate, UserRepository userRepository, DialogueRepository dialogueRepository,
                      WeatherDataService weatherDataService, WeatherService weatherService) {
        this.restTemplate = restTemplate;
        this.userRepository = userRepository;
        this.dialogueRepository = dialogueRepository;
        this.weatherDataService = weatherDataService;
        this.weatherService = weatherService;
        logger.info("GPTService initialized with API URL: {}", apiUrl);
    }

    public String generateInitialQuestion() {
        logger.info("Generating initial question");
        return "외출할 계획이 있으신가요?";
    }

    public String processUserResponse(Long userId, String userResponse) {
        logger.info("Processing user response for userId: {}, response: {}", userId, userResponse);
        try {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            Dialogue latestDialogue = dialogueRepository.findTopByUserEntityUserIdOrderByConversationDateDesc(userId);
            if (isDestinationQuestion(latestDialogue)) {
                return handleDestinationResponse(user, userResponse, latestDialogue);
            }

            boolean isPositiveResponse = checkPositiveResponse(userResponse);
            logger.info("Is positive response: {}", isPositiveResponse);

            if (isPositiveResponse) {
                return handlePositiveResponse(latestDialogue);
            }

            return handleNegativeResponse();
        } catch (Exception e) {
            logger.error("Error in processUserResponse", e);
            return "죄송합니다, 응답 처리 중 오류가 발생했습니다.";
        }
    }

    private boolean isDestinationQuestion(Dialogue dialogue) {
        return dialogue != null && dialogue.getDestination() == null && dialogue.getQuestionAsked() != null &&
                dialogue.getQuestionAsked().contains("목적지가 어디인가요?");
    }

    private String handleDestinationResponse(UserEntity user, String userResponse, Dialogue latestDialogue) {
        logger.info("Attempting to extract destination");
        String extractedDestination = extractDestinationUsingGPT(userResponse);
        if (extractedDestination != null && !extractedDestination.isEmpty()) {
            latestDialogue.setDestination(extractedDestination);
            dialogueRepository.save(latestDialogue);
            logger.info("Destination saved: {}", extractedDestination);
            return generateOutfitPrompt(user, extractedDestination);
        } else {
            logger.warn("Failed to extract destination from: {}", userResponse);
            return "죄송합니다, 목적지를 이해하지 못했습니다. 다시 한 번 명확하게 입력해주세요.";
        }
    }

    private String handlePositiveResponse(Dialogue latestDialogue) {
        if (latestDialogue != null && latestDialogue.getDestination() != null) {
            return "추가 옷 추천을 원하시나요?";
        }
        logger.debug("사용자가 외출할 계획이 있으며, 목적지를 물어보는 중.");
        return "목적지가 어디인가요? 날씨 정보를 정확히 제공하기 위해 가능한 구체적인 지역명을 알려주세요. " +
                "예: 성남시, 수원시, 서울, 부산, 경기도, 강원도 등. 시/군/구 단위가 가장 좋습니다.";
    }

    private String handleNegativeResponse() {
        return "감사합니다! 더 많은 추천이 필요하면 언제든지 물어보세요!\n\n" + generateInitialQuestion();
    }

// 계속...
private boolean checkPositiveResponse(String userResponse) {
    logger.info("Checking if response is positive: {}", userResponse);
    String prompt = String.format("다음 응답이 긍정적인지 부정적인지 판단해주세요: \"%s\". '긍정' 또는 '부정' 중 하나로만 대답해주세요.", userResponse);

    Map<String, Object> requestBody = createGPTRequestBody(
            "당신은 텍스트의 긍정/부정을 판단하는 어시스턴트입니다. '긍정' 또는 '부정' 중 하나로만 대답하세요.",
            prompt
    );

    try {
        Map response = callGPTAPI(requestBody);
        return extractPositiveResponseFromGPT(response);
    } catch (Exception e) {
        logger.error("GPT API 호출 중 오류 발생", e);
    }

    return false;
}

    private String extractDestinationUsingGPT(String userResponse) {
        logger.info("Calling GPT API to extract destination. User response: {}", userResponse);
        String prompt = createDestinationExtractionPrompt(userResponse);

        Map<String, Object> requestBody = createGPTRequestBody(
                "당신은 문장에서 지명을 추출하는 어시스턴트입니다. 오직 지명만 반환하세요.",
                prompt
        );

        try {
            logger.debug("Sending destination extraction request to GPT API: {}", prompt);
            Map response = callGPTAPI(requestBody);
            logger.debug("Received raw response from GPT API: {}", response);

            String content = extractContentFromGPTResponse(response);
            logger.info("Destination extracted from GPT API: {}", content);

            if (!content.isEmpty()) {
                return validateAndReturnDestination(content);
            } else {
                logger.warn("GPT API returned an empty string.");
            }
        } catch (RestClientException e) {
            logger.error("Error occurred while calling GPT API for destination extraction", e);
        } catch (Exception e) {
            logger.error("Unexpected error occurred during GPT API call", e);
        }

        logger.warn("Unable to extract destination.");
        return null;
    }

    private String createDestinationExtractionPrompt(String userResponse) {
        return String.format(
                "다음 문장에서 날씨를 확인할 수 있는 가장 구체적인 지역명을 추출해 주세요: \"%s\". " +
                        "우선순위는 다음과 같습니다: " +
                        "1. 시/군/구 단위 (예: 성남시, 수원시, 용인시) " +
                        "2. 광역시 (예: 서울, 부산, 인천) " +
                        "3. 도 단위 (예: 경기도, 강원도) " +
                        "가장 구체적인 단위의 지역명 하나만 반환해 주세요. " +
                        "만약 여러 단위가 언급되면 가장 구체적인 것을 선택하세요. " +
                        "예를 들어 '경기도 성남시'가 입력되면 '성남시'만 반환해 주세요.",
                userResponse
        );
    }

    private String generateOutfitPrompt(UserEntity user, String destination) {
        logger.debug("목적지에 따른 옷 추천 프롬프트 생성 중: {}", destination);
        try {
            String locationKey = weatherDataService.getLocationKey(destination);
            JSONObject currentWeather = weatherDataService.getCurrentWeather(locationKey);

            if (currentWeather == null || !currentWeather.has("WeatherText") || !currentWeather.has("Temperature")) {
                logger.error("오류: 목적지의 날씨 데이터를 가져올 수 없습니다: {}", destination);
                return "죄송합니다, 해당 목적지의 날씨 정보를 가져올 수 없습니다.";
            }

            WeatherEntity weatherEntity = createWeatherEntity(user, currentWeather);
            weatherService.saveWeather(weatherEntity);

            String prompt = createWeatherPrompt(destination, weatherEntity, user);
            Map response = callGPTAPI(createGPTRequestBody("당신은 옷차림을 추천하는 어시스턴트입니다.", prompt));
            String outfitRecommendation = extractContentFromGPTResponse(response);

            if (outfitRecommendation == null || outfitRecommendation.isEmpty()) {
                logger.error("오류: GPT API가 옷 추천에 대한 빈 응답을 반환했습니다.");
                return "죄송합니다, 옷 추천을 생성할 수 없습니다.";
            }

            return outfitRecommendation + "\n\n추가 옷 추천을 원하시나요?";
        } catch (Exception e) {
            logger.error("날씨 데이터를 사용한 옷 추천 프롬프트 생성 중 오류 발생", e);
            return "죄송합니다, 옷 추천 생성 중 오류가 발생했습니다.";
        }
    }

// 계속...
private WeatherEntity createWeatherEntity(UserEntity user, JSONObject currentWeather) {
    WeatherEntity weatherEntity = new WeatherEntity();
    weatherEntity.setUserEntity(user);
    weatherEntity.setWeatherText(currentWeather.getString("WeatherText"));
    weatherEntity.setTemperature(currentWeather.getJSONObject("Temperature").getJSONObject("Metric").getDouble("Value"));
    weatherEntity.setRelativeHumidity(currentWeather.getInt("RelativeHumidity"));
    weatherEntity.setWind(currentWeather.getJSONObject("Wind").getJSONObject("Speed").getJSONObject("Metric").getDouble("Value"));
    weatherEntity.setHasPrecipitation(currentWeather.getBoolean("HasPrecipitation"));
    weatherEntity.setWeatherDate(LocalDateTime.now());
    return weatherEntity;
}

    private String createWeatherPrompt(String destination, WeatherEntity weather, UserEntity user) {
        return String.format(
                "사용자는 %s로 갑니다. 그곳의 날씨는 %s이고, 온도는 %.1f°C, 습도는 %d%%, 바람 속도는 %.1f km/h입니다. " +
                        "강수 여부는 %s입니다. 적절한 옷차림을 추천해 주세요. 사용자는 %s이며, 키는 %d cm, 몸무게는 %d kg입니다.",
                destination, weather.getWeatherText(), weather.getTemperature(), weather.getRelativeHumidity(),
                weather.getWind(), weather.isHasPrecipitation() ? "있음" : "없음",
                user.getGender(), user.getHeight(), user.getWeight()
        );
    }

    private Map<String, Object> createGPTRequestBody(String systemContent, String userContent) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemContent),
                Map.of("role", "user", "content", userContent)
        ));
        return requestBody;
    }

    private Map callGPTAPI(Map<String, Object> requestBody) throws RestClientException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        return restTemplate.postForObject(apiUrl, request, Map.class);
    }

    private boolean extractPositiveResponseFromGPT(Map response) {
        String content = extractContentFromGPTResponse(response).toLowerCase();
        if ("긍정".equals(content)) {
            return true;
        } else if ("부정".equals(content)) {
            return false;
        } else {
            logger.warn("GPT API가 예상치 못한 응답을 반환했습니다: {}", content);
            return false;
        }
    }

    private String extractContentFromGPTResponse(Map response) {
        if (response != null && response.containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> choice = choices.get(0);
                Map<String, String> message = (Map<String, String>) choice.get("message");
                return message.get("content").trim();
            }
        }
        logger.warn("GPT API response structure is different from expected.");
        return "";
    }

    private String validateAndReturnDestination(String destination) {
        try {
            String locationKey = weatherDataService.getLocationKey(destination);
            logger.info("LocationKey for extracted destination: {}", locationKey);
            return destination;
        } catch (RuntimeException e) {
            logger.warn("Cannot find locationKey for the extracted location: {}", destination);
            return null;
        }
    }
}