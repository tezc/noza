package noza.api;

public interface NozaApi
{
    int OK    = 0;
    int ERROR = -1;
    int YIELD = -2;
    int CLIENT_ID_REJECTED = -3;

    default void addCallbacks(Callbacks callbacks)
    {

    }



}
