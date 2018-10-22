package no.maddin.strom;

import com.opencsv.bean.*;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.collections4.MultiValuedMap;

import java.time.LocalDate;

@ToString
public class StromData {

    @Getter
    @CsvBindByPosition(position = 0, required = true)
    private String meterId;

    @Getter
    @CsvCustomBindByPosition(position = 1, converter = StringToDateConverter.class, required = true)
    private LocalDate rowDate;

    @Getter
    @CsvBindAndJoinByPosition(position = "2-", elementType = Double.class)
    private MultiValuedMap<String, Double> intervalValues;
}
