package no.maddin.strom;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

@Slf4j
public class CsvValueReader {
    private final Path dataFile;

    public CsvValueReader(Path dataFile) {
        this.dataFile = dataFile;
    }

    public void process(Consumer<StromData> datasetConsumer) {
        readFile(this.dataFile, datasetConsumer);

    }

    @SneakyThrows
    private void readFile(Path csvFile, Consumer<StromData> datasetConsumer) {
        try (FileReader fr = new FileReader(csvFile.toFile())) {
            CsvToBean<StromData> beans = new CsvToBeanBuilder<StromData>(fr)
                .withType(StromData.class)
                .withSkipLines(1)
                .build();

            for (StromData data : beans) {
                datasetConsumer.accept(data);
            }
        }
    }
}
