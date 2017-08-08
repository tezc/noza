package noza.base.log;

public interface LogOwner
{
    Log getLogger();
    String getTimestampStr();
    String getName();

    default void logDebug(Object ...args)
    {
        getLogger().debug(getTimestampStr(), getName(), null, args);
    }

    default void logInfo(Object ...args)
    {
        getLogger().info(getTimestampStr(), getName(), null,  args);
    }

    default void logBrief(Object ...args)
    {
        getLogger().brief(getTimestampStr(), getName(), null, args);
    }

    default void logWarn(Object ...args)
    {
        getLogger().warn(getTimestampStr(), getName(), null, args);
    }

    default void logError(Object ...args)
    {
        getLogger().error(getTimestampStr(), getName(), null, args);
    }

    default void logError(Throwable t, Object ...args)
    {
        getLogger().error(getTimestampStr(), getName(), t, args);
    }
}
