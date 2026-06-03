package com.example.multimodal.audio;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Wraps raw little-endian PCM samples in a minimal 44-byte WAV (RIFF) header so
 * browsers / media players can play them directly.
 *
 * <p>Gemini's TTS models return signed 16-bit, single-channel PCM (the MIME type
 * looks like {@code audio/L16;codec=pcm;rate=24000}). We parse the sample rate
 * out of that MIME type and default to 24 kHz mono / 16-bit when absent.
 */
public final class WavWriter {

    private WavWriter() {
    }

    public static byte[] fromPcm(byte[] pcm, String mimeType) {
        int sampleRate = parseRate(mimeType, 24_000);
        return fromPcm(pcm, sampleRate, 1, 16);
    }

    public static byte[] fromPcm(byte[] pcm, int sampleRate, int channels, int bitsPerSample) {
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;

        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put("RIFF".getBytes());
        header.putInt(36 + pcm.length);     // ChunkSize
        header.put("WAVE".getBytes());
        header.put("fmt ".getBytes());
        header.putInt(16);                  // Subchunk1Size (PCM)
        header.putShort((short) 1);         // AudioFormat = PCM
        header.putShort((short) channels);
        header.putInt(sampleRate);
        header.putInt(byteRate);
        header.putShort((short) blockAlign);
        header.putShort((short) bitsPerSample);
        header.put("data".getBytes());
        header.putInt(pcm.length);          // Subchunk2Size

        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + pcm.length);
        out.writeBytes(header.array());
        out.writeBytes(pcm);
        return out.toByteArray();
    }

    private static int parseRate(String mimeType, int fallback) {
        if (mimeType == null) {
            return fallback;
        }
        for (String part : mimeType.split(";")) {
            String p = part.trim();
            if (p.startsWith("rate=")) {
                try {
                    return Integer.parseInt(p.substring("rate=".length()).trim());
                } catch (NumberFormatException ignored) {
                    return fallback;
                }
            }
        }
        return fallback;
    }
}
