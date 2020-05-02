package software.bens.linker.oauth;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Ben Kinney
 */
public final class OAuth2
{
    private static final String BASE_URL = "https://discordapp.com/api";
    private static final String AUTH_URL = BASE_URL + "/oauth2/authorize";
    private static final String TOKEN_URL = BASE_URL + "/oauth2/token";
    private static final String USER_ME_URL = BASE_URL + "/users/@me";

    private final String     clientId;
    private final String     clientSecret;
    private final String     redirectUri;
    private final Set<Scope> scopes;
    private final String     botToken;

    public OAuth2(OAuth2Builder builder)
    {
        this.clientId     = builder.getClientId();
        this.clientSecret = builder.getClientSecret();
        this.redirectUri  = builder.getRedirectUri();
        this.scopes       = builder.getScopes();
        this.botToken     = builder.getBotToken();
    }

    public String createAuthURL()
    {
        return createAuthURL(null);
    }

    public String createAuthURL(String state)
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", clientId);
        parameters.put("redirect_uri", redirectUri);
        parameters.put("response_type", "code");
        parameters.put("scope", getScopesAsString());
        if(state != null)
            parameters.put("state", state);
        return AUTH_URL + "?" + urlEncodeMap(parameters);
    }

    public String getAccessToken(String code) throws IOException
    {
        HttpURLConnection connection = new HttpRequest(TOKEN_URL)
                .withHeader("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:70.0) Gecko/20100101 Firefox/70.0")
                .withFormContent("client_id", clientId)
                .withFormContent("client_secret", clientSecret)
                .withFormContent("redirect_uri", redirectUri)
                .withFormContent("scope", getScopesAsString())
                .withFormContent("grant_type", "authorization_code")
                .withFormContent("code", code)
                .request("POST");
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder builder = new StringBuilder();
        while((line = reader.readLine()) != null)
            builder.append(line).append("\n");
        connection.disconnect();
        JSONObject json = new JSONObject(builder.toString());
        return json.getString("access_token");
    }

    public String getRedirectUri()
    {
        return redirectUri;
    }

    public JSONObject getUser(String accessToken) throws IOException
    {
        HttpURLConnection connection = new HttpRequest(USER_ME_URL)
                .withHeader("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:70.0) Gecko/20100101 Firefox/70.0")
                .withHeader("Authorization", "Bearer " + accessToken)
                .request("GET");
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder builder = new StringBuilder();
        while((line = reader.readLine()) != null)
            builder.append(line).append("\n");
        connection.disconnect();
        return new JSONObject(builder.toString());
    }

    public void join(String accessToken, String serverId, UserJoinSettings userJoinSettings) throws IOException
    {
        JSONObject json = new JSONObject();
        json.put("access_token", accessToken);
        if(userJoinSettings.getNickname() != null)
            json.put("nick", userJoinSettings.getNickname());
        if(userJoinSettings.getRoles() != null)
            json.put("roles", userJoinSettings.getRoles());
        if(userJoinSettings.isDeaf() != null)
            json.put("deaf", userJoinSettings.isDeaf());
        if(userJoinSettings.isMute() != null)
            json.put("mute", userJoinSettings.isMute());
        HttpURLConnection connection = new HttpRequest(BASE_URL + "/guilds/" + serverId + "/members/" + userJoinSettings.getId())
                .withHeader("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:70.0) Gecko/20100101 Firefox/70.0")
                .withHeader("Authorization", "Bot " + botToken)
                .withJSONContent(json)
                .request("PUT");
        connection.getResponseCode();
        connection.disconnect();
    }

    public String getScopesAsString()
    {
        StringBuilder builder = new StringBuilder();
        for(Scope scope : scopes)
        {
            if(builder.length() != 0)
                builder.append(" ");
            builder.append(scope.getId());
        }
        return builder.toString();
    }

    private static String urlEncodeMap(Map<String, String> map)
    {
        try
        {
            StringBuilder builder = new StringBuilder();
            for(Map.Entry<String, String> entry : map.entrySet())
            {
                if(builder.length() != 0)
                    builder.append('&');
                builder.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append('=').append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            return builder.toString();
        }
        catch(UnsupportedEncodingException exception)
        {
            return null;
        }
    }
}
