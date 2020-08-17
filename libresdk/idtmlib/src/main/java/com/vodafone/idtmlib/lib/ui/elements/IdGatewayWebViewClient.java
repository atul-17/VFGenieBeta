package com.vodafone.idtmlib.lib.ui.elements;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.text.TextUtils;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import com.vodafone.idtmlib.lib.network.IdtmApi;
import com.vodafone.idtmlib.lib.utils.Printer;

import javax.inject.Inject;

import okhttp3.CertificatePinner;

public class IdGatewayWebViewClient extends CertificatePinnerWebViewClient {
    private static final String STATE_PARAM = "state";
    private static final String CODE_PARAM = "code";
    private static final String ERROR_PARAM = "error";
    private static final String ERROR__DESCRIPTION_PARAM = "error_description";
    private static final String ERROR_PARAM_ACCESS_DENIED = "ACCESS_DENIED";
    private static final String ERROR_USER_FAILED_TO_AUTHENTICATE = "login_required";

    private Printer printer;
    private Uri redirectUri;
    private String nonce;
    private String state;
    private IdGatewayWebviewClientCallback callback;
    private Context context;

    @Inject
    public IdGatewayWebViewClient(Printer printer, CertificatePinner certificatePinner, IdtmApi idtmApi, Context c) {
        super(printer, certificatePinner, idtmApi, c);
        this.printer = printer;
        this.context = c;
    }

    public void setRedirectConditions(Uri redirectUri, String nonce, String state,
                                      IdGatewayWebviewClientCallback callback) {
        this.redirectUri = redirectUri;
        this.nonce = nonce;
        this.state = state;
        this.callback = callback;
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description,
                                String failingUrl) {
        printer.e("Error [dep] Description: ", description, ", ErrorCode: ", errorCode);
        callback.onRedirectResult(null);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request,
                                WebResourceError error) {
        printer.e("Error Description: ", error.getDescription(), ", ErrorCode: ", error.getErrorCode());
        if (request.isForMainFrame()) {
            callback.onRedirectResult(null);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                    WebResourceResponse errorResponse) {
        printer.e("HTTP Error status code: ", errorResponse.getStatusCode());
        if (request.isForMainFrame()) {
            callback.onRedirectResult(null);
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        printer.i("Loading url [dep]: ", url);
        return checkRedirectAuthorized(Uri.parse(Uri.decode(url)));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        printer.i("Loading url: ", request.getUrl());
        return checkRedirectAuthorized(request.getUrl());
    }

    private boolean checkRedirectAuthorized(Uri uri) {

        printer.d("Method called: checkRedirectAuthorized with URI ", uri.toString());

        // extract authId if possible
        if(uri != null)
        {
            String uriString = uri.toString();
            if(uriString != null && !uriString.isEmpty())
            {
                int idx = uriString.indexOf("authorize#/trx/");
                if(idx > 0)
                {
                    String authId = uriString.substring(idx + "authorize#/trx/".length());
                    printer.d("AuthorizationId =  ", authId);
                    callback.onAuthId(authId);
                }
            }
        }
        
        printer.d("RedirectAuthorized redirectUri: " + redirectUri);
        printer.d("RedirectAuthorized state: " + state);
        printer.d("RedirectAuthorized nonce: " + nonce);

        printer.d("RedirectAuthorized redirectUri.getScheme(): " + redirectUri.getScheme());
        printer.d("RedirectAuthorized uri.getScheme(): " + uri.getScheme());

        printer.d("RedirectAuthorized redirectUri.getPort(): " + redirectUri.getPort());
        printer.d("RedirectAuthorized uri.getPort(): " + uri.getPort());

        printer.d("RedirectAuthorized redirectUri.getHost(): " + redirectUri.getHost());
        printer.d("RedirectAuthorized uri.getHost(): " + uri.getHost());

        printer.d("RedirectAuthorized redirectUri.getPath(): " + redirectUri.getPath());
        printer.d("RedirectAuthorized uri.getPath(): " + uri.getPath());

        if (redirectUri != null && !TextUtils.isEmpty(state) && !TextUtils.isEmpty(nonce) &&
                TextUtils.equals(redirectUri.getScheme(), uri.getScheme()) &&
                redirectUri.getPort() == uri.getPort() &&
                TextUtils.equals(redirectUri.getHost(), uri.getHost()) &&
                TextUtils.equals(redirectUri.getPath(), uri.getPath())) {
            String error = uri.getQueryParameter(ERROR_PARAM);
            String errorDescription = uri.getQueryParameter(ERROR__DESCRIPTION_PARAM);
            if (TextUtils.isEmpty(error)) {
                String uriState = uri.getQueryParameter(STATE_PARAM);
                String uriCode = uri.getQueryParameter(CODE_PARAM);
                if (TextUtils.equals(state, uriState)) {
                    if (TextUtils.isEmpty(uriCode)) {
                        printer.e("Redirect error: Missing Code parameter");
                        callback.onRedirectResult(null);
                    } else {
                        printer.i("Redirect successful with code: ", uriCode);
                        callback.onRedirectResult(uriCode);
                    }
                } else {
                    printer.e("Redirect error: State parameter not matching (expected: ", state,
                            ", received: " + uriState);
                    callback.onRedirectResult(null);
                }
            } else if (TextUtils.equals(error, ERROR_PARAM_ACCESS_DENIED)) {
                printer.e("Redirect error: ", error, ", Description: " + errorDescription);
                callback.onUserCanceled();
            } else if (TextUtils.equals(error, ERROR_USER_FAILED_TO_AUTHENTICATE)) {
                printer.e("Redirect error: ", error, ", Description: " + errorDescription);
                callback.onUserFailedToLogin();
            }else {
                printer.e("Redirect error: ", error, ", Description: " + errorDescription);
                callback.onRedirectResult(null);
            }
            return true;
        }
        return false;
    }
}