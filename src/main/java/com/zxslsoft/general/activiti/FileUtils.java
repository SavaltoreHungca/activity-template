package com.zxslsoft.general.activiti;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Set;
import java.util.regex.Pattern;

@SuppressWarnings("all")
public class FileUtils {


    public static byte[] getFileBytes(String path) {
        File file = new File(path);
        try (
                FileInputStream inputStream = new FileInputStream(file);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {

            FileChannel channel = inputStream.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            for (int len; (len = channel.read(buffer)) != -1; buffer.clear()) {
                outputStream.write(buffer.array(), 0, len);
            }
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("读取文件失败");
        }
    }

    public static byte[] getBytes(InputStream inputStream){
        try (
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {

            byte[] buffer = new byte[1024];
            for (int len; (len = inputStream.read(buffer)) != -1;) {
                outputStream.write(buffer, 0, len);
            }
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("读取输入流失败");
        }
    }

    public static void saveFile(byte[] bytes, String path, String fileName) {
        mkdirs(path);
        try (FileOutputStream fileOutputStream = new FileOutputStream(join(path, fileName))) {
            fileOutputStream.write(bytes);
            fileOutputStream.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveFile(byte[] bytes, String filePath) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
            fileOutputStream.write(bytes);
            fileOutputStream.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean exists(String path) {
        return new File(path).exists();
    }

    public static void touch(String path) {
        try {
            if (exists(path)) return;
            new File(path).createNewFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mkdirs(String path) {
        if (exists(path)) return;
        new File(path).mkdirs();
    }

    public static String join(String... paths) {
        String uri = paths[0];

        for (int i = 1; i < paths.length; i++) {
            String path = paths[i];
            if (uri.endsWith("/")) {
                if (path.startsWith("/")) {
                    uri = uri + path.replaceFirst("/", "");
                } else {
                    uri = uri + path;
                }
                continue;
            }
            if (uri.endsWith("\\")) {
                if (path.startsWith("\\")) {
                    uri = uri + path.replaceFirst("\\\\", "");
                } else {
                    uri = uri + path;
                }
                continue;
            }
            if (path.startsWith("/") || path.startsWith("\\")) {
                uri = uri + path;
            } else {
                uri = uri + "/" + path;
            }
        }

        return uri;
    }

    // 如 /quaterz.properties
    public static InputStream getClassPathResource(String classpath) {
        ClassPathResource classPathResource = new ClassPathResource(classpath);
        try {
            return classPathResource.getInputStream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void append(String filePath, byte[] content){
        try{
            if (!exists(filePath)){
                touch(filePath);
            }
            FileOutputStream fileOutputStream = new FileOutputStream(filePath, true);
            fileOutputStream.write(content);
            fileOutputStream.flush();
            fileOutputStream.close();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static String getUserHome(){
        return System.getProperty("user.home");
    }

    public static Set<String> getResources(String dir, String pattern){
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forResource(dir))
                .setScanners(new ResourcesScanner()));
        return reflections.getResources(Pattern.compile(pattern));
    }

    public static void main(String[] args) {
    }
}
