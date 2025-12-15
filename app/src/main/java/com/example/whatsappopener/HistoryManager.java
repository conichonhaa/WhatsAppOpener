package com.example.whatsappopener;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryManager {
    private static final String PREFS_NAME = "WhatsAppOpenerHistory";
    private static final String KEY_HISTORY = "phone_history";
    private static final int MAX_HISTORY_SIZE = 50;

    private SharedPreferences prefs;
    private Gson gson;

    public static class HistoryEntry {
        public String phoneNumber;
        public String countryCode;
        public String countryName;
        public long timestamp;
        public int useCount;

        public HistoryEntry(String phoneNumber, String countryCode, String countryName) {
            this.phoneNumber = phoneNumber;
            this.countryCode = countryCode;
            this.countryName = countryName;
            this.timestamp = System.currentTimeMillis();
            this.useCount = 1;
        }
    }

    public HistoryManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void addToHistory(String phoneNumber, String countryCode, String countryName) {
        List<HistoryEntry> history = getHistory();

        // Vérifier si le numéro existe déjà
        boolean found = false;
        for (HistoryEntry entry : history) {
            if (entry.phoneNumber.equals(phoneNumber)) {
                entry.useCount++;
                entry.timestamp = System.currentTimeMillis();
                found = true;
                break;
            }
        }

        // Si nouveau, ajouter en tête
        if (!found) {
            history.add(0, new HistoryEntry(phoneNumber, countryCode, countryName));
        }

        // Limiter la taille
        if (history.size() > MAX_HISTORY_SIZE) {
            history = history.subList(0, MAX_HISTORY_SIZE);
        }

        saveHistory(history);
    }

    public List<HistoryEntry> getHistory() {
        String json = prefs.getString(KEY_HISTORY, "[]");
        Type type = new TypeToken<List<HistoryEntry>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public Map<String, Integer> getCountryStats() {
        List<HistoryEntry> history = getHistory();
        Map<String, Integer> stats = new HashMap<>();

        for (HistoryEntry entry : history) {
            String country = entry.countryName != null ? entry.countryName : entry.countryCode;
            stats.put(country, stats.getOrDefault(country, 0) + entry.useCount);
        }

        return stats;
    }

    private void saveHistory(List<HistoryEntry> history) {
        String json = gson.toJson(history);
        prefs.edit().putString(KEY_HISTORY, json).apply();
    }

    public void clearHistory() {
        prefs.edit().clear().apply();
    }
}