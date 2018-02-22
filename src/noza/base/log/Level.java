package noza.base.log;

public enum Level
{
    ALL  (100, "[  ALL  ]"),
    TRACE(200, "[ TRACE ]"),
    DEBUG(300, "[ DEBUG ]"),
    INFO (400, "[ INFO  ]"),
    BRIEF(500, "[ BRIEF ]"),
    WARN (600, "[ WARN  ]"),
    ERROR(700, "[ ERROR ]"),
    FATAL(800, "[ FATAL ]"),
    OFF  (900, "[  OFF  ]");

    public final int value;
    public final String printable;

    Level(int value, String printable)
    {
        this.value     = value;
        this.printable = printable;
    }
}
