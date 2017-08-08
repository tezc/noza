package noza.api.beans;

public class ClientBean
{
    private String clientId;
    private String username;
    private int state;
    private boolean cleanSession;
    private String willMsg;
    private String localAddress;
    private String remoteAddress;
    private String firstTimestamp;
    private String latestTimestamp;

    public ClientBean(String clientId, String username,
                      int state, boolean cleanSession,
                      String willMsg, String localAddress, String remoteAddress,
                      String firstTimestamp, String latestTimestamp)
    {
        this.clientId        = clientId;
        this.username        = username;
        this.state           = state;
        this.cleanSession    = cleanSession;
        this.willMsg         = willMsg;
        this.localAddress    = localAddress;
        this.remoteAddress   = remoteAddress;
        this.firstTimestamp  = firstTimestamp;
        this.latestTimestamp = latestTimestamp;
    }

    public String getClientId()
    {
        return clientId;
    }

    public void setClientId(String clientId)
    {
        this.clientId = clientId;
    }

    public String getUsername()
    {
        return clientId;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public int getState()
    {
        return state;
    }

    public void setState(int state)
    {
        this.state = state;
    }

    public boolean isCleanSession()
    {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession)
    {
        this.cleanSession = cleanSession;
    }

    public String getWillMsg()
    {
        return willMsg;
    }

    public void setWillMsg(String willMsg)
    {
        this.willMsg = willMsg;
    }

    public String getLocalAddress()
    {
        return localAddress;
    }

    public void setLocalAddress(String localAddress)
    {
        this.localAddress = localAddress;
    }

    public String getRemoteAddress()
    {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress)
    {
        this.remoteAddress = remoteAddress;
    }

    public String getFirstTimestamp()
    {
        return firstTimestamp;
    }

    public void setFirstTimestamp(String firstTimestamp)
    {
        this.firstTimestamp = firstTimestamp;
    }

    public String getLatestTimestamp()
    {
        return latestTimestamp;
    }

    public void setLatestTimestamp(String latestTimestamp)
    {
        this.latestTimestamp = latestTimestamp;
    }

}
