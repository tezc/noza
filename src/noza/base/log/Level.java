package noza.base.log;

public enum Level
{
    ALL  (100, "[  ALL  ]"),
    DEBUG(200, "[ DEBUG ]"),
    INFO (300, "[ INFO  ]"),
    BRIEF(400, "[ BRIEF ]"),
    WARN (500, "[ WARN  ]"),
    ERROR(600, "[ ERROR ]"),
    OFF  (700, "[  OFF  ]");

    public final int value;
    public final String printable;

    Level(int value, String printable)
    {
        this.value     = value;
        this.printable = printable;
    }
}
