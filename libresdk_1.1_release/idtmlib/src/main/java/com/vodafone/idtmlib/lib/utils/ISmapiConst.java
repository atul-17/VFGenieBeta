package com.vodafone.idtmlib.lib.utils;


public interface ISmapiConst {


    String EVENT_KEYSTORE_ISSUE = "IDTM: Problem to access key store: ";

    String EVENT_INIT_STARTED = "IDTM SDK initialisation: started";
    String EVENT_INIT_UNABLE_TO_CONNECT_TO_NETWORK = "IDTM SDK initialisation: no network found";
    String EVENT_INIT_UNABLE_TO_GENERATE_PROOF_ID = "IDTM SDK initialisation: unable to generate proof id";
    String EVENT_INIT_UNABLE_TO_INITIALIZE_FIREBASE = "IDTM SDK initialisation: unable to initialize firebase";
    String EVENT_INIT_GENERATE_INSTANCE = "IDTM SDK generating instance";
    String EVENT_INIT_ALREADY_INITIALIZED = "IDTM SDK initialisation: already initalized";
    String EVENT_INIT_CALL_BACKEND_INITIALIZE = "IDTM SDK initialisation: calling backend";
    String EVENT_INIT_BACKEND_INITIALIZE_SUCCESS = "IDTM SDK initialisation: backend returned success";
    String EVENT_INIT_BACKEND_INITIALIZE_CONTEXT_ISSUE = "IDTM SDK initialisation: Context is not SETUP COMPLETE";
    String EVENT_INIT_BACKEND_INITIALIZE_IOEXCEPTION = "IDTM SDK initialisation: backend IO Exception ";
    String EVENT_INIT_BACKEND_INITIALIZE_FAILURE = "IDTM SDK initialisation: backend returned failure ";
    String EVENT_INIT_BACKEND_INITIALIZE_SDK_NOT_FOUND = "IDTM SDK initialisation: backend returned SDK not found";
    String EVENT_INIT_BACKEND_INITIALIZE_TEMPORARY_ISSUE = "IDTM SDK initialisation: backend returned temporary issue ";
    String EVENT_INIT_BACKEND_FAILED_TO_DECRYPT = "IDTM SDK initialisation: backend returnd invalid jwe";
    String EVENT_INIT_BACKEND_INVALID_JSON = "IDTM SDK initialisation: backend returned invalid JSON";
    String EVENT_INIT_UNEXPECTED_EXCEPTION = "IDTM SDK initialisation: unexpected exception: ";
    String EVENT_INIT_CERTIFICATE_ISSUE = "IDTM SDK initialisation: Certificate Pinning Failed";
    String EVENT_AUTH_CERTIFICATE_ISSUE_WEBVIEW = "IDTM SDK Authentication: Certificate Pinning Failed in webview";
    String EVENT_AUTH_CHECK_REFRESH_CERTFICATE = "IDTM SDK Authentication: Checking refresh certificates in webview";
    String EVENT_AUTH_WEBVIEW_ISSUE = "IDTM SDK Authentication: Webview loading failed";

    String EVENT_GENERIC = "IDTM";
    String EVENT_INIT = "IDTM SDK init";
    String EVENT_AUTHENTICATE = "IDTM SDK authenticate";
    String EVENT_LOG_OUT = "IDTM SDK log out";

    String EVENT_NONCE_REQUEST_STARTED = "IDTM SDK nonce request: started";
    String EVENT_NONCE_BACKEND_FAILURE = "IDTM SDK nonce request: backend returned failure ";

    String EVENT_WEBVIEW_STARTED = "IDTM SDK web view started";
    String EVENT_IDGW_REDIRECT = "IDTM redirecting to IDGW";
    String EVENT_IDGW_REDIRECT_FAILURE = "IDTM redirecting to IDGW fails";
    String EVENT_WEBVIEW_FINISHED = "IDTM SDK web view finished";
    String EVENT_ACCESS_TOKEN_REQUEST = "IDTM SDK access token request with code - started";
    String EVENT_REFRESH_TOKEN = "IDTM SDK refresh token: started";

    String EVENT_AUTHORIZE_EXISTING = "IDTM SDK access token still valid";
    String EVENT_AUTHORIZE_EXPIRED = "IDTM SDK access token expired, needs refresh";
    String EVENT_AUTHORIZE_INVALID_TOKEN = "IDTM SDK access token invalid, needs refresh";

