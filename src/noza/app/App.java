package noza.app;

import noza.api.Noza;

public class App
{
    public static void main(String[] args)
    {


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
