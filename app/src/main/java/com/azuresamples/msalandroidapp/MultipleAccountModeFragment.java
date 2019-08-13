package com.azuresamples.msalandroidapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation sample for 'Multiple account' mode.
 */
public class MultipleAccountModeFragment extends Fragment {
    private static final String TAG = SingleAccountModeFragment.class.getSimpleName();

    /* Azure AD v2 Configs */
    final static String AUTHORITY = "https://login.microsoftonline.com/common";

    /* UI & Debugging Variables */
    Button removeAccountButton;
    Button callGraphApiInteractiveButton;
    Button callGraphApiSilentButton;
    TextView scopeTextView;
    TextView graphResourceTextView;
    TextView logTextView;
    Spinner accountListSpinner;

    /* Azure AD Variables */
    private IMultipleAccountPublicClientApplication mMultipleAccountApp;

    private List<IAccount> accountList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_multiple_account_mode, container, false);
        initializeUI(view);

        // Creates a PublicClientApplication object with res/raw/auth_config_single_account.json
        PublicClientApplication.createMultipleAccountPublicClientApplication(getContext(),
                R.raw.auth_config_multiple_account,
                new PublicClientApplication.ApplicationCreatedListener() {
                    @Override
                    public void onCreated(IPublicClientApplication application) {

                        if (application instanceof IMultipleAccountPublicClientApplication) {
                            mMultipleAccountApp = (IMultipleAccountPublicClientApplication) application;
                            loadAccount();
                        } else {
                            /* If the device is in shared mode, it will ALWAYS return ISingleAccountPublicClientApplication. */
                            logTextView.setText("Please switch to 'Single Account' mode.");
                            removeAccountButton.setEnabled(false);
                            callGraphApiInteractiveButton.setEnabled(false);
                            callGraphApiSilentButton.setEnabled(false);
                        }
                    }

                    @Override
                    public void onError(MsalException exception) {
                        logTextView.setText(exception.toString());
                    }
                });

        return view;
    }

    /**
     * Initializes UI variables and callbacks.
     */
    private void initializeUI(@NonNull final View view) {
        removeAccountButton = view.findViewById(R.id.btn_removeAccount);
        callGraphApiInteractiveButton = view.findViewById(R.id.btn_callGraphInteractively);
        callGraphApiSilentButton = view.findViewById(R.id.btn_callGraphSilently);
        scopeTextView = view.findViewById(R.id.scope);
        graphResourceTextView = view.findViewById(R.id.msgraph_url);
        logTextView = view.findViewById(R.id.txt_log);
        accountListSpinner = view.findViewById(R.id.account_list);

        removeAccountButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mMultipleAccountApp == null) {
                    return;
                }

                /**
                 * Removes the selected account and cached tokens from this app (or device, if the device is in shared mode).
                 */
                mMultipleAccountApp.removeAccount(accountList.get(accountListSpinner.getSelectedItemPosition()),
                        new IMultipleAccountPublicClientApplication.RemoveAccountCallback() {
                            @Override
                            public void onRemoved() {
                                Toast.makeText(getContext(), "Account removed.", Toast.LENGTH_SHORT)
                                        .show();

                                /* Reload account asynchronously to get the up-to-date list. */
                                loadAccount();
                            }

                            @Override
                            public void onError(@NonNull MsalException exception) {
                                displayError(exception);
                            }
                        });
            }
        });

        callGraphApiInteractiveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mMultipleAccountApp == null) {
                    return;
                }

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
                mMultipleAccountApp.acquireToken(getActivity(), getScopes(), getAuthInteractiveCallback());
            }
        });

        callGraphApiSilentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMultipleAccountApp == null) {
                    return;
                }

                /**
                 * Performs acquireToken without interrupting the user.
                 *
                 * This requires an account object of the account you're obtaining a token for.
                 * (can be obtained via getAccount()).
                 */
                mMultipleAccountApp.acquireTokenSilentAsync(getScopes(),
                        accountList.get(accountListSpinner.getSelectedItemPosition()),
                        AUTHORITY,
                        getAuthSilentCallback());
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
     * Load the currently signed-in account, if there's any.
     * In the shared device mode, if the user is signed out from the device, the app can also perform the clean-up work in onAccountChanged().
     */
    private void loadAccount() {
        if (mMultipleAccountApp == null) {
            return;
        }

        mMultipleAccountApp.getAccounts(new IPublicClientApplication.LoadAccountsCallback() {
            @Override
            public void onTaskCompleted(final List<IAccount> result) {
                accountList = result;
                updateUI(accountList);
            }

            @Override
            public void onError(MsalException exception) {
                logTextView.setText(exception.toString());
            }
        });
    }

    /**
     * Callback used in for silent acquireToken calls.
     * Looks if tokens are in the cache (refreshes if necessary and if we don't forceRefresh)
     * else errors that we need to do an interactive request.
     */
    private AuthenticationCallback getAuthSilentCallback() {
        return new AuthenticationCallback() {

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Log.d(TAG, "Successfully authenticated");

                /* Successfully got a token, use it to call a protected resource - MSGraph */
                callGraphAPI(authenticationResult);
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

            @Override
            public void onCancel() {
                /* User cancelled the authentication */
                Log.d(TAG, "User cancelled login.");
            }
        };
    }

    /**
     * Callback used for interactive request.
     * If succeeds we use the access token to call the Microsoft Graph.
     * Does not check cache.
     */
    private AuthenticationCallback getAuthInteractiveCallback() {
        return new AuthenticationCallback() {

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                /* Successfully got a token, use it to call a protected resource - MSGraph */
                Log.d(TAG, "Successfully authenticated");
                Log.d(TAG, "ID Token: " + authenticationResult.getAccount().getClaims().get("id_token"));

                /* call graph */
                callGraphAPI(authenticationResult);

                /* Reload account asynchronously to get the up-to-date list. */
                loadAccount();
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

    /**
     * Make an HTTP request to obtain MSGraph data
     */
    private void callGraphAPI(final IAuthenticationResult authenticationResult) {
        MSGraphRequestWrapper.callGraphAPIWithVolley(
                getContext(),
                graphResourceTextView.getText().toString(),
                authenticationResult.getAccessToken(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        /* Successfully called graph, process data and send to UI */
                        Log.d(TAG, "Response: " + response.toString());
                        displayGraphResult(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "Error: " + error.toString());
                        displayError(error);
                    }
                });
    }

    //
    // Helper methods manage UI updates
    // ================================
    // displayGraphResult() - Display the graph response
    // displayError() - Display the graph response
    // updateSignedInUI() - Updates UI when the user is signed in
    // updateSignedOutUI() - Updates UI when app sign out succeeds
    //

    /**
     * Display the graph response
     */
    private void displayGraphResult(@NonNull final JSONObject graphResponse) {
        logTextView.setText(graphResponse.toString());
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
            callGraphApiInteractiveButton.setEnabled(true);
            callGraphApiSilentButton.setEnabled(true);
        } else {
            removeAccountButton.setEnabled(false);
            callGraphApiInteractiveButton.setEnabled(true);
            callGraphApiSilentButton.setEnabled(false);
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
