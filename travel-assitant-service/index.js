// index.js
require('dotenv').config();
const express = require('express');
const { generateContent } = require('./geminiService'); // Import the gemini service

const app = express();
const port = process.env.PORT || 3000;

app.use(express.json());

app.get('/', (req, res) => {
  res.send('Travel Assistant Microservice is running!');
});

// --- Gemini Integrated Endpoints ---

/**
 * Endpoint to answer FAQs.
 * Expects a JSON body with a "question" property.
 * Example: POST /api/faq
 * Body: { "question": "What are the check-in times?" }
 */
app.post('/api/faq', async (req, res) => {
  const { question } = req.body;

  if (!question) {
    return res.status(400).json({ error: 'Question is required in the request body.' });
  }

  // You can enhance this prompt with context specific to your travel service
  const prompt = `
    You are a helpful travel assistant for "Wanderlust Travel Agency".
    Answer the following frequently asked question: "${question}"

    If the question is about our services, you can mention:
    - We offer personalized travel packages.
    - We specialize in adventure and cultural tours.
    - Our booking process is online and easy.

    If the question is generic travel advice, provide helpful information.
    If the question is something you cannot answer or is unrelated, politely say so.
  `;

  try {
    const answer = await generateContent(prompt);
    res.json({ question, answer });
  } catch (error) {
    console.error("Error in /api/faq endpoint:", error);
    res.status(500).json({ error: 'Failed to get an answer from the assistant.' });
  }
});

/**
 * Endpoint to suggest itineraries.
 * Expects a JSON body with properties like "destination", "duration", "interests".
 * Example: POST /api/suggest-itinerary
 * Body: { "destination": "Paris", "duration": "3 days", "interests": "museums, food" }
 */
app.post('/api/suggest-itinerary', async (req, res) => {
  const { destination, duration, interests } = req.body;

  if (!destination || !duration) {
    return res.status(400).json({ error: 'Destination and duration are required.' });
  }

  const prompt = `
    You are a helpful travel assistant for "Wanderlust Travel Agency".
    Suggest a travel itinerary based on the following details:
    - Destination: ${destination}
    - Duration: ${duration}
    - Interests: ${interests || 'general sightseeing'}

    Provide a brief, day-by-day suggestion.
    Format the output clearly.
    Start with a friendly introduction.
  `;

  try {
    const itinerarySuggestion = await generateContent(prompt);
    res.json({ request: req.body, itinerarySuggestion });
  } catch (error) {
    console.error("Error in /api/suggest-itinerary endpoint:", error);
    res.status(500).json({ error: 'Failed to suggest an itinerary from the assistant.' });
  }
});

/**
 * Endpoint to draft itinerary emails.
 * Expects a JSON body with "customerName", "itineraryDetails" (could be plain text or structured).
 * Example: POST /api/draft-email
 * Body: { "customerName": "John Doe", "itineraryDetails": "A 3-day trip to Paris focusing on museums and food..." }
 */
app.post('/api/draft-email', async (req, res) => {
  const { customerName, itineraryDetails } = req.body;

  if (!customerName || !itineraryDetails) {
    return res.status(400).json({ error: 'Customer name and itinerary details are required.' });
  }

  const prompt = `
    You are a helpful travel assistant for "Wanderlust Travel Agency".
    Draft a friendly and professional email to a customer about their travel itinerary.

    Customer Name: ${customerName}
    Itinerary Details:
    ---
    ${itineraryDetails}
    ---

    The email should:
    1. Start with a greeting to the customer.
    2. Briefly confirm the itinerary.
    3. Include a closing statement offering further assistance.
    4. Sign off from "The Wanderlust Travel Agency Team".

    Do not include a subject line, only the body of the email.
  `;

  try {
    const emailDraft = await generateContent(prompt);
    res.json({ request: req.body, emailDraft });
  } catch (error) {
    console.error("Error in /api/draft-email endpoint:", error);
    res.status(500).json({ error: 'Failed to draft the email using the assistant.' });
  }
});


app.listen(port, () => {
  console.log(`Server listening on port ${port}`);
  console.log('Gemini API Key Loaded:', !!process.env.GEMINI_API_KEY);
});