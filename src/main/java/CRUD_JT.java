import jnr.ffi.LibraryLoader;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessageUnpacker;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class CRUD_JT {
    // Інтерфейс до нативної Rust бібліотеки
    public interface MyRustLib {
        void encrypted_key(String token);
        String __create(byte[] buffer, long size, int ttl, int silence_read);
        String __read(String token);
        boolean __update(String token, byte[] buffer, long size, int ttl, int silence_read);
        boolean __delete(String token);
    }

    // Завантажуємо бібліотеку через JNR-FFI
    private static MyRustLib lib = LibraryLoader.create(MyRustLib.class).load("store_jt_x86_64");

    // encrypted_key метод для роботи з кешем
    public static void encrypted_key(String encrypted_key) {
        lib.encrypted_key(encrypted_key);
    }

    // q метод, який пакує HashMap у байти та викликає __create
    public static String create(Map<String, Object> hash, int ttl, int silence_read) throws IOException {
        // Пакуємо дані через MessagePack
        byte[] packedData = pack(hash);

        // Викликаємо нативний метод Rust через JNR-FFI
        return lib.__create(packedData, packedData.length, ttl, silence_read);
    }

    // w метод для роботи з кешем
    public static Map<String, Object> read(String token) {
        // return lib.__read(token);
        String str = lib.__read(token);
        if (str.isEmpty()) {
            return null;
        }

        Map<String, Object> result = new JSONObject(str).toMap();
        return result.size() > 0 ? result : null;
    }

    // e метод, який пакує HashMap і передає її в Rust
    public static boolean update(String token, Map<String, Object> hash, int ttl, int silence_read) throws IOException {
        byte[] packedData = pack(hash);
        return lib.__update(token, packedData, packedData.length, ttl, silence_read);
    }

    // r метод
    public static boolean delete(String token) {
        return lib.__delete(token);
    }

    // Допоміжний метод для пакування HashMap у байти
    private static byte[] pack(Map<String, Object> map) throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        packer.packMapHeader(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            packer.packString(entry.getKey());
            packer.packString(entry.getValue().toString());
        }
        packer.close();
        return packer.toByteArray();
    }

    // lib.encrypted_key("Cm7B68NWsMNNYjzMDREacmpe5sI1o0g40ZC9w1yQW3WOes7Gm59UsiasdfOHR2dciYiwmaYq98l3tG8h9yXVCxg==");

    // // Приклад основного методу для тестування
    public static void main(String[] args) throws IOException {
        lib.encrypted_key("Cm7B68NWsMNNYjzMDREacmpe5sI1o0g40ZC9w1yQW3WOes7Gm59UsittLOHR2dciYiwmaYq98l3tG8h9yXVCxg==");

        // // Тест для q методу
        // Map<String, Object> testMap = new HashMap<>();
        // testMap.put("key", "token");
        // String result = create(testMap, -1, -1);
        // System.out.println("Result from create: " + result);
    }
}
