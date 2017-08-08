package noza.core;


public class Mqtt
{
    public static final int FIXED_HEADER_LEN        = 2;
    public static final String MQTT_STR             = "MQTT";
    public static final int MQTT_STR_LEN            = 4;
    public static final int STR_SIZELEN             = 2;
    public static final int MQTT_PROTOCOL_LEVEL_LEN = 1;
    public static final byte PROTOCOL_LEVEL         = 4;
    public static final int PACKET_ID_LEN           = 2;
    public static final int QOS_BYTE_LEN            = 1;
    public static final int MQTT_MAX_TOPIC_LEN      = 65535;
    public static final int MQTT_SUBSCRIBE_FAIL     = 1;
    public static final int MQTT_SUBSCRIBE_WILDCARD = 2;
    public static final int MIN_MSG_LEN             = 2;
    public static final int MAX_FIXED_HDR_LEN       = 5;

    public static final String clientIdChars
            = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static final String[] msgStrings = {
            "MQTT_RESERVED",
            "CONNECT",
            "CONNACK",
            "PUBLISH",
            "PUBACK",
            "PUBREC",
            "PUBREL",
            "PUBCOMP",
            "SUBSCRIBE",
            "SUBACK",
            "UNSUBSCRIBE",
            "UNSUBACK",
            "PINGREQ",
            "PINGRESP",
            "DISCONNECT",
    };
}
