package br.fecap.pi.saferide;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DirectionsJSONParser {
    private static final String TAG = "DirectionsJSONParser";

    public List<List<HashMap<String, String>>> parse(JSONObject jObject) {
        List<List<HashMap<String, String>>> routes = new ArrayList<>();
        JSONArray jRoutes;
        JSONArray jLegs;
        JSONArray jSteps;

        try {
            if (!jObject.has("routes")) {
                Log.e(TAG, "JSON não contém rotas");
                return routes;
            }

            jRoutes = jObject.getJSONArray("routes");
            Log.d(TAG, "Encontradas " + jRoutes.length() + " rotas no JSON");

            // Percorrer todas as rotas
            for (int i = 0; i < jRoutes.length(); i++) {
                List<HashMap<String, String>> path = new ArrayList<>();
                jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");

                Log.d(TAG, "Na rota " + (i+1) + ", encontradas " + jLegs.length() + " legs");

                // Percorrer todas as pernas (legs) da rota
                for (int j = 0; j < jLegs.length(); j++) {
                    jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                    Log.d(TAG, "Na leg " + (j+1) + ", encontrados " + jSteps.length() + " passos");

                    // Percorrer todos os passos (steps) da perna
                    for (int k = 0; k < jSteps.length(); k++) {
                        String polyline = "";
                        polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                        List<LatLng> list = decodePoly(polyline);

                        Log.d(TAG, "No passo " + (k+1) + ", decodificados " + list.size() + " pontos da polyline");

                        // Percorrer todos os pontos da linha poligonal (polyline)
                        for (int l = 0; l < list.size(); l++) {
                            HashMap<String, String> hm = new HashMap<>();
                            hm.put("lat", Double.toString(list.get(l).latitude));
                            hm.put("lng", Double.toString(list.get(l).longitude));
                            path.add(hm);
                        }
                    }
                }

                // Adiciona a rota completa à lista de rotas
                routes.add(path);
            }

        } catch (JSONException e) {
            Log.e(TAG, "Erro ao processar JSON: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Erro geral ao fazer parse: " + e.getMessage(), e);
        }

        return routes;
    }

    /**
     * Método para decodificar a string polyline de formato codificado do Google para uma lista de LatLng
     */
    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        try {
            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng p = new LatLng((double) lat / 1E5, (double) lng / 1E5);
                poly.add(p);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao decodificar polyline: " + e.getMessage(), e);
        }

        return poly;
    }
}