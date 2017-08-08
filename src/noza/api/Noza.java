package noza.api;

import noza.core.Core;

public class Noza implements Callbacks, NozaApi
{
    private Core core;

    public Noza(Callbacks callbacks, String configsPath)
    {
        if (callbacks == null) {
            callbacks = this;
        }

        core = new Core(callbacks, configsPath);
    }

    public void setCallbacks(Callbacks callbacks)
    {
        if (callbacks == null) {
            throw new IllegalArgumentException("Callbacks cannot be null");
        }

        core.setCallbacks(callbacks);
    }

    public void start()
    {
        core.start();
    }

    public void stop()
    {
        core.stop();
    }
}
