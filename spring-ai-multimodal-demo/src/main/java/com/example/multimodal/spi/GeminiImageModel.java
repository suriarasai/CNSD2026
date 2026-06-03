package com.example.multimodal.spi;

import com.example.multimodal.gemini.GeminiClient;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageMessage;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A Gemini-backed implementation of Spring AI's portable {@link ImageModel}.
 *
 * <p>Spring AI 1.1.x ships {@code ImageModel} adapters for OpenAI, Azure OpenAI,
 * Stability, ZhiPuAI and QianFan — but <strong>not</strong> Google. Implementing
 * the interface here is the core "extensibility" demonstration: application code
 * (and the {@code /api/image} controller) depends only on the framework's
 * {@code ImageModel} abstraction and is oblivious to the fact that Gemini sits
 * behind it. Swapping in {@code OpenAiImageModel} later would not touch callers.
 */
@Component
public class GeminiImageModel implements ImageModel {

    private final GeminiClient gemini;

    public GeminiImageModel(GeminiClient gemini) {
        this.gemini = gemini;
    }

    @Override
    public ImageResponse call(ImagePrompt request) {
        String prompt = request.getInstructions().stream()
                .map(ImageMessage::getText)
                .collect(Collectors.joining(" "))
                .strip();

        GeminiClient.InlineData inline = gemini.generateImage(prompt);
        String b64Json = Base64.getEncoder().encodeToString(inline.data());

        // Spring AI's Image carries either a URL or base64 JSON payload.
        Image image = new Image(null, b64Json);
        return new ImageResponse(List.of(new ImageGeneration(image)));
    }
}
