package com.example.multimodal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI multimodal playground.
 *
 * <p>Demonstrates Spring AI's portable model abstractions for three modalities
 * that the framework does NOT ship a Google adapter for, by implementing the
 * interfaces ourselves against the Gemini REST API:
 * <ul>
 *   <li>Text &rarr; Image  ({@code org.springframework.ai.image.ImageModel})</li>
 *   <li>Audio &rarr; Text  ({@code org.springframework.ai.audio.transcription.TranscriptionModel})</li>
 *   <li>Text &rarr; Speech (a local contract mirroring Spring AI's TTS model)</li>
 * </ul>
 */
@SpringBootApplication
public class MultimodalDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultimodalDemoApplication.class, args);
    }
}
