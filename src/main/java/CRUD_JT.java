import jnr.ffi.LibraryLoader;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessageUnpacker;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.json.JSONException;

public class CRUD_JT {
    // Інтерфейс до нативної Rust бібліотеки
    public interface MyRustLib {
        void encrypted_key(String token);
        String __create(byte[] buffer, long size, long ttl, long silence_read);
        String __read(String token);
        boolean __update(String token, byte[] buffer, long size, long ttl, long silence_read);
        boolean __delete(String token);
    }

    private static String osName = System.getProperty("os.name").toLowerCase();
    private static String osArch = System.getProperty("os.arch").toLowerCase();
    private static MyRustLib lib;

    static {
        try {
            File tempFile = loadNativeLibrary();  // Завантажуємо бібліотеку у тимчасовий файл
            lib = LibraryLoader.create(MyRustLib.class).load(tempFile.getAbsolutePath());  // Завантажуємо з правильним шляхом
        } catch (IOException e) {
            throw new RuntimeException("Failed to load native library", e);
        }
    }

    private static File loadNativeLibrary() throws IOException {
        String libPath = getLibraryPath();
        InputStream inputStream = CRUD_JT.class.getResourceAsStream(libPath);

        if (inputStream == null) {
            throw new IOException("Cannot find the library file in resources: " + libPath);
        }

        // Створюємо тимчасовий файл для бібліотеки
        File tempFile = File.createTempFile("store_jt", getLibraryExtension());
        tempFile.deleteOnExit(); // Видалення файлу при завершенні програми

        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }

        // Повертаємо шлях до тимчасового файлу
        return tempFile;
    }

    private static String getLibraryPath() {
        String osFolder;
        String archFolder;

        // Визначаємо папку для ОС
        if (osName.contains("win")) {
            osFolder = "windows";
        } else if (osName.contains("mac")) {
            osFolder = "macos";
        } else if (osName.contains("nux")) {
            osFolder = "linux";
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + osName);
        }

        // Визначаємо папку для архітектури
        if (Pattern.compile("arm64|aarch").matcher(osArch).find()) {
            archFolder = "arm64";
        } else if (Pattern.compile("x86_64|x64|amd64").matcher(osArch).find()) {
            archFolder = "x86_64";
        } else {
            throw new UnsupportedOperationException("Unsupported architecture: " + osArch);
        }

        // Повертаємо шлях до бібліотеки
        return "/native/" + osFolder + "/store_jt_" + archFolder + getLibraryExtension();
    }

    private static String getLibraryExtension() {
        if (osName.contains("win")) {
            // return ".dll";
            throw new UnsupportedOperationException("Unsupported operating system: " + osName);
        } else if (osName.contains("mac")) {
            return ".dylib";
        } else if (osName.contains("nux")) {
            return ".so";
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + osName);
        }
    }

    // Метод для використання бібліотеки
    public static MyRustLib getLib() {
        return lib;
    }

    private static final LRUCache lru_cache;

    static { lru_cache = new LRUCache(value -> {
      // System.out.println(value);
      return lib.__read(value);
    }); }

    // encrypted_key метод для роботи з кешем
    public static void encrypted_key(String encrypted_key) {
        lib.encrypted_key(encrypted_key);
    }

    // q метод, який пакує HashMap у байти та викликає __create
    public static String create(Map<String, Object> hash, long ttl, long silence_read) throws IOException {
        Validation.validateInsertion(hash, ttl, silence_read);

        // Пакуємо дані через MessagePack
        byte[] packedData = pack(hash);

        // Викликаємо нативний метод Rust через JNR-FFI
        String token = lib.__create(packedData, packedData.length, ttl, silence_read);

        lru_cache.insert(token, hash, ttl, silence_read);

        return token;
    }

    // w метод для роботи з кешем
    public static Map<String, Object> read(String token) {
        Map<String, Object> output = lru_cache.get(token);
        if (output != null) {
          return output;
        }

        String str = lib.__read(token);
        if (str.isEmpty()) {
            return null;
        }

        Map<String, Object> result = new JSONObject(str).toMap();

        if (result.size() > 0) {
          lru_cache.forceInsert(token, result);
          return result;
        } else {
          return null;
        }
    }

    // e метод, який пакує HashMap і передає її в Rust
    public static boolean update(String token, Map<String, Object> hash, long ttl, long silence_read) throws IOException {
        byte[] packedData = pack(hash);
        boolean result = lib.__update(token, packedData, packedData.length, ttl, silence_read);

        if (result) {
          lru_cache.insert(token, hash, ttl, silence_read);
        }
        return result;
    }

    // r метод
    public static boolean delete(String token) {
        lru_cache.delete(token);
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

    // // Приклад основного методу для тестування
    public static void main(String[] args) throws IOException {
        lib.encrypted_key("Cm7B68NWsMNNYjzMDREacmpe5sI1o0g40ZC9w1yQW3WOes7Gm59UsittLOHR2dciYiwmaYq98l3tG8h9yXVCxg==");
    }
}
