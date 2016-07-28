package org.vaadin.artur.temperature;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class Measurement implements Serializable {
    private LocalDateTime time;
    private double temperature;
    private String location;

    public Measurement(LocalDateTime time, double temperature,
            String location) {
        this.time = time;
        this.temperature = temperature;
        this.location = location;
    }

    public String getTime() {
        return time
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                        .withLocale(new Locale("fi", "FI")));
    }

    public double getTemperature() {
        return temperature;
    }

    public String getLocation() {
        return location;
    }

}