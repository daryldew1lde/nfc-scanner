package com.example.test;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.Manifest;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.EnvironmentCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    /* access modifiers changed from: private */
    public boolean isAuthenticationDone = false;
    private static final int REQUEST_INTERNET_PERMISSION = 1;
    /* access modifiers changed from: private */
    public boolean isPopupShowing = false;
    private Tag mytag;

    private SharedPreferences sharedPreferences;

    private NfcAdapter nfcAdapter;
    /* access modifiers changed from: private */
    public TextView nfcContents;

    public interface CredentialsValidationListener {
        void onCredentialsValidated(String str);
    }

    interface EmployeeListListener {
        void onEmployeeListReceived(List<Staff> list);
    }

    /* access modifiers changed from: protected */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

         SharedPreferences sharedPreferences = getSharedPreferences("my_app_preferences", 0);
            if (!sharedPreferences.contains("request_address")) {
                // Key doesn't exist, so insert the value
                sharedPreferences.edit().putString("request_address", "https://controlaccess.fgc-leasing.com").apply();
            }


        setContentView(R.layout.auth_page);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            // Request the permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, REQUEST_INTERNET_PERMISSION);
        }

        final EditText usernameEditText = (EditText) findViewById(R.id.id);
        final EditText passwordEditText = (EditText) findViewById(R.id.password);
        ((Button) findViewById(R.id.login_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MainActivity.this.isValidCredentials(usernameEditText.getText().toString(), passwordEditText.getText().toString(), new CredentialsValidationListener() {
                    public void onCredentialsValidated(String status) {
                        System.out.println(status);
                        if (status == null) {
                            Toast.makeText(MainActivity.this, "Erreur de connnection", Toast.LENGTH_SHORT).show();
                        } else if (!status.equals("failed")) {
                            System.out.println(status);
                            boolean unused = MainActivity.this.isAuthenticationDone = true;
                            if (status.equals("fgcl")) {
                                MainActivity.this.setContentView(R.layout.content_main);
                                MainActivity.this.initializeMainPageUser();
                                return;
                            }
                            MainActivity.this.setContentView(R.layout.activity_main);
                            MainActivity.this.initializeMainPageAdmin();
                        } else {
                            Toast.makeText(MainActivity.this, "Mot de passe ou id invalide", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    /* access modifiers changed from: private */
    public void isValidCredentials(String id, String password, final CredentialsValidationListener listener) {
        SharedPreferences sharedPreferences = getSharedPreferences("my_app_preferences", 0);

        String retrievedString = sharedPreferences.getString("request_address", "https://controlaccess.fgc-leasing.com");
        Log.d("MyActivity", "Retrieved string: " + retrievedString);

        ApiClient.sendGetRequest(retrievedString + "/authenticate?id=" + id + "&password=" + password, new ApiClient.ApiResponseListener() {
            public void onResponseReceived(String response) throws InterruptedException, JSONException {
                try {
                    listener.onCredentialsValidated(new JSONObject(response).getString("mes"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    listener.onCredentialsValidated((String) null);
                }
            }

            public void onError(String errorMessage) {
                listener.onCredentialsValidated((String) null);
            }
        });
    }

    /* access modifiers changed from: private */
    public void initializeMainPageUser() {
        TextView textView = (TextView) findViewById(R.id.nfc_contents);
        nfcContents = textView;
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        nfcContents.setGravity(17);
    }

    /* access modifiers changed from: private */
    public void getEmployeeList(final EmployeeListListener listener) {
        SharedPreferences sharedPreferences = getSharedPreferences("my_app_preferences", 0);

        String retrievedString = sharedPreferences.getString("request_address", "https://controlaccess.fgc-leasing.com");
        Log.d("MyActivity", "Retrieved string: " + retrievedString);

        ApiClient.sendGetRequest(retrievedString+ "/getall", new ApiClient.ApiResponseListener() {
            public void onResponseReceived(String response) throws InterruptedException {
                List<Staff> staff = new ArrayList<>();
                JSONException e;
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray employeesArray = jsonObject.getJSONArray("employees");
                    JSONArray internArray = jsonObject.getJSONArray("stagiaires");
                    for (int i = 0; i < employeesArray.length(); i++) {
                        JSONObject empObj = employeesArray.getJSONObject(i);
                        String id = empObj.getString("matricule");
                        String name = empObj.getString("nom");
                        staff.add(new Staff(id, name + " " + empObj.getString("prenom")));
                    }
                    for (int i2 = 0; i2 < internArray.length(); i2++) {
                        JSONObject internObj = internArray.getJSONObject(i2);
                        String id2 = internObj.getString("matricule");
                        String name2 = internObj.getString("nom");
                        staff.add(new Staff(id2, name2 + " " + internObj.getString("prenom")));
                    }
                    listener.onEmployeeListReceived(staff);
                } catch (JSONException e2) {
                    e = e2;
                    e.printStackTrace();
                }
            }

            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, "Error fetching data", Toast.LENGTH_SHORT).show();
                listener.onEmployeeListReceived(new ArrayList<>());
            }
        });
    }

    /* access modifiers changed from: private */
    public void initializeMainPageAdmin() {
        SharedPreferences sharedPreferences = getSharedPreferences("my_app_preferences", 0);
        TextView textView = (TextView) findViewById(R.id.nfc_contents);
        this.nfcContents = textView;
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        this.nfcContents.setGravity(17);
        ((Button) findViewById(R.id.ActivateButton)).setOnClickListener(new View.OnClickListener() {
            /* access modifiers changed from: private */
            public PopupWindow popupWindow;

            public void onClick(View v) {
                if (!MainActivity.this.isPopupShowing) {
                    View popupView = MainActivity.this.getLayoutInflater().inflate(R.layout.popup_layout, (ViewGroup) null);
                    PopupWindow popupWindow2 = new PopupWindow(popupView, -1, -1);
                    this.popupWindow = popupWindow2;
                    popupWindow2.setBackgroundDrawable(new ColorDrawable(-1));
                    this.popupWindow.setFocusable(true);
                    boolean unused = MainActivity.this.isPopupShowing = true;
                    final LinearLayout popupLayout = (LinearLayout) popupView.findViewById(R.id.popup_layout);
                    final LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                    List[] listArr = {null};
                    MainActivity.this.getEmployeeList(new EmployeeListListener() {
                        public void onEmployeeListReceived(List<Staff> staffList) {
                            if (staffList != null) {
                                for (Staff employee : staffList) {
                                    View listItemView = inflater.inflate(R.layout.list_item, (ViewGroup) null);
                                    TextView textView = (TextView) listItemView.findViewById(R.id.list_item_text);
                                    textView.setText(employee.getName());
                                    final Staff finalEmployee = employee;
                                    System.out.println(employee);
                                    textView.setOnClickListener(new View.OnClickListener() {
                                        public void onClick(View v) {
                                            String employeeId = finalEmployee.getId();
                                            MainActivity.this.writeNdefMessage(employeeId);
                                            popupWindow.dismiss();
                                            Toast.makeText(MainActivity.this, "Employee ID: " + employeeId, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    popupLayout.addView(listItemView);
                                }
                                return;
                            }
                            Toast.makeText(MainActivity.this, "Error fetching data", Toast.LENGTH_SHORT).show();
                        }
                    });
                    this.popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                        public void onDismiss() {
                            boolean unused = MainActivity.this.isPopupShowing = false;
                            MainActivity.this.nfcContents.setText("PLACEZ VOTRE CARTE AU NIVEAU DU LECTEUR POUR LA LIRE");
                        }
                    });
                    this.popupWindow.showAtLocation(v, 17, 0, 0);
                }
            }
        });
        ((Button) findViewById(R.id.addressButton)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                MainActivity.this.setContentView(R.layout.address_change);
                final EditText addressEditText = (EditText) findViewById(R.id.address);
                String retrievedString = sharedPreferences.getString("request_address", "https://controlaccess.fgc-leasing.com");
                Log.d("MyActivity", "Retrieved string: " + retrievedString);
                addressEditText.setText(retrievedString);
                ((Button) findViewById(R.id.saveButton)).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        String myString = addressEditText.getText().toString();
                        sharedPreferences.edit().putString("request_address", myString).apply();
                        Log.d("MyActivity", "Storedstring: " + myString);
                        Toast.makeText(MainActivity.this, "Enregistré avec success: "  , Toast.LENGTH_SHORT).show();
                    }
                });


            }
        });
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        super.onResume();
        enableForegroundDispatch();
    }

    /* access modifiers changed from: protected */
    public void onPause() {
        super.onPause();
        disableForegroundDispatch();
    }

    private void enableForegroundDispatch() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        IntentFilter[] intentFiltersArray = new IntentFilter[]{
                new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        };
        String[][] techListsArray = new String[][]{
                new String[]{
                        android.nfc.tech.Ndef.class.getName(),
                        android.nfc.tech.NdefFormatable.class.getName()
                }
        };
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray);
    }

    private void disableForegroundDispatch() {
        nfcAdapter.disableForegroundDispatch(this);
    }

    /* access modifiers changed from: protected */

    public void onNewIntent(Intent intent) {
        SharedPreferences sharedPreferences = getSharedPreferences("my_app_preferences", 0);
        System.out.println(intent.getAction());
        System.out.println(NfcAdapter.ACTION_TAG_DISCOVERED);
        NdefMessage ndefMessage;
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {

            System.out.println("on our way");
            Tag tag = (Tag) intent.getParcelableExtra("android.nfc.extra.TAG");

            this.mytag = tag;
            if (tag != null && (ndefMessage = readNdefMessage(tag)) != null) {
                String matricule = "";
                for (NdefRecord record : ndefMessage.getRecords()) {
                    matricule = new String(record.getPayload(), StandardCharsets.UTF_8).substring(3);
                }
                Log.d("NFCActivity", matricule);
                String type = checkMatriculeFormat(matricule);
                if (!isPopupShowing) {
                    System.out.println("not showing");

                    String retrievedString = sharedPreferences.getString("request_address", "https://controlaccess.fgc-leasing.com");
                    Log.d("MyActivity", "Retrieved string: " + retrievedString);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ApiClient.sendGetRequest(retrievedString + "/insertArriveRetour?type=" + type + "&matricule=" + matricule + "&date=" + getCurrentDate() + "&time=" + getCurrentTime(), new ApiClient.ApiResponseListener() {
                            public void onResponseReceived(String response) throws InterruptedException {
                                nfcContents.setTextSize(27.0f);
                                nfcContents.setText("Enregistré avec succes");
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        nfcContents.setText("PLACEZ VOTRE CARTE AU NIVEAU DU LECTEUR POUR LA LIRE");
                                    }
                                }, 7000);
                                System.out.println(response);
                            }

                            public void onError(String errorMessage) {
                                MainActivity.this.nfcContents.setText("Echec de l'enregistrement \nveuillez reésayer");
                            }
                        });
                    }
                }
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private static String getCurrentTime() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static String getCurrentDate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        return null;
    }

    private static String checkMatriculeFormat(String matricule) {
        Pattern stagiarePattern = Pattern.compile("^STA\\d{3}$");
        Pattern employePattern = Pattern.compile("^EMP\\d{3}$");
        Matcher stagiareMatcher = stagiarePattern.matcher(matricule);
        Matcher employeMatcher = employePattern.matcher(matricule);
        if (stagiareMatcher.matches()) {
            return "stagiaire";
        }
        if (employeMatcher.matches()) {
            return "employe";
        }
        return EnvironmentCompat.MEDIA_UNKNOWN;
    }

    private NdefMessage readNdefMessage(Tag tag) {
        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            return null;
        }
        try {
            ndef.connect();
            NdefMessage ndefMessage = ndef.getNdefMessage();
            try {
                ndef.close();
            } catch (Exception e) {
                Log.e("NFCActivity", "Error closing NDEF connection: " + e.getMessage());
            }
            return ndefMessage;
        } catch (Exception e2) {
            Log.e("NFCActivity", "Error reading NDEF message: " + e2.getMessage());
            try {
                ndef.close();
                return null;
            } catch (Exception e3) {
                Log.e("NFCActivity", "Error closing NDEF connection: " + e3.getMessage());
                return null;
            }
        } catch (Throwable th) {
            try {
                ndef.close();
            } catch (Exception e4) {
                Log.e("NFCActivity", "Error closing NDEF connection: " + e4.getMessage());
            }
            throw th;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        int length = bytes.length;
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02x", new Object[]{Byte.valueOf(bytes[i])}));
            sb.append(":");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    /* access modifiers changed from: private */
    public void writeNdefMessage(String message) {
        if (this.mytag != null) {
            writeNdefMessageToTag(createNdefMessage(message), this.mytag);
        } else {
            Toast.makeText(this, "Veuillez placer la carte au niveau du lecteur", Toast.LENGTH_SHORT).show();
        }
    }

    private NdefMessage createNdefMessage(String message) {
        return new NdefMessage(new NdefRecord[]{NdefRecord.createTextRecord((String) null, message)});
    }

    private void writeNdefMessageToTag(NdefMessage message, Tag tag) {
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (ndef.isWritable()) {
                    ndef.writeNdefMessage(message);
                    Toast.makeText(this, "Message written to NFC tag", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "NFC tag is read-only", Toast.LENGTH_SHORT).show();
                }
                ndef.close();
                return;
            }
            Toast.makeText(this, "NFC tag is not compatible with NDEF", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("MainActivity", "Error writing NDEF message to tag: " + e.getMessage());
            Toast.makeText(this, "Error writing NDEF message to tag", Toast.LENGTH_SHORT).show();
        }
    }
}
