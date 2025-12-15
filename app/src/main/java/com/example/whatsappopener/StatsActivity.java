package com.example.whatsappopener;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class StatsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        HistoryManager historyManager = new HistoryManager(this);
        Map<String, Integer> stats = historyManager.getCountryStats();

        TextView textStats = findViewById(R.id.textStats);

        if (stats.isEmpty()) {
            textStats.setText("Aucune statistique disponible.\nUtilisez l'application pour voir vos statistiques !");
            return;
        }

        // Trier par nombre d'utilisations
        List<Map.Entry<String, Integer>> sortedStats = new ArrayList<>(stats.entrySet());
        Collections.sort(sortedStats, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
                return e2.getValue().compareTo(e1.getValue());
            }
        });

        // Calculer le total
        int total = 0;
        for (Integer count : stats.values()) {
            total += count;
        }

        // Créer l'affichage
        StringBuilder sb = new StringBuilder();
        sb.append("📊 STATISTIQUES PAR PAYS\n\n");
        sb.append("Total d'utilisations : ").append(total).append("\n");
        sb.append("Pays contactés : ").append(stats.size()).append("\n\n");
        sb.append("──────────────────────\n\n");

        for (Map.Entry<String, Integer> entry : sortedStats) {
            String country = entry.getKey();
            int count = entry.getValue();
            float percentage = (count * 100.0f) / total;

            // Barre de progression visuelle
            int barLength = (int) (percentage / 5); // 5% = 1 caractère
            String bar = "█".repeat(Math.max(1, barLength));

            sb.append(String.format("%s\n%d utilisations (%.1f%%)\n%s\n\n",
                    country, count, percentage, bar));
        }

        textStats.setText(sb.toString());
    }
}