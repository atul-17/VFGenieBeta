package com.vodafone.idtmlib.lib.utils;


import android.content.Context;

import com.google.common.collect.ImmutableMap;
import com.vodafone.idtmlib.BuildConfig;
import com.vodafone.lib.seclibng.SecLib;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Smapi implements ISmapiConst {

    private static final String USECASE = "x-vf-trace-usecase-id";
    private static final String TRANSACTION = "x-vf-trace-transaction-id";
    private static final String AUTHORIZATION = "x-vf-trace-authorization-id";
    private static final String IDTM_VERSION = "x-vf-trace-idtm-version";

    //init usecases

    //init transaction

    private static final String PAGE_NAME = "NA";
    private static final String SUB_PAGE = "NA";
    private Printer printer;

    @Inject
    public Smapi(Printer printer, Context context) {
        // SecLib must be initialized by the main app
        this.printer = printer;
    }

    private void logEvent(String action, String context, String useCase,
                          String transaction, Map<String, String> payload) {
        /*try {
            Class.forName("com.vodafone.lib.sec.SecLib");
            com.vodafone.lib.sec.Event event = com.vodafone.lib.sec.Event.clientEvent(action,
                    context, null);
            event.addPayload(USECASE, useCase);
            event.addPayload(TRANSACTION, transaction);
            if (payload != null) {
                for (Map.Entry<String, String> entry : payload.entrySet()) {
                    event.addPayload(entry.getKey(), entry.getValue());
                }
            }
            printer.i("Event: ", event, event.getPayload());
            com.vodafone.lib.sec.SecLib.logEvent(event);


        } catch (ClassNotFoundException e) {
            // SecLib not present
        }*/
    }

    public void logEventNG(Context context, String eventElement, String eventDescription, String pageName, String subPage,
                           String useCase, String transaction, String authId, Map<String, String> payload) {
        printer.d("logEventNG SecLibNG eventEle: " + eventElement);
        printer.d("logEventNG SecLibNG eventDescp: " + eventDescription);
        printer.d("logEventNG SecLibNG authId: " + authId);
        try {
          //  Class.forName("com.vodafone.lib.seclibng.SecLibNG");
            //Adding custom payload
            Map<String, Object> eventMapObj = new HashMap<>();
            eventMapObj.put(USECASE, useCase);
            eventMapObj.put(IDTM_VERSION, BuildConfig.VERSION_NAME);
            eventMapObj.put(TRANSACTION, transaction);

            if (authId != null) {
                eventMapObj.put(AUTHORIZATION, authId);
            }
            if (payload != null) {
                for (Map.Entry<String, String> entry : payload.entrySet()) {
                    eventMapObj.put(entry.getKey(), entry.getValue());
                }
            }

            SecLib.getInstance().logCustomEvent(eventElement, eventDescription, pageName, subPage, eventMapObj);

        } catch (Exception e) {
            e.printStackTrace();
            printer.e(e.getMessage());
        }
    }

    /******** Generic Smapi events ************/
    public void logKeystoreIssue(Context context, String transactionID, String errorDescription) {
        logEventNG(context, EVENT_GENERIC, EVENT_KEYSTORE_ISSUE + errorDescription, PAGE_NAME, SUB_PAGE, USECASE_GENERIC, transactionID, null, null);
    }

    /******** Init Smapi events ************/

    public void logInitStarted(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_INIT_STARTED, PAGE_NAME, SUB_PAGE, USECASE_INIT_STARTED, transactionID, null, null);
    }

    public void logNoNetworkConnection(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_INIT_UNABLE_TO_CONNECT_TO_NETWORK, PAGE_NAME, SUB_PAGE, USECASE_INIT_NETWORK, transactionID, null, null);
    }

    public void logInitIssueGenerateProofId(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_INIT_UNABLE_TO_GENERATE_PROOF_ID, PAGE_NAME, SUB_PAGE, USECASE_INIT_PROOFID, transactionID, null, null);
    }

    public void logInitIssueInitializeFirebase(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_INIT_UNABLE_TO_INITIALIZE_FIREBASE, PAGE_NAME, SUB_PAGE, USECASE_INIT_PROOFID, transactionID, null, null);
    }

    public void logInitGenerateInstance(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_INIT_GENERATE_INSTANCE, PAGE_NAME, SUB_PAGE, USECASE_INIT_GEN_INSTANCE, transactionID, null, null);
    }

    public void logInitAlreadyInit(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_INIT_ALREADY_INITIALIZED, PAGE_NAME, SUB_PAGE, USECASE_INIT_ALREADY_INIT, transactionID, null, null);
    }

    public void logInitCallbackInitialised(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_INIT_CALL_BACKEND_INITIALIZE, PAGE_NAME, SUB_PAGE, USECASE_INIT_BACKEND_INIT, transactionID, null, null);
    }

    public void logInitBackendInitSuccess(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_INIT_BACKEND_INITIALIZE_SUCCESS, PAGE_NAME, SUB_PAGE, USECASE_INIT_SUCCESS, transactionID, null, null);
    }

    public void logInitBackendInitContextIssue(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_INIT_BACKEND_INITIALIZE_CONTEXT_ISSUE, PAGE_NAME, SUB_PAGE, USECASE_INIT_CONTEXT_ISSUE, transactionID, null, null);
    }

    public void logInitBackendInitIOException(Context context, String transactionID, String errorDescription) {
        logEventNG(context, EVENT_INIT, EVENT_INIT_BACKEND_INITIALIZE_IOEXCEPTION + "\n Error Description: " + errorDescription, PAGE_NAME, SUB_PAGE, USECASE_INIT_FAILURE, transactionID, null, null);
    }

    public void logInitBackendInitFailure(Context context, String transactionID, String errorDescription) {
        logEventNG(context, EVENT_INIT, EVENT_INIT_BACKEND_INITIALIZE_FAILURE + "\n Error Description: " + errorDescription, PAGE_NAME, SUB_PAGE, USECASE_INIT_FAILURE, transactionID, null, null);
    }

    public void logInitBackendSdkNotFound(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_INIT_BACKEND_INITIALIZE_SDK_NOT_FOUND, PAGE_NAME, SUB_PAGE, USECASE_INIT_SDK_NOT_FOUND, transactionID, null, null);
    }

    public void logInitBackendTempIssue(Context context, String transactionID, String errorDescription) {
        logEventNG(context, EVENT_INIT, EVENT_INIT_BACKEND_INITIALIZE_TEMPORARY_ISSUE + "\n Error Description: " + errorDescription, PAGE_NAME, SUB_PAGE, USECASE_INIT_BACKEND_TEMP_ISSUE, transactionID, null, null);
    }

    public void logInitFailedToDecrypt(Context context, String transactionID, String errorDescription) {
        logEventNG(context, EVENT_INIT, EVENT_INIT_BACKEND_FAILED_TO_DECRYPT + "\n Error Description: " + errorDescription, PAGE_NAME, SUB_PAGE, USECASE_INIT_FAILED_DECRYPT, transactionID, null, null);
    }

    public void logInitUnexpectedException(Context context, String transactionID, String errorDescription) {
        logEventNG(context, EVENT_INIT, EVENT_INIT_UNEXPECTED_EXCEPTION + "\n Error Description: " + errorDescription, PAGE_NAME, SUB_PAGE, USECASE_INIT_FAILURE, transactionID, null, null);
    }

    public void logInitCertificateIssue(Context context, String transactionID, String errorDescription) {
        logEventNG(context, EVENT_INIT, EVENT_INIT_CERTIFICATE_ISSUE + "\n Error Description: " + errorDescription, PAGE_NAME, SUB_PAGE, USECASE_INIT_CERTIFICATE_ISSUE,
                transactionID, null, null);
    }

    public void logInitCertificateIssueInWebview(Context context, String transactionID, String errorDescription) {
        logEventNG(context, EVENT_AUTHENTICATE, EVENT_AUTH_CERTIFICATE_ISSUE_WEBVIEW + "\n Error Description: " + errorDescription, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_WEBVIEW_CERTIFICATE_ISSUE,
                transactionID, null, null);
    }

    public void logCheckRefreshCertificateInWebview(Context context, String transactionID) {
        logEventNG(context, EVENT_AUTHENTICATE, EVENT_AUTH_CHECK_REFRESH_CERTFICATE, PAGE_NAME, SUB_PAGE, USECASE_AUTH_CHECK_REFRESH_CERTFICATE,
                transactionID, null, null);
    }

    public void logExceptionInWebview(Context context, String transactionID, String errorDescription) {
        logEventNG(context, EVENT_AUTHENTICATE, EVENT_AUTH_WEBVIEW_ISSUE + "\n Error Description: " + errorDescription, PAGE_NAME, SUB_PAGE, USECASE_AUTH_WEBVIEW_ISSUE,
                transactionID, null, null);
    }
    /*public void logInitSendCallbackFailed(Context context) {
        logEventNG(context, EVENT_INIT, EVENT_INIT_SENDING_CALLBACK_FAILED, PAGE_NAME, SUB_PAGE, USECASE_INIT_SEND_CALLBACK_FAILED, TRANSACTION_INIT_SEND_CALLBACK_FAILED, null);
    }

    public void logInitSendCallbackSuccess(Context context) {
        logEventNG(context, EVENT_INIT, EVENT_INIT_SEND_CALLBACK_SUCCESS, PAGE_NAME, SUB_PAGE, USECASE_INIT_SEND_CALLBACK_SUCCESS, TRANSACTION_INIT_SEND_CALLBACK_SUCCESS, null);
    }*/

    /******** Authenticate Smapi events ************/

    public void logAuthenticate(Context context, String transactionID) {
        logEventNG(context, EVENT_AUTHENTICATE, EVENT_AUTHENTICATE_STARTED, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE,
                transactionID, null, null);
    }

    public void logAuthenticateNotinitialised(Context context, String transactionID) {
        logEventNG(context, EVENT_AUTHENTICATE, EVENT_AUTHENTICATE_INIT_NOT_INIT, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_INIT_NOT_INIT,
                transactionID, null, null);
    }

    public void logAuthenticateSuccess(Context context, String transactionID, String authId) {
        logEventNG(context, EVENT_AUTHENTICATE, EVENT_AUTHENTICATE_SYNC_SUCCESS, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_SUCCESS,
                transactionID, authId, null);
    }

    public void logNonceRequest(Context context, String transactionID) {
        logEventNG(context, EVENT_NONCE_REQUEST_STARTED, EVENT_NONCE_REQUEST_STARTED, PAGE_NAME, SUB_PAGE, USECASE_NONCE_REQUEST,
                transactionID, null, null);
    }

    public void logNonceBackendFailure(Context context, String transactionID, String errorDescription) {
        logEventNG(context, EVENT_NONCE_BACKEND_FAILURE, EVENT_NONCE_BACKEND_FAILURE + "\n Error Description: " + errorDescription, PAGE_NAME, SUB_PAGE, USECASE_NONCE_REQUEST,
                transactionID, null, null);
    }

    public void logInitInvalidJson(Context context, String transactionID, String errorDescription) {
        logEventNG(context, EVENT_INIT, EVENT_INIT_BACKEND_INVALID_JSON + "\n Error Description: " + errorDescription, PAGE_NAME, SUB_PAGE, USECASE_INIT_INVALID_JSON, transactionID,
                null, null);
    }


    public void logWebviewStarted(Context context, String transactionID) {
        logEventNG(context, EVENT_WEBVIEW_STARTED, EVENT_WEBVIEW_STARTED, PAGE_NAME, SUB_PAGE, USECASE_WEBVIEW_STARTED,
                transactionID, null, null);
    }

    public void logWebviewRedirectedToAuth(Context context, String transactionID, String authId) {
        logEventNG(context, EVENT_IDGW_REDIRECT, EVENT_IDGW_REDIRECT, PAGE_NAME, SUB_PAGE, USECASE_WEBVIEW_STARTED,
                transactionID, authId, null);
    }

    public void logWebviewRedirectedToAuthFailure(Context context, String transactionID, String authId, String errorDescription) {
        logEventNG(context, EVENT_IDGW_REDIRECT, EVENT_IDGW_REDIRECT_FAILURE + "\n Error Description: " + errorDescription, PAGE_NAME, SUB_PAGE, USECASE_WEBVIEW_STARTED,
                transactionID, authId, null);
    }

    public void logWebviewFinished(Context context, String transactionID, String authId) {
        logEventNG(context, EVENT_WEBVIEW_FINISHED, EVENT_WEBVIEW_FINISHED, PAGE_NAME, SUB_PAGE, USECASE_WEBVIEW_FINISHED,
                transactionID, authId, null);
    }

    public void logAccessTokenRequest(Context context, String transactionID, String authId, String code, String nonce) {
        logEventNG(context, EVENT_ACCESS_TOKEN_REQUEST, EVENT_ACCESS_TOKEN_REQUEST, PAGE_NAME, SUB_PAGE,
                USECASE_ACCESS_TOKEN_REQUEST, transactionID, authId,
                ImmutableMap.of("code", code, "nonce", nonce));
    }

    public void logAuthenticateBackendSuccess(Context context, String transactionID, String authId) {
        logEventNG(context, EVENT_AUTHENTICATE_BACKEND_SUCCESS, EVENT_AUTHENTICATE_BACKEND_SUCCESS, PAGE_NAME, SUB_PAGE,
                USECASE_ACCESS_TOKEN_REQUEST, transactionID, authId,
                null);
    }

    public void logAuthenticateAccessTokenGenerated(Context context, String transactionID, String authId) {
        logEventNG(context, EVENT_AUTHENTICATE_ACCESS_TOKEN_GENERATED, EVENT_AUTHENTICATE_ACCESS_TOKEN_GENERATED, PAGE_NAME, SUB_PAGE,
                USECASE_ACCESS_TOKEN_REQUEST, transactionID, authId,
                null);
    }

    public void logAuthorizeExisting(Context context, String transactionID) {
        logEventNG(context, EVENT_AUTHORIZE_EXISTING, EVENT_AUTHORIZE_EXISTING, PAGE_NAME, SUB_PAGE, USECASE_AUTHORIZE_EXISTING,
                transactionID, null, null);
    }

    public void logAuthorizeExpired(Context context, String transactionID) {
        logEventNG(context, EVENT_AUTHORIZE_EXPIRED, EVENT_AUTHORIZE_EXPIRED, PAGE_NAME, SUB_PAGE, USECASE_AUTHORIZE_EXPIRED,
                transactionID, null, null);
    }

    public void logAuthorizeInvalidToken(Context context, String transactionID, String authId) {
        logEventNG(context, EVENT_AUTHORIZE_INVALID_TOKEN, EVENT_AUTHORIZE_INVALID_TOKEN, PAGE_NAME, SUB_PAGE, USECASE_AUTHORIZE_FAILED,
                transactionID, authId, null);
    }

    public void logRefreshToken(Context context, String transactionID) {
        logEventNG(context, EVENT_REFRESH_TOKEN, EVENT_REFRESH_TOKEN, PAGE_NAME, SUB_PAGE, USECASE_REFRESH_TOKEN,
                transactionID, null, null);
    }

    public void logAuthenticateIdGatewayRequired(Context context, String transactionID) {
        logEventNG(context, EVENT_AUTHENTICATE, EVENT_AUTHENTICATE_IDGATEWAY_REQUIRED, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_IDGATEWAY_REQUIRED,
                transactionID, null, null);
    }

    public void logAuthenticateBackendTempIssue(Context context, String transactionID, String authId) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_BACKEND_TEMP_ISSUE, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_BACKEND_TEMP_ISSUE,
                transactionID, authId, null);
    }

    public void logAuthenticateCertificateIssue(Context context, String transactionID, String authId) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_CERTIFICATE_ISSUE, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_CERTIFICATE_ISSUE,
                transactionID, authId, null);
    }

    public void logAuthenticateServerIssue(Context context, String transactionID, String authId, String ErrorDescription) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_SERVER_ISSUE + "Error Description: " + ErrorDescription, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_SERVER_ISSUE,
                transactionID, authId, null);
    }

    public void logAuthenticateUnexpectedException(Context context, String transactionID, String authId, String ErrorDescription) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_UNEXPECTED_EXCEPTION + "Error Description: " + ErrorDescription, PAGE_NAME, SUB_PAGE, USECASE_AUTHORIZE_FAILED,
                transactionID, authId, null);
    }

    public void logAuthenticateDecryptIssue(Context context, String transactionID, String authId) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_DECRYPT_ISSUE, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_DECRYPT_ISSUE,
                transactionID, authId, null);
    }

    public void logAuthenticateUserCancelled(Context context, String transactionID, String authId) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_USER_CANCELLED, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_USER_CANCELLED,
                transactionID, authId, null);
    }

    public void logAuthenticateUserFailedToLogin(Context context, String transactionID, String authId) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_USER_FAILED_TO_LOGIN, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_USER_CANCELLED,
                transactionID, authId, null);
    }


    public void logAuthenticateRefreshTokenSuccess(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_REFRESHTOKEN_SUCCESS, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_REFRESHTOKEN_SUCCESS,
                transactionID, null, null);
    }

    public void logAuthenticateRefreshTokenAPIXError(Context context, String transactionID, String errorDescription) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_REFRESHTOKEN_APIX_ERROR + "\n Error Description: " + errorDescription, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_REFRESHTOKEN_FAILED,
                transactionID, null, null);
    }

    public void logAuthenticateRefreshTokenNonRetriableError(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_REFRESHTOKEN_NON_RETRABLE_ERROR, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_REFRESHTOKEN_FAILED,
                transactionID, null, null);
    }

    public void logAuthenticateRefreshTokenRetriableErrorMaxRetryCount(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_REFRESHTOKEN_RETRABLE_MAX_RETRY_COUNT, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_REFRESHTOKEN_FAILED,
                transactionID, null, null);
    }

    public void logAuthenticateRefreshTokenNewManualLogin(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_REFRESHTOKEN_NEW_MANUAL_LOGIN, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_REFRESHTOKEN_FAILED,
                transactionID, null, null);
    }

    public void logAuthenticateRefreshTokenManualLoginFailed(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_REFRESHTOKEN_NEW_MANUAL_LOGIN_FAILED, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_REFRESHTOKEN_FAILED,
                transactionID, null, null);
    }

    public void logAuthenticateRefreshTokenNonRetriableNoGatewayAllowed(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_REFRESHTOKEN_NON_RETIABLE_NO_GATEWAY_ALLOWED, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_REFRESHTOKEN_FAILED,
                transactionID, null, null);
    }

    public void logAuthenticateRefreshTokenNonRetriableDuringAutoLogin(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_REFRESHTOKEN_NON_RETRIABLE_DURING_AUTO_LOGIN, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_REFRESHTOKEN_FAILED,
                transactionID, null, null);
    }

    public void logAuthenticateRefreshTokenRetriableError(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_REFRESHTOKEN_RETRIABLE_ERROR, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_REFRESHTOKEN_FAILED,
                transactionID, null, null);
    }

    public void logAuthenticateRefreshTokenRetriableErrorDuringAutoRefresh(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_REFRESHTOKEN_RETRIABLE_ERROR_DURING_AUTO_REFRESH, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_REFRESHTOKEN_FAILED,
                transactionID, null, null);
    }

    public void logAuthenticateRefreshTokenAESException(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_REFRESHTOKEN_AES_EXCEPTION, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_REFRESHTOKEN_FAILED,
                transactionID, null, null);
    }


    public void logAuthenticateAutoRefreshTokenJob(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_AUTO_REFRESH_JOB, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_AUTO_REFRESH_JOB,
                transactionID, null, null);
    }

    public void logAuthenticateAutoRefreshTokenJobCancel(Context context, String transactionID) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_AUTO_REFRESH_JOB_CANCEL, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_AUTO_REFRESH_JOB_CANCEL,
                transactionID, null, null);
    }

    public void logAuthenticateRefreshTokenJobError(Context context, String transactionID, String errorDescription) {
        logEventNG(context, EVENT_INIT, EVENT_AUTHENTICATE_AUTO_REFRESH_JOB_ERROR + "\n Error Description: " + errorDescription, PAGE_NAME, SUB_PAGE, USECASE_AUTHENTICATE_AUTO_REFRESH_JOB_ERROR,
                transactionID, null, null);
    }

    public void logLogout(Context context, String transactionID) {
        logEventNG(context, EVENT_LOG_OUT, EVENT_LOGOUT, PAGE_NAME, SUB_PAGE, USECASE_LOGOUT,
                transactionID, null, null);
    }

    public void logRevokeAccessToken(Context context, String transactionID) {
        logEventNG(context, EVENT_LOG_OUT, EVENT_REVOKE_ACCESS_TOKEN, PAGE_NAME, SUB_PAGE, USECASE_REVOKE_ACCESS_TOKEN,
                transactionID, null, null);
    }

    public void logRevokeRefreshToken(Context context, String transactionID) {
        logEventNG(context, EVENT_LOG_OUT, EVENT_REVOKE_REFRESH_TOKEN, PAGE_NAME, SUB_PAGE, USECASE_REVOKE_REFRESH_TOKEN,
                transactionID, null, null);
    }

    public void logRevokeTokenSuccess(Context context, String transactionID, String tokenHint) {
        logEventNG(context, EVENT_LOG_OUT, EVENT_REVOKE_TOKEN_SUCCESS + tokenHint, PAGE_NAME, SUB_PAGE, USECASE_REVOKE_TOKEN_SUCCESS,
                transactionID, null, null);
    }

    public void logRevokeTokenFailed(Context context, String transactionID, String errorDescription) {
        logEventNG(context, EVENT_LOG_OUT, EVENT_REVOKE_TOKEN_FAILED + "\n Error Description: " + errorDescription, PAGE_NAME, SUB_PAGE, USECASE_REVOKE_TOKEN_FAILED,
                transactionID, null, null);
    }
}
