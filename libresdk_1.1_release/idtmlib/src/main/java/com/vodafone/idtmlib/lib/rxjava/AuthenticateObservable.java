package com.vodafone.idtmlib.lib.rxjava;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.nimbusds.jose.JOSEException;
import com.vodafone.idtmlib.AccessToken;
import com.vodafone.idtmlib.exceptions.IDTMException;
import com.vodafone.idtmlib.exceptions.IdGatewayRequiredException;
import com.vodafone.idtmlib.exceptions.IdtmIgnoreException;
import com.vodafone.idtmlib.exceptions.IdtmInProgressException;
import com.vodafone.idtmlib.exceptions.IdtmRetryException;
import com.vodafone.idtmlib.exceptions.IdtmSSLPeerUnverifiedException;
import com.vodafone.idtmlib.exceptions.IdtmServerException;
import com.vodafone.idtmlib.exceptions.IdtmTemporaryIssueException;
import com.vodafone.idtmlib.exceptions.NoNetworkConnectionException;
import com.vodafone.idtmlib.exceptions.NotInitializedException;
import com.vodafone.idtmlib.exceptions.SecureStorageException;
import com.vodafone.idtmlib.exceptions.UserCanceledException;
import com.vodafone.idtmlib.lib.AutoRefreshTokenJobService;
import com.vodafone.idtmlib.lib.network.BadStatusCodeException;
import com.vodafone.idtmlib.lib.network.Environment;
import com.vodafone.idtmlib.lib.network.IdtmApi;
import com.vodafone.idtmlib.lib.network.JwtHelper;
import com.vodafone.idtmlib.lib.network.Response;
import com.vodafone.idtmlib.lib.network.RevokeTokens;
import com.vodafone.idtmlib.lib.network.models.bodies.GetAccessTokenBody;
import com.vodafone.idtmlib.lib.network.models.bodies.RefreshAccessTokenBody;
import com.vodafone.idtmlib.lib.network.models.responses.AccessTokenResponse;
import com.vodafone.idtmlib.lib.network.models.responses.ErrorResponse;
import com.vodafone.idtmlib.lib.network.models.responses.JwtResponse;
import com.vodafone.idtmlib.lib.network.models.responses.SetupResponse;
import com.vodafone.idtmlib.lib.storage.DataCrypt;
import com.vodafone.idtmlib.lib.storage.Prefs;
import com.vodafone.idtmlib.lib.storage.basic.AesException;
import com.vodafone.idtmlib.lib.storage.basic.Preferences;
import com.vodafone.idtmlib.lib.ui.IdGatewayActivity;
import com.vodafone.idtmlib.lib.ui.elements.IdGatewaySyncer;
import com.vodafone.idtmlib.lib.utils.Device;
import com.vodafone.idtmlib.lib.utils.Printer;
import com.vodafone.idtmlib.lib.utils.Smapi;

import java.io.IOException;
import java.net.SocketException;
import java.text.ParseException;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.SSLPeerUnverifiedException;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

@Singleton
public class AuthenticateObservable implements ObservableOnSubscribe<AuthenticateProgress> {
    public static final long AUTO_REFRESH_PRE_TIME_SEC = 240L;
    private static final long HOST_INACTIVE_BEFORE_DISABLING_AUTO_REFRESH_TIME_MS =
            TimeUnit.HOURS.toMillis(6);
    private static final int MAX_RETRY_COUNT = 3;
    private Printer printer;
    private Context context;
    private DataCrypt dataCrypt;
    private IdtmApi idtmApi;
    private Environment environment;
    private Gson gson;
    private Preferences preferences;
    private IdGatewaySyncer idGatewaySyncer;
    private FirebaseJobDispatcher dispatcher;
    private Smapi smapi;
    private String smapiTransactionId = UUID.randomUUID().toString();
    private Semaphore idtmSemaphore;

    private boolean allowIdGateway;
    private String invalidAccessToken;
    private boolean autoRefresh;
    private Observable<AuthenticateProgress> observable = Observable.create(this)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .share();
    private String sdkId;
    private SecretKey serverSymmetricKey;
    private String sdkIdJwtToken;

