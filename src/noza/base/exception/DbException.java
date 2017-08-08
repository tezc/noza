package noza.base.exception;


public class DbException extends RuntimeException
{
    public DbException(Exception e)
    {
        super(e);
    }
}
