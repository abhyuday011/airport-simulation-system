package uk.ac.warwick.cs261.ui;

public class UIUtil 
{
    public static final double NANO_SECONDS_TO_SECONDS = 1000000000;

    public static String timeToString(long seconds) 
    {
        if (seconds < 0)
            return "##:##:##";
        
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    public static double lerp(double start, double end, double time)
    {
        return start + ((end - start) * time);
    }
}
