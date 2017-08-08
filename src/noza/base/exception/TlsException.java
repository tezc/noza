package noza.base.exception;

/**
 * Created by zxt on 18.07.2017.
 */
public class TlsException extends RuntimeException
{
    public TlsException(String log)
    {
        super(log);
    }

    public TlsException(Exception e)
    {
        super(e);
    }
}
