package com.github.thomasdarimont.keycloak.faceauth;

import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.UsernameForm;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.RedirectionException;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@JBossLog
public class FaceAuthenticatorForm implements Authenticator {

    static final String ID = "demo-faceauth";
    private static final String API_ENDPOINT = "http://localhost:1717/captureData";
    private static final int CONNECT_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 10000; // 10 seconds
    private final KeycloakSession session;

    public FaceAuthenticatorForm(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        Response response = context.form()
                .createForm("faceauth-form.ftl");
        context.challenge(response);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formParameters = context.getHttpRequest().getDecodedFormParameters();
        String biometricImage = formParameters.getFirst("faceImage");
        HttpURLConnection connection = null;
        try {
            // Setup connection
            URL url = new URL(API_ENDPOINT);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            // Setup request headers
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Write request body
            try (OutputStream outputStream = connection.getOutputStream()) {
                String jsonRequest = "{\"type\":\"slap_left\"}";
                outputStream.write(jsonRequest.getBytes("UTF-8"));
            }

            // Handle response
            System.out.println("hello");

            int responseCode = connection.getResponseCode();
            System.out.println(responseCode);
            String response = readResponse(connection);
            log.info("Response Body: " + response);
            context.success();
            return;

        } catch (Exception e) {
            log.error("Error during fingerprint capture", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    private String readResponse(HttpURLConnection connection) throws IOException {
        InputStream inputStream;
        if (connection.getResponseCode() >= 200 && connection.getResponseCode() <= 299) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
        // NOOP
    }
}
