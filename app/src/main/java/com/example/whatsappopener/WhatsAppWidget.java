package com.example.whatsappopener;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import java.util.List;

public class WhatsAppWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        try {
            // Intent pour ouvrir l'app principale
            Intent openAppIntent = new Intent(context, MainActivity.class);
            PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                    context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_open_app, openAppPendingIntent);

            // Récupérer l'historique
            HistoryManager historyManager = new HistoryManager(context);
            List<HistoryManager.HistoryEntry> history = historyManager.getHistory();

            if (history.isEmpty()) {
                views.setTextViewText(R.id.widget_number_1_text, "Aucun historique");
                views.setViewVisibility(R.id.widget_number_2, View.GONE);
                views.setViewVisibility(R.id.widget_number_3, View.GONE);
            } else {
                // Numéro 1
                if (history.size() > 0) {
                    HistoryManager.HistoryEntry entry1 = history.get(0);
                    String text1 = entry1.phoneNumber;
                    if (entry1.countryName != null && !entry1.countryName.isEmpty()) {
                        text1 += " (" + entry1.countryName + ")";
                    }
                    views.setTextViewText(R.id.widget_number_1_text, text1);
                    views.setViewVisibility(R.id.widget_number_1, View.VISIBLE);

                    String cleanNumber1 = entry1.phoneNumber.replace("+", "").replace(" ", "").replace("-", "");
                    Intent whatsappIntent1 = new Intent(Intent.ACTION_VIEW);
                    whatsappIntent1.setData(Uri.parse("https://wa.me/" + cleanNumber1));
                    PendingIntent pendingIntent1 = PendingIntent.getActivity(
                            context, 100, whatsappIntent1, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    views.setOnClickPendingIntent(R.id.widget_number_1, pendingIntent1);
                }

                // Numéro 2
                if (history.size() > 1) {
                    HistoryManager.HistoryEntry entry2 = history.get(1);
                    String text2 = entry2.phoneNumber;
                    if (entry2.countryName != null && !entry2.countryName.isEmpty()) {
                        text2 += " (" + entry2.countryName + ")";
                    }
                    views.setTextViewText(R.id.widget_number_2_text, text2);
                    views.setViewVisibility(R.id.widget_number_2, View.VISIBLE);

                    String cleanNumber2 = entry2.phoneNumber.replace("+", "").replace(" ", "").replace("-", "");
                    Intent whatsappIntent2 = new Intent(Intent.ACTION_VIEW);
                    whatsappIntent2.setData(Uri.parse("https://wa.me/" + cleanNumber2));
                    PendingIntent pendingIntent2 = PendingIntent.getActivity(
                            context, 101, whatsappIntent2, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    views.setOnClickPendingIntent(R.id.widget_number_2, pendingIntent2);
                } else {
                    views.setViewVisibility(R.id.widget_number_2, View.GONE);
                }

                // Numéro 3
                if (history.size() > 2) {
                    HistoryManager.HistoryEntry entry3 = history.get(2);
                    String text3 = entry3.phoneNumber;
                    if (entry3.countryName != null && !entry3.countryName.isEmpty()) {
                        text3 += " (" + entry3.countryName + ")";
                    }
                    views.setTextViewText(R.id.widget_number_3_text, text3);
                    views.setViewVisibility(R.id.widget_number_3, View.VISIBLE);

                    String cleanNumber3 = entry3.phoneNumber.replace("+", "").replace(" ", "").replace("-", "");
                    Intent whatsappIntent3 = new Intent(Intent.ACTION_VIEW);
                    whatsappIntent3.setData(Uri.parse("https://wa.me/" + cleanNumber3));
                    PendingIntent pendingIntent3 = PendingIntent.getActivity(
                            context, 102, whatsappIntent3, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    views.setOnClickPendingIntent(R.id.widget_number_3, pendingIntent3);
                } else {
                    views.setViewVisibility(R.id.widget_number_3, View.GONE);
                }
            }

        } catch (Exception e) {
            // En cas d'erreur, afficher un message
            views.setTextViewText(R.id.widget_number_1_text, "Erreur de chargement");
            views.setViewVisibility(R.id.widget_number_2, View.GONE);
            views.setViewVisibility(R.id.widget_number_3, View.GONE);
        }

        // Mettre à jour le widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static void updateAllWidgets(Context context) {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new android.content.ComponentName(context, WhatsAppWidget.class));

            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId);
            }
        } catch (Exception e) {
            // Ignorer les erreurs de mise à jour du widget
        }
    }
}