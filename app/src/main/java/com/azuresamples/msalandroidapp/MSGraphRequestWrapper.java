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
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MSGraphRequestWrapper {
    private static final String TAG = MSGraphRequestWrapper.class.getSimpleName();

    // See: https://docs.microsoft.com/en-us/graph/deployments#microsoft-graph-and-graph-explorer-service-root-endpoints
    public static final String MS_GRAPH_ROOT_ENDPOINT = "https://graph.microsoft.com/";

    /**
     * Use Volley to make an HTTP request with
     * 1) a given MSGraph resource URL
     * 2) an access token
     * to obtain MSGraph data.
     **/
    public static void callGraphAPIUsingVolley(@NonNull final Context context,
                                               @NonNull final String graphResourceUrl,
                                               @NonNull final String accessToken,
                                               @NonNull final Response.Listener<JSONObject> responseListener,
                                               @NonNull final Response.ErrorListener errorListener) {
        Log.d(TAG, "Starting volley request to graph");

        /* Make sure we have a token to send to graph */
        if (accessToken == null || accessToken.length() == 0) {
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(context);
        JSONObject parameters = new JSONObject();

        try {
            parameters.put("key", "value");
        } catch (Exception e) {
            Log.d(TAG, "Failed to put parameters: " + e.toString());
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, graphResourceUrl,
                parameters, responseListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }
        };

        Log.d(TAG, "Adding HTTP GET to Queue, Request: " + request.toString());

        request.setRetryPolicy(new DefaultRetryPolicy(
                3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }
}
