package ca.bkaw.torque.assets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.function.Predicate;

public class ResourcePack implements AutoCloseable {
    private static final Gson GSON = new Gson();

    private final @Nullable FileSystem zipFileSystem;
    private final @NotNull Path root;

    private ResourcePack(@Nullable FileSystem zipFileSystem, @NotNull Path root) {
        this.zipFileSystem = zipFileSystem;
        this.root = root;
    }

    /**
     * Load a resource pack from a zip file and use the root of the zip as the root of
     * the resource pack.
     * <p>
     * The zip file will be created if it does not exist.
     *
     * @param zipFile The path of the zip file to read.
     * @return The loaded or created resource pack.
     * @throws IOException If an I/O error occurs.
     */
    public static ResourcePack loadZip(Path zipFile) throws IOException {
        URI uri = URI.create("jar:" + zipFile.toUri());
        FileSystem fileSystem = FileSystems.newFileSystem(uri, Map.of("create", true));
        Path root = fileSystem.getPath(".").normalize();
        return new ResourcePack(fileSystem, root);
    }

    /**
     * Load a resource pack from a directory and use the directory as the root of
     * the resource pack.
     * <p>
     * The directory must exist when calling this method.
     *
     * @param directory The path of the directory.
     * @return The resource pack.
     * @throws IOException If an I/O error occurs.
     */
    public static ResourcePack loadDirectory(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("The provided path is not a directory: " + directory);
        }
        return new ResourcePack(null, directory.toAbsolutePath().normalize());
    }

    @Override
    public void close() throws IOException {
        if (this.zipFileSystem != null) {
            this.zipFileSystem.close();
        }
    }

    /**
     * Get the root path of this pack.
     *
     * @return The root path.
     */
    public @NotNull Path getRoot() {
        return this.root;
    }

    /**
     * Get a path within the pack.
     * <p>
     * Example: {@code pack.getPath("pack.mcmeta");}
     * <p>
     * Note that the returned path may belong to a file system that
     * has a root that isn't the resource pack root, so it is not safe
     * to call {@code resolve} with a string starting with a slash.
     *
     * @param path The path to get. Leading slashes will be removed.
     * @return The path within the pack.
     */
    public Path getPath(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return this.root.resolve(path);
    }

    /**
     * Create the pack by creating the {@code pack.mcmeta} file.
     *
     * @param description A description of the pack.
     * @param packFormat The pack format to use.
     * @throws IllegalStateException If the pack already exists.
     * @throws IOException If an I/O error occurs.
     */
    public void create(String description, int packFormat) throws IOException {
        Path path = this.getPath("pack.mcmeta");

        if (Files.exists(path)) {
            throw new IllegalArgumentException("Tried to create a pack but one " +
                "already existed. The pack already has a pack.mcmeta file.");
        }

        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();
        root.add("pack", pack);
        pack.addProperty("pack_format", packFormat);
        pack.addProperty("description", description);
        Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    /**
     * Include all resources from the other pack in to this pack.
     *
     * @param other The pack to include resources from.
     * @param filter Determines which resources should be included and not.
     * @throws IOException If something goes wrong while copying the files, or if there
     * is a collision.
     */
    public void include(@NotNull ResourcePack other, @Nullable Predicate<String> filter) throws IOException {
        Path thisRoot = this.getRoot();
        Path otherRoot = other.getRoot();
        Files.walkFileTree(otherRoot, new SimpleFileVisitor<>() {
            @Override
            @NotNull
            public FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
                String relative = otherRoot.relativize(dir).toString();
                if (filter != null && !filter.test(relative)) return FileVisitResult.CONTINUE;

                Files.createDirectories(thisRoot.resolve(relative));
                return FileVisitResult.CONTINUE;
            }

            @Override
            @NotNull
            public FileVisitResult visitFile(@NotNull Path otherFile, @NotNull BasicFileAttributes attrs) throws IOException {
                String relative = otherRoot.relativize(otherFile).toString();
                if (filter != null && !filter.test(relative)) return FileVisitResult.CONTINUE;

                Path thisFile = thisRoot.resolve(relative);
                if (Files.exists(thisFile)) {
                    // The file already exists, we have a collision
                    throw new IOException("Resource collision: " + relative);
                } else {
                    Files.copy(otherFile, thisFile);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
