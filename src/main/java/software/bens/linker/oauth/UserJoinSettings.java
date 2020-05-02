package software.bens.linker.oauth;

/**
 * @author Ben Kinney
 */
public final class UserJoinSettings
{
    private final String id;

    private String nickname;
    private long[] roles;
    private Boolean mute;
    private Boolean deaf;

    public UserJoinSettings(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    public String getNickname()
    {
        return nickname;
    }

    public long[] getRoles()
    {
        return roles;
    }

    public Boolean isDeaf()
    {
        return deaf;
    }

    public Boolean isMute()
    {
        return mute;
    }

    public UserJoinSettings setDeaf(boolean deaf)
    {
        this.deaf = deaf;
        return this;
    }

    public UserJoinSettings setMute(boolean mute)
    {
        this.mute = mute;
        return this;
    }

    public UserJoinSettings setNickname(String nickname)
    {
        this.nickname = nickname;
        return this;
    }

    public UserJoinSettings setRoles(long[] roles)
    {
        this.roles = roles;
        return this;
    }
}
