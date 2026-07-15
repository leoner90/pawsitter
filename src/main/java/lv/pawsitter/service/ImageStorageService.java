package lv.pawsitter.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

//base image security
@Service
public class ImageStorageService
{
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

    private final Path uploadDirectory = Path.of("src/main/resources/static/images/sittersImages");

    public String saveSitterImage(MultipartFile image)
    {
        if (image == null || image.isEmpty())
        {
            return null;
        }

        if (image.getSize() > MAX_FILE_SIZE)
        {
            throw new IllegalArgumentException("Image cannot be larger than 5 MB");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(image.getContentType()))
        {
            throw new IllegalArgumentException("Only JPEG and PNG images are allowed");
        }

        try
        {
            Files.createDirectories(uploadDirectory);

            String extension = getExtension(image.getContentType());

            String fileName = UUID.randomUUID() + extension;

            Path filePath = uploadDirectory.resolve(fileName);

            Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "/images/sittersImages/" + fileName;
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("Failed to save profile image", exception);
        }
    }

    private String getExtension(String contentType)
    {
        return switch (contentType)
        {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> throw new IllegalArgumentException("Unsupported image type");
        };
    }


    //delete old image if there is one
    public void deleteSitterImage(String imageUrl)
    {
        if (imageUrl == null || imageUrl.isBlank()) { return; }

        if (imageUrl.endsWith("default-sitter.png")) { return; } // if default

        String fileName = Path.of(imageUrl).getFileName().toString();
        Path filePath = uploadDirectory.resolve(fileName);

        try
        {
            Files.deleteIfExists(filePath);
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("Failed to delete old profile image", exception);
        }
    }
}