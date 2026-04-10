// This binding was generated automatically to ensure consistency across languages
// Generated using ChatGPT (GPT-5) from the canonical Ruby SDK
// API is stable and production-ready

package crudjt;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.value.Value;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MsgPackUtils {
    public static byte[] pack(Map<String, Object> map) throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        packer.packMapHeader(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            packer.packString(entry.getKey());
            packer.packString(entry.getValue().toString());
        }
        packer.close();

        return packer.toByteArray();
    }

    public static Map<String, Object> unpack(byte[] data) throws IOException {
        Map<Value, Value> raw =
            MessagePack.newDefaultUnpacker(data)
                .unpackValue()
                .asMapValue()
                .map();

        Map<String, Object> out = new HashMap<>();
        for (var e : raw.entrySet()) {
            out.put(
                e.getKey().asStringValue().asString(),
                e.getValue().toString()
            );
        }
        return out;
    }
}
