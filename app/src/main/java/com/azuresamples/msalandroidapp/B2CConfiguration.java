package com.azuresamples.msalandroidapp;

import java.util.Arrays;
import java.util.List;

/**
 * The value in this class has to map with the json configuration file (auth_config_b2c.json).
 * i.e. If you are using the following json file.
 * {
 *   "client_id" : "90c0fe63-bcf2-44d5-8fb7-b8bbc0b29dc6",
 *   "redirect_uri" : "msauth://com.azuresamples.msalandroidapp/1wIqXSqBj7w%2Bh11ZifsnqwgyKrY%3D",
 *   "account_mode" : "MULTIPLE",
 *   "broker_redirect_uri_registered": false,
 *   "authorities": [
 *     {
 *       "type": "B2C",
 *       "authority_url": "https://fabrikamb2c.b2clogin.com/tfp/fabrikamb2c.onmicrosoft.com/b2c_1_susi/",
 *       "default": true
 *     },
 *     {
 *       "type": "B2C",
 *       "authority_url": "https://fabrikamb2c.b2clogin.com/tfp/fabrikamb2c.onmicrosoft.com/b2c_1_edit_profile/"
 *     },
 *     {
 *       "type": "B2C",
 *       "authority_url": "https://fabrikamb2c.b2clogin.com/tfp/fabrikamb2c.onmicrosoft.com/b2c_1_reset/"
 *     }
 *   ]
 * }
 * <p>
 * This file contains 2 B2C policies, namely "b2c_1_susi", "b2c_1_edit_profile" and "b2c_1_reset"
 * Its azureAdB2CHostName is "fabrikamb2c.b2clogin.com"
 * Its tenantName is "fabrikamb2c.onmicrosoft.com"
 */
public class B2CConfiguration {
    /**
     * Name of the policies/user flows in your B2C tenant.
     * See https://docs.microsoft.com/en-us/azure/active-directory-b2c/active-directory-b2c-reference-policies for more info.
     */
    public final static String[] Policies = {
            "b2c_1_susi",
            "b2c_1_edit_profile",
            "b2c_1_reset"
    };

    /**
     * Name of your B2C tenant hostname.
     */
    final static String azureAdB2CHostName = "fabrikamb2c.b2clogin.com";

    /**
     * Name of your B2C tenant.
     */
    final static String tenantName = "fabrikamb2c.onmicrosoft.com";

    /**
     * Constructs an authority for the given policy name.
     */
    public static String getAuthorityFromPolicyName(final String policyName) {
        return "https://" + azureAdB2CHostName + "/tfp/" + tenantName + "/" + policyName + "/";
    }

    /**
     * An array of scopes you wish to acquire as part of the returned token result.
     * These scopes must be added in your B2C application page.
     */
    public static List<String> getScopes() {
        return Arrays.asList(
                "https://fabrikamb2c.onmicrosoft.com/helloapi/demo.read");
    }
}
