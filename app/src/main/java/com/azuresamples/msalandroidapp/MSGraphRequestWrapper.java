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

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.graph.requests.GraphServiceClient;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MSGraphRequestWrapper {
    private static final String TAG = MSGraphRequestWrapper.class.getSimpleName();

    // See: https://docs.microsoft.com/en-us/graph/deployments#microsoft-graph-and-graph-explorer-service-root-endpoints
    public static final String MS_GRAPH_ROOT_ENDPOINT = "https://graph.microsoft.com/";
    public static final String MS_GRAPH_V1_ENDPOINT = MS_GRAPH_ROOT_ENDPOINT + "v1.0";
    public static final String MS_GRAPH_BETA_ENDPOINT = MS_GRAPH_ROOT_ENDPOINT + "beta";

    /**
     * Use Graph SDK to make an HTTP request with
     * 1) a given MSGraph resource URL
     * 2) an access token
     * to obtain MSGraph data.
     **/
    @RequiresApi(api = Build.VERSION_CODES.N)
    @SuppressWarnings("unchecked")
    public static CompletableFuture<String> callGraphAPI(@NonNull final String graphResourceUrl,
                                                         @NonNull final String accessToken) {
        Log.d(TAG, "Starting SDK request to graph");

        /* Make sure we have a token to send to graph */
        if (accessToken == null || accessToken.length() == 0) {
            return CompletableFuture.completedFuture(null);
        }

        StaticTokenAuthProvider authProvider =
            new StaticTokenAuthProvider(accessToken);

        GraphServiceClient graphClient = GraphServiceClient.builder()
            .authenticationProvider(authProvider)
            .buildClient();

        String relativeUrl = "";

        if (graphResourceUrl.toLowerCase().contains(MS_GRAPH_V1_ENDPOINT)) {
            relativeUrl = graphResourceUrl.substring(MS_GRAPH_V1_ENDPOINT.length());
        } else if (graphResourceUrl.toLowerCase().contains(MS_GRAPH_BETA_ENDPOINT)) {
            relativeUrl = graphResourceUrl.substring(MS_GRAPH_BETA_ENDPOINT.length());
        }

        // The customRequest builder allows you to call any relative URL
        // (like "/me") and get back a generic object
        // For an example that uses the SDK's fluent API to return strongly-typed objects,
        // see https://github.com/microsoftgraph/msgraph-training-android
        // or https://docs.microsoft.com/en-us/graph/tutorials/android
        return graphClient.customRequest(relativeUrl)
            .buildRequest()
            .getAsync()
            .thenApply(response -> response.toString());
    }
}
