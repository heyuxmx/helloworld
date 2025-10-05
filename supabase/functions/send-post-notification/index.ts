
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import * as jose from "jose";

// --- CONFIGURATION ---
// These environment variables MUST be set in your function's secrets
const FIREBASE_SERVICE_ACCOUNT_JSON_STRING = Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON");
const PROJECT_SUPABASE_URL = Deno.env.get("PROJECT_SUPABASE_URL");
const PROJECT_SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("PROJECT_SUPABASE_SERVICE_ROLE_KEY");

const GOOGLE_AUTH_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token";

// --- Google Auth Token Provider ---
// This object will handle getting and refreshing the OAuth2 token
const googleAuth = {
  accessToken: null,
  tokenExpiresAt: null,

  async getAccessToken() {
    // If we have a valid token, return it
    if (this.accessToken && this.tokenExpiresAt && new Date() < this.tokenExpiresAt) {
      return this.accessToken;
    }

    console.log("No valid access token found. Requesting a new one from Google.");

    if (!FIREBASE_SERVICE_ACCOUNT_JSON_STRING) {
      throw new Error("CRITICAL: FIREBASE_SERVICE_ACCOUNT_JSON secret is not set.");
    }
    const serviceAccount = JSON.parse(FIREBASE_SERVICE_ACCOUNT_JSON_STRING);

    // Create a JWT to sign our request for an access token
    const privateKey = await jose.importPkcs8(serviceAccount.private_key, "RS256");
    const jwt = await new jose.SignJWT({})
      .setProtectedHeader({ alg: "RS256", typ: "JWT" })
      .setIssuer(serviceAccount.client_email)
      .setSubject(serviceAccount.client_email)
      .setAudience(GOOGLE_TOKEN_URI)
      .setIssuedAt()
      .setExpirationTime("1h")
      .sign(privateKey);

    // Exchange the JWT for a Google API access token
    const response = await fetch(GOOGLE_TOKEN_URI, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
        assertion: jwt,
      }),
    });

    const tokens = await response.json();
    if (!response.ok) {
      throw new Error(`Failed to get access token: ${JSON.stringify(tokens)}`);
    }

    this.accessToken = tokens.access_token;
    // Set expiry to 55 minutes to be safe (token is valid for 60)
    this.tokenExpiresAt = new Date(new Date().getTime() + 55 * 60 * 1000); 

    console.log("Successfully obtained new Google API access token.");
    return this.accessToken;
  },
};

// --- MAIN SERVER LOGIC ---
serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method Not Allowed", { status: 405 });
  }

  try {
    // Basic validation
    if (!PROJECT_SUPABASE_URL || !PROJECT_SUPABASE_SERVICE_ROLE_KEY) {
      throw new Error("Supabase URL or Service Role Key is not configured in Function Secrets.");
    }
    const serviceAccount = JSON.parse(FIREBASE_SERVICE_ACCOUNT_JSON_STRING);
    const firebaseProjectId = serviceAccount.project_id;
    if (!firebaseProjectId) {
      throw new Error("project_id not found in Firebase service account JSON.");
    }
    
    // Get the new post data from the request body (sent by the database webhook)
    const { record: post } = await req.json();
    if (!post || !post.content) {
      console.warn("Received webhook with no post content. Aborting.");
      return new Response("Missing post content", { status: 400 });
    }

    // --- 1. Fetch all FCM tokens from Supabase ---
    const supabaseAdmin = createClient(PROJECT_SUPABASE_URL, PROJECT_SUPABASE_SERVICE_ROLE_KEY);
    const { data: tokens, error: tokenError } = await supabaseAdmin.from("fcm_tokens").select("token");

    if (tokenError) throw tokenError;
    if (!tokens || tokens.length === 0) {
      console.log("No FCM tokens found. Nothing to do.");
      return new Response("No tokens to send to.", { status: 200 });
    }
    
    // --- 2. Get a valid Google API Access Token ---
    const accessToken = await googleAuth.getAccessToken();

    // --- 3. Construct and Send the FCM Notification ---
    // Note: This uses the modern FCM HTTP v1 API
    const fcmApiUrl = `https://fcm.googleapis.com/v1/projects/${firebaseProjectId}/messages:send`;
    
    // We send one message per token. For batching, a different structure is needed.
    // This approach is simple and robust for a moderate number of users.
    let successCount = 0;
    let failureCount = 0;
    
    for (const userToken of tokens) {
      const fcmMessagePayload = {
        message: {
          token: userToken.token,
          notification: {
            title: "有新动态！",
            body: post.content.substring(0, 200),
          },
        },
      };

      try {
        const fcmResponse = await fetch(fcmApiUrl, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${accessToken}`,
          },
          body: JSON.stringify(fcmMessagePayload),
        });

        if (fcmResponse.ok) {
          successCount++;
        } else {
          failureCount++;
          const errorBody = await fcmResponse.json();
          // Log specific errors for debugging, e.g., if a token is invalid
          console.warn(`Failed to send to token ${userToken.token.substring(0,10)}...:`, JSON.stringify(errorBody));
        }
      } catch (e) {
         failureCount++;
         console.error(`Network or other error sending to a token:`, e);
      }
    }

    const resultMessage = `Sent notifications. Success: ${successCount}, Failures: ${failureCount}.`;
    console.log(resultMessage);
    
    return new Response(JSON.stringify({ success: true, message: resultMessage }), {
      headers: { "Content-Type": "application/json" },
      status: 200,
    });

  } catch (error) {
    console.error("--- UNHANDLED ERROR IN FUNCTION ---:", error);
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { "Content-Type": "application/json" },
      status: 500,
    });
  }
});
