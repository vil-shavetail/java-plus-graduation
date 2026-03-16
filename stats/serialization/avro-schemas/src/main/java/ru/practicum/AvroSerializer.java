package ru.practicum;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AvroSerializer<T extends GenericRecord> {
    private final SpecificDatumWriter<T> datumWriter;
    private final Schema schema;

    public AvroSerializer(Class<T> clazz) {
        this.schema = getSchemaFromClass(clazz);
        this.datumWriter = new SpecificDatumWriter<>(schema);
    }

    private Schema getSchemaFromClass(Class<T> clazz) {
        try {
            return (Schema) clazz.getField("SCHEMA$").get(null);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Class " + clazz.getName() + " does not have SCHEMA$ field", e
            );
        }
    }

    public byte[] serialize(T data) {
        if (data == null) {
            throw new IllegalArgumentException("Cannot serialize null data");
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
            datumWriter.write(data, encoder);
            encoder.flush();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error serializing Avro data", e);
        }
    }
}