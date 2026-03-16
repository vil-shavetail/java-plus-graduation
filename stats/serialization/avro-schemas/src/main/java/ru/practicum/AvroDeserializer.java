package ru.practicum;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@SuppressWarnings("unchecked")
public class AvroDeserializer<T extends GenericRecord> {
    private final SpecificDatumReader<T> datumReader;
    private final Schema schema;

    public AvroDeserializer(Class<T> clazz) {
        this.schema = getSchemaFromClass(clazz);
        this.datumReader = new SpecificDatumReader<>(schema);
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

    public T deserialize(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Cannot deserialize null data");
        }
        if (data.length == 0) {
            throw new IllegalArgumentException("Cannot deserialize empty byte array");
        }
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(inputStream, null);
            return datumReader.read(null, decoder);
        } catch (IOException e) {
            throw new RuntimeException("Error deserializing Avro data", e);
        }
    }
}