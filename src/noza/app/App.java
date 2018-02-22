package noza.app;

import noza.api.Noza;
import noza.cluster.Cluster;

import java.nio.ByteBuffer;

public class App
{
    public static void main(String[] args)
    {
        ByteBuffer buf = ByteBuffer.allocate(1000);

        Noza noza = null;

        try {
            noza = new Noza(null, null);
            noza.start();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
