package com.example.multimodal.spi;

/**
 * A local Text-to-Speech contract that deliberately mirrors the shape of every
 * Spring AI model: a {@code call(prompt)} method plus request/response wrappers
 * with a {@code getResult().getOutput()} accessor.
 *
 * <p>Spring AI <em>does</em> ship a TTS abstraction
 * ({@code org.springframework.ai.audio.tts.TextToSpeechModel}), but only with
 * OpenAI and ElevenLabs adapters. We define a tiny, dependency-free equivalent
 * here so the demo is a single self-contained illustration of the pattern. In a
 * production codebase you would implement Spring AI's {@code TextToSpeechModel}
 * directly, exactly as {@link GeminiImageModel} implements {@code ImageModel}
 * and {@link GeminiTranscriptionModel} implements {@code TranscriptionModel}.
 */
public interface SpeechModel {

    SpeechResponse call(SpeechPrompt prompt);

    /** Convenience: synthesize using the configured default voice. */
    default byte[] call(String text) {
        return call(new SpeechPrompt(text, null)).getResult().getOutput();
    }

    /** Request: text to speak and an optional voice override (null = default). */
    record SpeechPrompt(String text, String voice) {
    }

    /** A single synthesized audio result, as ready-to-play WAV bytes. */
    record Speech(byte[] audio) {
        public byte[] getOutput() {
            return audio;
        }
    }

    /** Response wrapper, paralleling Spring AI's {@code ...Response} types. */
    record SpeechResponse(Speech result) {
        public Speech getResult() {
            return result;
        }
    }
}
