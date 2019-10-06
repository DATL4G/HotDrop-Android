package de.datlag.hotdrop.auth;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.interaapps.firebasemanager.auth.AnonymousAuth;
import de.interaapps.firebasemanager.auth.EmailAuth;
import de.interaapps.firebasemanager.auth.GoogleAuth;
import de.interaapps.firebasemanager.auth.OAuth;
import de.interaapps.firebasemanager.core.FirebaseManager;
import de.interaapps.firebasemanager.core.auth.Auth;

public class UserManager {

    private Activity activity;
    private FirebaseManager firebaseManager;
    private InteraAccount interaAccount;
    private InteraAuth interaAuth;

    public UserManager(Activity activity, FirebaseManager firebaseManager) {
        this.activity = activity;
        this.firebaseManager = firebaseManager;

        init();
    }

    private void init() {
        interaAuth = new InteraAuth(activity);
    }

    public boolean isLoggedIn() {
        boolean returnValue = false;
        for (Auth auth : firebaseManager.getLogin()) {
            if (auth.getUser() != null) {
                returnValue = true;
                break;
            }
        }

        return returnValue;
    }

    public ProviderEnum getProvider(@NotNull AuthResult authResult) {
        if (authResult.getUser().getProviderData().size() > 1) {
            return checkProvider(authResult.getUser().getProviderData().get(1).getProviderId());
        }
        return ProviderEnum.ANONYM;
    }

    public ProviderEnum getProvider(@NotNull FirebaseUser user) {
        if (user.getProviderData().size() > 1) {
            return checkProvider(user.getProviderData().get(1).getProviderId());
        }
        return ProviderEnum.ANONYM;
    }

    private ProviderEnum checkProvider(@NotNull String providerId) {
        if (providerId.equals(ProviderEnum.GOOGLE.getProviderId())) {
            return ProviderEnum.GOOGLE;
        } else if (providerId.equals(ProviderEnum.EMAIL.getProviderId())) {
            return ProviderEnum.EMAIL;
        } else if (providerId.equals(ProviderEnum.GITHUB.getProviderId())) {
            return ProviderEnum.GITHUB;
        }

        return ProviderEnum.GOOGLE;
    }

