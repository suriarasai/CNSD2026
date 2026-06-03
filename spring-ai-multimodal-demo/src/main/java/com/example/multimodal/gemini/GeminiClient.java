package com.example.multimodal.gemini;

import com.example.multimodal.config.GeminiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Thin client over the Gemini {@code generativelanguage} REST API.
 *
 * <p>It speaks raw JSON (via {@link JsonNode}) so it stays resilient to the
 * frequent additive changes Google makes to response payloads. The three public
 * methods map 1:1 to the three modalities the playground exposes. Each Spring AI
 * SPI implementation (see the {@code spi} package) delegates here.
 */
@Component
public class GeminiClient {

    private final RestClient restClient;
    private final GeminiProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    public GeminiClient(RestClient geminiRestClient, GeminiProperties props) {
        this.restClient = geminiRestClient;
        this.props = props;
    }

    /** A piece of inline binary content returned by Gemini, with its declared MIME type. */
    public record InlineData(String mimeType, byte[] data) {
    }

    // ---------------------------------------------------------------------
    // Text -> Image
    // ---------------------------------------------------------------------

    public InlineData generateImage(String prompt) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                ))
        );
        JsonNode root = post(props.getImage().getModel(), body);
        InlineData inline = firstInlineData(root);
        if (inline == null) {
            throw new GeminiException("No image returned. Model said: " + firstText(root));
        }
        return inline;
    }

    // ---------------------------------------------------------------------
    // Audio -> Text
    // ---------------------------------------------------------------------

    public String transcribe(byte[] audio, String mimeType) {
        String b64 = Base64.getEncoder().encodeToString(audio);
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(
                                Map.of("inlineData", Map.of("mimeType", mimeType, "data", b64)),
                                Map.of("text", "Transcribe this audio verbatim. "
                                        + "Return only the spoken words, with no commentary.")
                        )
                ))
        );
        JsonNode root = post(props.getTranscription().getModel(), body);
        String text = firstText(root);
        if (text == null || text.isBlank()) {
            throw new GeminiException("No transcript returned by the model.");
        }
        return text.strip();
    }

    // ---------------------------------------------------------------------
    // Text -> Speech
    // ---------------------------------------------------------------------

    /** Returns raw PCM (L16) audio plus its MIME type (carries the sample rate). */
    public InlineData synthesizeSpeech(String text, String voice) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", text))
                )),
                "generationConfig", Map.of(
                        "responseModalities", List.of("AUDIO"),
                        "speechConfig", Map.of(
                                "voiceConfig", Map.of(
                                        "prebuiltVoiceConfig", Map.of("voiceName", voice)
                                )
                        )
                )
        );
        JsonNode root = post(props.getSpeech().getModel(), body);
        InlineData inline = firstInlineData(root);
        if (inline == null) {
            throw new GeminiException("No audio returned. Model said: " + firstText(root));
        }
        return inline;
    }

    // ---------------------------------------------------------------------
    // Shared plumbing
    // ---------------------------------------------------------------------

    private JsonNode post(String model, Object body) {
        try {
            String json = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return mapper.readTree(json);
        } catch (GeminiException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiException("Gemini call to '" + model + "' failed: " + e.getMessage(), e);
        }
    }

    /** Walks candidates[0].content.parts[*] and returns the first inlineData block. */
    private InlineData firstInlineData(JsonNode root) {
        for (JsonNode part : parts(root)) {
            JsonNode inline = part.get("inlineData");
            if (inline == null) {
                inline = part.get("inline_data"); // tolerate snake_case
            }
            if (inline != null && inline.hasNonNull("data")) {
                String mime = inline.path("mimeType").asText(inline.path("mime_type").asText("application/octet-stream"));
                byte[] data = Base64.getDecoder().decode(inline.get("data").asText());
                return new InlineData(mime, data);
            }
        }
        return null;
    }

    /** Concatenates any text parts in candidates[0]. */
    private String firstText(JsonNode root) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : parts(root)) {
            if (part.hasNonNull("text")) {
                sb.append(part.get("text").asText());
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private JsonNode parts(JsonNode root) {
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        return parts.isArray() ? parts : mapper.createArrayNode();
    }
}
