package edu.udg.tfg.FileManagement.utils;

import edu.udg.tfg.FileManagement.entities.FileEntity;
import edu.udg.tfg.FileManagement.entities.FolderEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

    public static Resource createZipFromElements(List<Object> elements, FileUtil fileUtil) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Object element : elements) {
                if (element instanceof FolderEntity) {
                    FolderEntity folder = (FolderEntity) element;
                    addFolderToZip(folder, folder.getName(), zos, fileUtil);
                } else if (element instanceof FileEntity) {
                    FileEntity file = (FileEntity) element;
                    addFileToZip(file, file.getName(), zos, fileUtil);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error creating ZIP", e);
        }

        return new ByteArrayResource(baos.toByteArray()) {
            @Override
            public String getFilename() { return "download.zip"; }
        };
    }

    private static void addFileToZip(FileEntity file, String pathInZip, ZipOutputStream zos, FileUtil fileUtil) throws IOException {
        Resource resource = fileUtil.loadAsResource(file.getId());
        if (resource.exists() && resource.isReadable()) {
            ZipEntry zipEntry = new ZipEntry(pathInZip);
            zos.putNextEntry(zipEntry);
            try (InputStream inputStream = resource.getInputStream()) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
            }
            zos.closeEntry();
        }
    }

    private static void addFolderToZip(FolderEntity folder, String pathInZip, ZipOutputStream zos, FileUtil fileUtil) throws IOException {
        String folderPath = pathInZip.endsWith("/") ? pathInZip : pathInZip + "/";

        if (folder.getChildren().isEmpty() && folder.getFiles().isEmpty()) {
            zos.putNextEntry(new ZipEntry(folderPath));
            zos.closeEntry();
            return;
        }
        for (FileEntity file : folder.getFiles()) {
            addFileToZip(file, folderPath + file.getName(), zos, fileUtil);
        }
        for (FolderEntity subFolder : folder.getChildren()) {
            addFolderToZip(subFolder, folderPath + subFolder.getName(), zos, fileUtil);
        }
    }
}