    public void getUserName(UserNameCallback userNameCallback) {
        FirebaseAuth firebaseAuth = null;
        AuthResult authResult = null;
        FirebaseUser user = null;
        ProviderEnum providerEnum;

        for (Auth auth : firebaseManager.getLogin()) {
            if (auth.getAuthResult() != null) {
                authResult = auth.getAuthResult();
            }
            if (auth.getUser() != null) {
                user = auth.getUser();
            }
            if (auth.getAuth() != null) {
                firebaseAuth = auth.getAuth();
            }
        }

        if (authResult == null) {
            providerEnum = getProvider(user);
        } else {
            providerEnum = getProvider(authResult);
        }

        if (providerEnum == ProviderEnum.EMAIL) {
            if (interaAccount != null) {
                userNameCallback.onSuccess(interaAccount.getUsername());
            } else {
                interaAuth.getUserInfo(user, new InteraAuth.UserInfoCallback() {
                    @Override
                    public void onUserInfoSuccess(String response) {
                        interaAccount = new InteraAccount(response);
                        userNameCallback.onSuccess(interaAccount.getUsername());
                    }

                    @Override
                    public void onUserInfoFailed(Exception exception) {
                        userNameCallback.onFailed(exception);
                    }
                });
            }
        } else if (providerEnum == ProviderEnum.GITHUB) {
            if (authResult != null) {
                userNameCallback.onSuccess(authResult.getAdditionalUserInfo().getUsername());
            } else {
                firebaseAuth.getAccessToken(true).addOnSuccessListener(activity, new OnSuccessListener<GetTokenResult>() {
                    @Override
                    public void onSuccess(GetTokenResult getTokenResult) {
                        Log.e("Github Success", getTokenResult.getToken());
                        userNameCallback.onSuccess("Github Account");
                    }
                }).addOnFailureListener(activity, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Github Error", e.getMessage());
                        userNameCallback.onFailed(e);
                    }
                });
            }
        } else if (providerEnum == ProviderEnum.GOOGLE) {
            userNameCallback.onSuccess(user.getDisplayName());
        } else {
            userNameCallback.onSuccess("Anonymous");
        }
    }

    public void login(Auth auth, LoginCallback loginCallback) {
                if (auth instanceof GoogleAuth) {
                    ((GoogleAuth) auth).startLogin(new GoogleAuth.LoginCallbacks() {
                        @Override
                        public void onLoginSuccessful(@Nullable AuthResult authResult) {
                            loginCallback.onLoginSuccess(authResult);
                        }

                        @Override
                        public void onLoginFailed(Exception exception) {
                            loginCallback.onLoginFailed(exception);
                        }
                    });
                } else if (auth instanceof EmailAuth) {
                    Log.e("Intera", "Login");
                    interaAuth.startLogin((EmailAuth) auth, new InteraAuth.LoginCallback() {
                        @Override
                        public void onLoginSuccess(@NotNull AuthResult authResult) {
                            interaAuth.getUserInfo(authResult.getUser(), new InteraAuth.UserInfoCallback() {
                                @Override
                                public void onUserInfoSuccess(String response) {
                                    interaAccount = new InteraAccount(response);
                                    loginCallback.onLoginSuccess(authResult);
                                    Log.e("Intera", interaAccount.getUsername());
                                }

                                @Override
                                public void onUserInfoFailed(Exception exception) {
                                    loginCallback.onLoginSuccess(authResult);
                                    Log.e("Intera", exception.getMessage());
                                }
                            });
                        }

                        @Override
                        public void onLoginFailed(Exception exception) {
                            loginCallback.onLoginFailed(exception);
                        }
                    });
                } else if (auth instanceof OAuth) {
                    ((OAuth) auth).startLogin(new OAuth.LoginCallbacks() {
                        @Override
                        public void onLoginSuccessful(AuthResult authResult) {
                            loginCallback.onLoginSuccess(authResult);
                        }

                        @Override
                        public void onLoginFailed(Exception exception) {
                            loginCallback.onLoginFailed(exception);
                        }
                    });
                } else if (auth instanceof AnonymousAuth) {
                    ((AnonymousAuth) auth).startLogin(new AnonymousAuth.LoginCallbacks() {
                        @Override
                        public void onLoginSuccessful(AuthResult authResult) {
                            loginCallback.onLoginSuccess(authResult);
                        }

                        @Override
                        public void onLoginFailed(Exception exception) {
                            loginCallback.onLoginFailed(exception);
                        }
                    });
                }
    }

    public void logout(LogoutCallback logoutCallback) {
        for (Auth auth : firebaseManager.getLogin()) {
            if (auth instanceof GoogleAuth) {
                ((GoogleAuth) auth).signOut(new GoogleAuth.LogoutCallbacks() {
                    @Override
                    public void onLogoutSuccessful() {
                        auth.signOut();
                        ((GoogleAuth) auth).revokeAccess(new GoogleAuth.RevokeAccessCallbacks() {
                            @Override
                            public void onRevokeAccessSuccessful(Void aVoid) {

                            }

                            @Override
                            public void onRevokeAccessFailed(Exception exception) {

                            }
                        });
                        logoutCallback.onLogoutSuccess();
                    }

                    @Override
                    public void onLogoutFailed(Exception exception) {
                        logoutCallback.onLogoutFailed();
                    }
                });
            } else if (auth instanceof EmailAuth) {
                auth.signOut();
                logoutCallback.onLogoutSuccess();
            } else if (auth instanceof OAuth) {
                auth.signOut();
                logoutCallback.onLogoutSuccess();
            } else if (auth instanceof AnonymousAuth) {
                auth.signOut();
                logoutCallback.onLogoutSuccess();
            }
        }
    }

    public interface LoginCallback {
        void onLoginSuccess(AuthResult authResult);

        void onLoginFailed(Exception exception);
    }

    public interface LogoutCallback {
        void onLogoutSuccess();

        void onLogoutFailed();
    }

    public interface UserNameCallback {
        void onSuccess(String username);

        void onFailed(Exception exception);
    }
}