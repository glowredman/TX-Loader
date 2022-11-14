package glowredman.txloader;

import com.google.common.reflect.TypeToken;
import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;

class TXConfigHandler {

    private static File configFile;
    private static final Type TYPE = new TypeToken<List<Asset>>() {
        private static final long serialVersionUID = 1L;
    }.getType();

    static void load() {
        configFile = new File(TXLoaderCore.configDir, "config.json");

        if (!configFile.exists()) {
            try {
                FileUtils.write(configFile, TXLoaderCore.GSON.toJson(new ArrayList<>()), StandardCharsets.UTF_8);
            } catch (Exception e) {
                TXLoaderCore.LOGGER.error("Failed to create config file!", e);
            }
            return;
        }

        try {
            TXLoaderCore.REMOTE_ASSETS.addAll(
                    TXLoaderCore.GSON.fromJson(FileUtils.readFileToString(configFile, StandardCharsets.UTF_8), TYPE));
        } catch (Exception e) {
            TXLoaderCore.LOGGER.error("Failed to read config file!", e);
            return;
        }

        TXLoaderCore.LOGGER.info("Successfully read config file.");

        moveRLAssets();
    }

    static boolean save() {
        try {
            FileUtils.write(
                    configFile,
                    TXLoaderCore.GSON.toJson(
                            TXLoaderCore.REMOTE_ASSETS.parallelStream()
                                    .filter(a -> !a.addedByMod)
                                    .collect(Collectors.toList()),
                            TYPE),
                    StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            TXLoaderCore.LOGGER.error("Failed saving config!", e);
            return false;
        }
    }

    static void moveRLAssets() {
        File resources = new File(TXLoaderCore.mcLocation, "resources");
        File oresources = new File(TXLoaderCore.mcLocation, "oresources");

        if (resources.exists()) {
            TXLoaderCore.LOGGER.info("Attempting to move assets from ./resources/ to ./config/txloader/load/ ...");

            for (File f : resources.listFiles()) {
                try {
                    FileUtils.moveToDirectory(f, TXLoaderCore.resourcesDir, false);
                    TXLoaderCore.LOGGER.debug("Successfully moved " + f.getName() + " to ./config/txloader/load/");
                } catch (Exception e) {
                    TXLoaderCore.LOGGER.warn("Failed to move " + f.getName() + " to ./config/txloader/load/", e);
                }
            }

            try {
                resources.delete();
            } catch (Exception e) {
                TXLoaderCore.LOGGER.warn("Failed to delete ./resources/", e);
            }
        }

        if (oresources.exists()) {
            TXLoaderCore.LOGGER.info(
                    "Attempting to move assets from ./oresources/ to ./config/txloader/forceload/ ...");

            for (File f : oresources.listFiles()) {
                try {
                    FileUtils.moveToDirectory(f, TXLoaderCore.forceResourcesDir, false);
                    TXLoaderCore.LOGGER.debug("Successfully moved " + f.getName() + " to ./config/txloader/forceload/");
                } catch (Exception e) {
                    TXLoaderCore.LOGGER.warn("Failed to move " + f.getName() + " to ./config/txloader/forceload/", e);
                }
            }

            try {
                oresources.delete();
            } catch (Exception e) {
                TXLoaderCore.LOGGER.warn("Failed to delete ./oresources/", e);
            }
        }
    }

    static class Asset {

        String resourceLocation;
        String resourceLocationOverride;
        boolean forceLoad = false;
        String version = TXRemoteHandler.latestRelease;
        transient boolean addedByMod = false;

        Asset(String resourceLocation, String version) {
            this(resourceLocation, version, null, false);
        }

        Asset(String resourceLocation, String version, String resourceLocationOverride) {
            this(resourceLocation, version, resourceLocationOverride, false);
        }

        Asset(String resourceLocation, String version, boolean force) {
            this(resourceLocation, version, null, force);
        }

        Asset(String resourceLocation, String version, String resourceLocationOverride, boolean force) {
            this.resourceLocation = resourceLocation;
            this.resourceLocationOverride = resourceLocationOverride;
            this.forceLoad = force;
            this.version = version;
        }

        File getFile() {
            File path = this.forceLoad ? TXLoaderCore.forceResourcesDir : TXLoaderCore.resourcesDir;
            String resourceLocation =
                    this.resourceLocationOverride == null ? this.resourceLocation : this.resourceLocationOverride;
            return new File(path, resourceLocation);
        }
    }
}
