package noza.api.msgs;

public interface MsgTopic
{
    /*int QOS0     = 0x00;
    int QOS1     = 0x01;
    int QOS2     = 0x02;
    int QOS_FAIL = 0x80;*/

    String getStr();

    void setStr(String str);

    int getQos();

    void setQos(int qos);

    int getResult();

    void setResult(int result);
}
