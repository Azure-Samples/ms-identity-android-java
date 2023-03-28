package com.azuresamples.msalandroidapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This fragment support CIAM Authority in Multiple Account mode. Dedicated CIAM Authority support has not been fully released yet, this fragment
 * and it's json configuration file will be updated once that release is complete (ETA Q2 2023)
 */
public class CIAMModeFragment extends Fragment {
    private static final String TAG = CIAMModeFragment.class.getSimpleName();

    /* UI & Debugging Variables */
    Button removeAccountButton;
    Button callAcquireTokenInteractiveButton;
    Button callAcquireTokenSilentButton;
    TextView scopeTextView;
    TextView logTextView;
    Spinner accountListSpinner;

    /* Azure AD Variables */
    private IMultipleAccountPublicClientApplication mCiamApp;
    private List<IAccount> accountList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_ciam_mode, container, false);
        initializeUI(view);

        // Creates a PublicClientApplication object with res/raw/auth_config_ciam.json
        PublicClientApplication.createMultipleAccountPublicClientApplication(getContext(),
                R.raw.auth_config_ciam,
                new IPublicClientApplication.IMultipleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(IMultipleAccountPublicClientApplication application) {
                        mCiamApp = application;
                        loadAccounts();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        displayError(exception);
                        removeAccountButton.setEnabled(false);
                        callAcquireTokenInteractiveButton.setEnabled(false);
                        callAcquireTokenSilentButton.setEnabled(false);
                    }
                });

        return view;
    }

    /**
     * Initializes UI variables and callbacks.
     */
    private void initializeUI(@NonNull final View view) {
        removeAccountButton = view.findViewById(R.id.btn_removeAccount);
        callAcquireTokenInteractiveButton = view.findViewById(R.id.btn_acquireTokenInteractively);
        callAcquireTokenSilentButton = view.findViewById(R.id.btn_acquireTokenSilently);
        scopeTextView = view.findViewById(R.id.scope);
        logTextView = view.findViewById(R.id.txt_log);
        accountListSpinner = view.findViewById(R.id.account_list);

        removeAccountButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mCiamApp == null) {
                    return;
                }

                /**
                 * Removes the selected account and cached tokens from this app (or device, if the device is in shared mode).
                 */
                mCiamApp.removeAccount(accountList.get(accountListSpinner.getSelectedItemPosition()),
                        new IMultipleAccountPublicClientApplication.RemoveAccountCallback() {
                            @Override
                            public void onRemoved() {
                                Toast.makeText(getContext(), "Account removed.", Toast.LENGTH_SHORT)
                                        .show();

                                /* Reload account asynchronously to get the up-to-date list. */
                                loadAccounts();
                            }

                            @Override
                            public void onError(@NonNull MsalException exception) {
                                displayError(exception);
                            }
                        });
            }
        });

        callAcquireTokenInteractiveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mCiamApp == null) {
                    return;
                }

                displayTokenResult("");

                /**
                 * Acquire token interactively. It will also create an account object for the silent call as a result (to be obtained by getAccount()).
                 *
                 * If acquireTokenSilent() returns an error that requires an interaction,
                 * invoke acquireToken() to have the user resolve the interrupt interactively.
                 *
                 * Some example scenarios are
                 *  - password change
                 *  - the resource you're acquiring a token for has a stricter set of requirement than your SSO refresh token.
                 *  - you're introducing a new scope which the user has never consented for.
                 */
                final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(getActivity())
                        .withScopes(Arrays.asList(getScopes()))
                        .withCallback(getAuthInteractiveCallback())
                        .build();

                mCiamApp.acquireToken(parameters);
            }
        });

        callAcquireTokenSilentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCiamApp == null) {
                    return;
                }

                displayTokenResult("");

                final IAccount selectedAccount = accountList.get(accountListSpinner.getSelectedItemPosition());

                /**
                 * Performs acquireToken without interrupting the user.
                 *
                 * This requires an account object of the account you're obtaining a token for.
                 * (can be obtained via getAccount()).
                 */
                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .forAccount(selectedAccount)
                        .fromAuthority(selectedAccount.getAuthority())
                        .withScopes(Arrays.asList(getScopes()))
                        .forceRefresh(false)
                        .withCallback(getAuthSilentCallback())
                        .build();

                mCiamApp.acquireTokenSilentAsync(silentParameters);
            }
        });

    }

    /**
     * Extracts a scope array from a text field,
     * i.e. from "User.Read User.ReadWrite" to ["user.read", "user.readwrite"]
     */
    private String[] getScopes() {
        return scopeTextView.getText().toString().toLowerCase().split(" ");
    }

    /**
     * Load currently signed-in accounts, if there's any.
     */
    private void loadAccounts() {
        if (mCiamApp == null) {
            return;
        }

        mCiamApp.getAccounts(new IPublicClientApplication.LoadAccountsCallback() {
            @Override
            public void onTaskCompleted(final List<IAccount> result) {
                // You can use the account data to update your UI or your app database.
                accountList = result;
                updateUI(accountList);
            }

            @Override
            public void onError(MsalException exception) {
                displayError(exception);
            }
        });
    }

    /**
     * Callback used in for silent acquireToken calls.
     */
    private SilentAuthenticationCallback getAuthSilentCallback() {
        return new SilentAuthenticationCallback() {

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Log.d(TAG, "Successfully authenticated");

                /* Display Access Token */
                displayTokenResult("Silent Request Success:\n" + authenticationResult.getAccessToken());
            }

            @Override
            public void onError(MsalException exception) {
                /* Failed to acquireToken */
                Log.d(TAG, "Authentication failed: " + exception.toString());
                displayError(exception);

                if (exception instanceof MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception instanceof MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                } else if (exception instanceof MsalUiRequiredException) {
                    /* Tokens expired or no session, retry with interactive */
                }
            }
        };
    }

    /**
     * Callback used for interactive request.
     * If succeeds, we display the access token
     * Does not check cache.
     */
    private AuthenticationCallback getAuthInteractiveCallback() {
        return new AuthenticationCallback() {

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                /* Successfully got a token */
                Log.d(TAG, "Successfully authenticated");
                Log.d(TAG, "ID Token: " + authenticationResult.getAccount().getClaims().get("id_token"));

                /* Display Access Token */
                displayTokenResult("Interactive Request Success:\n" + authenticationResult.getAccessToken());

                /* Reload account asynchronously to get the up-to-date list. */
                loadAccounts();
            }

            @Override
            public void onError(MsalException exception) {
                /* Failed to acquireToken */
                Log.d(TAG, "Authentication failed: " + exception.toString());
                displayError(exception);

                if (exception instanceof MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception instanceof MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                }
            }

            @Override
            public void onCancel() {
                /* User canceled the authentication */
                Log.d(TAG, "User cancelled login.");
            }
        };
    }

    //
    // Helper methods manage UI updates
    // ================================
    // displayError() - Display the error message
    // updateUI() - Updates UI based on account list
    //

    /**
     * Display the access token
     */
    private void displayTokenResult(@NonNull final String accessToken) {
        logTextView.setText(accessToken);
    }

    /**
     * Display the error message
     */
    private void displayError(@NonNull final Exception exception) {
        logTextView.setText(exception.toString());
    }

    /**
     * Updates UI based on the obtained account list.
     */
    private void updateUI(@NonNull final List<IAccount> result) {

        if (result.size() > 0) {
            removeAccountButton.setEnabled(true);
            callAcquireTokenInteractiveButton.setEnabled(true);
            callAcquireTokenSilentButton.setEnabled(true);
        } else {
            removeAccountButton.setEnabled(false);
            callAcquireTokenInteractiveButton.setEnabled(true);
            callAcquireTokenSilentButton.setEnabled(false);
        }

        final ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item,
                new ArrayList<String>() {{
                    for (final IAccount account : result)
                        add(account.getUsername());
                }}
        );

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accountListSpinner.setAdapter(dataAdapter);
        dataAdapter.notifyDataSetChanged();
    }
}
