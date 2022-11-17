package glowredman.txloader;

import glowredman.txloader.TXConfigHandler.Asset;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

class TXRemoteHandler {

    static String latestRelease;
    static final Map<String, String> VERSIONS = new LinkedHashMap<>();

    static boolean getVersions() {
        JVersionManifest manifest;

        try {
            manifest = TXLoaderCore.GSON.fromJson(
                    IOUtils.toString(
                            new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json"),
                            StandardCharsets.UTF_8),
                    JVersionManifest.class);
        } catch (Exception e) {
            TXLoaderCore.LOGGER.error("Failed to get Minecraft versions!", e);
            return false;
        }

        latestRelease = manifest.latest.release;

        for (JVersion version : manifest.versions) {
            VERSIONS.put(version.id, version.url);
        }

        TXLoaderCore.LOGGER.info("Successfully fetched Minecraft versions.");

        return true;
    }

    static void getAssets() {
        Map<String, Map<String, JAsset>> assets = new HashMap<>();
        int added = 0;
        int skipped = 0;
        int failed = 0;

        for (Asset asset : TXLoaderCore.REMOTE_ASSETS) {
            File file = asset.getFile();
            if (file.exists()) {
                skipped++;
                continue;
            }

            if (!assets.containsKey(asset.version)) {
                String url = VERSIONS.get(asset.version);
                if (url == null) {
                    failed++;
                    continue;
                }
                try {
                    String assetsURL = TXLoaderCore.GSON.fromJson(
                                    IOUtils.toString(new URL(url), StandardCharsets.UTF_8), JVersionDetails.class)
                            .assetIndex
                            .url;
                    assets.put(
                            asset.version,
                            TXLoaderCore.GSON.fromJson(
                                            IOUtils.toString(new URL(assetsURL), StandardCharsets.UTF_8),
                                            JObjects.class)
                                    .objects);
                } catch (Exception e) {
                    TXLoaderCore.LOGGER.error("Failed to get asset information!", e);
                    assets.put(asset.version, new HashMap<>()); // don't check this version again...
                    failed++;
                    continue;
                }
            }
            JAsset jasset = assets.get(asset.version).get(asset.resourceLocation);
            if (jasset == null) {
                failed++;
                continue;
            }

            byte[] data;
            try {
                data = IOUtils.toByteArray(jasset.getURL());
            } catch (Exception e) {
                TXLoaderCore.LOGGER.error("Failed to get asset!", e);
                failed++;
                continue;
            }
            file.getParentFile().mkdirs();
            try {
                FileUtils.writeByteArrayToFile(file, data);
            } catch (Exception e) {
                TXLoaderCore.LOGGER.error("Failed to save asset!", e);
                failed++;
            }
            TXLoaderCore.LOGGER.debug("Successfully fetched {}", asset.resourceLocation);
            added++;
        }
        TXLoaderCore.LOGGER.info("Successfully added {} assets. ({} skipped, {} failed)", added, skipped, failed);
    }

    private static class JVersionManifest {

        private JLatest latest;
        private List<JVersion> versions;
    }

    private static class JLatest {

        private String release;
    }

    private static class JVersion {

        private String id;
        private String url;
    }

    private static class JVersionDetails {

        private JAssetIndex assetIndex;
    }

    private static class JAssetIndex {

        private String url;
    }

    private static class JObjects {

        private Map<String, JAsset> objects;
    }

    private static class JAsset {

        private String hash;

        private URL getURL() throws MalformedURLException {
            StringBuilder sb = new StringBuilder(84);
            sb.append("https://resources.download.minecraft.net/");
            sb.append(this.hash, 0, 2);
            sb.append('/');
            sb.append(this.hash);
            return new URL(sb.toString());
        }
    }
}
