package noza.base.exception;

/**
 * Created by zxt on 16.07.2017.
 */
public class WebSocketException extends RuntimeException
{
    public WebSocketException(String log)
    {
        super(log);
    }

    public WebSocketException(Exception e)
    {
        super(e);
    }
}