    @Inject
    public AuthenticateObservable(Printer printer, Context context, DataCrypt dataCrypt,
                                  IdtmApi idtmApi, Environment environment, Gson gson,
                                  Preferences preferences, IdGatewaySyncer idGatewaySyncer,
                                  FirebaseJobDispatcher dispatcher, Smapi smapi, Semaphore idtmSemaphore) {
        this.printer = printer;
        this.context = context;
        this.dataCrypt = dataCrypt;
        this.idtmApi = idtmApi;
        this.environment = environment;
        this.gson = gson;
        this.preferences = preferences;
        this.idGatewaySyncer = idGatewaySyncer;
        this.dispatcher = dispatcher;
        this.smapi = smapi;
        this.idtmSemaphore = idtmSemaphore;

        printer.d("AuthenticateObservable: IDTMSemaphore set to: ", idtmSemaphore);


        RxJavaPlugins.setErrorHandler(e -> {

            printer.d("AuthenticateObservable: Entered error handler: ", e);

            if (e instanceof UndeliverableException) {
                e = e.getCause();
            }
            if ((e instanceof IOException) || (e instanceof SocketException)) {
                // fine, irrelevant network problem or API that throws on cancellation
                return;
            }
            if (e instanceof InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return;
            }
            if ((e instanceof NullPointerException) || (e instanceof IllegalArgumentException)) {
                // that's likely a bug in the application
                return;
            }
            if (e instanceof IllegalStateException) {
                // that's a bug in RxJava or in a custom operator
                return;
            }
        });
    }

    public void start(boolean allowIdGateway, String invalidAccessToken,
                      Observer<AuthenticateProgress> observer) {
        printer.i("start AuthenticateObservable");

        this.allowIdGateway = allowIdGateway;
        this.invalidAccessToken = invalidAccessToken;
        this.autoRefresh = false;
        observable.subscribe(observer);
    }

    public AuthenticateProgress start(boolean allowIdGateway, String invalidAccessToken)
            throws Exception {

        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            throw new RuntimeException("MAIN THREAD");
        }

