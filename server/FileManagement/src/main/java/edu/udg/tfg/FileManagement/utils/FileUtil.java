package edu.udg.tfg.FileManagement.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import com.google.common.hash.Hashing;
import java.io.InputStream;
@Component
public class FileUtil {

    @Value("${file.storage.path}")
    private String storagePath;

//    public void storeFile(UUID fileId, MultipartFile file) throws IOException {
//        String fileName = fileId.toString(); // Rename file to its ID
//        Path filePath = load(fileName);
//        Files.copy(file.getInputStream(), filePath);
//    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(storagePath));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not initialize storage location");
        }
    }
    public void storeFile(UUID fileId, MultipartFile file) throws IOException {
        String fileName = fileId.toString(); 
        Path filePath = load(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
    }

    public void storeFile(UUID fileId, InputStream inputStream) throws IOException {
        String fileName = fileId.toString();
        Path filePath = load(fileName);
        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
    }


    public byte[] getFileDataById(UUID fileId) throws IOException {
        String fileName = fileId.toString();
        Path filePath = load(fileName);
        return Files.readAllBytes(filePath);
    }

    public Path load(String fileName) {
        return Paths.get(storagePath, fileName);
    }

    public Resource loadAsResource(UUID fileId) {
        try {
            Path file = load(fileId.toString());
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }
        }
        catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read file");
        }
    }

    public void copyFile(UUID sourceFileId, UUID destinationFileId) throws IOException {
        Path sourcePath = load(sourceFileId.toString());
        Path destinationPath = load(destinationFileId.toString());
        
        if(sourcePath.toFile().toPath().equals(destinationPath.toFile().toPath())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination files are the same");
        }
        if(!sourcePath.toFile().exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source file does not exist");
        }
        Files.copy(sourcePath.toFile().toPath(), destinationPath.toFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public void deleteFile(UUID fileId) throws IOException {
        Path filePath = load(fileId.toString());
        Files.deleteIfExists(filePath);
    }

    public String getHash(UUID fileId) {
        Path file = load(fileId.toString());
        try {
            return Hashing.sha256().hashBytes(Files.readAllBytes(file)).toString();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read file");
        }
    }
}
