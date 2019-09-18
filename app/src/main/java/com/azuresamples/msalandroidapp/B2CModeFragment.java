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

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation sample for 'B2C' mode.
 */
public class B2CModeFragment extends Fragment {
    private static final String TAG = B2CModeFragment.class.getSimpleName();

    /* UI & Debugging Variables */
    Button removeAccountButton;
    Button runUserFlowButton;
    Button acquireTokenSilentButton;
    TextView graphResourceTextView;
    TextView logTextView;
    Spinner policyListSpinner;
    Spinner b2cUserList;

    private List<B2CUser> users;

    /* Azure AD Variables */
    private IMultipleAccountPublicClientApplication mB2cApp;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_b2c_mode, container, false);
        initializeUI(view);

        // Creates a PublicClientApplication object with res/raw/auth_config_single_account.json
        PublicClientApplication.createMultipleAccountPublicClientApplication(getContext(),
                R.raw.auth_config_b2c,
                new IPublicClientApplication.IMultipleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(IMultipleAccountPublicClientApplication application) {
                        mB2cApp = application;
                        loadAccount();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        displayError(exception);
                        removeAccountButton.setEnabled(false);
                        runUserFlowButton.setEnabled(false);
                        acquireTokenSilentButton.setEnabled(false);
                    }
                });

        return view;
    }

    /**
     * Initializes UI variables and callbacks.
     */
    private void initializeUI(@NonNull final View view) {
        removeAccountButton = view.findViewById(R.id.btn_removeAccount);
        runUserFlowButton = view.findViewById(R.id.btn_runUserFlow);
        acquireTokenSilentButton = view.findViewById(R.id.btn_acquireTokenSilently);
        graphResourceTextView = view.findViewById(R.id.msgraph_url);
        logTextView = view.findViewById(R.id.txt_log);
        policyListSpinner = view.findViewById(R.id.policy_list);
        b2cUserList = view.findViewById(R.id.user_list);

        final ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item,
                new ArrayList<String>() {{
                    for (final String policyName : B2CConfiguration.Policies)
                        add(policyName);
                }}
        );

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        policyListSpinner.setAdapter(dataAdapter);
        dataAdapter.notifyDataSetChanged();

        runUserFlowButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mB2cApp == null) {
                    return;
                }

                /**
                 * Runs user flow interactively.
                 * <p>
                 * Once the user finishes with the flow, you will also receive an access token containing the claims for the scope you passed in (see B2CConfiguration.getScopes()),
                 * which you can subsequently use to obtain your resources.
                 */

                AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(getActivity())
                        .fromAuthority(B2CConfiguration.getAuthorityFromPolicyName(policyListSpinner.getSelectedItem().toString()))
                        .withScopes(B2CConfiguration.getScopes())
                        .withPrompt(Prompt.LOGIN)
                        .withCallback(getAuthInteractiveCallback())
                        .build();

                mB2cApp.acquireToken(parameters);

            }
        });

        acquireTokenSilentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mB2cApp == null) {
                    return;
                }

                final B2CUser selectedUser = users.get(b2cUserList.getSelectedItemPosition());
                selectedUser.acquireTokenSilentAsync(mB2cApp,
                        policyListSpinner.getSelectedItem().toString(),
                        B2CConfiguration.getScopes(),
                        getAuthSilentCallback());
            }
        });

        removeAccountButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mB2cApp == null) {
                    return;
                }

                final B2CUser selectedUser = users.get(b2cUserList.getSelectedItemPosition());
                selectedUser.signOutAsync(mB2cApp,
                        new IMultipleAccountPublicClientApplication.RemoveAccountCallback() {
                            @Override
                            public void onRemoved() {
                                logTextView.setText("Signed Out.");
                                loadAccount();
                            }

                            @Override
                            public void onError(@NonNull MsalException exception) {
                                displayError(exception);
                            }
                        });
            }
        });
    }

    /**
     * Load the currently signed-in account, if there's any.
     */
    private void loadAccount() {
        if (mB2cApp == null) {
            return;
        }

        mB2cApp.getAccounts(new IPublicClientApplication.LoadAccountsCallback() {
            @Override
            public void onTaskCompleted(final List<IAccount> result) {
                users = B2CUser.getB2CUsersFromAccountList(result);
                updateUI(users);
            }

            @Override
            public void onError(MsalException exception) {
                displayError(exception);
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

                /* Successfully got a token. */
                displayResult(authenticationResult);
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

                /* display result info */
                displayResult(authenticationResult);

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

    //
    // Helper methods manage UI updates
    // ================================
    // displayResult() - Display the authentication result.
    // displayError() - Display the token error.
    // updateSignedInUI() - Updates UI when the user is signed in
    // updateSignedOutUI() - Updates UI when app sign out succeeds
    //

    /**
     * Display the graph response
     */
    private void displayResult(@NonNull final IAuthenticationResult result) {
        final String output =
                "Access Token :" + result.getAccessToken() + "\n" +
                        "Scope : " + result.getScope() + "\n" +
                        "Expiry : " + result.getExpiresOn() + "\n" +
                        "Tenant ID : " + result.getTenantId() + "\n";

        logTextView.setText(output);
    }

    /**
     * Display the error message
     */
    private void displayError(@NonNull final Exception exception) {
        logTextView.setText(exception.toString());
    }

    /**
     * Updates UI based on the obtained user list.
     */
    private void updateUI(final List<B2CUser> users) {
        if (users.size() != 0) {
            removeAccountButton.setEnabled(true);
            acquireTokenSilentButton.setEnabled(true);
        } else {
            removeAccountButton.setEnabled(false);
            acquireTokenSilentButton.setEnabled(false);
        }

        final ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item,
                new ArrayList<String>() {{
                    for (final B2CUser user : users)
                        add(user.getDisplayName());
                }}
        );

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b2cUserList.setAdapter(dataAdapter);
        dataAdapter.notifyDataSetChanged();
    }

}

