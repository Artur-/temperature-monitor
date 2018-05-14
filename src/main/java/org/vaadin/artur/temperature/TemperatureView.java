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

import org.vaadin.artur.temperature.TemperatureView.TemperatureModel;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.Exclude;
import com.vaadin.flow.templatemodel.TemplateModel;

@Tag("temperature-view")
@HtmlImport("temperature-view.html")
@Route("")
public class TemperatureView extends PolymerTemplate<TemperatureModel> {

    private ScheduledExecutorService executor = Executors
            .newScheduledThreadPool(2);

    private static DBConnection connection = new DBConnection();

    private ScheduledFuture<?> scheduledTask = null;

    private transient volatile Map<String, LocalDateTime> lastQueryTime = new HashMap<>();

    public static class SensorData {
        private Sensor sensor;
        private List<Measurement> measurements;

        public Sensor getSensor() {
            return sensor;
        }

        public void setSensor(Sensor sensor) {
            this.sensor = sensor;
        }

        public List<Measurement> getMeasurements() {
            return measurements;
        }

        @Exclude("time")
        public void setMeasurements(List<Measurement> measurements) {
            this.measurements = measurements;
        }

    }

    public interface TemperatureModel extends TemplateModel {
        void setLast24HMeasurements(List<SensorData> sensorData);

        List<SensorData> getLast24HMeasurements();

        double getChartsStart();

        void setChartsStart(double chartsStart);

        List<Sensor> getSensors();

        void setSensors(List<Sensor> series);
        // @Exclude("time")
        // void setLast7DaysMeasurements(List<Measurement> measurements);
        //
        // List<Measurement> getLast7DaysMeasurements();
    }

    @Override
    protected TemperatureModel getModel() {
        return super.getModel();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        getModel().setSensors(connection.getSensors());
        getModel().setLast24HMeasurements(new ArrayList<>());
        getModel().setChartsStart(Measurement
                .localDateTimeToHighcharts(LocalDateTime.now().minusDays(1)));
        getModel().setChartsStart(1471205717557.0);
        getModel().getSensors()
                .forEach(sensor -> lastQueryTime.put(sensor.getId(),
                        LocalDateTime.ofEpochSecond(1, 1, ZoneOffset.UTC)));

        updateIfNewDataAvailable();

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
                findModelSensorData(sensor).setMeasurements(values);
            });
        } else {
            findModelSensorData(sensor).setMeasurements(values);
        }
    }

    private SensorData findModelSensorData(Sensor sensor) {
        List<SensorData> measurements = getModel().getLast24HMeasurements();
        Optional<SensorData> data = measurements.stream()
                .filter(sd -> sd.getSensor().equals(sensor)).findFirst();
        if (data.isPresent()) {
            return data.get();
        }

        SensorData sensorData = new SensorData();
        sensorData.setSensor(sensor);
        measurements.add(sensorData);
        return sensorData;
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

}
