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
    public interface MyRustLib {
        String __create(byte[] buffer, long size, long ttl, long silence_read);
        String __read(String token);
        boolean __update(String token, byte[] buffer, long size, long ttl, long silence_read);
        boolean __delete(String token);

        String start_store_jt(String encrypted_key, String store_jt_path);
    }

    private static String osName = System.getProperty("os.name").toLowerCase();
    private static String osArch = System.getProperty("os.arch").toLowerCase();
    private static final MyRustLib lib;

    static {
        try {
            File tempFile = loadNativeLibrary();
            lib = LibraryLoader.create(MyRustLib.class).load(tempFile.getAbsolutePath());
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

        File tempFile = File.createTempFile("store_jt", getLibraryExtension());
        tempFile.deleteOnExit();

        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }

        return tempFile;
    }

    private static String getLibraryPath() {
        String osFolder;
        String archFolder;

        if (osName.contains("win")) {
            osFolder = "windows";
        } else if (osName.contains("mac")) {
            osFolder = "macos";
        } else if (osName.contains("nux")) {
            osFolder = "linux";
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + osName);
        }

        if (Pattern.compile("arm64|aarch").matcher(osArch).find()) {
            archFolder = "arm64";
        } else if (Pattern.compile("x86_64|x64|amd64").matcher(osArch).find()) {
            archFolder = "x86_64";
        } else {
            throw new UnsupportedOperationException("Unsupported architecture: " + osArch);
        }

        return "/native/" + osFolder + "/store_jt_" + archFolder + getLibraryExtension();
    }

    private static String getLibraryExtension() {
        if (osName.contains("win")) {
            throw new UnsupportedOperationException("Unsupported operating system: " + osName);
        } else if (osName.contains("mac")) {
            return ".dylib";
        } else if (osName.contains("nux")) {
            return ".so";
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + osName);
        }
    }

    public static MyRustLib getLib() {
        return lib;
    }

    private static final CRUD_JT_LRUCache lru_cache;

    static { lru_cache = new CRUD_JT_LRUCache(value -> {
      return lib.__read(value);
    }); }

    public static String create(Map<String, Object> hash, long ttl, long silence_read) throws IOException {
      if (!Config.wasStarted()) {
          throw new RuntimeException(
              CRUD_JT_Validation.errorMessage(CRUD_JT_Validation.ERROR_NOT_STARTED)
          );
        }

        CRUD_JT_Validation.validateInsertion(hash, ttl, silence_read);

        byte[] packedData = pack(hash);
        CRUD_JT_Validation.validateHashBytesize(packedData.length);

        String token = lib.__create(packedData, packedData.length, ttl, silence_read);
        if (token == null) {
            throw new InternalError("Something went wrong. Ups");
        }

        lru_cache.insert(token, hash, ttl, silence_read);

        return token;
    }

    public static Map<String, Object> read(String token) {
      if (!Config.wasStarted()) {
          throw new RuntimeException(
              CRUD_JT_Validation.errorMessage(CRUD_JT_Validation.ERROR_NOT_STARTED)
          );
        }

        Map<String, Object> output = lru_cache.get(token);
        if (output != null) {
          return output;
        }

        String str = lib.__read(token);
        if (str == null) {
            return null;
        }

        Map<String, Object> result = new JSONObject(str).toMap();

        if (!(Boolean) result.get("ok")) {
            String code = (String) result.get("code");
            String msg = (String) result.get("error_message");

            throw CRUD_JT_Errors.createErrorByCode(code, msg);
        }

        Object data = result.get("data");
        if (data == null) {
            return null;
        }

        Map<String, Object> dataObj = new JSONObject(data).toMap();
        lru_cache.forceInsert(token, dataObj);

        return dataObj;
    }

    public static boolean update(String token, Map<String, Object> hash, long ttl, long silence_read) throws IOException {
      if (!Config.wasStarted()) {
          throw new RuntimeException(
              CRUD_JT_Validation.errorMessage(CRUD_JT_Validation.ERROR_NOT_STARTED)
          );
        }

        byte[] packedData = pack(hash);
        CRUD_JT_Validation.validateHashBytesize(packedData.length);
        boolean result = lib.__update(token, packedData, packedData.length, ttl, silence_read);

        if (result) {
          lru_cache.insert(token, hash, ttl, silence_read);
        }
        return result;
    }

    public static boolean delete(String token) {
      if (!Config.wasStarted()) {
          throw new RuntimeException(
              CRUD_JT_Validation.errorMessage(CRUD_JT_Validation.ERROR_NOT_STARTED)
          );
        }

        lru_cache.delete(token);

        return lib.__delete(token);
    }

    public static class Config {
      private static final Map<String, Object> settings = new HashMap<>();
      private static boolean wasStarted = false;

      public static Config encrypted_key(String value) {
          CRUD_JT_Validation.validateEncrypted_key(value);
          settings.put("encrypted_key", value);
          return ConfigHolder.INSTANCE;
      }

      public static Config store_jtPath(String value) {
          settings.put("store_jt_path", value);
          return ConfigHolder.INSTANCE;
      }

      public static boolean wasStarted() {
          return wasStarted;
      }

      public static void start() {
          if (!settings.containsKey("encrypted_key")) {
              throw new IllegalStateException(
                  CRUD_JT_Validation.errorMessage(CRUD_JT_Validation.ERROR_ENCRYPTED_KEY_NOT_SET)
              );
          }
          if (wasStarted) {
              throw new IllegalStateException(
                  CRUD_JT_Validation.errorMessage(CRUD_JT_Validation.ERROR_ALREADY_STARTED)
              );
          }

          String encrypted_key = (String) settings.get("encrypted_key");
          String store_jtPath = (String) settings.get("store_jt_path");

          String response = (String) lib.start_store_jt(encrypted_key, store_jtPath);
          Map<String, Object> result = new JSONObject(response).toMap();
          if (!(Boolean) result.get("ok")) {
              String code = (String) result.get("code");
              String msg = (String) result.get("error_message");

              throw CRUD_JT_Errors.createErrorByCode(code, msg);
          }

          wasStarted = true;
      }

      private static class ConfigHolder {
          private static final Config INSTANCE = new Config();
      }
}
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
}
