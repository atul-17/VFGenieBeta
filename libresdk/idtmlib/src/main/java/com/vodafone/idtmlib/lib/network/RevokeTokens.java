package com.vodafone.idtmlib.lib.network;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;
import android.webkit.CookieManager;

import com.vodafone.idtmlib.lib.storage.DataCrypt;
import com.vodafone.idtmlib.lib.storage.Prefs;
import com.vodafone.idtmlib.lib.storage.basic.AesException;
import com.vodafone.idtmlib.lib.storage.basic.Preferences;
import com.vodafone.idtmlib.lib.utils.Printer;
import com.vodafone.idtmlib.lib.utils.Smapi;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

public class RevokeTokens {
    private Context context;
    private Preferences preferences;
    private Printer printer;
    private DataCrypt dataCrypt;
    private Smapi smapi;
    private String smapiTransactionId = UUID.randomUUID().toString();
    private String accessToken = null;
    private String refreshToken = null;
    private String clientIdRevoke = null;
    private String authHeader = null;
    private Environment environment;

    public RevokeTokens() {
    }

    public RevokeTokens(Context context, Preferences preferences, Printer printer, DataCrypt dataCrypt, Smapi smapi, Environment environment) {
        this.context = context;
        this.printer = printer;
        this.preferences = preferences;
        this.dataCrypt = dataCrypt;
        this.smapi = smapi;
        this.environment = environment;
    }

    public void startLogout() {
        printer.i("Logout - revoke called");
        smapi.logLogout(context, smapiTransactionId);
        try {
            this.accessToken = dataCrypt.getString(Prefs.ACCESS_TOKEN);
            this.refreshToken = dataCrypt.getString(Prefs.ACCESS_TOKEN_REFRESH_TOKEN);
            this.clientIdRevoke = dataCrypt.getString(Prefs.CLIENT_ID);
        } catch (AesException e) {
            printer.e("Error while getting tokens :" + e);
            preferences.remove(Prefs.ACCESS_TOKEN, Prefs.ACCESS_TOKEN_TYPE,
                    Prefs.ACCESS_TOKEN_REFRESH_TOKEN, Prefs.ACCESS_TOKEN_EXPIRE_TIME_MS,
                    Prefs.ACCESS_TOKEN_SUB, Prefs.ACCESS_TOKEN_ACR);
            return;
        }

        CookieManager cookieManager = CookieManager.getInstance();
        printer.d("Removing cookies for sdk");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(value -> printer.d("onReceiveValue " + value));
        } else {
            cookieManager.removeAllCookie();
        }

        preferences.remove(Prefs.ACCESS_TOKEN, Prefs.ACCESS_TOKEN_TYPE,
                Prefs.ACCESS_TOKEN_REFRESH_TOKEN, Prefs.ACCESS_TOKEN_EXPIRE_TIME_MS,
                Prefs.ACCESS_TOKEN_SUB, Prefs.ACCESS_TOKEN_ACR);

        if (!TextUtils.isEmpty(accessToken) && !TextUtils.isEmpty(refreshToken) && !TextUtils.isEmpty(clientIdRevoke)) {
            this.authHeader = toBase64(clientIdRevoke);
            RevokeAccessTokenAsynTask revokeAccessTokenAsynTask = new RevokeAccessTokenAsynTask();
            revokeAccessTokenAsynTask.execute();
        } else {
            printer.i("Getting empty values for tokens or clientId, tokens are already revoked.");
        }
    }

    public class RevokeAccessTokenAsynTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            smapi.logRevokeAccessToken(context, smapiTransactionId);
            revokeToken(accessToken, "access_token");
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            printer.i("Calling Api to revoke refresh token");
            RevokeRefreshTokenAsynTask revokeRefreshAsynTask = new RevokeRefreshTokenAsynTask();
            revokeRefreshAsynTask.execute();
        }
    }

    public class RevokeRefreshTokenAsynTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            smapi.logRevokeRefreshToken(context, smapiTransactionId);
            revokeToken(refreshToken, "refresh_token");
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    public void revokeToken(String token, String token_type_hint) {
        printer.d("Revoke", "token :" + token);
        printer.d("Revoke", "token_type_hint :" + token_type_hint);
        printer.d("Revoke", "env :" + environment);

        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(environment.getRevokeUrl());
            urlConnection = (HttpURLConnection) url.openConnection();
            if (urlConnection != null && (urlConnection instanceof HttpsURLConnection)) {
                ((HttpsURLConnection) urlConnection).setSSLSocketFactory(new TLSSocketFactory());
            }
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Authorization", "Basic " + authHeader);

            if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(token_type_hint)) {
                String data = URLEncoder.encode("token_type_hint", "UTF-8")
                        + "=" + URLEncoder.encode(token_type_hint, "UTF-8");

                data += "&" + URLEncoder.encode("token", "UTF-8") + "="
                        + URLEncoder.encode(token, "UTF-8");

                urlConnection.connect();

                OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
                wr.write(data);
                wr.flush();
                smapi.logRevokeTokenSuccess(context, smapiTransactionId, token_type_hint);
                int state = urlConnection.getResponseCode();
                printer.d("Revoke", "Response code: " + state);
            } else {
                printer.i("Revoke","Tokens are empty before connecting revoke api");
            }

        } catch (KeyManagementException e) {
            smapi.logRevokeTokenFailed(context, smapiTransactionId, e.getMessage());
            printer.e(e);
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            smapi.logRevokeTokenFailed(context, smapiTransactionId, e.getMessage());
            printer.e(e);
            e.printStackTrace();
        } catch (IOException e) {
            smapi.logRevokeTokenFailed(context, smapiTransactionId, e.getMessage());
            printer.e(e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    public String toBase64(String clientId) {
        byte[] data;
        data = clientId.getBytes(StandardCharsets.UTF_8);
        String base64Sms = Base64.encodeToString(data, Base64.DEFAULT);
        return base64Sms;
    }

}
