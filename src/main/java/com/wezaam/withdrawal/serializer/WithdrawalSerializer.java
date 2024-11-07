package com.wezaam.withdrawal.serializer;

import com.wezaam.withdrawal.model.Withdrawal;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class WithdrawalSerializer implements Serializer<Withdrawal> {
    private String encoding = "UTF8";

    public WithdrawalSerializer() {
    }

    public void configure(Map<String, ?> configs, boolean isKey) {
        String propertyName = isKey ? "key.serializer.encoding" : "value.serializer.encoding";
        Object encodingValue = configs.get(propertyName);
        if (encodingValue == null) {
            encodingValue = configs.get("serializer.encoding");
        }

        if (encodingValue instanceof String) {
            this.encoding = (String)encodingValue;
        }

    }

    public byte[] serialize(String topic, Withdrawal withdrawal) {
        try {
            return withdrawal == null ? null : withdrawal.toString().getBytes(this.encoding);
        } catch (UnsupportedEncodingException var4) {
            throw new SerializationException("Error when serializing Withdrawal to byte[] due to unsupported encoding " + this.encoding);
        }
    }
}