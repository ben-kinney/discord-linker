package software.bens.linker;

import java.util.UUID;

/**
 * @author Ben Kinney
 */
public class Token
{
    private String token;
    private UUID creator;
    private long creationTime;

    public Token(String token, UUID creator)
    {
        this.token = token;
        this.creator = creator;
        this.creationTime = System.currentTimeMillis();
    }

    public String getToken()
    {
        return token;
    }

    public UUID getCreator()
    {
        return creator;
    }

    public boolean isValid()
    {
        // 5 minute validity time
        return System.currentTimeMillis() - creationTime <= 300000;
    }
}
