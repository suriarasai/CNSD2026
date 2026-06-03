package com.example.multimodal.spi;

import com.example.multimodal.audio.WavWriter;
import com.example.multimodal.config.GeminiProperties;
import com.example.multimodal.gemini.GeminiClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Gemini-backed {@link SpeechModel}. Gemini TTS returns raw 16-bit PCM, so we
 * wrap it in a WAV container ({@link WavWriter}) before handing back bytes the
 * browser can play directly.
 */
@Component
public class GeminiSpeechModel implements SpeechModel {

    private final GeminiClient gemini;
    private final GeminiProperties props;

    public GeminiSpeechModel(GeminiClient gemini, GeminiProperties props) {
        this.gemini = gemini;
        this.props = props;
    }

    @Override
    public SpeechResponse call(SpeechPrompt prompt) {
        String voice = StringUtils.hasText(prompt.voice())
                ? prompt.voice()
                : props.getSpeech().getVoice();

        GeminiClient.InlineData pcm = gemini.synthesizeSpeech(prompt.text(), voice);
        byte[] wav = WavWriter.fromPcm(pcm.data(), pcm.mimeType());
        return new SpeechResponse(new Speech(wav));
    }
}
