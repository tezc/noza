package noza.api;


import noza.api.msgs.Connect;
import noza.api.msgs.Publish;
import noza.api.msgs.Subscribe;

public interface Callbacks
{
    default void onLog(String owner, String level, String log)
    {

    }

    default void onTerminate(String msg)
    {

    }

    default int onAccept(String localAddress, String remoteAddress)
    {
        return NozaApi.OK;
    }

    default int onConnect(String localAddress, String address, Connect connect)
    {
        return NozaApi.OK;
    }

    default int onSubscribe(String clientId, Subscribe subscribe)
    {
        return NozaApi.OK;
    }

    default int onPublish(String clientId, Publish publish)
    {

        return NozaApi.OK;
    }

    default void onDisconnect(String clientId, String address)
    {

    }
}
