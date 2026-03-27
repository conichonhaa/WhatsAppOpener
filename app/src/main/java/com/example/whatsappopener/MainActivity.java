package com.example.whatsappopener;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private AutoCompleteTextView editTextPhone;
    private Button buttonOpenWhatsApp;
    private Button buttonStats;
    private Button buttonCountry;
    private Button buttonClearPhone;
    private TextView textViewValidation;
    private LinearLayout clipboardSuggestion;
    private TextView textClipboardNumber;
    private Button buttonUseClipboard;
    private ListView listViewHistory;
    private PhoneNumberUtil phoneUtil;
    private String selectedCountryCode = "FR";
    private HistoryManager historyManager;
    private List<CountryItem> countryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneUtil = PhoneNumberUtil.getInstance();
        historyManager = new HistoryManager(this);

        editTextPhone = findViewById(R.id.editTextPhone);
        buttonOpenWhatsApp = findViewById(R.id.buttonOpenWhatsApp);
        buttonStats = findViewById(R.id.buttonStats);
        buttonCountry = findViewById(R.id.buttonCountry);
        buttonClearPhone = findViewById(R.id.buttonClearPhone);
        textViewValidation = findViewById(R.id.textViewValidation);
        clipboardSuggestion = findViewById(R.id.clipboardSuggestion);
        textClipboardNumber = findViewById(R.id.textClipboardNumber);
        buttonUseClipboard = findViewById(R.id.buttonUseClipboard);
        listViewHistory = findViewById(R.id.listViewHistory);

        setupCountryButton();
        checkClipboard();
        setupAutoComplete();
        refreshHistory();

        editTextPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString();
                buttonClearPhone.setVisibility(text.length() > 0 ? View.VISIBLE : View.GONE);
                if (text.length() >= 8) {
                    autoDetectCountry(text);
                }
                validatePhoneNumber(text);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        buttonClearPhone.setOnClickListener(v -> {
            editTextPhone.setText("");
            editTextPhone.requestFocus();
        });

        buttonUseClipboard.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                String text = item.getText().toString();
                editTextPhone.setText(text);
                clipboardSuggestion.setVisibility(View.GONE);
            }
        });

        buttonOpenWhatsApp.setOnClickListener(v -> openWhatsApp());

        buttonStats.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StatsActivity.class);
            startActivity(intent);
        });

        listViewHistory.setOnItemClickListener((AdapterView.OnItemClickListener) (parent, view, position, id) -> {
            @SuppressWarnings("unchecked")
            HashMap<String, String> item = (HashMap<String, String>) parent.getItemAtPosition(position);
            editTextPhone.setText(item.get("number"));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkClipboard();
        refreshHistory();
    }

    private void autoDetectCountry(String phoneNumber) {
        if (phoneNumber.startsWith("+")) {
            // Format international : détecter via l'indicatif
            try {
                Phonenumber.PhoneNumber number = phoneUtil.parse(phoneNumber, "ZZ");
                if (phoneUtil.isValidNumber(number)) {
                    String detected = phoneUtil.getRegionCodeForNumber(number);
                    if (detected != null && !detected.equals(selectedCountryCode)) {
                        updateSelectedCountry(detected);
                    }
                }
            } catch (NumberParseException e) {
                // Pas encore assez de chiffres
            }
        } else {
            int len = phoneNumber.replaceAll("[^0-9]", "").length();
            // Un numéro à 9 chiffres sans 0 initial = format local luxembourgeois.
            // libphonenumber valide aussi ce format comme français (+33 6xx...) ce qui
            // crée une ambiguïté : on vérifie LU en priorité absolue dans ce cas.
            boolean isLikelyLuxembourg = (len == 9 && !phoneNumber.startsWith("0"));
            if (isLikelyLuxembourg) {
                try {
                    Phonenumber.PhoneNumber num = phoneUtil.parse(phoneNumber, "LU");
                    if (phoneUtil.isValidNumber(num)) {
                        if (!selectedCountryCode.equals("LU")) {
                            updateSelectedCountry("LU");
                        }
                        return;
                    }
                } catch (NumberParseException e) {
                    // Pas un numéro luxembourgeois, continuer
                }
            }

            // Vérifier si le pays actuel reconnaît déjà le numéro
            boolean currentValid = false;
            try {
                Phonenumber.PhoneNumber num = phoneUtil.parse(phoneNumber, selectedCountryCode);
                currentValid = phoneUtil.isValidNumber(num);
            } catch (NumberParseException e) {
                // Le pays actuel ne reconnaît pas ce numéro
            }

            if (!currentValid) {
                // Essayer France (06xxxxxxxx, 07xxxxxxxx avec 0 initial = 10 chiffres)
                if (!selectedCountryCode.equals("FR")) {
                    try {
                        Phonenumber.PhoneNumber num = phoneUtil.parse(phoneNumber, "FR");
                        if (phoneUtil.isValidNumber(num)) {
                            updateSelectedCountry("FR");
                            return;
                        }
                    } catch (NumberParseException e) {
                        // Pas un numéro français
                    }
                }
                // Essayer Luxembourg
                if (!selectedCountryCode.equals("LU")) {
                    try {
                        Phonenumber.PhoneNumber num = phoneUtil.parse(phoneNumber, "LU");
                        if (phoneUtil.isValidNumber(num)) {
                            updateSelectedCountry("LU");
                        }
                    } catch (NumberParseException e) {
                        // Pas un numéro luxembourgeois
                    }
                }
            }
        }
    }

    private void updateSelectedCountry(String countryCode) {
        selectedCountryCode = countryCode;
        if (countryList != null) {
            for (CountryItem item : countryList) {
                if (item.getCode().equals(countryCode)) {
                    buttonCountry.setText(item.toString());
                    return;
                }
            }
        }
        buttonCountry.setText(countryCode);
    }

    private void checkClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            String text = item.getText().toString();

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
                new int[]{android.R.id.text1, android.R.id.text2});
        listViewHistory.setAdapter(adapter);
    }

    private void setupCountryButton() {
        countryList = new ArrayList<>();

        // Europe
        countryList.add(new CountryItem("🇦🇱 Albanie", "AL"));
        countryList.add(new CountryItem("🇩🇪 Allemagne", "DE"));
        countryList.add(new CountryItem("🇦🇩 Andorre", "AD"));
        countryList.add(new CountryItem("🇦🇹 Autriche", "AT"));
        countryList.add(new CountryItem("🇧🇪 Belgique", "BE"));
        countryList.add(new CountryItem("🇧🇾 Biélorussie", "BY"));
        countryList.add(new CountryItem("🇧🇦 Bosnie-Herzégovine", "BA"));
        countryList.add(new CountryItem("🇧🇬 Bulgarie", "BG"));
        countryList.add(new CountryItem("🇭🇷 Croatie", "HR"));
        countryList.add(new CountryItem("🇩🇰 Danemark", "DK"));
        countryList.add(new CountryItem("🇪🇸 Espagne", "ES"));
        countryList.add(new CountryItem("🇪🇪 Estonie", "EE"));
        countryList.add(new CountryItem("🇫🇮 Finlande", "FI"));
        countryList.add(new CountryItem("🇫🇷 France", "FR"));
        countryList.add(new CountryItem("🇬🇷 Grèce", "GR"));
        countryList.add(new CountryItem("🇭🇺 Hongrie", "HU"));
        countryList.add(new CountryItem("🇮🇪 Irlande", "IE"));
        countryList.add(new CountryItem("🇮🇸 Islande", "IS"));
        countryList.add(new CountryItem("🇮🇹 Italie", "IT"));
        countryList.add(new CountryItem("🇽🇰 Kosovo", "XK"));
        countryList.add(new CountryItem("🇱🇻 Lettonie", "LV"));
        countryList.add(new CountryItem("🇱🇮 Liechtenstein", "LI"));
        countryList.add(new CountryItem("🇱🇹 Lituanie", "LT"));
        countryList.add(new CountryItem("🇱🇺 Luxembourg", "LU"));
        countryList.add(new CountryItem("🇲🇰 Macédoine du Nord", "MK"));
        countryList.add(new CountryItem("🇲🇹 Malte", "MT"));
        countryList.add(new CountryItem("🇲🇩 Moldavie", "MD"));
        countryList.add(new CountryItem("🇲🇨 Monaco", "MC"));
        countryList.add(new CountryItem("🇲🇪 Monténégro", "ME"));
        countryList.add(new CountryItem("🇳🇴 Norvège", "NO"));
        countryList.add(new CountryItem("🇳🇱 Pays-Bas", "NL"));
        countryList.add(new CountryItem("🇵🇱 Pologne", "PL"));
        countryList.add(new CountryItem("🇵🇹 Portugal", "PT"));
        countryList.add(new CountryItem("🇨🇿 République tchèque", "CZ"));
        countryList.add(new CountryItem("🇷🇴 Roumanie", "RO"));
        countryList.add(new CountryItem("🇬🇧 Royaume-Uni", "GB"));
        countryList.add(new CountryItem("🇷🇺 Russie", "RU"));
        countryList.add(new CountryItem("🇷🇸 Serbie", "RS"));
        countryList.add(new CountryItem("🇸🇰 Slovaquie", "SK"));
        countryList.add(new CountryItem("🇸🇮 Slovénie", "SI"));
        countryList.add(new CountryItem("🇸🇪 Suède", "SE"));
        countryList.add(new CountryItem("🇨🇭 Suisse", "CH"));
        countryList.add(new CountryItem("🇺🇦 Ukraine", "UA"));
        countryList.add(new CountryItem("🇻🇦 Vatican", "VA"));

        // Afrique
        countryList.add(new CountryItem("🇿🇦 Afrique du Sud", "ZA"));
        countryList.add(new CountryItem("🇩🇿 Algérie", "DZ"));
        countryList.add(new CountryItem("🇦🇴 Angola", "AO"));
        countryList.add(new CountryItem("🇧🇯 Bénin", "BJ"));
        countryList.add(new CountryItem("🇧🇼 Botswana", "BW"));
        countryList.add(new CountryItem("🇧🇫 Burkina Faso", "BF"));
        countryList.add(new CountryItem("🇧🇮 Burundi", "BI"));
        countryList.add(new CountryItem("🇨🇲 Cameroun", "CM"));
        countryList.add(new CountryItem("🇨🇻 Cap-Vert", "CV"));
        countryList.add(new CountryItem("🇨🇫 Centrafrique", "CF"));
        countryList.add(new CountryItem("🇰🇲 Comores", "KM"));
        countryList.add(new CountryItem("🇨🇬 Congo", "CG"));
        countryList.add(new CountryItem("🇨🇩 Congo (RDC)", "CD"));
        countryList.add(new CountryItem("🇨🇮 Côte d'Ivoire", "CI"));
        countryList.add(new CountryItem("🇩🇯 Djibouti", "DJ"));
        countryList.add(new CountryItem("🇪🇬 Égypte", "EG"));
        countryList.add(new CountryItem("🇪🇷 Érythrée", "ER"));
        countryList.add(new CountryItem("🇪🇹 Éthiopie", "ET"));
        countryList.add(new CountryItem("🇬🇦 Gabon", "GA"));
        countryList.add(new CountryItem("🇬🇲 Gambie", "GM"));
        countryList.add(new CountryItem("🇬🇭 Ghana", "GH"));
        countryList.add(new CountryItem("🇬🇳 Guinée", "GN"));
        countryList.add(new CountryItem("🇬🇼 Guinée-Bissau", "GW"));
        countryList.add(new CountryItem("🇬🇶 Guinée équatoriale", "GQ"));
        countryList.add(new CountryItem("🇰🇪 Kenya", "KE"));
        countryList.add(new CountryItem("🇱🇸 Lesotho", "LS"));
        countryList.add(new CountryItem("🇱🇷 Libéria", "LR"));
        countryList.add(new CountryItem("🇱🇾 Libye", "LY"));
        countryList.add(new CountryItem("🇲🇬 Madagascar", "MG"));
        countryList.add(new CountryItem("🇲🇼 Malawi", "MW"));
        countryList.add(new CountryItem("🇲🇱 Mali", "ML"));
        countryList.add(new CountryItem("🇲🇦 Maroc", "MA"));
        countryList.add(new CountryItem("🇲🇺 Maurice", "MU"));
        countryList.add(new CountryItem("🇲🇷 Mauritanie", "MR"));
        countryList.add(new CountryItem("🇲🇿 Mozambique", "MZ"));
        countryList.add(new CountryItem("🇳🇦 Namibie", "NA"));
        countryList.add(new CountryItem("🇳🇪 Niger", "NE"));
        countryList.add(new CountryItem("🇳🇬 Nigéria", "NG"));
        countryList.add(new CountryItem("🇺🇬 Ouganda", "UG"));
        countryList.add(new CountryItem("🇷🇼 Rwanda", "RW"));
        countryList.add(new CountryItem("🇸🇹 Sao Tomé-et-Principe", "ST"));
        countryList.add(new CountryItem("🇸🇳 Sénégal", "SN"));
        countryList.add(new CountryItem("🇸🇨 Seychelles", "SC"));
        countryList.add(new CountryItem("🇸🇱 Sierra Leone", "SL"));
        countryList.add(new CountryItem("🇸🇴 Somalie", "SO"));
        countryList.add(new CountryItem("🇸🇸 Soudan du Sud", "SS"));
        countryList.add(new CountryItem("🇸🇩 Soudan", "SD"));
        countryList.add(new CountryItem("🇸🇿 Eswatini", "SZ"));
        countryList.add(new CountryItem("🇹🇿 Tanzanie", "TZ"));
        countryList.add(new CountryItem("🇹🇩 Tchad", "TD"));
        countryList.add(new CountryItem("🇹🇬 Togo", "TG"));
        countryList.add(new CountryItem("🇹🇳 Tunisie", "TN"));
        countryList.add(new CountryItem("🇿🇲 Zambie", "ZM"));
        countryList.add(new CountryItem("🇿🇼 Zimbabwe", "ZW"));

        // Amérique du Nord
        countryList.add(new CountryItem("🇨🇦 Canada", "CA"));
        countryList.add(new CountryItem("🇺🇸 États-Unis", "US"));
        countryList.add(new CountryItem("🇲🇽 Mexique", "MX"));

        // Amérique Centrale et Caraïbes
        countryList.add(new CountryItem("🇧🇸 Bahamas", "BS"));
        countryList.add(new CountryItem("🇧🇧 Barbade", "BB"));
        countryList.add(new CountryItem("🇧🇿 Belize", "BZ"));
        countryList.add(new CountryItem("🇨🇷 Costa Rica", "CR"));
        countryList.add(new CountryItem("🇨🇺 Cuba", "CU"));
        countryList.add(new CountryItem("🇩🇴 République dominicaine", "DO"));
        countryList.add(new CountryItem("🇸🇻 Salvador", "SV"));
        countryList.add(new CountryItem("🇬🇹 Guatemala", "GT"));
        countryList.add(new CountryItem("🇭🇹 Haïti", "HT"));
        countryList.add(new CountryItem("🇭🇳 Honduras", "HN"));
        countryList.add(new CountryItem("🇯🇲 Jamaïque", "JM"));
        countryList.add(new CountryItem("🇳🇮 Nicaragua", "NI"));
        countryList.add(new CountryItem("🇵🇦 Panama", "PA"));
        countryList.add(new CountryItem("🇵🇷 Porto Rico", "PR"));
        countryList.add(new CountryItem("🇹🇹 Trinité-et-Tobago", "TT"));

        // Amérique du Sud
        countryList.add(new CountryItem("🇦🇷 Argentine", "AR"));
        countryList.add(new CountryItem("🇧🇴 Bolivie", "BO"));
        countryList.add(new CountryItem("🇧🇷 Brésil", "BR"));
        countryList.add(new CountryItem("🇨🇱 Chili", "CL"));
        countryList.add(new CountryItem("🇨🇴 Colombie", "CO"));
        countryList.add(new CountryItem("🇪🇨 Équateur", "EC"));
        countryList.add(new CountryItem("🇬🇾 Guyana", "GY"));
        countryList.add(new CountryItem("🇵🇾 Paraguay", "PY"));
        countryList.add(new CountryItem("🇵🇪 Pérou", "PE"));
        countryList.add(new CountryItem("🇸🇷 Suriname", "SR"));
        countryList.add(new CountryItem("🇺🇾 Uruguay", "UY"));
        countryList.add(new CountryItem("🇻🇪 Venezuela", "VE"));

        // Asie
        countryList.add(new CountryItem("🇦🇫 Afghanistan", "AF"));
        countryList.add(new CountryItem("🇸🇦 Arabie saoudite", "SA"));
        countryList.add(new CountryItem("🇦🇲 Arménie", "AM"));
        countryList.add(new CountryItem("🇦🇿 Azerbaïdjan", "AZ"));
        countryList.add(new CountryItem("🇧🇭 Bahreïn", "BH"));
        countryList.add(new CountryItem("🇧🇩 Bangladesh", "BD"));
        countryList.add(new CountryItem("🇧🇹 Bhoutan", "BT"));
        countryList.add(new CountryItem("🇧🇳 Brunei", "BN"));
        countryList.add(new CountryItem("🇰🇭 Cambodge", "KH"));
        countryList.add(new CountryItem("🇨🇳 Chine", "CN"));
        countryList.add(new CountryItem("🇰🇵 Corée du Nord", "KP"));
        countryList.add(new CountryItem("🇰🇷 Corée du Sud", "KR"));
        countryList.add(new CountryItem("🇦🇪 Émirats arabes unis", "AE"));
        countryList.add(new CountryItem("🇬🇪 Géorgie", "GE"));
        countryList.add(new CountryItem("🇭🇰 Hong Kong", "HK"));
        countryList.add(new CountryItem("🇮🇳 Inde", "IN"));
        countryList.add(new CountryItem("🇮🇩 Indonésie", "ID"));
        countryList.add(new CountryItem("🇮🇶 Irak", "IQ"));
        countryList.add(new CountryItem("🇮🇷 Iran", "IR"));
        countryList.add(new CountryItem("🇮🇱 Israël", "IL"));
        countryList.add(new CountryItem("🇯🇵 Japon", "JP"));
        countryList.add(new CountryItem("🇯🇴 Jordanie", "JO"));
        countryList.add(new CountryItem("🇰🇿 Kazakhstan", "KZ"));
        countryList.add(new CountryItem("🇰🇬 Kirghizistan", "KG"));
        countryList.add(new CountryItem("🇰🇼 Koweït", "KW"));
        countryList.add(new CountryItem("🇱🇦 Laos", "LA"));
        countryList.add(new CountryItem("🇱🇧 Liban", "LB"));
        countryList.add(new CountryItem("🇲🇴 Macao", "MO"));
        countryList.add(new CountryItem("🇲🇾 Malaisie", "MY"));
        countryList.add(new CountryItem("🇲🇻 Maldives", "MV"));
        countryList.add(new CountryItem("🇲🇳 Mongolie", "MN"));
        countryList.add(new CountryItem("🇲🇲 Myanmar", "MM"));
        countryList.add(new CountryItem("🇳🇵 Népal", "NP"));
        countryList.add(new CountryItem("🇴🇲 Oman", "OM"));
        countryList.add(new CountryItem("🇵🇰 Pakistan", "PK"));
        countryList.add(new CountryItem("🇵🇸 Palestine", "PS"));
        countryList.add(new CountryItem("🇵🇭 Philippines", "PH"));
        countryList.add(new CountryItem("🇶🇦 Qatar", "QA"));
        countryList.add(new CountryItem("🇸🇬 Singapour", "SG"));
        countryList.add(new CountryItem("🇱🇰 Sri Lanka", "LK"));
        countryList.add(new CountryItem("🇸🇾 Syrie", "SY"));
        countryList.add(new CountryItem("🇹🇯 Tadjikistan", "TJ"));
        countryList.add(new CountryItem("🇹🇼 Taïwan", "TW"));
        countryList.add(new CountryItem("🇹🇭 Thaïlande", "TH"));
        countryList.add(new CountryItem("🇹🇱 Timor oriental", "TL"));
        countryList.add(new CountryItem("🇹🇷 Turquie", "TR"));
        countryList.add(new CountryItem("🇹🇲 Turkménistan", "TM"));
        countryList.add(new CountryItem("🇺🇿 Ouzbékistan", "UZ"));
        countryList.add(new CountryItem("🇻🇳 Viêt Nam", "VN"));
        countryList.add(new CountryItem("🇾🇪 Yémen", "YE"));

        // Océanie
        countryList.add(new CountryItem("🇦🇺 Australie", "AU"));
        countryList.add(new CountryItem("🇫🇯 Fidji", "FJ"));
        countryList.add(new CountryItem("🇰🇮 Kiribati", "KI"));
        countryList.add(new CountryItem("🇲🇭 Îles Marshall", "MH"));
        countryList.add(new CountryItem("🇫🇲 Micronésie", "FM"));
        countryList.add(new CountryItem("🇳🇷 Nauru", "NR"));
        countryList.add(new CountryItem("🇳🇿 Nouvelle-Zélande", "NZ"));
        countryList.add(new CountryItem("🇵🇼 Palaos", "PW"));
        countryList.add(new CountryItem("🇵🇬 Papouasie-Nouvelle-Guinée", "PG"));
        countryList.add(new CountryItem("🇼🇸 Samoa", "WS"));
        countryList.add(new CountryItem("🇸🇧 Salomon", "SB"));
        countryList.add(new CountryItem("🇹🇴 Tonga", "TO"));
        countryList.add(new CountryItem("🇹🇻 Tuvalu", "TV"));
        countryList.add(new CountryItem("🇻🇺 Vanuatu", "VU"));

        Collections.sort(countryList, (c1, c2) -> c1.toString().compareTo(c2.toString()));

        // Définir la France comme pays par défaut
        for (CountryItem item : countryList) {
            if (item.getCode().equals("FR")) {
                buttonCountry.setText(item.toString());
                break;
            }
        }

        buttonCountry.setOnClickListener(v -> showCountrySearchDialog());
    }

    private void showCountrySearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sélectionner un pays");

        int dp = (int) getResources().getDisplayMetrics().density;

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(16 * dp, 8 * dp, 16 * dp, 0);

        final EditText searchField = new EditText(this);
        searchField.setHint("🔍 Rechercher un pays...");
        searchField.setSingleLine(true);
        searchField.setInputType(InputType.TYPE_CLASS_TEXT);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        searchParams.bottomMargin = 8 * dp;
        dialogLayout.addView(searchField, searchParams);

        final ListView listView = new ListView(this);
        final List<CountryItem> filtered = new ArrayList<>(countryList);
        final ArrayAdapter<CountryItem> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, filtered);
        listView.setAdapter(adapter);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 400 * dp);
        dialogLayout.addView(listView, listParams);

        builder.setView(dialogLayout);
        builder.setNegativeButton("Annuler", null);

        final AlertDialog dialog = builder.create();

        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase(Locale.getDefault()).trim();
                filtered.clear();
                for (CountryItem item : countryList) {
                    if (query.isEmpty() || item.toString().toLowerCase(Locale.getDefault()).contains(query)) {
                        filtered.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            CountryItem selected = filtered.get(position);
            updateSelectedCountry(selected.getCode());
            validatePhoneNumber(editTextPhone.getText().toString());
            dialog.dismiss();
        });

        dialog.show();
        searchField.requestFocus();
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber.isEmpty()) {
            textViewValidation.setVisibility(View.GONE);
            return;
        }

        Phonenumber.PhoneNumber number = null;
        String detectedCountry = "";

        if (phoneNumber.startsWith("+")) {
            try {
                number = phoneUtil.parse(phoneNumber, "ZZ");
                if (phoneUtil.isValidNumber(number)) {
                    detectedCountry = phoneUtil.getRegionCodeForNumber(number);
                } else {
                    number = null;
                }
            } catch (NumberParseException e) {
                number = null;
            }
        }

        if (number == null) {
            try {
                number = phoneUtil.parse(phoneNumber, selectedCountryCode);
                if (phoneUtil.isValidNumber(number)) {
                    detectedCountry = phoneUtil.getRegionCodeForNumber(number);
                } else {
                    number = null;
                }
            } catch (NumberParseException e) {
                number = null;
            }
        }

        if (number != null && phoneUtil.isValidNumber(number)) {
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
    }

    private void openWhatsApp() {
        String phoneNumber = editTextPhone.getText().toString().trim();

        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer un numéro de téléphone", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Phonenumber.PhoneNumber number;

            try {
                number = phoneUtil.parse(phoneNumber, selectedCountryCode);
                if (!phoneUtil.isValidNumber(number)) {
                    number = phoneUtil.parse(phoneNumber, "ZZ");
                }
            } catch (NumberParseException e) {
                number = phoneUtil.parse(phoneNumber, "ZZ");
            }

            if (!phoneUtil.isValidNumber(number)) {
                Toast.makeText(this, "Le numéro n'est pas valide", Toast.LENGTH_SHORT).show();
                return;
            }

            String formattedNumber = phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
            String detectedCountry = phoneUtil.getRegionCodeForNumber(number);
            String countryName = getCountryName(detectedCountry);

            historyManager.addToHistory(formattedNumber, detectedCountry, countryName);
            WhatsAppWidget.updateAllWidgets(this);

            String cleanNumber = formattedNumber.replace("+", "");

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

    private static class CountryItem {
        private final String name;
        private final String code;

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
