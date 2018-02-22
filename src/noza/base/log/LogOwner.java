package noza.base.log;

public interface LogOwner
{
    Log getLogger();
    String getTimestampStr();
    String getName();

    default void logTrace(Throwable t, Object ...args)
    {
        getLogger().fatal(getTimestampStr(), getName(), null, args);
    }

    default void logTrace(Object ...args)
    {
        getLogger().trace(getTimestampStr(), getName(), null, args);
    }

    default void logDebug(Throwable t, Object ...args)
    {
        getLogger().fatal(getTimestampStr(), getName(), null, args);
    }

    default void logDebug(Object ...args)
    {
        getLogger().debug(getTimestampStr(), getName(), null, args);
    }

    default void logInfo(Object ...args)
    {
        getLogger().info(getTimestampStr(), getName(), null,  args);
    }

    default void logWarn(Object ...args)
    {
        getLogger().warn(getTimestampStr(), getName(), null, args);
    }

    default void logWarn(Throwable t, Object ...args)
    {
        getLogger().error(getTimestampStr(), getName(), t, args);
    }

    default void logError(Object ...args)
    {
        getLogger().error(getTimestampStr(), getName(), null, args);
    }

    default void logError(Throwable t, Object ...args)
    {
        getLogger().error(getTimestampStr(), getName(), t, args);
    }

    default void logFatal(Object ...args)
    {
        getLogger().fatal(getTimestampStr(), getName(), null, args);
    }

    default void logFatal(Throwable t, Object ...args)
    {
        getLogger().fatal(getTimestampStr(), getName(), null, args);
    }



}
