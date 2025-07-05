package ca.bkaw.torque.util;

import org.joml.Vector3dc;

public class Util {
    public static String formatSi(String unit, double value) {
        if (value < 1000) {
            return String.format("%.2f %s", value, unit);
        }

        String[] suffixes = {"", "k", "M", "G", "T"};
        int i = 0;
        while (value >= 1000 && i < suffixes.length - 1) {
            value /= 1000;
            i++;
        }
        return String.format("%.2f %s%s", value, suffixes[i], unit);
    }

    public static String formatSi(String unit, Vector3dc vector) {
        return formatSi(unit, vector.length());
    }
}
