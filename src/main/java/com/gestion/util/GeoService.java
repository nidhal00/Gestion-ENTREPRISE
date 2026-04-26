package com.gestion.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GeoService {

    /**
     * Utilise l'API OpenStreetMap Nominatim pour obtenir la latitude et la longitude d'une adresse.
     * @param adresse L'adresse texte (ex: "Lac 2, Tunis, Tunisia")
     * @return un tableau {latitude, longitude} ou null si non trouvé/erreur.
     */
    public static double[] getCoordinates(String adresse) {
        if (adresse == null || adresse.trim().isEmpty()) {
            return null;
        }

        try {
            String encodedAddress = URLEncoder.encode(adresse.trim(), StandardCharsets.UTF_8.toString());
            String urlStr = "https://nominatim.openstreetmap.org/search?q=" + encodedAddress + "&format=json&limit=1";
            
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            // Nominatim requires a User-Agent to avoid being blocked
            conn.setRequestProperty("User-Agent", "MindAudit-JavaFX-App/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                JSONArray jsonArray = new JSONArray(response.toString());
                if (jsonArray.length() > 0) {
                    JSONObject firstResult = jsonArray.getJSONObject(0);
                    double lat = Double.parseDouble(firstResult.getString("lat"));
                    double lon = Double.parseDouble(firstResult.getString("lon"));
                    return new double[]{lat, lon};
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur API Géocodage : " + e.getMessage());
        }
        return null;
    }
}
