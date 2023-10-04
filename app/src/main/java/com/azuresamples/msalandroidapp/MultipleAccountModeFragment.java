// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation sample for 'Multiple account' mode.
 */
public class MultipleAccountModeFragment extends Fragment {
    private static final String TAG = MultipleAccountModeFragment.class.getSimpleName();

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
                new IPublicClientApplication.IMultipleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(IMultipleAccountPublicClientApplication application) {
                        mMultipleAccountApp = application;
                        loadAccounts();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        displayError(exception);
                        removeAccountButton.setEnabled(false);
                        callGraphApiInteractiveButton.setEnabled(false);
                        callGraphApiSilentButton.setEnabled(false);
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

        final String defaultGraphResourceUrl = MSGraphRequestWrapper.MS_GRAPH_ROOT_ENDPOINT + "v1.0/me";
        graphResourceTextView.setText(defaultGraphResourceUrl);

        removeAccountButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mMultipleAccountApp == null) {
                    return;
                }

                /*
                 * Removes the selected account and cached tokens from this app (or device, if the device is in shared mode).
                 */
                mMultipleAccountApp.removeAccount(accountList.get(accountListSpinner.getSelectedItemPosition()),
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

        callGraphApiInteractiveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mMultipleAccountApp == null) {
                    return;
                }

                final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(getActivity())
                        .withScopes(Arrays.asList(getScopes()))
                        .withCallback(getAuthInteractiveCallback())
                        .build();
                /*
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
                mMultipleAccountApp.acquireToken(parameters);
            }
        });

        callGraphApiSilentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMultipleAccountApp == null) {
                    return;
                }

                final IAccount selectedAccount = accountList.get(accountListSpinner.getSelectedItemPosition());

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .forAccount(selectedAccount)
                        .fromAuthority(selectedAccount.getAuthority())
                        .withScopes(Arrays.asList(getScopes()))
                        .forceRefresh(false)
                        .withCallback(getAuthSilentCallback())
                        .build();

                /*
                 * Performs acquireToken without interrupting the user.
                 *
                 * This requires an account object of the account you're obtaining a token for.
                 * (can be obtained via getAccount()).
                 */
                mMultipleAccountApp.acquireTokenSilentAsync(silentParameters);
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
        if (mMultipleAccountApp == null) {
            return;
        }

        mMultipleAccountApp.getAccounts(new IPublicClientApplication.LoadAccountsCallback() {
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

    /**
     * Make an HTTP request to obtain MSGraph data
     *
     * The sample is using the global service cloud as a default.
     * If you're developing an app for sovereign cloud users, please change the Microsoft Graph Resource URL accordingly.
     * https://docs.microsoft.com/en-us/graph/deployments#microsoft-graph-and-graph-explorer-service-root-endpoints
     */
    private void callGraphAPI(final IAuthenticationResult authenticationResult) {
        MSGraphRequestWrapper.callGraphAPIUsingVolley(
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
