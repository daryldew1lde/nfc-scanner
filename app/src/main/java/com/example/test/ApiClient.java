package com.example.test;

import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONException;

public class ApiClient {

    public interface ApiResponseListener {
        void onError(String str);

        void onResponseReceived(String str) throws InterruptedException, JSONException;
    }

    public static void sendGetRequest(String apiUrl, final ApiResponseListener listener) {
        new AsyncTask<String, Void, String>() {
            /* access modifiers changed from: protected */
            public String doInBackground(String... urls) {
                String result;
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try {
                    HttpURLConnection connection2 = (HttpURLConnection) new URL(urls[0]).openConnection();
                    connection2.setRequestMethod("GET");
                    int responseCode = connection2.getResponseCode();
                    if (responseCode == 200) {
                        reader = new BufferedReader(new InputStreamReader(connection2.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        while (true) {
                            String readLine = reader.readLine();
                            String line = readLine;
                            if (readLine == null) {
                                break;
                            }
                            response.append(line);
                        }
                        result = response.toString();
                    } else {
                        Log.e("API Error", "Error response code: " + responseCode);
                        result = null;
                    }
                    if (connection2 != null) {
                        connection2.disconnect();
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e2) {
                    e2.printStackTrace();
                    result = null;
                    if (connection != null) {
                        connection.disconnect();
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } catch (Throwable th) {
                    if (connection != null) {
                        connection.disconnect();
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e3) {
                            e3.printStackTrace();
                        }
                    }
                    throw th;
                }
                return result;
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(String result) {
                if (result != null) {
                    try {
                        listener.onResponseReceived(result);
                    } catch (InterruptedException | JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    listener.onError("Error occurred during API call");
                }
            }
        }.execute(new String[]{apiUrl});
    }
}
