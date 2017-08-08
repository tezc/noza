package noza.base.poller;


public interface Fd
{
    default void onAccept()
    {
        throw new UnsupportedOperationException();
    }

    default void onRead()
    {
        throw new UnsupportedOperationException();
    }

    default void onWrite()
    {
        throw new UnsupportedOperationException();
    }
}
