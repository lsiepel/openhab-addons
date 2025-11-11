package org.openhab.binding.amazonechocontrol.internal.connection;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class AmazonLoginClient {
    private static final String AMAZON_DEVICE_TYPE = "A3NWHXTQ4EBCZS";
    private final String email;
    private final String password;
    private final String domain;
    private final HttpClient httpClient;

    public record LoginResult(String accessToken, String refreshToken, String deviceSerial, String devicePrivateKey,
            String adpToken, long expiresEpochSeconds) {
    }

    public AmazonLoginClient(String email, String password, String domain, HttpClient httpClient) {
        this.email = email;
        this.password = password;
        this.domain = domain; // e.g. "com", "de", "co.uk", etc
        this.httpClient = httpClient; // http follow redirect always
    }

    public static void writeStringToFile(String content, String filePath) throws IOException {
        Files.writeString(Path.of(filePath), content);
    }

    public LoginResult login(String otp) throws Exception {
        String deviceSerial = generateSerial();
        byte[] codeVerifier = createCodeVerifier();
        String clientId = buildClientId(deviceSerial);
        String oauthUrl = buildOAuthUrl(codeVerifier, clientId);

        // STEP 1: Load login form
        Document loginPage = httpGetHtml(oauthUrl);

        // STEP 2: Submit email + password
        Map<String, String> loginFields = extractHiddenFields(loginPage);
        loginFields.put("email", email);
        loginFields.put("password", password);
        Document mfaPage = submitForm(loginPage, loginFields, "amazonechocontrol-mfa-response.html");

        // STEP 3: Submit OTP
        Map<String, String> mfaFields = extractHiddenFields(mfaPage);
        mfaFields.put("otpCode", otp);
        Document redirectPage = submitForm(mfaPage, mfaFields, "amazonechocontrol-redirect-response.html");

        // STEP 4: Extract authorization_code from redirect URL
        String authorizationCode = extractAuthCodeFromRedirect(redirectPage);

        // STEP 5: Register device → get tokens
        return registerDevice(authorizationCode, codeVerifier, deviceSerial);
    }

    /* ---- Methods filled in next step ---- */

    private Document httpGetHtml(String url) throws Exception {
        ContentResponse response = httpClient.newRequest(URI.create(url)).method(HttpMethod.GET).send();
        var content = response.getContentAsString();
        System.out.println("DEBUG GET response: " + content);
        writeStringToFile(content, "amazonechocontrol-initial-response.html");
        return Jsoup.parse(response.getContentAsString());
    }

    private Document submitForm(Document page, Map<String, String> fields, String filename) throws Exception {
        Element form = page.select("form").first();
        if (form == null) {
            throw new IllegalStateException("No form found on page for submit.");
        }
        String action = form.attr("action");
        String url = action.startsWith("http") ? action : "https://www.amazon." + domain + action;

        StringBuilder body = new StringBuilder();
        for (var entry : fields.entrySet()) {
            if (body.length() > 0)
                body.append("&");
            body.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)).append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        ContentResponse response = httpClient.newRequest(URI.create(url))
                .content(new StringContentProvider(body.toString(), StandardCharsets.UTF_8)).method(HttpMethod.POST)
                .header("Content-Type", "application/x-www-form-urlencoded").send();
        var content = response.getContentAsString();

        writeStringToFile(content, filename);
        System.out.println("DEBUG register response: " + content);
        return Jsoup.parse(content);
    }

    private Map<String, String> extractHiddenFields(Document doc) {
        Map<String, String> data = new HashMap<>();
        doc.select("input[type=hidden]").forEach(e -> data.put(e.attr("name"), e.attr("value")));
        return data;
    }

    private String extractAuthCodeFromRedirect(Document redirectPage) throws Exception {
        // The final redirected URL is stored as the baseUri of the parsed Document
        String finalUrl = redirectPage.baseUri();
        URI uri = new URI(finalUrl);

        // Parse query parameters
        String query = uri.getRawQuery();
        if (query == null) {
            throw new IllegalStateException("No query parameters found in redirect URL");
        }

        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals("openid.oa2.authorization_code")) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }

        throw new IllegalStateException("Authorization code not found in redirect URL: " + finalUrl);
    }

    private LoginResult registerDevice(String authorizationCode, byte[] codeVerifier, String deviceSerial)
            throws Exception {

        URI uri = URI.create("https://api.amazon.com/auth/register");
        String json = """
                {
                  "requested_extensions":["device_info","customer_info"],
                  "cookies":{"website_cookies":[],"domain":".amazon.%s"},
                  "registration_data":{
                    "domain":"Device",
                    "app_version":"2.2.400523.0",
                    "device_type":"A3NWHXTQ4EBCZS",
                    "device_name":"JavaLoginClient",
                    "os_version":"14.2",
                    "device_serial":"%s",
                    "device_model":"Java",
                    "app_name":"Amazon Alexa",
                    "software_version":"1"
                  },
                  "auth_data":{
                    "use_global_authentication":"true",
                    "client_id":"%s",
                    "authorization_code":"%s",
                    "code_verifier":"%s",
                    "code_algorithm":"SHA-256",
                    "client_domain":"DeviceLegacy"
                  },
                  "requested_token_type":["bearer","mac_dms","store_authentication_cookie"]
                }
                """.formatted(domain, deviceSerial, buildClientId(deviceSerial), authorizationCode,
                Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier));

        ContentResponse response = httpClient.newRequest(URI.create(uri.toString()))
                .content(new StringContentProvider(json, StandardCharsets.UTF_8)).method(HttpMethod.POST)
                .header("Content-Type", "application/json").send();

        // You will parse JSON here → ask and I add JSON parsing.
        System.out.println("DEBUG register response: " + response.getContentAsString());

        // Placeholder return until we parse JSON:
        return new LoginResult("<accessToken>", "<refreshToken>", deviceSerial, "<privateKey>", "<adpToken>",
                Instant.now().getEpochSecond() + 3600);
    }

    /* Helpers */
    private String generateSerial() {
        return java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private byte[] createCodeVerifier() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encode(bytes);
    }

    // private String createCodeVerifier() {
    /// byte[] bytes = new byte[32];
    // new SecureRandom().nextBytes(bytes);
    // return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    // }

    // private String buildClientId(String serial) {
    // return (serial + "#A3NWHXTQ4EBCZS").getBytes(StandardCharsets.UTF_8).toString();
    // }

    private String buildClientId(String serial) {
        byte[] clientBytes = (serial + "#" + AMAZON_DEVICE_TYPE).getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        for (byte b : clientBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String base64UrlNoPadding(byte[] input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
    }

    private static String createS256CodeChallenge(byte[] codeVerifier) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier);
        return base64UrlNoPadding(hash);
    }

    private String buildOAuthUrl(byte[] codeVerifier, String clientId) throws Exception {
        String codeChallenge = createS256CodeChallenge(codeVerifier);
        String language = "nl_NL"; // or en_US, de_DE, etc.
        Map<String, String> oauthParams = new LinkedHashMap<>();
        oauthParams.put("openid.return_to", "https://www.amazon.com/ap/maplanding");
        oauthParams.put("openid.oa2.code_challenge_method", "S256");
        oauthParams.put("openid.assoc_handle", "amzn_dp_project_dee_ios");
        oauthParams.put("openid.identity", "http://specs.openid.net/auth/2.0/identifier_select");
        oauthParams.put("pageId", "amzn_dp_project_dee_ios");
        oauthParams.put("accountStatusPolicy", "P1");
        oauthParams.put("openid.claimed_id", "http://specs.openid.net/auth/2.0/identifier_select");
        oauthParams.put("openid.mode", "checkid_setup");
        oauthParams.put("openid.ns.oa2", "http://www.amazon.com/ap/ext/oauth/2");
        oauthParams.put("openid.oa2.client_id", "device:" + clientId);
        oauthParams.put("language", language);
        oauthParams.put("openid.ns.pape", "http://specs.openid.net/extensions/pape/1.0");
        oauthParams.put("openid.oa2.code_challenge", codeChallenge);
        oauthParams.put("openid.oa2.scope", "device_auth_access");
        oauthParams.put("openid.ns", "http://specs.openid.net/auth/2.0");
        oauthParams.put("openid.pape.max_auth_age", "0");
        oauthParams.put("openid.oa2.response_type", "code");

        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : oauthParams.entrySet()) {
            if (query.length() > 0)
                query.append('&');
            query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            query.append('=');
            query.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        return "https://www.amazon.com/ap/signin?" + query;
    }
}
