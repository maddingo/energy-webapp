package no.maddin.strom;


import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;

import java.io.File;
import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

@Builder
@Slf4j
public class App {

    private final File dataFile;
    private String downloadUser;
    private String downloadPassword;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public static void main(String[] args) throws Exception {

        App.builder()
            .downloadUser(args[0])
            .downloadPassword(args[1])
            .dbUrl("http://localhost:8086")
            .dbUser("root")
            .dbPassword("5up3rS3cr3t")
            .build()
            .download()
            .save();
    }

    // https://www.lysenett.no/nedlasting-av-stromforbruk/category15207.html?meter=707057500070545570&year=2017-2018&month=1-12&day=01-31
    // https://www.lysenett.no/streamhourmeterexport.php
    //     POST
    //     action=streamfiles&goto-if-warnings=https%3A%2F%2Fwww.lysenett.no%2Fnedlasting-av-stromforbruk%2Fcategory15207.html&fromdate=01.01.2017&todate=31.12.2018&exportformat=1&entityIdList%5B%5D=SP-134342-001-1
    private App download() throws URISyntaxException, IOException, InterruptedException {
        CookieManager cookieHandler = new CookieManager();
        HttpClient client = HttpClient.newBuilder()
//            .authenticator(new Authenticator() {
//            })
            .cookieHandler(cookieHandler)
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

        // Start page
        HttpRequest minsideRequest = HttpRequest.newBuilder(new URI("https://www.lysenett.no/minside/"))
            .GET()
            .build();
        HttpResponse<String> minsideResponse = client.send(minsideRequest, HttpResponse.BodyHandlers.ofString());
/*
X-Prototype-Version: 1.7
X-Requested-With: XMLHttpRequest
 */
        // authenticate
        HttpRequest authenticateRequest = HttpRequest.newBuilder(new URI("https://www.lysenett.no/restendpoint/lyse.authentication/v1/authenticate"))
            .POST(HttpRequest.BodyPublishers.ofByteArray(authenticationPayload()))
            .setHeader("X-Prototpye-Version", "1.7")
            .setHeader("X-Requested-With", "XMLHttpRequest")
            .setHeader("Referer", "https://www.lysenett.no/minside/")
            .build();
        HttpResponse<String> authResponse = client.send(authenticateRequest, HttpResponse.BodyHandlers.ofString());

        HttpRequest downloadRequest = HttpRequest.newBuilder(new URI("https://www.lysenett.no/maleravlesning/category15171.html"))
            .build();
        HttpResponse<String> response = client.send(
            downloadRequest,
            HttpResponse.BodyHandlers.ofString()
        );
        return this;
    }

    private byte[] authenticationPayload() {
        return ("{\"username\":\"" + downloadUser + "\",\"password\":\"" + downloadPassword + "\"}").getBytes();
    }

    public void save() throws IOException {
        // start a DB with
        try (InfluxDB influxDB = InfluxDBFactory.connect(dbUrl, dbUser, dbPassword)) {
            String dbName = "strom";
            influxDB.createDatabase(dbName);
            influxDB.setDatabase(dbName);
//            String rpName = "aRetentionPolicy";
//            influxDB.createRetentionPolicy(rpName, dbName, "30d", "30m", 2, true);
//            influxDB.setRetentionPolicy(rpName);

//            influxDB.enableBatch(BatchOptions.DEFAULTS);


            new CsvValueReader(dataFile.toPath()).process(d -> processData(d, influxDB));
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
