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
        try (InfluxDB influxDB = InfluxDBFactory.connect("http://localhost:8086", "root", "5up3rS3cr3t")) {
            String dbName = "strom";
            influxDB.createDatabase(dbName);
            influxDB.setDatabase(dbName);
//            String rpName = "aRetentionPolicy";
//            influxDB.createRetentionPolicy(rpName, dbName, "30d", "30m", 2, true);
//            influxDB.setRetentionPolicy(rpName);

//            influxDB.enableBatch(BatchOptions.DEFAULTS);


            new CsvValueReader(Path.of(readerDirectoryPath)).process(d -> processData(d, influxDB));
            influxDB.flush();
        }
    }

    private void processData(StromData data, InfluxDB influxDB) {
        LocalDateTime ldt0 = LocalDateTime.of(data.getRowDate(), LocalTime.of(0, 0));
        for (Double value : data.getIntervalValues().values()) {
            LocalDateTime ldt1 = ldt0.plusHours(1);
            Point point = Point.measurement("strom")
                .time(toLongTime(ldt1), TimeUnit.MILLISECONDS)
                .addField("start_interval", toLongTime(ldt0))
                .addField("end_interval", toLongTime(ldt1))
                .addField("value", value)
                .build();
            influxDB.write(point);

            log.info("processing dataset: " + point);
            ldt0 = ldt1;
        }
    }

    private long toLongTime(LocalDateTime ldt) {
        return java.util.Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime();
    }
}
