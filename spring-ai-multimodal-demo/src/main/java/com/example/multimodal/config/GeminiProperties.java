package com.example.multimodal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed configuration, populated from {@code application.yml}, which in
 * turn pulls secrets/overrides from the {@code .env} file (via spring-dotenv).
 *
 * <p>All model ids default to current Gemini models but can be overridden per
 * environment, e.g. {@code gemini.image.model=gemini-3.1-flash-image}.
 */
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    /** API key from Google AI Studio. Sourced from GEMINI_API_KEY in .env. */
    private String apiKey;

    /** Base URL for the Gemini "generativelanguage" REST API. */
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";

    private final Image image = new Image();
    private final Transcription transcription = new Transcription();
    private final Speech speech = new Speech();

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Image getImage() {
        return image;
    }

    public Transcription getTranscription() {
        return transcription;
    }

    public Speech getSpeech() {
        return speech;
    }

    public static class Image {
        /** Gemini "Nano Banana" image model. Returns inline base64 image data. */
        private String model = "gemini-2.5-flash-image";

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public static class Transcription {
        /** Any multimodal Gemini model can transcribe audio passed as inline data. */
        private String model = "gemini-2.5-flash";

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public static class Speech {
        /** Gemini TTS model. Returns 16-bit PCM (L16) audio. */
        private String model = "gemini-2.5-flash-preview-tts";

        /** One of Gemini's 30 prebuilt voices (Kore, Puck, Charon, ...). */
        private String voice = "Kore";

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getVoice() {
            return voice;
        }

        public void setVoice(String voice) {
            this.voice = voice;
        }
    }
}
