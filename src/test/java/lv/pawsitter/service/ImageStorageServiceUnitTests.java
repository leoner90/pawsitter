package lv.pawsitter.service;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ImageStorageServiceUnitTests {
    private final ImageStorageService imageStorageService = new ImageStorageService();

    private static final Path[] DIRS = {
            Path.of("src/main/resources/static/images/sittersImages"),
            Path.of("src/main/resources/static/images/ownersImages"),
            Path.of("src/main/resources/static/images/petsImages")
    };

    @AfterEach
    void cleanUp() throws IOException {
        for (Path dir : DIRS) {
            if (Files.exists(dir)) {
                try (Stream<Path> walk = Files.walk(dir)) {
                    walk.sorted(Comparator.reverseOrder())
                            .filter(p -> !p.equals(dir))
                            .forEach(p -> p.toFile().delete());
                }
            }
        }
    }

    @Test
    void saveSitterImage_returnsNull_whenFileIsNull() {
        assertThat(imageStorageService.saveSitterImage(null)).isNull();
    }

    @Test
    void saveSitterImage_returnsNull_whenFileIsEmpty() {
        MockMultipartFile empty = new MockMultipartFile("image", "empty.png", "image/png", new byte[0]);
        assertThat(imageStorageService.saveSitterImage(empty)).isNull();
    }

    @Test
    void saveSitterImage_savesFile_andReturnsUrl_whenValidPng() {
        MockMultipartFile file = new MockMultipartFile("image", "photo.png", "image/png", "content".getBytes());

        String url = imageStorageService.saveSitterImage(file);

        assertThat(url).startsWith("/images/sittersImages/");
        assertThat(url).endsWith(".png");
    }

    @Test
    void saveOwnerImage_savesFile_andReturnsUrl_whenValidJpeg() {
        MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", "content".getBytes());

        String url = imageStorageService.saveOwnerImage(file);

        assertThat(url).startsWith("/images/ownersImages/");
        assertThat(url).endsWith(".jpg");
    }

    @Test
    void savePetImage_savesFile_andReturnsUrl_whenValid() {
        MockMultipartFile file = new MockMultipartFile("image", "pet.jpg", "image/jpeg", "content".getBytes());

        String url = imageStorageService.savePetImage(file);

        assertThat(url).startsWith("/images/petsImages/");
    }

    @Test
    void saveImage_throwsIllegalArgumentException_whenFileTooLarge() {
        byte[] bigContent = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("image", "big.png", "image/png", bigContent);

        assertThatThrownBy(() -> imageStorageService.saveSitterImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    void saveImage_throwsIllegalArgumentException_whenContentTypeNotAllowed() {
        MockMultipartFile file = new MockMultipartFile("image", "doc.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> imageStorageService.saveSitterImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JPEG and PNG");
    }

    @Test
    void deleteSitterImage_doesNothing_whenUrlIsNull() {
        imageStorageService.deleteSitterImage(null);
    }

    @Test
    void deleteSitterImage_doesNothing_whenUrlIsBlank() {
        imageStorageService.deleteSitterImage("  ");
    }

    @Test
    void deleteSitterImage_doesNothing_whenUrlIsDefaultImage() {
        imageStorageService.deleteSitterImage("/images/sittersImages/default-sitter.png");
    }

    @Test
    void deleteOwnerImage_doesNothing_whenUrlIsDefaultImage() {
        imageStorageService.deleteOwnerImage("/images/ownersImages/default-owner.png");
    }

    @Test
    void deletePetImage_doesNothing_whenUrlIsDefaultImage() {
        imageStorageService.deletePetImage("/images/petsImages/default-pet.png");
    }

    @Test
    void deleteSitterImage_removesFile_whenItExists() {
        MockMultipartFile file = new MockMultipartFile("image", "photo.png", "image/png", "content".getBytes());
        String url = imageStorageService.saveSitterImage(file);

        imageStorageService.deleteSitterImage(url);

        String fileName = url.substring(url.lastIndexOf('/') + 1);
        assertThat(Files.exists(DIRS[0].resolve(fileName))).isFalse();
    }

    @Test
    void deleteOwnerImage_doesNotThrow_whenFileDoesNotExist() {
        imageStorageService.deleteOwnerImage("/images/ownersImages/nonexistent-file.png");
    }


}
