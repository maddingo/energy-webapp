package no.maddin.strom;


import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Builder
@Slf4j
public class App {

    private String readerDirectoryPath;

    public static void main( String[] args ) throws Exception {

        App.builder()
            .readerDirectoryPath("target/read-path")
            .build()
            .start();
    }

    private void start() throws IOException {
        try (InfluxDB influxDB = InfluxDBFactory.connect("http://172.18.0.2:8086", "root", "5up3rS3cr3t")) {
            String dbName = "strom";
            influxDB.createDatabase(dbName);
            influxDB.setDatabase(dbName);
            String rpName = "aRetentionPolicy";
            influxDB.createRetentionPolicy(rpName, dbName, "30d", "30m", 2, true);
            influxDB.setRetentionPolicy(rpName);

            influxDB.enableBatch(BatchOptions.DEFAULTS);


            new CsvValueReader(Path.of(readerDirectoryPath)).process(d -> processData(d, influxDB));
        }
    }

    private void processData(StromData data, InfluxDB influxDB) {
        int hour = 1;
        for (Double value : data.getIntervalValues().values()) {
            LocalDateTime ldt0 = LocalDateTime.of(data.getRowDate(), LocalTime.of(hour, 0));
            LocalDateTime ldt1 = LocalDateTime.of(data.getRowDate(), LocalTime.of(++hour, 0));
            influxDB.write(Point.measurement("strom")
                .time(toLongTime(ldt1), TimeUnit.HOURS)
                .addField("start_interval", toLongTime(ldt0))
                .addField("end_interval", toLongTime(ldt1))
                .addField("value", value)
                .build());

            log.info("processing dataset: " + data);
        }
    }

    private long toLongTime(LocalDateTime ldt) {
        return java.util.Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime();
    }
}
