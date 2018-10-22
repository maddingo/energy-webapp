package no.maddin.strom;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

import java.time.LocalDate;

public class StringToDateConverter extends AbstractBeanField<LocalDate> {
    @Override
    protected LocalDate convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {

        return  LocalDate.parse(value);
    }
}
