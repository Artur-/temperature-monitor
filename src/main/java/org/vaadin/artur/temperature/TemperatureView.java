package org.vaadin.artur.temperature;

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

    private DBConnection connection;

    private ScheduledFuture<?> scheduledTask = null;

    private transient volatile Map<String, LocalDateTime> lastQueryTime = new HashMap<>();

    private List<String> sensors;

    public interface TemperatureModel extends TemplateModel {
        @Exclude("time")
        void setLast24HMeasurements(List<Measurement> measurements);

        List<Measurement> getLast24HMeasurements();

        // @Exclude("time")
        // void setLast7DaysMeasurements(List<Measurement> measurements);
        //
        // List<Measurement> getLast7DaysMeasurements();
    }

    public TemperatureView() {
        connection = new DBConnection();
        sensors = connection.getSensors();
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
        sensors.forEach(this::updateIfNewDataAvailable);
    }

    private void updateIfNewDataAvailable(String sensor) {
        LocalDateTime newestDataTime = connection.getLastMeasurement(sensor)
                .getTime();
        LocalDateTime lastChecked = lastQueryTime.get(sensor);
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

        List<Measurement> newValues = connection.getAllMeasurements(sensor,
                lastChecked);
        lastQueryTime.put(sensor, LocalDateTime.now());
        Optional<UI> ui = getUI();

        System.out.println(newValues.size() + " new values for " + sensor);
        if (ui.isPresent()) {
            ui.get().access(() -> {
                getModel().getLast24HMeasurements().addAll(newValues);
            });
        } else {
            getModel().getLast24HMeasurements().addAll(newValues);
        }
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
        sensors.forEach(sensor -> lastQueryTime.put(sensor,
                LocalDateTime.ofEpochSecond(1, 1, ZoneOffset.UTC)));

        updateIfNewDataAvailable();

        // lastQueryTime = LocalDateTime.now();
        // LocalDateTime oneDayAgo = lastQueryTime.minusDays(1);

        // List<Measurement> measurements = connection
        // .getAllMeasurements(oneDayAgo);
        // getModel().setLast24HMeasurements(measurements);

    }
}
