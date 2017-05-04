package org.vaadin.artur.temperature;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.vaadin.artur.temperature.components.VaadinLineChart;

import com.vaadin.annotations.Exclude;
import com.vaadin.annotations.Uses;
import com.vaadin.hummingbird.router.LocationChangeEvent;
import com.vaadin.hummingbird.template.model.TemplateModel;
import com.vaadin.ui.AttachEvent;
import com.vaadin.ui.DetachEvent;
import com.vaadin.ui.Template;
import com.vaadin.ui.UI;

@Uses(VaadinLineChart.class)
public class TemperatureView extends Template {

    private ScheduledExecutorService executor = Executors
            .newScheduledThreadPool(2);

    private static DBConnection connection = new DBConnection();

    private ScheduledFuture<?> scheduledTask = null;

    private transient volatile Map<String, LocalDateTime> lastQueryTime = new HashMap<>();

    // private List<Sensor> sensors;

    public interface TemperatureModel extends TemplateModel {
        @Exclude("time")
        void setLast24HMeasurements(List<Measurement> measurements);

        List<Measurement> getLast24HMeasurements();

        double getChartsStart();

        void setChartsStart(double chartsStart);

        List<Sensor> getSensors();

        void setSensors(List<Sensor> series);
        // @Exclude("time")
        // void setLast7DaysMeasurements(List<Measurement> measurements);
        //
        // List<Measurement> getLast7DaysMeasurements();
    }

    public TemperatureView() throws IOException {
        super(getTemplate());
        getModel().setSensors(connection.getSensors());
    }

    private static InputStream getTemplate() throws IOException {
        String templateFileNameAndPath = TemperatureView.class.getSimpleName()
                + ".html";
        String templateFileName = new File(templateFileNameAndPath).getName();
        InputStream templateContentStream = TemperatureView.class
                .getResourceAsStream(templateFileName);
        String template = IOUtils.toString(templateContentStream,
                StandardCharsets.UTF_8);
        String[] parts = template.split("#tpl#", 3);
        StringBuilder loop = new StringBuilder();
        String loopTemplate = parts[1];

        List<Sensor> sensors = connection.getSensors();
        sensors.forEach(sensor -> {
            String sensorTag = loopTemplate
                    .replaceAll("#name#", sensor.getDescription())
                    .replaceAll("#id#", sensor.getId());
            loop.append(sensorTag);
        });
        String result = parts[0] + loop.toString() + parts[2];
        return new ByteArrayInputStream(
                result.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected TemperatureModel getModel() {
        return (TemperatureModel) super.getModel();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (scheduledTask != null) {
            Logger.getLogger(TemperatureView.class.getName())
                    .severe("A scheduledTask was defined in onAttach");
        }
        scheduledTask = executor.scheduleWithFixedDelay(() -> {
            updateIfNewDataAvailable();
        }, 10, 10, TimeUnit.SECONDS);
    }

    private void updateIfNewDataAvailable() {
        getModel().getSensors().forEach(this::updateIfNewDataAvailable);
    }

    private void updateIfNewDataAvailable(Sensor sensor) {
        LocalDateTime newestDataTime = connection.getLastMeasurement(sensor)
                .getTime();
        LocalDateTime lastChecked = lastQueryTime.get(sensor.getId());
        if (!newestDataTime.isAfter(lastChecked)) {
            return;
        }

        // LocalDateTime sevenDaysAgo = lastChecked.minusDays(7);
        // List<Measurement> weekMeasurements = connection
        // .getAllMeasurements(sevenDaysAgo);
        // if (weekMeasurements.size() > 1000) {
        // int nth = (int) Math.ceil(weekMeasurements.size() / 1000);
        // IntStream a = IntStream.range(0, weekMeasurements.size())
        // .filter(n -> n % nth == 0);
        // weekMeasurements = a.mapToObj(weekMeasurements::get)
        // .collect(Collectors.toList());
        // }
        // getModel().setLast7DaysMeasurements(weekMeasurements);

        List<Measurement> newValues = connection
                .getAllMeasurements(sensor.getId(), lastChecked);
        lastQueryTime.put(sensor.getId(), LocalDateTime.now());
        Optional<UI> ui = getUI();

        System.out.println(newValues.size() + " new values for " + sensor);
        List<Measurement> values = limit(newValues, 1000);
        System.out.println(values.size() + " new limited values for " + sensor);

        if (ui.isPresent()) {
            ui.get().access(() -> {
                getModel().getLast24HMeasurements().addAll(values);
            });
        } else {
            getModel().getLast24HMeasurements().addAll(values);
        }
    }

    private List<Measurement> limit(List<Measurement> values, int max) {
        if (values.size() <= max) {
            return values;
        }

        List<Measurement> limited = new ArrayList<>();

        double nth = values.size() / (double) max;
        for (double i = 0; i < values.size(); i += nth) {
            limited.add(values.get((int) i));
        }

        return limited;
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        } else {
            Logger.getLogger(TemperatureView.class.getName())
                    .severe("No scheduledTask defined in onDetach");
        }
    }

    @Override
    public void onLocationChange(LocationChangeEvent locationChangeEvent) {
        super.onLocationChange(locationChangeEvent);

        getModel().setLast24HMeasurements(new ArrayList<>());
        getModel().setChartsStart(Measurement
                .localDateTimeToHighcharts(LocalDateTime.now().minusDays(1)));
        getModel().setChartsStart(1471205717557.0);
        getModel().getSensors()
                .forEach(sensor -> lastQueryTime.put(sensor.getId(),
                        LocalDateTime.ofEpochSecond(1, 1, ZoneOffset.UTC)));

        updateIfNewDataAvailable();

        // lastQueryTime = LocalDateTime.now();
        // LocalDateTime oneDayAgo = lastQueryTime.minusDays(1);

        // List<Measurement> measurements = connection
        // .getAllMeasurements(oneDayAgo);
        // getModel().setLast24HMeasurements(measurements);

    }
}
