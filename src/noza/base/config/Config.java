package noza.base.config;


import java.util.*;

import static noza.base.config.Config.ValueType.*;
import static noza.base.config.ConfigGroup.*;

enum ConfigGroup
{
    UNIQUE,
    NO_SUBLIST,
    TRANSPORT,
    TOPIC,
    PATTERN,
    INTERNAL
}

public enum Config
{
    BROKER_WORKER_COUNT              (UNIQUE,   NO_SUBLIST,  "broker.worker.count",              INTEGER,   8                                    ),
    BROKER_ALLOW_ANONYMOUS           (UNIQUE,   NO_SUBLIST,  "broker.allow.anonymous",           BOOLEAN,   true                                 ),
    BROKER_AUDIT_INTERVAL            (UNIQUE,   NO_SUBLIST,  "broker.audit.interval",            INTEGER,   60                                   ),
    BROKER_CLIENT_EXPIRE_DURATION    (UNIQUE,   NO_SUBLIST,  "broker.client.expire.duration",    INTEGER,   Integer.MAX_VALUE                    ),
    BROKER_USERNAME_AS_CLIENTID      (UNIQUE,   NO_SUBLIST,  "broker.username.as.clientid",      BOOLEAN,   false                                ),
    BROKER_LOG_CALLBACK              (UNIQUE,   NO_SUBLIST,  "broker.log.callback",              BOOLEAN,   false                                ),
    BROKER_LOG_LEVEL                 (UNIQUE,   NO_SUBLIST,  "broker.log.level",                 STRING,    "info"                               ),
    BROKER_LOG_FILEPATH              (UNIQUE,   NO_SUBLIST,  "broker.log.filepath",              STRING,    "/data/log/"                         ),
    BROKER_LOG_FILESIZE              (UNIQUE,   NO_SUBLIST,  "broker.log.filesize",              INTEGER,   50000000                             ),
    BROKER_LOG_FILECOUNT             (UNIQUE,   NO_SUBLIST,  "broker.log.filecount",             INTEGER,   5                                    ),
    BROKER_LOG_BUFFERSIZE            (UNIQUE,   NO_SUBLIST,  "broker.log.buffersize",            INTEGER,   8192 * 4                             ),
    BROKER_DB_USERNAME               (UNIQUE,   NO_SUBLIST,  "broker.db.username",               STRING,    "noza"                               ),
    BROKER_DB_PASSWORD               (UNIQUE,   NO_SUBLIST,  "broker.db.password",               STRING,    "noza"                               ),
    BROKER_DB_PERSISTED              (UNIQUE,   NO_SUBLIST,  "broker.db.persisted",              BOOLEAN,   true                                 ),
    BROKER_LARGE_BUFSIZE             (INTERNAL, NO_SUBLIST,  "broker.large.bufsize",             INTEGER,   16921                                ),
    BROKER_GUI_ON                    (UNIQUE,   NO_SUBLIST,  "broker.gui.on",                    BOOLEAN,   true                                 ),
    BROKER_GUI_SECURE                (UNIQUE,   NO_SUBLIST,  "broker.gui.secure",                BOOLEAN,   false                                ),
    BROKER_GUI_HOSTNAME              (UNIQUE,   NO_SUBLIST,  "broker.gui.hostname",              STRING,    "127.0.0.1"                          ),
    BROKER_GUI_PORT                  (UNIQUE,   NO_SUBLIST,  "broker.gui.port",                  INTEGER,   8080                                 ),
    BROKER_GUI_KEYSTORE              (UNIQUE,   NO_SUBLIST,  "broker.gui.keystore",              STRING,    null                                 ),
    BROKER_GUI_KEYSTORE_PASSWORD     (UNIQUE,   NO_SUBLIST,  "broker.gui.keystore.password",     STRING,    null                                 ),
    BROKER_GUI_KEYSTORE_KEY_PASSWORD (UNIQUE,   NO_SUBLIST,  "broker.gui.keystore.key.password", STRING,    null                                 ),
    BROKER_GUI_TRUSTSTORE            (UNIQUE,   NO_SUBLIST,  "broker.gui.truststore",            STRING,    null                                 ),
    BROKER_GUI_TRUSTSTORE_PASSWORD   (UNIQUE,   NO_SUBLIST,  "broker.gui.truststore.password",   STRING,    null                                 ),
    BROKER_GUI_USERNAME              (UNIQUE,   NO_SUBLIST,  "broker.gui.username",              STRING,    null                                 ),
    BROKER_GUI_PASSWORD              (UNIQUE,   NO_SUBLIST,  "broker.gui.password",              STRING,    null                                 ),

