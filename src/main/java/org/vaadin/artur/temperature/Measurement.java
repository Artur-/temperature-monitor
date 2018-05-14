package org.vaadin.artur.temperature;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class Measurement implements Serializable {
    private LocalDateTime time;
    private double temperature;
//    private String sensor;

    public Measurement() {

    }

    public Measurement(LocalDateTime time, double temperature) {
        this.time = time;
        this.temperature = temperature;
    }

    public double getTimestamp() {
        // Highcharts uses UTC
        // long is not supported by Flow
        return localDateTimeToHighcharts(time);
    }

    public static double localDateTimeToHighcharts(LocalDateTime time) {
        return time.toEpochSecond(ZoneOffset.UTC) * 1000;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public String getTimeString() {
        return time
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                        .withLocale(new Locale("fi", "FI")));
    }

    public double getTemperature() {
        return temperature;
    }

//    public String getSensor() {
//        return sensor;
//    }

}