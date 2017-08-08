package noza.base.exception;


public class HttpException extends RuntimeException
{
    public HttpException(String log)
    {
        super(log);
    }

    public HttpException(Exception e)
    {
        super(e);
    }
}
