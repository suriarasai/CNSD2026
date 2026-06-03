package com.example.multimodal.web;

import com.example.multimodal.spi.SpeechModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * The HTTP surface of the playground. Note that every handler depends only on a
 * Spring AI abstraction ({@link ImageModel}, {@link TranscriptionModel}) or the
 * mirrored {@link SpeechModel} — never on Gemini directly. The provider is an
 * implementation detail injected by Spring.
 */
@RestController
@RequestMapping("/api")
public class MediaController {

    private final ImageModel imageModel;
    private final TranscriptionModel transcriptionModel;
    private final SpeechModel speechModel;

    public MediaController(ImageModel imageModel,
                           TranscriptionModel transcriptionModel,
                           SpeechModel speechModel) {
        this.imageModel = imageModel;
        this.transcriptionModel = transcriptionModel;
        this.speechModel = speechModel;
    }

    // ----- Text -> Image -------------------------------------------------

    public record ImageRequest(String prompt) {
    }

    @PostMapping("/image")
    public Map<String, String> image(@RequestBody ImageRequest req) {
        if (!StringUtils.hasText(req.prompt())) {
            throw new IllegalArgumentException("Prompt must not be empty.");
        }
        var response = imageModel.call(new ImagePrompt(req.prompt()));
        String b64 = response.getResult().getOutput().getB64Json();
        // Gemini's image models return PNG.
        return Map.of("mimeType", "image/png", "b64", b64);
    }

    // ----- Audio -> Text -------------------------------------------------

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> transcribe(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a non-empty audio file.");
        }
        final String filename = file.getOriginalFilename();
        Resource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        String text = transcriptionModel.transcribe(resource);
        return Map.of("text", text);
    }

    // ----- Text -> Speech ------------------------------------------------

    public record SpeechRequest(String text, String voice) {
    }

    @PostMapping(value = "/speech", produces = "audio/wav")
    public ResponseEntity<byte[]> speech(@RequestBody SpeechRequest req) {
        if (!StringUtils.hasText(req.text())) {
            throw new IllegalArgumentException("Text must not be empty.");
        }
        byte[] wav = speechModel.call(new SpeechModel.SpeechPrompt(req.text(), req.voice())).getResult().getOutput();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"speech.wav\"")
                .contentType(MediaType.parseMediaType("audio/wav"))
                .body(wav);
    }
}
