package noza.core.msg;


import noza.api.msgs.MsgTopic;

public class Topic implements MsgTopic
{
    private String str;
    private int qos;
    private int result;

    public Topic(String str, int qos)
    {
        this.str = str;
        this.qos = qos;
    }

    @Override
    public String getStr()
    {
        return str;
    }

    @Override
    public void setStr(String str)
    {
        this.str = str;
    }

    @Override
    public int getQos()
    {
        return qos;
    }

    @Override
    public void setQos(int qos)
    {
        this.qos = qos;
    }

    @Override
    public int getResult()
    {
        return result;
    }

    @Override
    public void setResult(int result)
    {
        this.result = result;
    }

    public String toString()
    {
        return "topic : " + str + " qos : " + qos;
    }
}
