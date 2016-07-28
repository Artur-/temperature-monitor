package org.vaadin.artur.temperature;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.hummingbird.router.LocationChangeEvent;
import com.vaadin.hummingbird.template.model.TemplateModel;
import com.vaadin.ui.Template;

public class TemperatureView extends Template {

    private DBConnection connection = new DBConnection();

    public interface TemperatureModel extends TemplateModel {
        void setLast24HMeasurements(List<Measurement> measurements);

        List<Measurement> getLast24HMeasurements();

        void setLast(Measurement last);

        Measurement getLast();

        void setLast24HMeasurementValues(List<Double> values);

        List<Double> getLast24HMeasurementValues();
    }

    @Override
    protected TemperatureModel getModel() {
        return (TemperatureModel) super.getModel();
    }

    @Override
    public void onLocationChange(LocationChangeEvent locationChangeEvent) {
        super.onLocationChange(locationChangeEvent);

        List<Measurement> measurements = connection
                .getMeasurements(LocalDateTime.now().minusDays(1));
        getModel().setLast24HMeasurements(measurements);

        getModel().setLast(connection.getLastMeasurement());
        getModel().setLast24HMeasurementValues(measurements.stream()
                .map(measurement -> measurement.getTemperature())
                .collect(Collectors.toList()));
    }
}