    // authenticate events:
    String EVENT_AUTHENTICATE_STARTED = "IDTM SDK authentication: started";
    String EVENT_AUTHENTICATE_BACKEND_SUCCESS = "IDTM SDK access token request: backend returned success";
    String EVENT_AUTHENTICATE_ACCESS_TOKEN_GENERATED = "IDTM SDK access token successfully generated";
    String EVENT_AUTHENTICATE_SYNC_SUCCESS = "IDTM SDK authentication: returning success";
    String EVENT_AUTHENTICATE_INIT_NOT_INIT = "IDTM SDK authentication: SDK not initialized";
    String EVENT_AUTHENTICATE_IDGATEWAY_REQUIRED = "IDTM SDK authentication: Cannot open ID Gateway";
    String EVENT_AUTHENTICATE_BACKEND_TEMP_ISSUE = "IDTM SDK authentication: Backend Temporary Issue";
    String EVENT_AUTHENTICATE_CERTIFICATE_ISSUE = "IDTM SDK authentication: Certificate Pinning Failed";
    String EVENT_AUTHENTICATE_DECRYPT_ISSUE = "IDTM SDK authentication: IDTM Keys Decryption Issue";
    String EVENT_AUTHENTICATE_SERVER_ISSUE = "IDTM SDK authentication: IDTM Server issue: ";
    String EVENT_AUTHENTICATE_UNEXPECTED_EXCEPTION = "IDTM SDK authentication: unexpected exception: ";
    String EVENT_AUTHENTICATE_USER_CANCELLED = "IDTM SDK authentication: User cancelled webview authentication";
    String EVENT_AUTHENTICATE_USER_FAILED_TO_LOGIN = "IDTM SDK authentication: User failed to login during webview authentication";

    //refresh-token events
    String EVENT_AUTHENTICATE_REFRESHTOKEN_SUCCESS = "IDTM SDK authentication: Refresh token successfully generated";
    String EVENT_AUTHENTICATE_REFRESHTOKEN_APIX_ERROR = "IDTM SDK authentication: APIX returns error code while refreshing access token";
    String EVENT_AUTHENTICATE_REFRESHTOKEN_NON_RETRABLE_ERROR = "IDTM SDK authentication: Trying to refresh access token failed with non-retriable exception.";
    String EVENT_AUTHENTICATE_REFRESHTOKEN_RETRABLE_MAX_RETRY_COUNT = "IDTM SDK authentication: Handling APIX retriable issue as non-retriable as max retry count was reached";
    String EVENT_AUTHENTICATE_REFRESHTOKEN_NEW_MANUAL_LOGIN = "IDTM SDK authentication: Initiate new login after non-retriable error with refresh token";
    String EVENT_AUTHENTICATE_REFRESHTOKEN_NEW_MANUAL_LOGIN_FAILED = "IDTM SDK authentication: New login after non-retriable refresh error failed";
    String EVENT_AUTHENTICATE_REFRESHTOKEN_NON_RETIABLE_NO_GATEWAY_ALLOWED = "IDTM SDK authentication: Cannot handle non-retriable error gracefully because IDGateway is not allowed";
    String EVENT_AUTHENTICATE_REFRESHTOKEN_NON_RETRIABLE_DURING_AUTO_LOGIN = "IDTM SDK authentication: Non-retriable exception during auto-refresh. Auto-refresh stopped";
    String EVENT_AUTHENTICATE_REFRESHTOKEN_RETRIABLE_ERROR = "IDTM SDK authentication: Trying to refresh access token failed with retriable exception";
    String EVENT_AUTHENTICATE_REFRESHTOKEN_RETRIABLE_ERROR_DURING_AUTO_REFRESH = "IDTM SDK authentication: Retriable error during auto-refresh - schedule next attempt";
    String EVENT_AUTHENTICATE_REFRESHTOKEN_AES_EXCEPTION = "IDTM SDK authentication: AES exception during token refresh";

    String EVENT_AUTHENTICATE_AUTO_REFRESH_JOB = "IDTM SDK authentication: Starting auto refresh token job";
    String EVENT_AUTHENTICATE_AUTO_REFRESH_JOB_CANCEL = "IDTM SDK authentication: Not scheduling auto refresh time";
    String EVENT_AUTHENTICATE_AUTO_REFRESH_JOB_ERROR = "IDTM SDK authentication: Error while setting the auto refresh";

    String EVENT_LOGOUT = "IDTM SDK logout: started";
    String EVENT_REVOKE_ACCESS_TOKEN = "IDTM SDK logout: revoke access token started";
    String EVENT_REVOKE_REFRESH_TOKEN = "IDTM SDK logout: revoke refresh token started";
    String EVENT_REVOKE_TOKEN_SUCCESS = "IDTM SDK logout: successfully revoked: ";
    String EVENT_REVOKE_TOKEN_FAILED = "IDTM SDK: Revoke token failure : Backend failed to revoke";


