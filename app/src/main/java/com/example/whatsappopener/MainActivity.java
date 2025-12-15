package com.example.whatsappopener;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private AutoCompleteTextView editTextPhone;
    private Button buttonOpenWhatsApp;
    private Button buttonStats;
    private Spinner spinnerCountry;
    private TextView textViewValidation;
    private LinearLayout clipboardSuggestion;
    private TextView textClipboardNumber;
    private Button buttonUseClipboard;
    private ListView listViewHistory;
    private PhoneNumberUtil phoneUtil;
    private String selectedCountryCode = "FR";
    private HistoryManager historyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialiser PhoneNumberUtil et HistoryManager
        phoneUtil = PhoneNumberUtil.getInstance();
        historyManager = new HistoryManager(this);

        // Récupérer les éléments de l'interface
        editTextPhone = findViewById(R.id.editTextPhone);
        buttonOpenWhatsApp = findViewById(R.id.buttonOpenWhatsApp);
        buttonStats = findViewById(R.id.buttonStats);
        spinnerCountry = findViewById(R.id.spinnerCountry);
        textViewValidation = findViewById(R.id.textViewValidation);
        clipboardSuggestion = findViewById(R.id.clipboardSuggestion);
        textClipboardNumber = findViewById(R.id.textClipboardNumber);
        buttonUseClipboard = findViewById(R.id.buttonUseClipboard);
        listViewHistory = findViewById(R.id.listViewHistory);

        // Configurer le sélecteur de pays
        setupCountrySpinner();

        // Détecter le presse-papier
        checkClipboard();

        // Configurer l'auto-complétion
        setupAutoComplete();

        // Afficher l'historique
        refreshHistory();

        // Validation en temps réel pendant la saisie
        editTextPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePhoneNumber(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Bouton pour utiliser le numéro du presse-papier
        buttonUseClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (clipboard != null && clipboard.hasPrimaryClip()) {
                    ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                    String text = item.getText().toString();
                    editTextPhone.setText(text);
                    clipboardSuggestion.setVisibility(View.GONE);
                }
            }
        });

        // Bouton ouvrir WhatsApp
        buttonOpenWhatsApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWhatsApp();
            }
        });

        // Bouton statistiques
        buttonStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StatsActivity.class);
                startActivity(intent);
            }
        });

        // Clic sur un élément de l'historique
        listViewHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                @SuppressWarnings("unchecked")
                HashMap<String, String> item = (HashMap<String, String>) parent.getItemAtPosition(position);
                editTextPhone.setText(item.get("number"));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkClipboard();
        refreshHistory();
    }

    private void checkClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            String text = item.getText().toString();

            // Vérifier si c'est un numéro de téléphone valide
            try {
                String cleanText = text.replaceAll("[^0-9+]", "");
                if (cleanText.length() >= 8) {
                    Phonenumber.PhoneNumber number = phoneUtil.parse(cleanText, "ZZ");
                    if (phoneUtil.isValidNumber(number)) {
                        String formatted = phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
                        textClipboardNumber.setText("📋 Numéro détecté : " + formatted);
                        clipboardSuggestion.setVisibility(View.VISIBLE);
                        return;
                    }
                }
            } catch (NumberParseException e) {
                // Pas un numéro valide
            }
        }
        clipboardSuggestion.setVisibility(View.GONE);
    }

    private void setupAutoComplete() {
        List<HistoryManager.HistoryEntry> history = historyManager.getHistory();
        List<String> suggestions = new ArrayList<>();

        for (HistoryManager.HistoryEntry entry : history) {
            suggestions.add(entry.phoneNumber);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, suggestions);
        editTextPhone.setAdapter(adapter);
    }

    private void refreshHistory() {
        List<HistoryManager.HistoryEntry> history = historyManager.getHistory();

        // Limiter à 10 derniers
        if (history.size() > 10) {
            history = history.subList(0, 10);
        }

        List<Map<String, String>> data = new ArrayList<>();
        for (HistoryManager.HistoryEntry entry : history) {
            Map<String, String> item = new HashMap<>();
            item.put("number", entry.phoneNumber);
            item.put("country", entry.countryName + " (" + entry.useCount + "x)");
            data.add(item);
        }

        SimpleAdapter adapter = new SimpleAdapter(
                this, data, android.R.layout.simple_list_item_2,
                new String[]{"number", "country"},
                new int[]{android.R.id.text1, android.R.id.text2}
        );

        listViewHistory.setAdapter(adapter);
    }

    private void setupCountrySpinner() {
        // Liste COMPLÈTE de tous les pays
        List<CountryItem> countries = new ArrayList<>();

        // Europe
        countries.add(new CountryItem("🇦🇱 Albanie", "AL"));
        countries.add(new CountryItem("🇩🇪 Allemagne", "DE"));
        countries.add(new CountryItem("🇦🇩 Andorre", "AD"));
        countries.add(new CountryItem("🇦🇹 Autriche", "AT"));
        countries.add(new CountryItem("🇧🇪 Belgique", "BE"));
        countries.add(new CountryItem("🇧🇾 Biélorussie", "BY"));
        countries.add(new CountryItem("🇧🇦 Bosnie-Herzégovine", "BA"));
        countries.add(new CountryItem("🇧🇬 Bulgarie", "BG"));
        countries.add(new CountryItem("🇭🇷 Croatie", "HR"));
        countries.add(new CountryItem("🇩🇰 Danemark", "DK"));
        countries.add(new CountryItem("🇪🇸 Espagne", "ES"));
        countries.add(new CountryItem("🇪🇪 Estonie", "EE"));
        countries.add(new CountryItem("🇫🇮 Finlande", "FI"));
        countries.add(new CountryItem("🇫🇷 France", "FR"));
        countries.add(new CountryItem("🇬🇷 Grèce", "GR"));
        countries.add(new CountryItem("🇭🇺 Hongrie", "HU"));
        countries.add(new CountryItem("🇮🇪 Irlande", "IE"));
        countries.add(new CountryItem("🇮🇸 Islande", "IS"));
        countries.add(new CountryItem("🇮🇹 Italie", "IT"));
        countries.add(new CountryItem("🇽🇰 Kosovo", "XK"));
        countries.add(new CountryItem("🇱🇻 Lettonie", "LV"));
        countries.add(new CountryItem("🇱🇮 Liechtenstein", "LI"));
        countries.add(new CountryItem("🇱🇹 Lituanie", "LT"));
        countries.add(new CountryItem("🇱🇺 Luxembourg", "LU"));
        countries.add(new CountryItem("🇲🇰 Macédoine du Nord", "MK"));
        countries.add(new CountryItem("🇲🇹 Malte", "MT"));
        countries.add(new CountryItem("🇲🇩 Moldavie", "MD"));
        countries.add(new CountryItem("🇲🇨 Monaco", "MC"));
        countries.add(new CountryItem("🇲🇪 Monténégro", "ME"));
        countries.add(new CountryItem("🇳🇴 Norvège", "NO"));
        countries.add(new CountryItem("🇳🇱 Pays-Bas", "NL"));
        countries.add(new CountryItem("🇵🇱 Pologne", "PL"));
        countries.add(new CountryItem("🇵🇹 Portugal", "PT"));
        countries.add(new CountryItem("🇨🇿 République tchèque", "CZ"));
        countries.add(new CountryItem("🇷🇴 Roumanie", "RO"));
        countries.add(new CountryItem("🇬🇧 Royaume-Uni", "GB"));
        countries.add(new CountryItem("🇷🇺 Russie", "RU"));
        countries.add(new CountryItem("🇷🇸 Serbie", "RS"));
        countries.add(new CountryItem("🇸🇰 Slovaquie", "SK"));
        countries.add(new CountryItem("🇸🇮 Slovénie", "SI"));
        countries.add(new CountryItem("🇸🇪 Suède", "SE"));
        countries.add(new CountryItem("🇨🇭 Suisse", "CH"));
        countries.add(new CountryItem("🇺🇦 Ukraine", "UA"));
        countries.add(new CountryItem("🇻🇦 Vatican", "VA"));

        // Afrique
        countries.add(new CountryItem("🇿🇦 Afrique du Sud", "ZA"));
        countries.add(new CountryItem("🇩🇿 Algérie", "DZ"));
        countries.add(new CountryItem("🇦🇴 Angola", "AO"));
        countries.add(new CountryItem("🇧🇯 Bénin", "BJ"));
        countries.add(new CountryItem("🇧🇼 Botswana", "BW"));
        countries.add(new CountryItem("🇧🇫 Burkina Faso", "BF"));
        countries.add(new CountryItem("🇧🇮 Burundi", "BI"));
        countries.add(new CountryItem("🇨🇲 Cameroun", "CM"));
        countries.add(new CountryItem("🇨🇻 Cap-Vert", "CV"));
        countries.add(new CountryItem("🇨🇫 Centrafrique", "CF"));
        countries.add(new CountryItem("🇰🇲 Comores", "KM"));
        countries.add(new CountryItem("🇨🇬 Congo", "CG"));
        countries.add(new CountryItem("🇨🇩 Congo (RDC)", "CD"));
        countries.add(new CountryItem("🇨🇮 Côte d'Ivoire", "CI"));
        countries.add(new CountryItem("🇩🇯 Djibouti", "DJ"));
        countries.add(new CountryItem("🇪🇬 Égypte", "EG"));
        countries.add(new CountryItem("🇪🇷 Érythrée", "ER"));
        countries.add(new CountryItem("🇪🇹 Éthiopie", "ET"));
        countries.add(new CountryItem("🇬🇦 Gabon", "GA"));
        countries.add(new CountryItem("🇬🇲 Gambie", "GM"));
        countries.add(new CountryItem("🇬🇭 Ghana", "GH"));
        countries.add(new CountryItem("🇬🇳 Guinée", "GN"));
        countries.add(new CountryItem("🇬🇼 Guinée-Bissau", "GW"));
        countries.add(new CountryItem("🇬🇶 Guinée équatoriale", "GQ"));
        countries.add(new CountryItem("🇰🇪 Kenya", "KE"));
        countries.add(new CountryItem("🇱🇸 Lesotho", "LS"));
        countries.add(new CountryItem("🇱🇷 Libéria", "LR"));
        countries.add(new CountryItem("🇱🇾 Libye", "LY"));
        countries.add(new CountryItem("🇲🇬 Madagascar", "MG"));
        countries.add(new CountryItem("🇲🇼 Malawi", "MW"));
        countries.add(new CountryItem("🇲🇱 Mali", "ML"));
        countries.add(new CountryItem("🇲🇦 Maroc", "MA"));
        countries.add(new CountryItem("🇲🇺 Maurice", "MU"));
        countries.add(new CountryItem("🇲🇷 Mauritanie", "MR"));
        countries.add(new CountryItem("🇲🇿 Mozambique", "MZ"));
        countries.add(new CountryItem("🇳🇦 Namibie", "NA"));
        countries.add(new CountryItem("🇳🇪 Niger", "NE"));
        countries.add(new CountryItem("🇳🇬 Nigéria", "NG"));
        countries.add(new CountryItem("🇺🇬 Ouganda", "UG"));
        countries.add(new CountryItem("🇷🇼 Rwanda", "RW"));
        countries.add(new CountryItem("🇸🇹 Sao Tomé-et-Principe", "ST"));
        countries.add(new CountryItem("🇸🇳 Sénégal", "SN"));
        countries.add(new CountryItem("🇸🇨 Seychelles", "SC"));
        countries.add(new CountryItem("🇸🇱 Sierra Leone", "SL"));
        countries.add(new CountryItem("🇸🇴 Somalie", "SO"));
        countries.add(new CountryItem("🇸🇸 Soudan du Sud", "SS"));
        countries.add(new CountryItem("🇸🇩 Soudan", "SD"));
        countries.add(new CountryItem("🇸🇿 Eswatini", "SZ"));
        countries.add(new CountryItem("🇹🇿 Tanzanie", "TZ"));
        countries.add(new CountryItem("🇹🇩 Tchad", "TD"));
        countries.add(new CountryItem("🇹🇬 Togo", "TG"));
        countries.add(new CountryItem("🇹🇳 Tunisie", "TN"));
        countries.add(new CountryItem("🇿🇲 Zambie", "ZM"));
        countries.add(new CountryItem("🇿🇼 Zimbabwe", "ZW"));

        // Amérique du Nord
        countries.add(new CountryItem("🇨🇦 Canada", "CA"));
        countries.add(new CountryItem("🇺🇸 États-Unis", "US"));
        countries.add(new CountryItem("🇲🇽 Mexique", "MX"));

        // Amérique Centrale et Caraïbes
        countries.add(new CountryItem("🇧🇸 Bahamas", "BS"));
        countries.add(new CountryItem("🇧🇧 Barbade", "BB"));
        countries.add(new CountryItem("🇧🇿 Belize", "BZ"));
        countries.add(new CountryItem("🇨🇷 Costa Rica", "CR"));
        countries.add(new CountryItem("🇨🇺 Cuba", "CU"));
        countries.add(new CountryItem("🇩🇴 République dominicaine", "DO"));
        countries.add(new CountryItem("🇸🇻 Salvador", "SV"));
        countries.add(new CountryItem("🇬🇹 Guatemala", "GT"));
        countries.add(new CountryItem("🇭🇹 Haïti", "HT"));
        countries.add(new CountryItem("🇭🇳 Honduras", "HN"));
        countries.add(new CountryItem("🇯🇲 Jamaïque", "JM"));
        countries.add(new CountryItem("🇳🇮 Nicaragua", "NI"));
        countries.add(new CountryItem("🇵🇦 Panama", "PA"));
        countries.add(new CountryItem("🇵🇷 Porto Rico", "PR"));
        countries.add(new CountryItem("🇹🇹 Trinité-et-Tobago", "TT"));

        // Amérique du Sud
        countries.add(new CountryItem("🇦🇷 Argentine", "AR"));
        countries.add(new CountryItem("🇧🇴 Bolivie", "BO"));
        countries.add(new CountryItem("🇧🇷 Brésil", "BR"));
        countries.add(new CountryItem("🇨🇱 Chili", "CL"));
        countries.add(new CountryItem("🇨🇴 Colombie", "CO"));
        countries.add(new CountryItem("🇪🇨 Équateur", "EC"));
        countries.add(new CountryItem("🇬🇾 Guyana", "GY"));
        countries.add(new CountryItem("🇵🇾 Paraguay", "PY"));
        countries.add(new CountryItem("🇵🇪 Pérou", "PE"));
        countries.add(new CountryItem("🇸🇷 Suriname", "SR"));
        countries.add(new CountryItem("🇺🇾 Uruguay", "UY"));
        countries.add(new CountryItem("🇻🇪 Venezuela", "VE"));

        // Asie
        countries.add(new CountryItem("🇦🇫 Afghanistan", "AF"));
        countries.add(new CountryItem("🇸🇦 Arabie saoudite", "SA"));
        countries.add(new CountryItem("🇦🇲 Arménie", "AM"));
        countries.add(new CountryItem("🇦🇿 Azerbaïdjan", "AZ"));
        countries.add(new CountryItem("🇧🇭 Bahreïn", "BH"));
        countries.add(new CountryItem("🇧🇩 Bangladesh", "BD"));
        countries.add(new CountryItem("🇧🇹 Bhoutan", "BT"));
        countries.add(new CountryItem("🇧🇳 Brunei", "BN"));
        countries.add(new CountryItem("🇰🇭 Cambodge", "KH"));
        countries.add(new CountryItem("🇨🇳 Chine", "CN"));
        countries.add(new CountryItem("🇰🇵 Corée du Nord", "KP"));
        countries.add(new CountryItem("🇰🇷 Corée du Sud", "KR"));
        countries.add(new CountryItem("🇦🇪 Émirats arabes unis", "AE"));
        countries.add(new CountryItem("🇬🇪 Géorgie", "GE"));
        countries.add(new CountryItem("🇭🇰 Hong Kong", "HK"));
        countries.add(new CountryItem("🇮🇳 Inde", "IN"));
        countries.add(new CountryItem("🇮🇩 Indonésie", "ID"));
        countries.add(new CountryItem("🇮🇶 Irak", "IQ"));
        countries.add(new CountryItem("🇮🇷 Iran", "IR"));
        countries.add(new CountryItem("🇮🇱 Israël", "IL"));
        countries.add(new CountryItem("🇯🇵 Japon", "JP"));
        countries.add(new CountryItem("🇯🇴 Jordanie", "JO"));
        countries.add(new CountryItem("🇰🇿 Kazakhstan", "KZ"));
        countries.add(new CountryItem("🇰🇬 Kirghizistan", "KG"));
        countries.add(new CountryItem("🇰🇼 Koweït", "KW"));
        countries.add(new CountryItem("🇱🇦 Laos", "LA"));
        countries.add(new CountryItem("🇱🇧 Liban", "LB"));
        countries.add(new CountryItem("🇲🇴 Macao", "MO"));
        countries.add(new CountryItem("🇲🇾 Malaisie", "MY"));
        countries.add(new CountryItem("🇲🇻 Maldives", "MV"));
        countries.add(new CountryItem("🇲🇳 Mongolie", "MN"));
        countries.add(new CountryItem("🇲🇲 Myanmar", "MM"));
        countries.add(new CountryItem("🇳🇵 Népal", "NP"));
        countries.add(new CountryItem("🇴🇲 Oman", "OM"));
        countries.add(new CountryItem("🇵🇰 Pakistan", "PK"));
        countries.add(new CountryItem("🇵🇸 Palestine", "PS"));
        countries.add(new CountryItem("🇵🇭 Philippines", "PH"));
        countries.add(new CountryItem("🇶🇦 Qatar", "QA"));
        countries.add(new CountryItem("🇸🇬 Singapour", "SG"));
        countries.add(new CountryItem("🇱🇰 Sri Lanka", "LK"));
        countries.add(new CountryItem("🇸🇾 Syrie", "SY"));
        countries.add(new CountryItem("🇹🇯 Tadjikistan", "TJ"));
        countries.add(new CountryItem("🇹🇼 Taïwan", "TW"));
        countries.add(new CountryItem("🇹🇭 Thaïlande", "TH"));
        countries.add(new CountryItem("🇹🇱 Timor oriental", "TL"));
        countries.add(new CountryItem("🇹🇷 Turquie", "TR"));
        countries.add(new CountryItem("🇹🇲 Turkménistan", "TM"));
        countries.add(new CountryItem("🇺🇿 Ouzbékistan", "UZ"));
        countries.add(new CountryItem("🇻🇳 Viêt Nam", "VN"));
        countries.add(new CountryItem("🇾🇪 Yémen", "YE"));

        // Océanie
        countries.add(new CountryItem("🇦🇺 Australie", "AU"));
        countries.add(new CountryItem("🇫🇯 Fidji", "FJ"));
        countries.add(new CountryItem("🇰🇮 Kiribati", "KI"));
        countries.add(new CountryItem("🇲🇭 Îles Marshall", "MH"));
        countries.add(new CountryItem("🇫🇲 Micronésie", "FM"));
        countries.add(new CountryItem("🇳🇷 Nauru", "NR"));
        countries.add(new CountryItem("🇳🇿 Nouvelle-Zélande", "NZ"));
        countries.add(new CountryItem("🇵🇼 Palaos", "PW"));
        countries.add(new CountryItem("🇵🇬 Papouasie-Nouvelle-Guinée", "PG"));
        countries.add(new CountryItem("🇼🇸 Samoa", "WS"));
        countries.add(new CountryItem("🇸🇧 Salomon", "SB"));
        countries.add(new CountryItem("🇹🇴 Tonga", "TO"));
        countries.add(new CountryItem("🇹🇻 Tuvalu", "TV"));
        countries.add(new CountryItem("🇻🇺 Vanuatu", "VU"));

        // Trier par ordre alphabétique
        Collections.sort(countries, new Comparator<CountryItem>() {
            @Override
            public int compare(CountryItem c1, CountryItem c2) {
                return c1.toString().compareTo(c2.toString());
            }
        });

        ArrayAdapter<CountryItem> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, countries);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCountry.setAdapter(adapter);

        // Définir la France comme pays par défaut
        for (int i = 0; i < countries.size(); i++) {
            if (countries.get(i).getCode().equals("FR")) {
                spinnerCountry.setSelection(i);
                break;
            }
        }

        spinnerCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CountryItem selected = (CountryItem) parent.getItemAtPosition(position);
                selectedCountryCode = selected.getCode();
                validatePhoneNumber(editTextPhone.getText().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber.isEmpty()) {
            textViewValidation.setVisibility(View.GONE);
            return;
        }

        try {
            Phonenumber.PhoneNumber number;
            String detectedCountry = "";

            // Essayer avec le pays sélectionné
            try {
                number = phoneUtil.parse(phoneNumber, selectedCountryCode);
                if (phoneUtil.isValidNumber(number)) {
                    detectedCountry = phoneUtil.getRegionCodeForNumber(number);
                } else {
                    // Essayer en mode international
                    number = phoneUtil.parse(phoneNumber, "ZZ");
                    if (phoneUtil.isValidNumber(number)) {
                        detectedCountry = phoneUtil.getRegionCodeForNumber(number);
                    }
                }
            } catch (NumberParseException e) {
                // Essayer en mode international
                number = phoneUtil.parse(phoneNumber, "ZZ");
                if (phoneUtil.isValidNumber(number)) {
                    detectedCountry = phoneUtil.getRegionCodeForNumber(number);
                }
            }

            if (phoneUtil.isValidNumber(number)) {
                textViewValidation.setVisibility(View.VISIBLE);
                textViewValidation.setTextColor(0xFF00AA00);
                String countryName = detectedCountry.isEmpty() ? "" : " (" + getCountryName(detectedCountry) + ")";
                textViewValidation.setText("✓ Numéro valide : " +
                        phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL) + countryName);
            } else {
                textViewValidation.setVisibility(View.VISIBLE);
                textViewValidation.setTextColor(0xFFFF0000);
                textViewValidation.setText("✗ Numéro invalide");
            }
        } catch (NumberParseException e) {
            textViewValidation.setVisibility(View.VISIBLE);
            textViewValidation.setTextColor(0xFFFF6600);
            textViewValidation.setText("⚠ Format incomplet ou incorrect");
        }
    }

    private void openWhatsApp() {
        String phoneNumber = editTextPhone.getText().toString().trim();

        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer un numéro de téléphone", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Phonenumber.PhoneNumber number;

            // Essayer d'abord de parser avec le pays sélectionné
            try {
                number = phoneUtil.parse(phoneNumber, selectedCountryCode);

                // Si le numéro n'est pas valide pour le pays sélectionné,
                // essayer de le parser sans pays (format international requis)
                if (!phoneUtil.isValidNumber(number)) {
                    // Essayer en mode international (avec indicatif)
                    number = phoneUtil.parse(phoneNumber, "ZZ");
                }
            } catch (NumberParseException e) {
                // Si échec, essayer en mode international
                number = phoneUtil.parse(phoneNumber, "ZZ");
            }

            // Vérifier que le numéro est valide
            if (!phoneUtil.isValidNumber(number)) {
                Toast.makeText(this, "Le numéro n'est pas valide", Toast.LENGTH_SHORT).show();
                return;
            }

            // Formater le numéro au format E164 (ex: +33612345678)
            String formattedNumber = phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
            String detectedCountry = phoneUtil.getRegionCodeForNumber(number);
            String countryName = getCountryName(detectedCountry);

            // Ajouter à l'historique
            historyManager.addToHistory(formattedNumber, detectedCountry, countryName);
            // Mettre à jour le widget
            WhatsAppWidget.updateAllWidgets(this);

            // Retirer le + pour WhatsApp
            String cleanNumber = formattedNumber.replace("+", "");

            // Ouvrir WhatsApp
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://wa.me/" + cleanNumber));
                intent.setPackage("com.whatsapp");
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "WhatsApp n'est pas installé, ouverture dans le navigateur...",
                        Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://wa.me/" + cleanNumber));
                startActivity(intent);
            }

        } catch (NumberParseException e) {
            Toast.makeText(this, "Format de numéro invalide. Utilisez le format international (+33...) ou sélectionnez le bon pays",
                    Toast.LENGTH_LONG).show();
        }
    }

    private String getCountryName(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) {
            return "";
        }
        Locale locale = new Locale("", countryCode);
        return locale.getDisplayCountry(Locale.FRENCH);
    }

    // Classe interne pour les éléments du Spinner
    private static class CountryItem {
        private String name;
        private String code;

        public CountryItem(String name, String code) {
            this.name = name;
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}