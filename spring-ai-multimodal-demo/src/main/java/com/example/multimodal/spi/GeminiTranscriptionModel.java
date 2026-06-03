package com.example.multimodal.spi;

import com.example.multimodal.gemini.GeminiClient;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Map;

/**
 * A Gemini-backed implementation of Spring AI's portable {@link TranscriptionModel}
 * (the Speech-to-Text contract).
 *
 * <p>Spring AI ships only OpenAI Whisper and Azure OpenAI Whisper here. Gemini
 * has no dedicated transcription endpoint; instead we lean on its multimodal
 * understanding — the audio is sent as inline data to a normal chat model with a
 * "transcribe this" instruction. Callers still program against the framework's
 * {@code TranscriptionModel} / {@code transcribe(Resource)} API.
 */
@Component
public class GeminiTranscriptionModel implements TranscriptionModel {

    private static final Map<String, String> MIME_BY_EXT = Map.of(
            "wav", "audio/wav",
            "mp3", "audio/mpeg",
            "m4a", "audio/mp4",
            "aac", "audio/aac",
            "ogg", "audio/ogg",
            "opus", "audio/opus",
            "flac", "audio/flac",
            "webm", "audio/webm"
    );

    private final GeminiClient gemini;

    public GeminiTranscriptionModel(GeminiClient gemini) {
        this.gemini = gemini;
    }

    @Override
    public AudioTranscriptionResponse call(AudioTranscriptionPrompt prompt) {
        Resource audio = prompt.getInstructions();
        try {
            byte[] bytes = audio.getContentAsByteArray();
            String mime = mimeFor(audio.getFilename());
            String text = gemini.transcribe(bytes, mime);
            return new AudioTranscriptionResponse(new AudioTranscription(text));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read audio resource", e);
        }
    }

    private String mimeFor(String filename) {
        if (filename != null && filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            String mime = MIME_BY_EXT.get(ext);
            if (mime != null) {
                return mime;
            }
        }
        return "audio/wav";
    }
}
