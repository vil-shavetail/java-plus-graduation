package ru.practicum;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public class EventsSimilarityDeserializer extends AvroDeserializer<EventSimilarityAvro> {
    public EventsSimilarityDeserializer() {super(EventSimilarityAvro.getClassSchema());}
}