    TRANSPORT_LIST                   (UNIQUE,    TRANSPORT,  "transport",                        LIST,      new ArrayList<Map<Config, Object>>() ),
    TRANSPORT_PROTOCOL               (TRANSPORT, NO_SUBLIST, "transport.protocol",               STRING,    null                                 ),
    TRANSPORT_HOSTNAME               (TRANSPORT, NO_SUBLIST, "transport.hostname",               STRING,    null                                 ),
    TRANSPORT_URI                    (TRANSPORT, NO_SUBLIST, "transport.uri",                    STRING,    null                                 ),
    TRANSPORT_PORT                   (TRANSPORT, NO_SUBLIST, "transport.port",                   INTEGER,   null                                 ),
    TRANSPORT_KEYSTORE               (TRANSPORT, NO_SUBLIST, "transport.keystore",               STRING,    null                                 ),
    TRANSPORT_KEYSTORE_PASSWORD      (TRANSPORT, NO_SUBLIST, "transport.keystore.password",      STRING,    null                                 ),
    TRANSPORT_KEYSTORE_KEY_PASSWORD  (TRANSPORT, NO_SUBLIST, "transport.keystore.key.password",  STRING,    null                                 ),
    TRANSPORT_TRUSTSTORE             (TRANSPORT, NO_SUBLIST, "transport.truststore",             STRING,    null                                 ),
    TRANSPORT_TRUSTSTORE_PASSWORD    (TRANSPORT, NO_SUBLIST, "transport.truststore.password",    STRING,    null                                 ),

    SUBSCRIPTION_PATTERN_LIST        (UNIQUE,    PATTERN,    "subscription.pattern",             LIST,      new ArrayList<Map<Config, Object>>() ),
    SUBSCRIPTION_PATTERN_USERNAME    (PATTERN,   NO_SUBLIST, "subscription.pattern.username",    STRING,    "#"                                  ),
    SUBSCRIPTION_PATTERN_CLIENTID    (PATTERN,   NO_SUBLIST, "subscription.pattern.clientid",    STRING,    "#"                                  ),
    SUBSCRIPTION_PATTERN_TOPIC       (PATTERN,   NO_SUBLIST, "subscription.pattern.topic",       STRING,    null                                 ),
    SUBSCRIPTION_PATTERN_ACCESS      (PATTERN,   NO_SUBLIST, "subscription.pattern.access",      STRING,    "pubsub"                             ),

    MQTT_STORE_QOS0                  (UNIQUE,    NO_SUBLIST, "mqtt.store.qos0",                  BOOLEAN,   false                                ),
    MQTT_STORE_RETAIN                (UNIQUE,    NO_SUBLIST, "mqtt.store.retain",                BOOLEAN,   false                                ),
    MQTT_MAX_INFLIGHT                (UNIQUE,    NO_SUBLIST, "mqtt.max.inflight",                INTEGER,   50                                   ),
    MQTT_MSG_MAXSIZE                 (UNIQUE,    NO_SUBLIST, "mqtt.msg.maxsize",                 INTEGER,   Integer.MAX_VALUE                    );

    public enum ValueType
    {
        BOOLEAN,
        INTEGER,
        LONG,
        STRING,
        LIST
    }


    public static final List<Config> array = Arrays.asList(Config.values());

    static
    {
        for (Config config : array) {
            if (config.getSubGroup() != NO_SUBLIST) {
                EnumSet<Config> subItems = EnumSet.noneOf(Config.class);
                for (Config subItem : array) {
                    if (subItem.getGroup() == config.getSubGroup()) {
                        subItems.add(subItem);
                    }
                }

                config.setSubItems(subItems);
            }
        }
    }



    private final ConfigGroup group;
    private final ConfigGroup subGroup;
    private final String name;
    private final ValueType type;
    private final Object defaultValue;

    private EnumSet<Config> subItems;


    Config(ConfigGroup groupId, ConfigGroup subGroupId, String name,
           ValueType type, Object defaultValue)
    {
        this.group        = groupId;
        this.subGroup     = subGroupId;
        this.name         = name;
        this.type         = type;
        this.defaultValue = defaultValue;
    }

    public ConfigGroup getGroup()
    {
        return group;
    }

    public ConfigGroup getSubGroup()
    {
        return subGroup;
    }

    public String getName()
    {
        return name;
    }

    public EnumSet<Config> getSubItems()
    {
        return subItems;
    }

    public ValueType getType()
    {
        return type;
    }

    public Object getDefaultValue()
    {
        return defaultValue;
    }

    public void setSubItems(EnumSet<Config> subItems)
    {
        this.subItems = subItems;
    }
}