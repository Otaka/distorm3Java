package diStorm3.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author sad
 */
public class NativeLibLoader {

    /**
    Folder in which this loader will unpack library
     */
    private File unpackedLibraryPath;

    public NativeLibLoader() {
        unpackedLibraryPath = new File(System.getProperty("java.io.tmpdir"));
    }

    public void setUnpackedLibraryFolder(File unpackedLibraryFolder) {
        this.unpackedLibraryPath = unpackedLibraryFolder;
    }

    /**
    Accept path to resource where native library is stored<br>
    Path can contain placeholder {PLATFORM} that may receive [32] or [64] depending on current machine platform
     */
    public void loadLibraryFromResource(String resourcePath) throws IOException {
        String replacedPath = replacePlaceholders(resourcePath);
        String md5;
        try (InputStream stream = NativeLibLoader.class.getResourceAsStream(replacedPath)) {
            if (stream == null) {
                throw new IllegalArgumentException("Cannot find native library in resources [" + resourcePath + "]");
            }

            md5 = getMD5ChecksumOfInputStream(stream);
        }

        String fileName = getFileName(replacedPath);
        File tmpFile = new File(unpackedLibraryPath, fileName);
        File libraryFile=null;
        if (!tmpFile.exists()) {
            libraryFile = copyFileFromResources(tmpFile, replacedPath);
        }else{
            String fileMd5=getMD5CheckSumOfFile(tmpFile);
            if(fileMd5.equals(md5)){
                libraryFile=tmpFile;
            }else{
                libraryFile = copyFileFromResources(tmpFile, replacedPath);
            }
        }
        
        System.load(libraryFile.getAbsolutePath());
    }
    
    

    private File copyFileFromResources(File destinationPath, String resourcePath) throws IOException {
        OutputStream outputStream;
        try (InputStream stream = NativeLibLoader.class.getResourceAsStream(resourcePath)) {
            try {
                outputStream = new FileOutputStream(destinationPath);
            } catch (FileNotFoundException ex) {
                throw new IllegalArgumentException("Cannot write native library to file [" + destinationPath + "]", ex);
            }

            outputStream = new BufferedOutputStream(outputStream);
            while (true) {
                int value = stream.read();
                if (value == -1) {
                    break;
                }

                outputStream.write(value);
            }

            outputStream.close();
        }

        return destinationPath;
    }

    private String getFileName(String path) {
        path = path.replace("\\", "/");
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash == -1) {
            return path;
        }
        return path.substring(lastSlash + 1);
    }

    private String replacePlaceholders(String path) {
        if (path.contains("{PLATFORM}")) {
            String bitness = System.getProperty("sun.arch.data.model");
            path = path.replace("{PLATFORM}", bitness);
        }

        return path;
    }

    private byte[] createChecksum(InputStream stream) throws IOException {
        byte[] buffer = new byte[1024];
        MessageDigest complete;
        try {
            complete = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Cannot find MD5 message digester", ex);
        }
        int numRead;

        do {
            numRead = stream.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        return complete.digest();
    }

    private  String getMD5ChecksumOfInputStream(InputStream stream) throws IOException {
        byte[] b;
        b = createChecksum(stream);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            result.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));
        }

        return result.toString();
    }
    
    private String getMD5CheckSumOfFile(File file) throws IOException{
        try (InputStream stream = new FileInputStream(file)) {
            String md5=getMD5ChecksumOfInputStream(stream);
            return md5;
        } catch (FileNotFoundException ex) {
            throw new IllegalArgumentException("Cannot find file ["+file.getAbsolutePath()+"]",ex);
        }
    }
    
}
