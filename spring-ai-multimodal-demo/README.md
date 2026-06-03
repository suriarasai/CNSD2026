# Spring AI: Multimodal Benchmarking Architecture

This repository presents a Spring Boot application designed to investigate the [Spring AI](https://spring.io/projects/spring-ai) framework, with a specific focus on its portable model abstractions across three distinct computational modalities.

| Capability | Spring AI Interface | API Reference Documentation |
| --- | --- | --- |
| Text-to-Image Synthesis | `org.springframework.ai.image.ImageModel` | [Image Model API](https://docs.spring.io/spring-ai/reference/api/imageclient.html) |
| Audio-to-Text Transcription | `org.springframework.ai.audio.transcription.TranscriptionModel` | [Transcription API](https://docs.spring.io/spring-ai/reference/api/audio/transcriptions.html) |
| Text-to-Speech Generation | Mirror of `…audio.tts.TextToSpeechModel` | [TTS API](https://docs.spring.io/spring-ai/reference/api/audio/speech.html) |

The user interface and system interactions are consolidated within a single client-side application accessible via `http://localhost:8080`.

---

## Implementation Context: Gemini Integration

This project utilizes Spring AI as a core architectural framework, implementing its designated interfaces via a Gemini-backed service layer. This design explicitly demonstrates the extensibility of the Spring AI architecture. System authentication credentials must be provided via the `GEMINI_API_KEY` environment variable.

---

## System Architecture and Interface Extensibility

```text
 Browser (index.html)
        |  POST /api/image | /api/transcribe | /api/speech
        V
 MediaController  -- Depends exclusively on Spring AI interfaces --+
        |                                                          |
        V                  (Dependency Injection)                  |
 ImageModel             TranscriptionModel          SpeechModel    |  <- Interchangeable Components
        |                      |                       |           |
        V                      V                       V           |
 GeminiImageModel       GeminiTranscriptionModel    GeminiSpeechModel
        +----------------------+-----------------------+
                               V
                          GeminiClient  -- REST API Integration

```

The `MediaController` maintains strict decoupling from provider-specific dependencies. Substituting the `GeminiImageModel` with an alternative implementation (e.g., `OpenAiImageModel` via the `spring-ai-starter-model-openai` dependency) requires no modifications to the controller, the user interface, or the broader application logic. This operational portability represents the primary advantage of the model API abstraction.

It should be noted that `ImageModel` and `TranscriptionModel` implement the standard Spring AI interfaces. To ensure the demonstration remains self-contained, the Text-to-Speech component (`SpeechModel`) utilizes a lightweight, dependency-free structural mirror of `TextToSpeechModel`. Inline documentation within the source code details the procedure for integrating the standard interface.

---

## System Prerequisites

* **Java Development Kit (JDK):** Version 17 or higher
* **Build Automation:** Maven 3.9+ (or an IDE-bundled Maven distribution)
* **Authentication:** A valid Gemini API key, available via Google AI Studio ([https://aistudio.google.com/apikey](https://aistudio.google.com/apikey))

## Initialization Protocol

```bash
# 1. Provision environment configuration credentials
cp .env.example .env
#    Proceed to modify the .env file to assign the GEMINI_API_KEY variable.

# 2. Execute the application build and deployment
mvn spring-boot:run

# 3. Access the application interface
#    Navigate to http://localhost:8080

```

The application relies entirely on external APIs and requires no local database provisioning or auxiliary services.

---

## Application Programming Interface (API) Specification

The following examples demonstrate direct endpoint invocation utilizing `curl` commands.

```bash
# Text-to-Image Synthesis (Returns: { "mimeType": "...", "b64": "..." })
curl -s localhost:8080/api/image \
  -H 'Content-Type: application/json' \
  -d '{"prompt":"a teapot shaped like a hedgehog, studio photo"}'

# Audio-to-Text Transcription (Returns: { "text": "..." })
curl -s localhost:8080/api/transcribe -F file=@sample.wav

# Text-to-Speech Generation (Returns: audio/wav byte stream)
curl -s localhost:8080/api/speech \
  -H 'Content-Type: application/json' \
  -d '{"text":"Say cheerfully: hello from Spring AI","voice":"Puck"}' \
  --output speech.wav

```

---

## System Configuration Properties

The application incorporates default configurations. Administrators may override these parameters via the `.env` file or standard environment variables.

| Environment Variable | Default Value | Functional Purpose |
| --- | --- | --- |
| `GEMINI_API_KEY` | None (Required) | Google AI Studio authentication key |
| `GEMINI_BASE_URL` | `https://generativelanguage.googleapis.com/v1beta` | Base URI for REST API communications |
| `GEMINI_IMAGE_MODEL` | `gemini-2.5-flash-image` | Target model for image synthesis |
| `GEMINI_TRANSCRIPTION_MODEL` | `gemini-2.5-flash` | Multimodal model utilized for Speech-to-Text processing |
| `GEMINI_SPEECH_MODEL` | `gemini-2.5-flash-preview-tts` | Target model for Text-to-Speech generation |
| `GEMINI_SPEECH_VOICE` | `Kore` | Selection variable for pre-configured voice profiles |

### Technical Addenda Regarding Provider Models (Mid-2026)

* The designated Gemini image models represent the current recommended deployment path for image synthesis, returning inline Base64-encoded PNG data.
* Transcription functionality leverages Gemini's multimodal processing capabilities. Audio data is transmitted inline alongside a prescriptive transcription instruction, rather than utilizing a dedicated Speech-to-Text endpoint. This methodology is optimal for brief audio segments; long-form audio requires chunking strategies prior to transmission.
* The Text-to-Speech model returns raw 16-bit Pulse-Code Modulation (PCM) data (`audio/L16;rate=24000`). The internal `WavWriter` utility appends the requisite WAV header to facilitate standard browser playback.

---

## Codebase Structure and Component Organization

```text
src/main/java/com/example/multimodal/
├── MultimodalDemoApplication.java
├── config/   Configuration classes and RestClient initialization
├── gemini/   GeminiClient REST implementation and domain exceptions
├── spi/      Extensibility layer (GeminiImageModel, GeminiTranscriptionModel, SpeechModel)
├── audio/    Audio formatting utilities (PCM to WAV conversion)
└── web/      MediaController and global ApiExceptionHandler logic
src/main/resources/
├── application.yml   Application properties
└── static/index.html Single-page user interface payload

```

---

## Diagnostic Guidelines

* **Initialization Failure ("Missing Gemini API key"):** This indicates a failure to provision the `.env` file or properly declare the `GEMINI_API_KEY` variable prior to application startup.
* **HTTP 502 Bad Gateway ("No image returned"):** The upstream model returned text instead of the expected image payload. This is frequently triggered by safety policy violations. The error response payload will contain the specific provider feedback. Modify the prompt to comply with safety guidelines.
* **HTTP 403 Forbidden (Quota Exceeded):** Verify that the provided API key is active and that request volume has not exceeded the designated tier limits.
* **Empty Transcription Output:** Processing highly truncated or silent audio clips may result in a null response. Provide an audio file with clear articulation in a standard format (e.g., WAV, MP3, M4A).

---

*Note: This repository was constructed to evaluate Spring AI framework capabilities. Provider-side model availability and identifier schemas are subject to frequent modification. Consult the official provider documentation for the most current specifications.*