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

//****** VARs
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private final Path sitterUploadDirectory = Path.of("src/main/resources/static/images/sittersImages");
    private final Path ownerUploadDirectory = Path.of("src/main/resources/static/images/ownersImages");
    private final Path petUploadDirectory = Path.of("src/main/resources/static/images/petsImages");

//Save Images
    public String saveSitterImage(MultipartFile image)
    {
        return saveImage(image, sitterUploadDirectory, "/images/sittersImages/");
    }

    public String saveOwnerImage(MultipartFile image)
    {
        return saveImage(image, ownerUploadDirectory, "/images/ownersImages/");
    }

    public String savePetImage(MultipartFile image)
    {
        return saveImage(image, petUploadDirectory, "/images/petsImages/");
    }

//Delete Images
    public void deleteSitterImage(String imageUrl) {deleteImage(imageUrl, sitterUploadDirectory, "default-sitter.png");}
    public void deleteOwnerImage(String imageUrl)
    {
        deleteImage(imageUrl, ownerUploadDirectory, "default-owner.png");
    }
    public void deletePetImage(String imageUrl) {deleteImage(imageUrl, petUploadDirectory, "default-pet.png");}

//validation
    private String getExtension(String contentType)
    {
        return switch (contentType)
        {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> throw new IllegalArgumentException("Unsupported image type");
        };
    }

//delete old image logic
    private void deleteImage(String imageUrl, Path uploadDirectory, String defaultImage)
    {
        if (imageUrl == null || imageUrl.isBlank()) { return; }
        if (imageUrl.endsWith(defaultImage)) { return; }

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

//Saving Image Logic
    private String saveImage(MultipartFile image, Path uploadDirectory, String imageUrl)
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
            return imageUrl + fileName;
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("Failed to save image", exception);
        }
    }


}