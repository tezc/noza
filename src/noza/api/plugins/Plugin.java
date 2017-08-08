package noza.api.plugins;

import noza.api.Callbacks;
import noza.api.NozaApi;

public abstract class Plugin implements Callbacks
{
    public abstract void start(NozaApi noza);
}