    //New Initialize detailed events;
    String USECASE_GENERIC = "VFIDIDTMUC000";
    String USECASE_INIT_STARTED = "VFIDIDTMUC010";
    String USECASE_INIT_PROOFID = "VFIDIDTMUC010";
    String USECASE_INIT_GEN_INSTANCE = "VFIDIDTMUC010";
    String USECASE_INIT_ALREADY_INIT = "VFIDIDTMUC010";
    String USECASE_INIT_BACKEND_INIT = "VFIDIDTMUC010";
    String USECASE_INIT_SUCCESS = "VFIDIDTMUC010";
    String USECASE_INIT_CONTEXT_ISSUE = "VFIDIDTMUC010";
    String USECASE_INIT_FAILURE = "VFIDIDTMUC010";
    String USECASE_INIT_SDK_NOT_FOUND = "VFIDIDTMUC010";
    String USECASE_INIT_BACKEND_TEMP_ISSUE = "VFIDIDTMUC010";
    String USECASE_INIT_FAILED_DECRYPT = "VFIDIDTMUC010";
    String USECASE_INIT_INVALID_JSON = "VFIDIDTMUC010";
    String USECASE_INIT_NETWORK = "VFIDIDTMUC010";
    String USECASE_INIT_CERTIFICATE_ISSUE = "VFIDIDTMUC010";


    String USECASE_AUTHENTICATE = "VFIDIDTMUC011";
    String USECASE_NONCE_REQUEST = "VFIDIDTMUC012";
    String USECASE_WEBVIEW_STARTED = "VFIDIDTMUC013";
    String USECASE_WEBVIEW_FINISHED = "VFIDIDTMUC013";
    String USECASE_ACCESS_TOKEN_REQUEST = "VFIDIDTMUC015";
    String USECASE_AUTHORIZE_EXISTING = "VFIDIDTMUC016";
    String USECASE_AUTHORIZE_EXPIRED = "VFIDIDTMUC017";
    String USECASE_AUTHORIZE_FAILED = "VFIDIDTMUC018";
    String USECASE_REFRESH_TOKEN = "VFIDIDTMUC019";

    String USECASE_AUTHENTICATE_INIT_NOT_INIT = "VFIDIDTMUC011";
    String USECASE_AUTHENTICATE_IDGATEWAY_REQUIRED = "VFIDIDTMUC011";
    String USECASE_AUTHENTICATE_BACKEND_TEMP_ISSUE = "VFIDIDTMUC015";
    String USECASE_AUTHENTICATE_CERTIFICATE_ISSUE = "VFIDIDTMUC015";
    String USECASE_AUTHENTICATE_SERVER_ISSUE = "VFIDIDTMUC015";
    String USECASE_AUTHENTICATE_USER_CANCELLED = "VFIDIDTMUC013";
    String USECASE_AUTHENTICATE_SUCCESS = "VFIDIDTMUC011";

    String USECASE_AUTHENTICATE_REFRESHTOKEN_SUCCESS = "VFIDIDTMUC019";
    String USECASE_AUTHENTICATE_REFRESHTOKEN_FAILED = "VFIDIDTMUC019";

    String USECASE_AUTHENTICATE_AUTO_REFRESH_JOB = "VFIDIDTMUC044";
    String USECASE_AUTHENTICATE_AUTO_REFRESH_JOB_CANCEL = "VFIDIDTMUC044";
    String USECASE_AUTHENTICATE_AUTO_REFRESH_JOB_ERROR = "VFIDIDTMUC044";
    String USECASE_AUTHENTICATE_WEBVIEW_CERTIFICATE_ISSUE = "VFIDIDTMUC045";
    String USECASE_AUTH_CHECK_REFRESH_CERTFICATE = "VFIDIDTMUC046";
    String USECASE_AUTH_WEBVIEW_ISSUE = "VFIDIDTMUC047";

    String USECASE_LOGOUT = "VFIDIDTMUC020";
    String USECASE_REVOKE_TOKEN_SUCCESS = "VFIDIDTMUC020";
    String USECASE_REVOKE_TOKEN_FAILED = "VFIDIDTMUC020";
    String USECASE_AUTHENTICATE_DECRYPT_ISSUE = "VFIDIDTMUC020";
    String USECASE_REVOKE_ACCESS_TOKEN = "VFIDIDTMUC020";
    String USECASE_REVOKE_REFRESH_TOKEN = "VFIDIDTMUC020";

}
