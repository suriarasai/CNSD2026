// geminiService.js - (current version from previous step, for reference)
const { GoogleGenerativeAI } = require("@google/generative-ai");

const API_KEY = process.env.GEMINI_API_KEY;
if (!API_KEY) {
    // This would prevent the app from starting correctly if the key isn't even passed.
    // Check startup logs too.
    console.error("FATAL: GEMINI_API_KEY is not set in environment variables at service startup.");
    // You might want to throw here to prevent the app from starting silently without a key
    // throw new Error("GEMINI_API_KEY is not set in environment variables.");
}

const genAI = new GoogleGenerativeAI(API_KEY || "DEFAULT_KEY_IF_NOT_SET"); // Fallback to avoid crash if API_KEY is initially undefined
const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });

async function generateContent(prompt) {
    if (!API_KEY) { // Check again, in case it was initially undefined
        console.error("Error in generateContent: GEMINI_API_KEY is missing.");
        throw new Error("Gemini API Key is not configured.");
    }
    try {
        console.log("Attempting to generate content with Gemini. Prompt length:", prompt.length); // Log prompt
        const result = await model.generateContent(prompt);
        // It's good to inspect the raw result if issues persist
        // console.log("Full Gemini API Result:", JSON.stringify(result, null, 2));

        const response = result.response;
        if (!response) {
            console.error("Error generating content with Gemini: No response object in result.", result);
            // Check if there's candidate information or error details in 'result'
            if (result && result.candidates && result.candidates.length > 0 && result.candidates[0].finishReason !== 'STOP') {
                 console.error("Gemini response finished due to:", result.candidates[0].finishReason, "Safety ratings:", result.candidates[0].safetyRatings);
                 throw new Error(`Gemini API call failed. Reason: ${result.candidates[0].finishReason}`);
            }
            throw new Error("Failed to get a valid response from Gemini API (no response object).");
        }

        const text = response.text();
        console.log("Successfully generated content from Gemini.");
        return text;
    } catch (error) {
        // This will catch errors from model.generateContent() or response.text()
        console.error("Error generating content with Gemini:", error.message);
        // Log the full error for more details, especially if it's an API error object
        console.error("Full Gemini error object:", JSON.stringify(error, null, 2));
        throw new Error("Failed to generate content from Gemini API."); // This error is caught by the endpoint handler
    }
}

module.exports = { generateContent };