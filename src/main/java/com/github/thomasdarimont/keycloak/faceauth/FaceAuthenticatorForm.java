package com.github.thomasdarimont.keycloak.faceauth;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import javax.imageio.ImageIO;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@JBossLog
public class FaceAuthenticatorForm implements Authenticator {

    private static class BiometricVerificationResult {
        private final boolean success;
        private final String message;

        public BiometricVerificationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }



    static final String ID = "demo-faceauth";
    private static final String API_ENDPOINT = "http://localhost:1717/captureData";
    private static final String API_ENDPOINT_FINGER = "http://119.148.4.20:5684/get-fp-segmentation";
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

            int responseCode = connection.getResponseCode();
            String response = readResponse(connection);

            String path = context.getUser().getFirstAttribute("faceimage");
            System.out.println(path);
            File tempImageFile = convertImageFileToTemp(path);

            BiometricVerificationResult result = sendFingerprintSegmentationRequest(tempImageFile,"left_slab");
            System.out.println(result);
            // Parse JSON response if needed
            ObjectMapper mapper = new ObjectMapper();
            Map responseMap = mapper.readValue(response, Map.class);

            // Create form with response data
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("captureResponse", response);  // Full JSON response
            attributes.put("responseCode", responseCode); // Response code
            // Add any specific fields from response
            if (responseMap.containsKey("imageBase64")) {
                attributes.put("imageData", responseMap.get("imageBase64"));
            }
            System.out.println(responseMap.get("imageBase64"));


            // Send to FTL template
            Response challengeResponse = context.form()
                    .setAttribute("attributes", attributes) // Add all attributes
                    .createForm("faceauth-form.ftl");   // Your template name

            context.challenge(challengeResponse);
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
        // NOOP0
    }
    public BiometricVerificationResult sendFingerprintSegmentationRequest(File fingerprintImage, String imageType) throws IOException {
        HttpURLConnection connection = null;
        System.out.println("Finger print verification");
        try {
            // Setup connection
            URL url = new URL(API_ENDPOINT_FINGER);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            // Setup multipart request
            String boundary = "Boundary- " + System.currentTimeMillis();
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setDoOutput(true);


            // Write multipart form data
            try (OutputStream outputStream = connection.getOutputStream()) {
                writeMultipartData(outputStream, fingerprintImage, imageType, boundary);
            }

            // Handle response
            int responseCode = connection.getResponseCode();
            String response = readResponse(connection);
            log.info("Response Body: " + response);

            return new BiometricVerificationResult(
                    responseCode == HttpURLConnection.HTTP_OK,
                    response
            );

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void writeMultipartData(OutputStream outputStream, File fingerprintImage, String imageType, String boundary)
            throws IOException {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true)) {
            // Write image_type parameter
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"image_type\"").append("\r\n\r\n");
            writer.append(imageType).append("\r\n");

            // Write fingerprint image
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"fingerprint_image\"; filename=\"")
                    .append(fingerprintImage.getName()).append("\"\r\n");
            writer.append("Content-Type: application/octet-stream\r\n\r\n");
            writer.flush();

            // Write file content
            try (FileInputStream inputStream = new FileInputStream(fingerprintImage)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
            writer.append("\r\n");

            // End of multipart/form-data
            writer.append("--").append(boundary).append("--\r\n");
        }
    }

    public static File convertImageFileToTemp(String imagePath) {
        try {
            // Load the image from the file path
            File file = new File(imagePath);
            BufferedImage image = ImageIO.read(file);



            // Check if the image was successfully loaded
            if (image != null) {
                System.out.println("Image loaded successfully!");

                // Convert BufferedImage to byte array
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(image, "png", baos);
                    baos.flush();
                    byte[] imageBytes = baos.toByteArray();

                    // Create a temporary file
                    File tempFile = File.createTempFile("biometric_", ".png");
                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                        fos.write(imageBytes);
                    }

                    System.out.println("Image saved to temporary file: " + tempFile.getAbsolutePath());
                    return tempFile;

                } catch (IOException e) {
                    System.out.println("Error during image conversion: " + e.getMessage());
                }
            } else {
                System.out.println("Failed to load image!");
            }
        } catch (IOException e) {
            System.out.println("Error reading the image file: " + e.getMessage());
        }
        return null;
    }


}
