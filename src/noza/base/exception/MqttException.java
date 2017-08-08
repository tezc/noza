package noza.base.exception;


public class MqttException extends RuntimeException
{
    public MqttException(String log)
    {
        super(log);
    }
}
