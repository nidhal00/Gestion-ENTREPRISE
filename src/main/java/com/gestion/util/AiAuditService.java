package com.gestion.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AiAuditService {

    // REMPLACEZ CETTE CLÉ PAR VOTRE CLÉ API GEMINI GRATUITE
    // Obtenez la vôtre ici en 1 minute : https://aistudio.google.com/app/apikey
    private static final String API_KEY = "AIzaSyD1_kN_ooAmGBJ5mjgvsFbpLyLw5c3OiEE";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    /**
     * Envoie un prompt structuré à l'API Gemini pour analyser les risques de l'entreprise.
     * @param nom Nom de l'entreprise
     * @param secteur Secteur d'activité
     * @param taille Taille (small, medium, large)
     * @param pays Pays d'origine
     * @return L'analyse textuelle générée par l'IA.
     */
    public static String analyserRisquesEntreprise(String nom, String secteur, String taille, String pays) {
        if (API_KEY.equals("VOTRE_CLE_API_GEMINI_ICI")) {
            return "ERREUR : Vous devez d'abord mettre votre clé API Gemini dans la classe AiAuditService.java ! (Ligne 17)";
        }

        String prompt = "Tu es un auditeur financier et stratégique très strict. Fais une analyse des risques " +
                "pour l'entreprise suivante. Nom: " + nom + ", Secteur: " + secteur + ", Taille: " + taille + ", Pays: " + pays + ". " +
                "Donne 2 points forts et 2 risques potentiels majeurs liés à son secteur. Sois professionnel et concis (maximum 5 phrases).";

        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Construction du corps JSON pour l'API Gemini
            JSONObject requestBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            
            part.put("text", prompt);
            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);

            // Envoi de la requête
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                // Parsing de la réponse de Gemini
                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray candidates = jsonResponse.getJSONArray("candidates");
                if (candidates.length() > 0) {
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    JSONObject contentObj = firstCandidate.getJSONObject("content");
                    JSONArray partsArray = contentObj.getJSONArray("parts");
                    if (partsArray.length() > 0) {
                        return partsArray.getJSONObject(0).getString("text");
                    }
                }
            } else {
                return "L'IA a rencontré une erreur (Code HTTP " + responseCode + "). Vérifiez votre clé API.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur de connexion à l'API IA : " + e.getMessage();
        }
        
        return "Impossible d'obtenir une analyse.";
    }
}
