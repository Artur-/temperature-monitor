package org.vaadin.artur.temperature;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class DBConnection {

    private static final String JNDI_DS_NAME = "Temperature";
    private javax.sql.DataSource dataSource;

    public DBConnection() {
        try {
            InitialContext ic = new InitialContext();
            dataSource = (DataSource) ic.lookup("jdbc/" + JNDI_DS_NAME);
        } catch (NamingException e) {
            throw new IllegalStateException(
                    "Unable to find data source named '" + JNDI_DS_NAME + "'",
                    e);
        }
    }

    @FunctionalInterface
    public interface SQLConsumer {
        void accept(Connection connection) throws SQLException;
    }

    private void withConnection(SQLConsumer consumer) {
        Connection c = null;
        try {
            c = dataSource.getConnection();
            consumer.accept(c);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void storeMeasurement(LocalDateTime time, double value,
            String sensor) {
        withConnection(connection -> {
            PreparedStatement s = connection.prepareStatement(
                    "INSERT INTO log (datetime, temperature, sensor) VALUES (?,?,?)");

            Timestamp stamp = Timestamp.valueOf(time);
            s.setTimestamp(1, stamp);
            s.setDouble(2, value);
            s.setString(3, sensor);
            s.execute();
        });

    }

    public List<Sensor> getSensors() {
        List<Sensor> sensors = new ArrayList<>();

        withConnection(connection -> {
            PreparedStatement s = connection
                    .prepareStatement("SELECT id,location FROM sensor");
            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                sensors.add(new Sensor(rs.getString("id"),
                        rs.getString("location")));
            }
        });
        return sensors;

    }

    public List<Measurement> getAllMeasurements(String sensorId,
            LocalDateTime after) {
        List<Measurement> result = new ArrayList<>();
        withConnection(connection -> {
            PreparedStatement s = connection.prepareStatement(
                    "SELECT datetime,temperature,sensor FROM log where datetime > ? AND sensor = ?");
            s.setTimestamp(1, Timestamp.valueOf(after));
            s.setString(2, sensorId);

            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                LocalDateTime time = rs.getTimestamp("datetime")
                        .toLocalDateTime();
                Measurement measurement = new Measurement(time,
                        rs.getDouble("temperature"), rs.getString("sensor"));
                result.add(measurement);

            }
        });
        return result;
    }

    public Measurement getLastMeasurement(Sensor sensor) {
        AtomicReference<Measurement> m = new AtomicReference<>();
        withConnection(connection -> {
            PreparedStatement s = connection.prepareStatement(
                    "SELECT datetime,temperature,sensor FROM log WHERE sensor=? ORDER BY datetime DESC LIMIT 1");
            s.setString(1, sensor.getId());
            ResultSet rs = s.executeQuery();
            rs.next();
            LocalDateTime time = rs.getTimestamp("datetime").toLocalDateTime();
            Measurement measurement = new Measurement(time,
                    rs.getDouble("temperature"), rs.getString("sensor"));
            m.set(measurement);
        });
        return m.get();
    }

}
