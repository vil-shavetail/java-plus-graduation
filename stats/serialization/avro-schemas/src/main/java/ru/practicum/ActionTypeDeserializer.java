package ru.practicum;

import ru.practicum.ewm.stats.avro.UserActionAvro;

public class ActionTypeDeserializer extends AvroDeserializer<UserActionAvro> {
    public ActionTypeDeserializer() {super(UserActionAvro.getClassSchema());}
}