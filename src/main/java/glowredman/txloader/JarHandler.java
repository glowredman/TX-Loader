package glowredman.txloader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

class JarHandler {

    static final Map<String, Path> CACHED_CLIENT_JARS = new HashMap<>();
    static final Map<String, Path> CACHED_SERVER_JARS = new HashMap<>();

    static Path txloaderCache;

    static void indexJars() {
        String userHome = System.getProperty("user.home");
        txloaderCache = Paths.get(userHome, "txloader");

        List<Pair<Path, String>> clientLocations = new ArrayList<>();
        clientLocations.add(Pair.of(txloaderCache, "client.jar"));
        clientLocations.add(Pair.of(Paths.get(userHome, "AppData", "Roaming", ".minecraft", "versions"), "%s.jar"));
        clientLocations.add(
                Pair.of(
                        Paths.get(userHome, ".gradle", "caches", "forge_gradle", "minecraft_repo", "versions"),
                        "client.jar"));
        clientLocations.add(
                Pair.of(
                        Paths.get(userHome, ".gradle", "caches", "minecraft", "net", "minecraft", "minecraft"),
                        "minecraft-%s.jar"));
        clientLocations.add(
                Pair.of(Paths.get(userHome, ".gradle", "caches", "retro_futura_gradle", "mc-vanilla"), "client.jar"));

        List<Pair<Path, String>> serverLocations = new ArrayList<>();
        clientLocations.add(Pair.of(txloaderCache, "server.jar"));
        serverLocations.add(
                Pair.of(
                        Paths.get(userHome, ".gradle", "caches", "forge_gradle", "minecraft_repo", "versions"),
                        "server.jar"));
        serverLocations.add(
                Pair.of(
                        Paths.get(userHome, ".gradle", "caches", "minecraft", "net", "minecraft", "minecraft_server"),
                        "minecraft_server-%s.jar"));
        serverLocations.add(
                Pair.of(Paths.get(userHome, ".gradle", "caches", "retro_futura_gradle", "mc-vanilla"), "server.jar"));

        for (String version : RemoteHandler.VERSIONS.keySet()) {
            for (Pair<Path, String> location : clientLocations) {
                Path jarPath = location.getLeft().resolve(version).resolve(String.format(location.getRight(), version));
                if (Files.exists(jarPath)) {
                    CACHED_CLIENT_JARS.put(version, jarPath);
                    TXLoaderCore.LOGGER.debug("Found CLIENT jar for version {} at {}", version, jarPath);
                    break;
                }
            }
            for (Pair<Path, String> location : serverLocations) {
                Path jarPath = location.getLeft().resolve(version).resolve(String.format(location.getRight(), version));
                if (Files.exists(jarPath)) {
                    CACHED_SERVER_JARS.put(version, jarPath);
                    TXLoaderCore.LOGGER.debug("Found SERVER jar for version {} at {}", version, jarPath);
                    break;
                }
            }
        }
    }

}
