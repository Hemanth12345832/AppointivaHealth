const { GoogleGenAI } = require("@google/genai");

// Simple fallback rule-based symptom mapping if AI fails
const rules = [
  { doctorType: "Cardiologist", keywords: ["chest pain", "palpitations", "heart attack", "shortness of breath", "chest tightness"] },
  { doctorType: "Dermatologist", keywords: ["skin rash", "itching", "eczema", "acne", "psoriasis"] },
  { doctorType: "General Physician", keywords: ["fever", "cough", "cold", "flu", "sore throat", "headache", "body ache"] },
  { doctorType: "Gastroenterologist", keywords: ["abdominal pain", "ulcer", "acid reflux", "diarrhea", "constipation"] },
  { doctorType: "Neurologist", keywords: ["seizure", "migraine", "numbness", "stroke", "paralysis"] },
  { doctorType: "ENT Specialist", keywords: ["ear pain", "hearing loss", "sore throat", "vertigo", "nosebleed"] },
];

function fallbackRecommendDoctors(symptoms) {
  const text = (symptoms || "").toLowerCase();
  const scored = rules.map((rule) => {
    let score = 0;
    for (const kw of rule.keywords) {
      if (kw && text.includes(kw)) score += 1;
    }
    return { doctorType: rule.doctorType, score };
  });

  scored.sort((a, b) => b.score - a.score);
  const top = scored.filter((x) => x.score > 0).slice(0, 2);
  if (top.length === 0) {
    return [{ doctorType: "General Physician", confidence: 0.5 }];
  }
  const maxScore = Math.max(...top.map((t) => t.score), 1);
  return top.map((t) => ({
    doctorType: t.doctorType,
    confidence: Math.min(0.95, 0.35 + 0.6 * (t.score / maxScore)),
  }));
}

async function recommendDoctorsAI(symptoms) {
  try {
    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey) {
      console.warn("GEMINI_API_KEY is missing. Using fallback rule-based symptom checker.");
      return fallbackRecommendDoctors(symptoms);
    }

    const ai = new GoogleGenAI({ apiKey: apiKey });
    const prompt = `You are a helpful and professional AI Symptom Checker for a medical app. 
Analyze the following symptoms provided by a patient: "${symptoms}"

Provide a concise, extremely relevant, and friendly medical assessment. Describe exactly what doctor specialty they should visit and why. Include any relevant immediately actionable details (e.g. rest, hydration, emergency room visit). Start your response directly with the advice. DO NOT use Markdown formatting (like asterisks or hashtags), just plain text. Return the result STRICTLY as a JSON object with a single "message" key containing your plain text response. Example: {"message": "Based on your symptoms..."}`;

    const response = await ai.models.generateContent({
        model: 'gemini-2.5-flash',
        contents: prompt
    });

    let rawText = response.text;
    rawText = rawText.replace(/```json/g, "").replace(/```/g, "").trim();

    const parsed = JSON.parse(rawText);
    if (parsed && typeof parsed.message === "string") {
      return { message: parsed.message };
    }
    throw new Error("Invalid format from AI");
  } catch (error) {
    console.error("Gemini AI check failed, falling back to rule-based:", error.message);
    return { recommendations: fallbackRecommendDoctors(symptoms), note: "" };
  }
}

// Keep the same exported name 'recommendDoctors' for compatibility, but note it returns a Promise now.
module.exports = { recommendDoctors: recommendDoctorsAI };

