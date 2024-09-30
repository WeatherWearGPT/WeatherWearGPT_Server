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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

            // 1. 목적지 질문 상태 확인
            if (isDestinationQuestion(latestDialogue)) {
                return handleDestinationResponse(user, userResponse, latestDialogue);
            }

            // 2. 날짜 질문 상태 확인
            if (isDateQuestion(latestDialogue)) {
                return handleDateResponse(user, userResponse, latestDialogue);
            }

            // 3. "추가 옷 추천을 원하시나요?" 질문 상태일 경우
            if (latestDialogue.getQuestionAsked() != null && latestDialogue.getQuestionAsked().contains("추가 옷 추천을 원하시나요?")) {
                // 응답이 긍정인지/부정인지 판단
                boolean isPositiveResponse = checkPositiveResponse(userResponse);
                if (isPositiveResponse) {
                    // 긍정 응답일 경우 2번 루프로 이동 (목적지 확인 단계)
                    return "목적지가 어디인가요? 날씨 정보를 정확히 제공하기 위해 가능한 구체적인 지역명을 알려주세요. " +
                            "예: 성남시, 수원시, 서울, 부산, 경기도, 강원도 등. 시/군/구 단위가 가장 좋습니다.";
                } else {
                    // 부정 응답일 경우 대화 종료
                    return "감사합니다! 더 많은 추천이 필요하면 언제든지 물어보세요!";
                }
            }

            // 4. 외출 여부 질문 상태일 경우 처리
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

    private boolean isDateQuestion(Dialogue dialogue) {
        return dialogue != null && dialogue.getQuestionAsked() != null &&
                dialogue.getQuestionAsked().contains("며칠에 갈 예정인가요?");
    }

    private String handleDestinationResponse(UserEntity user, String userResponse, Dialogue latestDialogue) {
        logger.info("Attempting to extract destination");
        String extractedDestination = extractDestinationUsingGPT(userResponse);
        if (extractedDestination != null && !extractedDestination.isEmpty()) {
            latestDialogue.setDestination(extractedDestination);
            dialogueRepository.save(latestDialogue);
            logger.info("Destination saved: {}", extractedDestination);
            return "며칠에 갈 예정인가요? (예: 2024년 10월 02일(YYYY년 MM월 DD일))";
        } else {
            logger.warn("Failed to extract destination from: {}", userResponse);
            return "죄송합니다, 목적지를 이해하지 못했습니다. 다시 한 번 명확하게 입력해주세요. 계속 진행하시려면 아무 응답이나 해주세요.";
        }
    }

    private String handleDateResponse(UserEntity user, String userResponse, Dialogue latestDialogue) {
        logger.info("Attempting to extract date");
        String extractedDate = extractDateUsingGPT(userResponse);

        if (extractedDate != null && !extractedDate.isEmpty()) {
            String destination = getLastKnownDestination(user.getUserId());
            return generateOutfitPrompt(user, destination, extractedDate);
        } else {
            logger.warn("Failed to extract date from: {}", userResponse);
            return "죄송합니다, 날짜를 이해하지 못했습니다. 다시 한 번 명확하게 입력해주세요. (예: 2024년 10월 02일(YYYY년 MM월 DD일) 계속 진행하시려면 아무 응답이나 해주세요.)";
        }
    }

    private String handlePositiveResponse(Dialogue latestDialogue) {
        logger.debug("사용자가 외출할 계획이 있으며, 목적지를 물어보는 중.");
        return "목적지가 어디인가요? 날씨 정보를 정확히 제공하기 위해 가능한 구체적인 지역명을 알려주세요. " +
                "예: 성남시, 수원시, 서울, 부산, 경기도, 강원도 등. 시/군/구 단위가 가장 좋습니다.";
    }

    private String handleNegativeResponse() {
        return "감사합니다! 더 많은 추천이 필요하면 언제든지 물어보세요!\n\n" + generateInitialQuestion();
    }

    private String getLastKnownDestination(Long userId) {
        Optional<Dialogue> lastDialogueWithDestination = dialogueRepository
                .findTopByUserEntityUserIdAndDestinationIsNotNullOrderByConversationDateDesc(userId);

        return lastDialogueWithDestination.map(Dialogue::getDestination)
                .orElse(null);
    }

    private String generateOutfitPrompt(UserEntity user, String destination, String date) {
        logger.debug("목적지와 날짜에 따른 옷 추천 프롬프트 생성 중: {}, {}", destination, date);
        try {
            if (destination == null || destination.isEmpty()) {
                logger.error("목적지 정보가 없습니다.");
                return "죄송합니다, 목적지 정보가 없어 옷 추천을 할 수 없습니다. 목적지를 다시 입력해 주세요.";
            }

            LocalDate targetDate;
            try {
                targetDate = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                logger.error("날짜 파싱 중 오류 발생: {}", e.getMessage());
                return "죄송합니다, 입력된 날짜 형식이 올바르지 않습니다. 'YYYY-MM-DD' 형식으로 다시 입력해 주세요.";
            }

            String locationKey = weatherDataService.getLocationKey(destination);
            JSONObject weatherData;
            try {
                weatherData = weatherDataService.getWeatherForDate(locationKey, targetDate);
            } catch (RuntimeException e) {
                logger.error("날씨 데이터 조회 중 오류 발생: {}", e.getMessage());
                return "죄송합니다, 해당 날짜의 날씨 정보를 가져오는 데 실패했습니다. 다른 날짜를 입력해 주세요.";
            }

            if (weatherData == null || !weatherData.has("Day") || !weatherData.has("Temperature")) {
                logger.error("오류: 목적지의 날씨 데이터를 가져올 수 없습니다: {}", destination);
                return "죄송합니다, 해당 목적지의 날씨 정보를 가져올 수 없습니다. 다른 목적지를 입력해 주세요.";
            }

            WeatherEntity weatherEntity = createWeatherEntity(user, weatherData, targetDate);
            weatherService.saveWeather(weatherEntity);

            String prompt = createWeatherPrompt(destination, weatherEntity, user, targetDate);
            Map response = callGPTAPI(createGPTRequestBody("당신은 옷차림을 추천하는 어시스턴트입니다.", prompt));
            String outfitRecommendation = extractContentFromGPTResponse(response);

            if (outfitRecommendation == null || outfitRecommendation.isEmpty()) {
                logger.error("오류: GPT API가 옷 추천에 대한 빈 응답을 반환했습니다.");
                return "죄송합니다, 옷 추천을 생성할 수 없습니다. 다시 시도해 주세요.";
            }

            return outfitRecommendation + "\n\n추가 옷 추천을 원하시나요?";
        } catch (Exception e) {
            logger.error("날씨 데이터를 사용한 옷 추천 프롬프트 생성 중 오류 발생", e);
            return "죄송합니다, 옷 추천 생성 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
    private WeatherEntity createWeatherEntity(UserEntity user, JSONObject weatherData, LocalDate targetDate) {
        WeatherEntity weatherEntity = new WeatherEntity();
        weatherEntity.setUserEntity(user);
        weatherEntity.setWeatherText(weatherData.getJSONObject("Day").getString("IconPhrase"));
        weatherEntity.setTemperature(weatherData.getJSONObject("Temperature").getJSONObject("Maximum").getDouble("Value"));

        // RelativeHumidity 처리 수정
        JSONObject dayForecast = weatherData.getJSONObject("Day");
        if (dayForecast.has("RelativeHumidity")) {
            Object humidityValue = dayForecast.get("RelativeHumidity");
            if (humidityValue instanceof Integer) {
                weatherEntity.setRelativeHumidity((Integer) humidityValue);
            } else if (humidityValue instanceof Double) {
                weatherEntity.setRelativeHumidity(((Double) humidityValue).intValue());
            } else {
                weatherEntity.setRelativeHumidity(0); // 기본값 설정 또는 다른 처리 방법 선택
                logger.warn("RelativeHumidity is neither Integer nor Double: {}", humidityValue);
            }
        } else {
            weatherEntity.setRelativeHumidity(0); // RelativeHumidity 필드가 없는 경우 기본값 설정
            logger.warn("RelativeHumidity field is missing in the weather data");
        }

        weatherEntity.setWind(weatherData.getJSONObject("Day").getJSONObject("Wind").getJSONObject("Speed").getDouble("Value"));
        weatherEntity.setHasPrecipitation(weatherData.getJSONObject("Day").getBoolean("HasPrecipitation"));
        weatherEntity.setWeatherDate(targetDate.atStartOfDay());
        return weatherEntity;
    }

    private String createWeatherPrompt(String destination, WeatherEntity weather, UserEntity user, LocalDate targetDate) {
        // 화씨에서 섭씨로 변환
        double temperatureInCelsius = (weather.getTemperature() - 32) * 5.0 / 9.0;

        return String.format(
                "사용자는 %s에 %s로 갑니다. 그곳의 날씨는 %s이고, 최고 기온은 %.1f°C, 습도는 %d%%, 바람 속도는 %.1f km/h입니다. " +
                        "강수 여부는 %s입니다. 사용자는 %s이며, 키는 %d cm, 몸무게는 %d kg입니다. " +
                        "옷차림을 추천해 주세요. 색상 조합과 계절에 어울리는 코디를 고려해 주세요. " +
                        "상의, 하의, 신발, 모자와 같은 주요 아이템뿐만 아니라 가방, 시계, 안경, 목걸이와 같은 악세사리도 포함해 주세요. " +
                        "가능하다면, 실제 존재하는 브랜드 (예: 나이키, 아디다스, 유니클로, 자라, 구찌 등) 의 제품을 추천해 주세요.",
                targetDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")),
                destination, weather.getWeatherText(), temperatureInCelsius, weather.getRelativeHumidity(),
                weather.getWind(), weather.isHasPrecipitation() ? "있음" : "없음",
                user.getGender(), user.getHeight(), user.getWeight()
        );

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

    private String extractDateUsingGPT(String userResponse) {
        logger.info("Calling GPT API to extract date. User response: {}", userResponse);
        String prompt = String.format(
                "다음 문장에서 날짜를 추출해 주세요: \"%s\". " +
                        "날짜는 'YYYY-MM-DD' 형식으로 변환해 주세요. " +
                        "만약 날짜를 추출할 수 없다면 null을 반환해 주세요. " +
                        "JSON 형식으로 'date' 키에 해당 값을 담아 응답해 주세요.",
                userResponse
        );

        Map<String, Object> requestBody = createGPTRequestBody(
                "당신은 문장에서 날짜를 추출하는 어시스턴트입니다. JSON 형식으로만 응답하세요.",
                prompt
        );

        try {
            Map response = callGPTAPI(requestBody);
            String content = extractContentFromGPTResponse(response);
            JSONObject jsonObject = new JSONObject(content);

            return jsonObject.optString("date", null);
        } catch (Exception e) {
            logger.error("Error occurred while calling GPT API for date extraction", e);
            return null;
        }
    }

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