        this.allowIdGateway = allowIdGateway;
        this.invalidAccessToken = invalidAccessToken;
        this.autoRefresh = false;
        try {
            AuthenticateProgress progress = observable.blockingLast();
            return progress;
        } catch (RuntimeException e) {
            if (e.getCause() != null && e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    public void startAutoRefresh(Observer<AuthenticateProgress> observer) {
        printer.i("start AuthenticateObservable autoRefresh");
        this.smapiTransactionId = UUID.randomUUID().toString();
        this.allowIdGateway = false;
        this.invalidAccessToken = null;
        this.autoRefresh = true;
        observable.subscribe(observer);
    }

    @Override
    public void subscribe(ObservableEmitter<AuthenticateProgress> emitter) throws Exception {

        try {

            AccessToken accessToken = null;
            printer.i("Starting authenticate");
            this.smapiTransactionId = UUID.randomUUID().toString();

            smapi.logAuthenticate(context, smapiTransactionId);

            printer.d("Trying to acquire semaphore");
            if (!idtmSemaphore.tryAcquire()) {
                printer.d("Semaphore: Operation already in progress");
                throw new IdtmInProgressException();
            }

            printer.d("Acquired semaphore");

            ///////////////////////
            // Check if initialized
            String clientId = dataCrypt.getString(Prefs.CLIENT_ID);
            if (TextUtils.isEmpty(clientId)) {
                printer.e("clientId is empty");
                smapi.logAuthenticateNotinitialised(context, smapiTransactionId);
                throw new NotInitializedException();
            } else {
                try {
                    if (!autoRefresh) {
                        dataCrypt.set(Prefs.LAST_IDTMLIB_METHOD_CALL_TIME_MS, System.currentTimeMillis());
                    }

                    //////////////////////////
                    // Get cached access token
                    String accessTokenString = dataCrypt.getString(Prefs.ACCESS_TOKEN);
                    long expireTime = dataCrypt.getLong(Prefs.ACCESS_TOKEN_EXPIRE_TIME_MS, Long.MIN_VALUE);

                    ////////////////////////////////////////////////////////////
                    // Check if token still valid or expired or has been reported as invalid
                    boolean invalidated = TextUtils.equals(accessTokenString, invalidAccessToken);
                    if (invalidAccessToken != null && invalidated) {
                        printer.e("invalidated ...");
                        smapi.logAuthorizeInvalidToken(context, smapiTransactionId, IdGatewayActivity.authId);
                    }
                    if (accessTokenString != null && !accessTokenString.isEmpty() && System.currentTimeMillis() >= expireTime) {
                        printer.e("access token expired ...");
                        smapi.logAuthorizeExpired(context, smapiTransactionId);
                    }
                    if (System.currentTimeMillis() <= expireTime && !invalidated && !autoRefresh) {
                        printer.i("Access token still valid");
                        smapi.logAuthorizeExisting(context, smapiTransactionId);
                        accessToken = new AccessToken(accessTokenString,
                                dataCrypt.getString(Prefs.ACCESS_TOKEN_TYPE),
                                dataCrypt.getString(Prefs.ACCESS_TOKEN_SUB));
                    }
                    //////////////////////////////////////////////////////////////////////
                    // If we do not have an access token (accessTokenString null or empty)
                    // User nas to login again
                    // If we have an access token that is expired or invalid, try a refresh
                    else {
                        // We need a network connection in any case
                        if (!Device.checkNetworkConnection(context)) {
                            printer.e("Error : No connection found");
                            smapi.logNoNetworkConnection(context, smapiTransactionId);
                            throw new NoNetworkConnectionException();
                        }

                        // We need SDKID and symmetric key in any case
                        printer.i("Preparing data for authenticate");
                        sdkId = dataCrypt.getString(Prefs.SDK_ID);
                        serverSymmetricKey = dataCrypt.loadServerSymmetricKey();
                        sdkIdJwtToken = JwtHelper.aesEncrypt(serverSymmetricKey, sdkId);

                        // Decide if user needs to log in or if we try a refresh
                        if (TextUtils.isEmpty(accessTokenString)) {
                            if (allowIdGateway) {
                                printer.i("Getting new access token");
                                accessToken = newAccessToken(clientId, sdkId, serverSymmetricKey,
                                        sdkIdJwtToken, emitter);
                            } else {
                                printer.i("Need to get the new access token, but cannot open ID Gateway");
                                smapi.logAuthenticateIdGatewayRequired(context, smapiTransactionId);
                                throw new IdGatewayRequiredException();
                            }
                        } else {
                            printer.i("Refreshing access token ",
                                    invalidated ? "[invalid]" :
                                            (autoRefresh ? "[autorefresh]" : "[expired]"));
                            accessToken = refreshAccessToken(clientId, sdkId, serverSymmetricKey,
                                    sdkIdJwtToken, emitter);
                        }
                    }

                } catch (SSLPeerUnverifiedException e) {
                    printer.e("Error while certificate pinning auth ", e);
                    smapi.logAuthenticateCertificateIssue(context, smapiTransactionId, IdGatewayActivity.authId);

                    //Call API to get latest certificates
                    JwtResponse jwtResponse = Response.retrieve(JwtResponse.class,
                            idtmApi.getClientDetails(sdkIdJwtToken, clientId, sdkId));
                    String jsonResponse = JwtHelper.aesDecrypt(serverSymmetricKey, jwtResponse.getData());
                    SetupResponse setupResponse = gson.fromJson(jsonResponse, SetupResponse.class);
                    dataCrypt.set(Prefs.CERT_PINNING_HASHES,
                            setupResponse.getCertificates() == null ? null :
                                    gson.toJson(setupResponse.getCertificates()));

                    throw new IdtmSSLPeerUnverifiedException();
                    //throw new IdtmRetryException();
                } catch (AesException e) {
                    printer.e("Error loading secure storage ", e);
                    smapi.logAuthenticateDecryptIssue(context, smapiTransactionId, IdGatewayActivity.authId);
                    throw new SecureStorageException();
                } catch (BadStatusCodeException e) {
                    int responseCode = e.getResponse().getResponseCode();
                    if (responseCode == 404) {
                        ErrorResponse errorResponse = gson.fromJson(
                                e.getResponse().getResponseErrorBodyString(), ErrorResponse.class);
                        if (errorResponse != null && errorResponse.isSdkNotFound()) {
                            printer.e("Cannot find user ", e);
                            String referenceId = preferences.getString(Prefs.REFERENCE_ID);
                            preferences.clearAllPreferences();
                            preferences.set(Prefs.REFERENCE_ID, referenceId);
                            smapi.logAuthenticateNotinitialised(context, smapiTransactionId);
                            throw new NotInitializedException();
                        } else {
                            smapi.logAuthenticateServerIssue(context, smapiTransactionId, IdGatewayActivity.authId, "Server returned 404");
                            throw new IdtmRetryException();
                        }
                    } else if (responseCode >= 500 && responseCode < 600) {
                        if (!emitter.isDisposed()) {
                            printer.e("Server temporary issue ", e);
                            smapi.logAuthenticateBackendTempIssue(context, smapiTransactionId, IdGatewayActivity.authId);
                            throw new IdtmTemporaryIssueException();
                        }
                    } else {
                        printer.e("Server error", e);
                        smapi.logAuthenticateServerIssue(context, smapiTransactionId, IdGatewayActivity.authId, "Unexpected response code from server: " + responseCode);
                        throw new IdtmRetryException();
                    }
                } catch (IOException e) {
                    printer.e("Server error", e);
                    smapi.logAuthenticateServerIssue(context, smapiTransactionId, IdGatewayActivity.authId, "IOException - " + e.getMessage());
                    throw new IdtmServerException();
                } catch (JOSEException | ParseException | JsonParseException e) {
                    printer.e("Server error", e);
                    smapi.logAuthenticateServerIssue(context, smapiTransactionId, IdGatewayActivity.authId, "Unable to parse JSON result - " + e.getMessage());
                    throw new IdtmServerException();
                } catch (IDTMException e) {
                    // already handled
                    throw e;
                } catch (Exception e) {
                    smapi.logAuthenticateUnexpectedException(context, smapiTransactionId, IdGatewayActivity.authId, "Unexpected response code from server: " + e.getMessage());
                    throw new IdtmRetryException();
                }


            }
            if (accessToken != null && accessToken.getToken() != null && !accessToken.getToken().isEmpty()) {
                dataCrypt.set(Prefs.REFRESH_COUNTER, -1);
                dataCrypt.set(Prefs.AUTO_REFRESH_RETRY_COUNTER, -1);
                scheduleOrCancelAutoRefreshToken(false);
                smapi.logAuthenticateSuccess(context, smapiTransactionId, IdGatewayActivity.authId);
                emitter.onNext(new AuthenticateProgress(false, accessToken));
                emitter.onComplete();
            } else {
                throw new IdtmRetryException();
            }
        } catch (IDTMException e) {
            if (!(e instanceof IdtmInProgressException)) {
                printer.d("Release semaphore in error handler");
                idtmSemaphore.release();
            }
            throw e;
        } catch (Exception e) {
            smapi.logAuthenticateUnexpectedException(context, smapiTransactionId, IdGatewayActivity.authId, "Unexpected exception during authenticate: " + e.getMessage());

            if (!(e instanceof IdtmInProgressException)) {
                printer.d("Release semaphore in error handler");
                idtmSemaphore.release();
            }

            throw new IdtmRetryException();
        }

        printer.d("Releasing semaphore on success");
        idtmSemaphore.release();
    }

    private AccessToken newAccessToken(String clientId, String sdkId, SecretKey serverSymmetricKey,
                                       String sdkIdJwtToken,
                                       ObservableEmitter<AuthenticateProgress> emitter)
            throws BadStatusCodeException, IOException, AesException, JOSEException, ParseException,
            JsonParseException, UserCanceledException, IdtmServerException {
        idGatewaySyncer.reset();
        printer.i("Get nonce API");
        smapi.logNonceRequest(context, smapiTransactionId);
        JwtResponse jwtResponse = Response.retrieve(JwtResponse.class,
                idtmApi.getNonce(clientId, sdkIdJwtToken, sdkId));
        // TODO: Maybe the following response will be encrypted
        // String jsonResponse = JwtHelper.aesDecrypt(serverSymmetricKey, jwtResponse.getData());
        String nonce = jwtResponse.getData();
        printer.i("Nonce: ", nonce);

        // Do not open the IDGateway view if app is in background
        ActivityManager.RunningAppProcessInfo myProcess = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(myProcess);
        if (myProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
            // User has put app in background - this is the same as if user has cancelled login
            smapi.logAuthenticateUserCancelled(context, smapiTransactionId, IdGatewayActivity.authId);
            throw new UserCanceledException();
        }

        printer.i("Opening the ID Gateway");
        String idGatewayRedirectUrl = dataCrypt.getString(Prefs.ID_GATEWAY_REDIRECT_URL);
        String mobileAcrValues = dataCrypt.getString(Prefs.ID_GATEWAY_MOBILE_ACR_VALUES);
        String wifiAcrValues = dataCrypt.getString(Prefs.ID_GATEWAY_WIFI_ACR_VALUES);
        String scope = dataCrypt.getString(Prefs.ID_GATEWAY_SCOPE);
        String loginHint = dataCrypt.getString(Prefs.LOGIN_HINT);
        String idGatewayUrl = environment.getIdgatewayUrl(clientId, nonce,
                idGatewayRedirectUrl, mobileAcrValues, wifiAcrValues, scope, loginHint);
        printer.i("URL: " + idGatewayUrl);
        IdGatewayActivity.start(context, idGatewayUrl);
        try {
            idGatewaySyncer.waitForStart();
        } catch (InterruptedException e) {
            smapi.logNonceBackendFailure(context, smapiTransactionId, e.getMessage());
            throw new IdtmServerException();
        }
        smapi.logWebviewStarted(context, smapiTransactionId);
        emitter.onNext(new AuthenticateProgress(true, null));

        try {
            idGatewaySyncer.waitForRedirectToAuth();
            smapi.logWebviewRedirectedToAuth(context, smapiTransactionId, IdGatewayActivity.authId);
        } catch (InterruptedException e) {
            smapi.logWebviewRedirectedToAuthFailure(context, smapiTransactionId, IdGatewayActivity.authId, e.getMessage());
            throw new IdtmServerException();
        }

        try {
            idGatewaySyncer.waitForFinish();
        } catch (InterruptedException e) {
            throw new IdtmServerException();
        }

        smapi.logWebviewFinished(context, smapiTransactionId, IdGatewayActivity.authId);
        emitter.onNext(new AuthenticateProgress(false, null));
        if (idGatewaySyncer.isSuccess()) {
            printer.i("Get token API");
            smapi.logAccessTokenRequest(context, smapiTransactionId, null, idGatewaySyncer.getCode(), nonce);
            GetAccessTokenBody getAccessTokenBody = new GetAccessTokenBody(sdkId, nonce,
                    idGatewaySyncer.getCode(), idGatewayRedirectUrl);
            jwtResponse = Response.retrieve(JwtResponse.class,
                    idtmApi.getAccessToken(clientId, sdkIdJwtToken, sdkId, getAccessTokenBody));
            smapi.logAuthenticateBackendSuccess(context, smapiTransactionId, IdGatewayActivity.authId);
            String jsonResponse = JwtHelper.aesDecrypt(serverSymmetricKey, jwtResponse.getData());
            printer.i("Decrypted response: ", jsonResponse);
            AccessTokenResponse.OauthToken oauthToken = gson.fromJson(jsonResponse,
                    AccessTokenResponse.class).getOauthToken();
            printer.i("Saving data");
            dataCrypt.set(Prefs.ACCESS_TOKEN, oauthToken.getAccessToken());
            dataCrypt.set(Prefs.ACCESS_TOKEN_TYPE, oauthToken.getTokenType());
            dataCrypt.set(Prefs.ACCESS_TOKEN_REFRESH_TOKEN, oauthToken.getRefreshToken());
            printer.i("Current refresh token: ", oauthToken.getRefreshToken());
            dataCrypt.set(Prefs.ACCESS_TOKEN_EXPIRE_TIME_MS, System.currentTimeMillis() +
                    TimeUnit.SECONDS.toMillis(oauthToken.getExpiresIn()));
            dataCrypt.set(Prefs.ACCESS_TOKEN_SUB, oauthToken.getSub());
            dataCrypt.set(Prefs.ACCESS_TOKEN_ACR, oauthToken.getAcr());
            smapi.logAuthenticateAccessTokenGenerated(context, smapiTransactionId, IdGatewayActivity.authId);
            return new AccessToken(oauthToken.getAccessToken(),
                    oauthToken.getTokenType(), oauthToken.getSub());
        } else if (idGatewaySyncer.isCanceled()) {
            smapi.logAuthenticateUserCancelled(context, smapiTransactionId, IdGatewayActivity.authId);
            throw new UserCanceledException();
        } else if (idGatewaySyncer.isUserFailedToLogin()) {
            smapi.logAuthenticateUserFailedToLogin(context, smapiTransactionId, IdGatewayActivity.authId);
            throw new UserCanceledException();
        } else {
            smapi.logAuthenticateServerIssue(context, smapiTransactionId, IdGatewayActivity.authId, "Issue on IDGW - Login result is neither success nor cancelled");
            throw new IdtmServerException();
        }
    }

    private AccessToken refreshAccessToken(String clientId, String sdkId,
                                           SecretKey serverSymmetricKey, String sdkIdJwtToken,
                                           ObservableEmitter<AuthenticateProgress> emitter)
            throws BadStatusCodeException, IOException, AesException, JOSEException, ParseException,
            JsonParseException, UserCanceledException, IdtmServerException, IdtmRetryException, SecureStorageException, IdGatewayRequiredException, IdtmIgnoreException, IdtmSSLPeerUnverifiedException {
        try {

            smapi.logRefreshToken(context, smapiTransactionId);

            // Get current refresh token
            String refreshToken = dataCrypt.getString(Prefs.ACCESS_TOKEN_REFRESH_TOKEN);


            // Prepare request
            RefreshAccessTokenBody body = new RefreshAccessTokenBody(sdkId, refreshToken);
            printer.i("Refresh token API");

            // Call refresh token API
            JwtResponse jwtResponse = Response.retrieve(JwtResponse.class,
                    idtmApi.refreshAccessToken(clientId, sdkIdJwtToken, sdkId, body));

            // Evaluate response
            String jsonResponse = JwtHelper.aesDecrypt(serverSymmetricKey, jwtResponse.getData());
            printer.i("Decrypted response: ", jsonResponse);
            AccessTokenResponse.OauthToken oauthToken = gson.fromJson(jsonResponse,
                    AccessTokenResponse.class).getOauthToken();

            // Store new tokens
            printer.i("Saving data");
            dataCrypt.set(Prefs.ACCESS_TOKEN, oauthToken.getAccessToken());
            dataCrypt.set(Prefs.ACCESS_TOKEN_TYPE, oauthToken.getTokenType());
            dataCrypt.set(Prefs.ACCESS_TOKEN_REFRESH_TOKEN, oauthToken.getRefreshToken());
            printer.i("Current refresh token: ", oauthToken.getRefreshToken());
            dataCrypt.set(Prefs.ACCESS_TOKEN_EXPIRE_TIME_MS, System.currentTimeMillis() +
                    TimeUnit.SECONDS.toMillis(oauthToken.getExpiresIn()));
            smapi.logAuthenticateRefreshTokenSuccess(context, smapiTransactionId);

            return new AccessToken(oauthToken.getAccessToken(), oauthToken.getTokenType(),
                    dataCrypt.getString(Prefs.ACCESS_TOKEN_SUB));
        }
        // We received a response from the backend with response status != 200
        // - If http 400 OR Http 500 with UserRedirectRequiredException:
        //     Handle as non-retriable issue...
        //     - Invalidate all tokens
        //     - If IDGateway is allowed: Ask customer to log in again
        //     - If IDGateway is not allowed and call was not initiated by auto refresh: Return IDGatewayRequiredException
        //     - If IDGateway is not allowed and call was not initiated by auto refresh: Stop auto refresh task
        // - If http 500 without UserRedirectRequiredException:
        //     Handle as retriable exception...
        //     - Set / increase retry counter
        //     - If retry-counter <  Max retries AND call was not initiated by auto refresh:
        //          Return IDTMRetryException
        //     - If retry-counter <  Max retries AND call was initiated by auto refresh:
        //          Schedule next auto refresh
        //     - If retry-counter >=  Max retries AND call was not initiated by auto refresh:
        //          - Invalidate all tokens
        //          - If IDGateway is allowed: Ask customer to log in again
        //          - If IDGateway is not allowed: Return IDGatewayRequiredException
        //     - If retry-counter >=  Max retries AND call was initiated by auto refresh:
        //          - Invalidate all tokens
        //          - Stop auto refresh task
        catch (BadStatusCodeException e) {

            boolean nonRetriableError = false;

            int responseCode = 500;
            String errorResponseBody = "";
            String errorDescription = "";

            if (e.getResponse() != null) {
                responseCode = e.getResponse().getResponseCode();
                errorResponseBody = e.getResponse().getResponseErrorBodyString();
                errorDescription = "Code: " + responseCode + ", Body: " + errorResponseBody;
            }

            printer.e("APIX returns error code while refreshing access token. ", errorDescription);
            smapi.logAuthenticateRefreshTokenAPIXError(context, smapiTransactionId, errorDescription);

            // Check if error is retriable or not
            if (responseCode == 400) {
                printer.e("APIX error is a non-retriable error");
                smapi.logAuthenticateRefreshTokenNonRetriableError(context, smapiTransactionId);
                nonRetriableError = true;
            } else {
                // Check if response contains UserRedirectRequiredException
                if (errorResponseBody != null && errorResponseBody.contains("UserRedirectRequiredException")) {
                    printer.e("APIX error is a non-retriable error");
                    smapi.logAuthenticateRefreshTokenNonRetriableError(context, smapiTransactionId);
                    nonRetriableError = true;
                }
            }

            // Check retry counter and if necessary, turn retriable error into non-retriable error
            if (!nonRetriableError && !autoRefresh) {
                printer.e("APIX error is a retriable error");
                long counter = dataCrypt.getLong(Prefs.REFRESH_COUNTER, -1);
                if (counter == -1) {
                    dataCrypt.set(Prefs.REFRESH_COUNTER, 1);
                } else {
                    dataCrypt.set(Prefs.REFRESH_COUNTER, counter + 1);
                }
                printer.d("Retry count: ", counter);
                // Max retries - handle like non-retriable error
                if (counter + 1 >= MAX_RETRY_COUNT) {
                    printer.e("APIX error is a retriable error, but retry counter has reached max attempts. Handling as non-retriable error.");
                    smapi.logAuthenticateRefreshTokenRetriableErrorMaxRetryCount(context, smapiTransactionId);
                    nonRetriableError = true;
                }
            }

            if (nonRetriableError) {
                // Invalidate tokens
                printer.d("Invalidate tokens...");

                RevokeTokens revokeTokens = new RevokeTokens(context, preferences, printer, dataCrypt, smapi, environment);
                revokeTokens.startLogout();

                // Ask user to log in again if possible
                if (allowIdGateway) {
                    printer.d("Ask customer to log in again");
                    smapi.logAuthenticateRefreshTokenNewManualLogin(context, smapiTransactionId);

                    AccessToken accessToken = newAccessToken(clientId, sdkId, serverSymmetricKey, sdkIdJwtToken, emitter);

                    // If login was successful, return access token
                    // Handling of refresh counter and auto-refresh is done in calling method
                    if (accessToken != null) {
                        // return access token
                        return accessToken;
                    }
                    // If login was not successful
                    else {
                        printer.d("Log in was not successful.");
                        smapi.logAuthenticateRefreshTokenManualLoginFailed(context, smapiTransactionId);
                        printer.e("Throwing 5");
                        throw new IdtmServerException();
                    }
                } else {
                    // if call was initiated by container app, return IDGatewayRequiredException
                    if (!autoRefresh) {
                        printer.e("Cannot handle non-retriable error gracefully because IDGateway is not allowed.");
                        smapi.logAuthenticateRefreshTokenNonRetriableNoGatewayAllowed(context, smapiTransactionId);
                        throw new IdGatewayRequiredException();
                    }
                    // otherwise we have a non-retiable error caused by auto-refresh
                    else {
                        // Do nothing
                        printer.d("Non-retriable exception during auto-refresh. Auto-refresh stopped.");
                        smapi.logAuthenticateRefreshTokenNonRetriableDuringAutoLogin(context, smapiTransactionId);
                        // We must return a non-null value - otherwise next call will be blocked
                        throw new IdtmIgnoreException();
                    }
                }
            }
            // Handle retriable errors now
            else {
                // If call was initiated by container app, return dtmRetryException
                if (!autoRefresh) {
                    printer.e("Retriable error - inform container app");
                    smapi.logAuthenticateRefreshTokenRetriableError(context, smapiTransactionId);
                    throw new IdtmRetryException();
                }
                // If call was done by auto-refresh - schedule next refresh timer
                else {
                    printer.e("Retriable error during auto-refresh - schedule next attempt");
                    smapi.logAuthenticateRefreshTokenRetriableErrorDuringAutoRefresh(context, smapiTransactionId);
                    //SMAPI: Retriable error during auto-refresh - schedule next attempt
                    scheduleOrCancelAutoRefreshToken(true);
                    // We must return a non-null value - otherwise next call will be blocked
                    throw new IdtmIgnoreException();
                }
            }
        }
        // Handle AES exception separately
        // If call was not initiated by auto refresh: return SecureStorageException
        // If call was initiated by auto refresh: Schedule next retry
        catch (AesException e1) {
            printer.e("Error AesException ", e1);
            smapi.logAuthenticateRefreshTokenAESException(context, smapiTransactionId);
            if (!autoRefresh) {
                throw new SecureStorageException();
            } else {
                // try again
                scheduleOrCancelAutoRefreshToken(true);
                // We must return a non-null value - otherwise next call will be blocked
                throw new IdtmIgnoreException();
            }
        }
        // Any other exception is a server exception that is retriable and does not reqiure a retry counter
        // If call was not initiated by auto refresh: return IDTM retry exceptiom
        // If call was initiated by auto refresh: Schedule next retry
        catch (Exception e) {
            printer.e("Server error", e);
            smapi.logAuthenticateServerIssue(context, smapiTransactionId, IdGatewayActivity.authId, "Unexpected exception during refresh: " + e.getMessage());
            if (!autoRefresh) {
                throw new IdtmRetryException();
            } else {
                // try again
                scheduleOrCancelAutoRefreshToken(true);
                // We must return a non-null value - otherwise next call will be blocked
                throw new IdtmIgnoreException();
            }
        }
    }


    private void scheduleOrCancelAutoRefreshToken(boolean retry) {
        try {

            printer.d("scheduleOrCancelAutoRefreshToken - retry flag = ", retry);

            if (!retry) {
                long accessTokenExpireTimeMs =
                        dataCrypt.getLong(Prefs.ACCESS_TOKEN_EXPIRE_TIME_MS, Long.MIN_VALUE);
                smapi.logAuthenticateAutoRefreshTokenJob(context, smapiTransactionId);
                AutoRefreshTokenJobService.setAlarm(context, accessTokenExpireTimeMs - AUTO_REFRESH_PRE_TIME_SEC * 1000L);
            } else {
                long retryCounter = dataCrypt.getLong(Prefs.AUTO_REFRESH_RETRY_COUNTER, -1);
                if (retryCounter == -1) {
                    retryCounter = 1;
                } else if (retryCounter < 3) {
                    retryCounter += 1;
                }
                dataCrypt.set(Prefs.AUTO_REFRESH_RETRY_COUNTER, retryCounter);


                printer.d("scheduleOrCancelAutoRefreshToken - retry counter = ", retryCounter);

                long currentTime = System.currentTimeMillis();
                if (retryCounter == 1) {
                    AutoRefreshTokenJobService.setAlarm(context, currentTime + 3L * 60000L);
                } else if (retryCounter == 2) {
                    AutoRefreshTokenJobService.setAlarm(context, currentTime + 20L * 60000L);
                } else {
                    AutoRefreshTokenJobService.setAlarm(context, currentTime + 60L * 60000L);
                }
            }
        } catch (AesException e) {
            smapi.logAuthenticateRefreshTokenJobError(context, smapiTransactionId, e.getMessage());
            printer.e("Error while setting the auto refresh ", e);
        }
    }
}
