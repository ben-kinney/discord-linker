package software.bens.linker.oauth;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Ben Kinney
 */
public final class OAuth2Builder
{
    private String     clientId;
    private String     clientSecret;
    private String     redirectUri;
    private Set<Scope> scopes;
    private String     botToken;

    public OAuth2 build()
    {
        return new OAuth2(this);
    }

    public String getBotToken()
    {
        return botToken;
    }

    public String getClientId()
    {
        return clientId;
    }

    public String getClientSecret()
    {
        return clientSecret;
    }

    public String getRedirectUri()
    {
        return redirectUri;
    }

    public Set<Scope> getScopes()
    {
        return new HashSet<>(scopes);
    }

    public OAuth2Builder setBotToken(String token)
    {
        this.botToken = token;
        return this;
    }

    public OAuth2Builder setClientId(String clientId)
    {
        this.clientId = clientId;
        return this;
    }

    public OAuth2Builder setClientSecret(String clientSecret)
    {
        this.clientSecret = clientSecret;
        return this;
    }

    public OAuth2Builder setRedirectUri(String redirectUri)
    {
        this.redirectUri = redirectUri;
        return this;
    }

    public OAuth2Builder setScopes(Scope... scopes)
    {
        this.scopes = new HashSet<>();
        Collections.addAll(this.scopes, scopes);
        return this;
    }
}
