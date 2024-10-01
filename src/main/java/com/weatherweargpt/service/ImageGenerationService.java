package com.weatherweargpt.service;

import com.weatherweargpt.entity.Dialogue;
import com.weatherweargpt.entity.OutfitImageEntity;
import com.weatherweargpt.entity.UserEntity;
import com.weatherweargpt.repository.OutfitImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ImageGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(ImageGenerationService.class);

    @Value("${stablediffusion.api.key}")
    private String stableDiffusionApiKey;

    @Value("${stablediffusion.api.url}")
    private String stableDiffusionApiUrl;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.api.url}")
    private String openaiApiUrl;

    @Value("${app.image.upload.path:/home/ubuntu/docker-volume/img/}")
    private String imageUploadPath;

    @Value("${app.image.base.url:http://43.202.86.72/images/}")
    private String imageBaseUrl;

    private final RestTemplate restTemplate;
    private final OutfitImageRepository outfitImageRepository;

    private static final Map<String, String> CATEGORY_MAPPING = new HashMap<>() {{
        put("상의", "Top");
        put("아우터", "Outer");
        put("하의", "Bottom");
        put("신발", "Shoes");
        put("가방", "Bag");
        put("모자", "Hat");
        put("안경", "Glasses");
        put("시계", "Watch");
        put("목걸이", "Necklace");
    }};

    @Autowired
    public ImageGenerationService(RestTemplate restTemplate, OutfitImageRepository outfitImageRepository) {
        this.restTemplate = restTemplate;
        this.outfitImageRepository = outfitImageRepository;
    }

    public String generateAndSaveOutfitImage(Dialogue dialogue, String outfitRecommendation) {
        try {
            logger.info("Received outfit recommendation: {}", outfitRecommendation);
            String prompt = createPromptFromDialogue(dialogue, outfitRecommendation);

            logger.info("Generated Stable Diffusion prompt: {}", prompt);
            String imageUrl = generateOutfitImage(prompt);

            if (imageUrl != null) {
                // 이미지 파일이 실제로 존재하는지 확인
                String imagePath = imageUploadPath + imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                if (Files.exists(Paths.get(imagePath))) {
                    logger.info("Image file confirmed at path: {}", imagePath);

                    // OutfitImageEntity를 데이터베이스에 저장
                    OutfitImageEntity outfitImage = new OutfitImageEntity();
                    outfitImage.setUserEntity(dialogue.getUserEntity());
                    outfitImage.setDialogue(dialogue);
                    outfitImage.setImageUrl(imageUrl);
                    outfitImage.setGenerationPrompt(prompt);

                    outfitImageRepository.save(outfitImage);
                    logger.info("Outfit image generated and saved successfully for dialogue ID: {}. File saved at: {}", dialogue.getDialogId(), imageUrl);
                    return imageUrl;
                } else {
                    logger.error("Image file not found after supposed successful generation at path: {}", imagePath);
                }
            } else {
                logger.error("Failed to generate image for dialogue ID: {}", dialogue.getDialogId());
            }
        } catch (Exception e) {
            logger.error("Error generating outfit image for dialogue ID: {}", dialogue.getDialogId(), e);
        }
        return null;
    }

    private String createPromptFromDialogue(Dialogue dialogue, String outfitRecommendation) {
        UserEntity user = dialogue.getUserEntity();
        String gender = user.getGender().toLowerCase();

        StringBuilder promptBuilder = new StringBuilder("High-quality fashion e-commerce photo of a ");
        promptBuilder.append(gender.equals("male") ? "man" : "woman");
        promptBuilder.append(" in full-body view, posing confidently. Outfit: ");

        Map<String, String> outfitParts = extractOutfitParts(outfitRecommendation);

        for (Map.Entry<String, String> entry : outfitParts.entrySet()) {
            String category = CATEGORY_MAPPING.getOrDefault(entry.getKey(), entry.getKey());
            String description = processOutfitPart(entry.getValue());
            promptBuilder.append(category).append(": ").append(description).append(", ");
        }

        promptBuilder.append("Professional studio lighting, white background, high resolution, photorealistic.");

        return promptBuilder.toString().trim();
    }

    private Map<String, String> extractOutfitParts(String outfitRecommendation) {
        Map<String, String> outfitParts = new HashMap<>();
        List<Pattern> patterns = List.of(
                Pattern.compile("- (\\w+):\\s*([^\\n]+)"),
                Pattern.compile("(\\d+\\.\\s*(?:" + String.join("|", CATEGORY_MAPPING.keySet()) + ")):\\s*([^\\n]+)"),
                Pattern.compile("(" + String.join("|", CATEGORY_MAPPING.keySet()) + "):\\s*([^\\n]+)")
        );

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(outfitRecommendation);
            while (matcher.find()) {
                String key = matcher.group(1).replaceAll("\\d+\\.\\s*", "").trim();
                String value = matcher.group(2).trim();
                outfitParts.put(key, value);
            }
        }

        logger.info("Extracted outfit parts: {}", outfitParts);
        return outfitParts;
    }

    private String processOutfitPart(String koreanText) {
        String prompt = String.format(
                "Translate the following Korean text to English without using any brand names. Describe only the type, color, and style of clothing accurately. Remove specific product names and maintain only generic descriptions suitable for image generation: \"%s\"",
                koreanText
        );

        return callGptApi(prompt, "You are a fashion translator that removes all brand names while translating Korean to English. Make sure the text is concise and only includes relevant information about the clothing.");
    }

    private String callGptApi(String prompt, String systemMessage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + openaiApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");
        requestBody.put("messages", Arrays.asList(
                Map.of("role", "system", "content", systemMessage),
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.3);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            Map response = restTemplate.postForObject(openaiApiUrl, request, Map.class);
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) choice.get("message");
                    return message.get("content").trim();
                }
            }
        } catch (Exception e) {
            logger.error("Error calling GPT API", e);
        }

        return prompt; // 실패 시 원본 반환
    }

    private String generateOutfitImage(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + stableDiffusionApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text_prompts", Collections.singletonList(Map.of("text", prompt)));
        requestBody.put("negative_prompt", "blurry, low quality, distorted, deformed");
        requestBody.put("width", 1024);
        requestBody.put("height", 1024);
        requestBody.put("samples", 1);
        requestBody.put("num_inference_steps", 30);
        requestBody.put("guidance_scale", 7.5);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            Map response = restTemplate.postForObject(stableDiffusionApiUrl, request, Map.class);
            logger.info("Response from Stable Diffusion API: {}", response);

            if (response != null && response.containsKey("artifacts")) {
                List<Map<String, Object>> artifacts = (List<Map<String, Object>>) response.get("artifacts");
                if (!artifacts.isEmpty()) {
                    String base64Image = (String) artifacts.get(0).get("base64");
                    if (base64Image != null) {
                        return saveBase64Image(base64Image);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error calling Stable Diffusion API", e);
        }

        return null;
    }

    private String saveBase64Image(String base64Image) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            // 고유한 파일명 생성 (UUID 사용)
            String fileName = UUID.randomUUID().toString() + ".png";
            String filePath = imageUploadPath + fileName;

            // 디렉토리가 없으면 생성
            Files.createDirectories(Paths.get(imageUploadPath));
            Files.write(Paths.get(filePath), imageBytes);

            logger.info("Successfully saved image at: {}", filePath);
            return imageBaseUrl + fileName;
        } catch (IOException e) {
            logger.error("Failed to save base64 image to file", e);
        }

        return null;
    }
}