package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.io.DatumReader;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Slf4j
public class AvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {
    private final DecoderFactory decoderFactory;
    private final DatumReader<T> reader;

    public AvroDeserializer(Schema schema) {
        this(DecoderFactory.get(), schema);
    }

    public AvroDeserializer(DecoderFactory decoderFactory, Schema schema) {
        this.decoderFactory = decoderFactory;
        this.reader = new SpecificDatumReader<>(schema);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try (ByteArrayInputStream is = new ByteArrayInputStream(data)) {
            BinaryDecoder decoder = decoderFactory.binaryDecoder(is, null);
            return reader.read(null, decoder);

        } catch (IOException ex) {
            log.error("Error deserializing Avro data from the topic: {}", topic, ex);
            throw new SerializationException("Error deserializing Avro data from the topic: " + topic, ex);
        } catch (Exception ex) {
            log.error("Unknown deserialization error for topic: {}", topic, ex);
            throw new SerializationException("Unknown deserialization error for topic: " + topic, ex);
        }
    }
}