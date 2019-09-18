package com.azuresamples.msalandroidapp;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Represents a B2C user.
 */
public class B2CUser {
    /**
     * A factory method for generating B2C users based on the given IAccount list.
     */
    public static List<B2CUser> getB2CUsersFromAccountList(@NonNull final List<IAccount> accounts) {
        final HashMap<String, B2CUser> b2CUserHashMap = new HashMap<>();

        for (IAccount account : accounts) {
            /**
             * IMPORTANT: an account object will be created for every policies the user has successfully performed.
             *            You can use the 'Subject' claim to identify that those accounts belong to the same user.
             */
            final String subject = B2CUser.getSubjectFromAccount(account);

            B2CUser user = b2CUserHashMap.get(subject);
            if (user == null) {
                user = new B2CUser();
            }

            user.accounts.add(account);
            b2CUserHashMap.put(subject, user);
        }

        List<B2CUser> users = new ArrayList<>();
        users.addAll(b2CUserHashMap.values());
        return users;
    }

    /**
     * List of account objects that are associated to this B2C user.
     */
    private List<IAccount> accounts = new ArrayList<>();

    private B2CUser() {
    }

    /**
     * Gets this user's display name.
     * If the value is not set, returns 'subject' instead.
     */
    public String getDisplayName() {
        if (accounts.isEmpty()) {
            return null;
        }

        final String displayName = getB2CDisplayNameFromAccount(accounts.get(0));
        if (displayName != null) {
            return displayName;
        }

        return getSubjectFromAccount(accounts.get(0));
    }

    /**
     * Acquires a token without interrupting the user.
     */
    public void acquireTokenSilentAsync(final IMultipleAccountPublicClientApplication multipleAccountPublicClientApplication,
                                        final String policyName,
                                        final List<String> scopes,
                                        final SilentAuthenticationCallback callback) {

        for (IAccount account: accounts){
            if (policyName.equalsIgnoreCase(getB2CPolicyNameFromAccount(account))){
                AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
                        .fromAuthority(B2CConfiguration.getAuthorityFromPolicyName(policyName))
                        .withScopes(scopes)
                        .forAccount(account)
                        .withCallback(callback)
                        .build();

                multipleAccountPublicClientApplication.acquireTokenSilentAsync(parameters);
                return;
            }
        }

        callback.onError(new MsalUiRequiredException("Account associated to the policy is not found."));
    }

    /**
     * Signs the user out of your application.
     */
    public void signOutAsync(final IMultipleAccountPublicClientApplication multipleAccountPublicClientApplication,
                             final IMultipleAccountPublicClientApplication.RemoveAccountCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (IAccount account : accounts) {
                        multipleAccountPublicClientApplication.removeAccount(account);
                    }

                    accounts.clear();
                    callback.onRemoved();
                } catch (MsalException e) {
                    callback.onError(e);
                } catch (InterruptedException e) {
                    // Unexpected.
                }
            }
        }).start();
    }



    /**
     * Get name of the policy associated with the given B2C account.
     * See https://docs.microsoft.com/en-us/azure/active-directory-b2c/active-directory-b2c-reference-tokens for more info.
     */
    private static String getB2CPolicyNameFromAccount(@NonNull final IAccount account) {
        return (String) (account.getClaims().get("tfp"));
    }

    /**
     * Get subject of the given B2C account.
     * <p>
     * Subject is the principal about which the token asserts information, such as the user of an application.
     * See https://docs.microsoft.com/en-us/azure/active-directory-b2c/active-directory-b2c-reference-tokens for more info.
     */
    private static String getSubjectFromAccount(@NonNull final IAccount account) {
        return (String) (account.getClaims().get(IDToken.SUBJECT));
    }

    /**
     * Get a displayable name of the given B2C account.
     * This claim is optional.
     */
    private static String getB2CDisplayNameFromAccount(@NonNull final IAccount account) {
        Object displayName = account.getClaims().get(IDToken.NAME);

        if (displayName == null) {
            return null;
        }

        return displayName.toString();
    }
}
