package noza.base.exception;


public class GuiException extends RuntimeException
{
    public GuiException(String log)
    {
        super(log);
    }

    public GuiException(Exception e)
    {
        super(e);
    }
}

