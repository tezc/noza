package noza.api.msgs;

public interface Connect
{
    byte getConnectFlags();
    byte getProtoLevel();
    short getKeepAlive();
    boolean isCleanSession();
    boolean isUsernamePresent();
    boolean isPasswordPresent();
    boolean isWillPresent();
    String getClientId();
    String getUsername();
    String getPassword();
    Publish getWillMessage();
}
