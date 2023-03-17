package glowredman.txloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.google.gson.JsonSyntaxException;

import glowredman.txloader.Asset.Source;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.ProgressManager.ProgressBar;

class RemoteHandler {

    static String latestRelease;
    static final Map<String, JVersion> VERSIONS = new LinkedHashMap<>();

    static boolean getVersions() {
        JVersionManifest manifest;

        try {
            manifest = downloadManifest();
        } catch (Exception e) {
            TXLoaderCore.LOGGER.error("Failed to get Minecraft versions!", e);
            return false;
        }

        latestRelease = manifest.latest.release;

        manifest.versions.forEach(JVersion::cache);

        TXLoaderCore.LOGGER.info("Successfully fetched Minecraft versions.");

        return true;
    }

    static void getAssets() {
        final Map<JVersionDetails, Map<String, JAsset>> assets = new HashMap<>();
        final Map<String, JVersionDetails> versionDetailsCache = new HashMap<>();
        final ProgressBar bar = ProgressManager.push("Loading Remote Assets", TXLoaderCore.REMOTE_ASSETS.size());
        int added = 0;
        int skipped = 0;
        int failed = 0;

        for (Asset asset : TXLoaderCore.REMOTE_ASSETS) {
            bar.step(asset.getResourceLocation());
            File file = asset.getFile();
            if (file.exists()) {
                skipped++;
                continue;
            }

            String version = asset.getVersion();

            JVersionDetails versionDetails = versionDetailsCache
                    .computeIfAbsent(version, RemoteHandler::downloadDetails);
            if (versionDetails == null) {
                failed++;
                continue;
            }

            Source source = asset.getSource();

            if (source == Source.ASSET) {
                JAsset jAsset = assets.computeIfAbsent(versionDetails, JVersionDetails::getAssets)
                        .get(asset.resourceLocation);

                if (jAsset == null) {
                    failed++;
                    continue;
                }

                try {
                    jAsset.download(file);
                } catch (Exception e) {
                    TXLoaderCore.LOGGER.error("Failed to get asset!", e);
                    failed++;
                }
                continue;
            }

            // asset from client/server jar:
            Path jarPath = source == Source.CLIENT ? JarHandler.CACHED_CLIENT_JARS.get(version)
                    : JarHandler.CACHED_SERVER_JARS.get(version);
            if (jarPath == null) {
                if (source == Source.CLIENT) {
                    try {
                        jarPath = versionDetails.downloads.client.downloadJar(version, "client.jar");
                        JarHandler.CACHED_CLIENT_JARS.put(version, jarPath);
                    } catch (Exception e) {
                        TXLoaderCore.LOGGER.error("Failed to download client jar and no cached jar was found", e);
                        failed++;
                        continue;
                    }
                } else {
                    try {
                        jarPath = versionDetails.downloads.server.downloadJar(version, "server.jar");
                        JarHandler.CACHED_SERVER_JARS.put(version, jarPath);
                    } catch (Exception e) {
                        TXLoaderCore.LOGGER.error("Failed to download server jar and no cached jar was found", e);
                        failed++;
                        continue;
                    }
                }
            }

            try {
                JarFile jarFile = new JarFile(jarPath.toFile());
                InputStream is = jarFile.getInputStream(jarFile.getJarEntry("assets/" + asset.resourceLocation));
                FileUtils.copyInputStreamToFile(is, file);
            } catch (Exception e) {
                TXLoaderCore.LOGGER.error("Failed to extract asset from jar!", e);
                failed++;
                continue;
            }

            TXLoaderCore.LOGGER.debug("Successfully fetched {}", asset.resourceLocation);
            added++;
        }
        TXLoaderCore.LOGGER.info("Successfully added {} assets. ({} skipped, {} failed)", added, skipped, failed);
        ProgressManager.pop(bar);
    }

    private static JVersionManifest downloadManifest() throws JsonSyntaxException, IOException {
        final URL manifestURL = new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json");
        return TXLoaderCore.GSON
                .fromJson(IOUtils.toString(manifestURL, StandardCharsets.UTF_8), JVersionManifest.class);
    }

    private static JVersionDetails downloadDetails(String version) {
        try {
            final URL versionURL = new URL(VERSIONS.get(version).url);
            return TXLoaderCore.GSON
                    .fromJson(IOUtils.toString(versionURL, StandardCharsets.UTF_8), JVersionDetails.class);
        } catch (Exception e) {
            TXLoaderCore.LOGGER.error("Failed to get version details", e);
            return null;
        }
    }

    /*
     * JSON templates
     */

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

        private void cache() {
            VERSIONS.put(this.id, this);
        }
    }

    private static class JVersionDetails {

        private JSourceDetails assetIndex;
        private JDownloads downloads;

        private Map<String, JAsset> getAssets() {
            try {
                final URL assetsURL = new URL(this.assetIndex.url);
                return TXLoaderCore.GSON
                        .fromJson(IOUtils.toString(assetsURL, StandardCharsets.UTF_8), JAssetIndex.class).objects;
            } catch (Exception e) {
                TXLoaderCore.LOGGER.error("Failed to get asset index", e);
                // don't check this version again...
                return new HashMap<>();
            }
        }
    }

    private static class JSourceDetails {

        private String url;

        private Path downloadJar(String version, String fileName) throws IOException {
            File dir = JarHandler.txloaderCache.resolve(version).toFile();
            dir.mkdirs();
            File jar = new File(dir, fileName);
            TXLoaderCore.LOGGER.info("Downloading {} to {}", this.url, jar);
            FileUtils.copyURLToFile(new URL(url), jar, 2000, 10000);
            return jar.toPath();
        }
    }

    private static class JDownloads {

        private JSourceDetails client;
        private JSourceDetails server;
    }

    private static class JAssetIndex {

        private Map<String, JAsset> objects;
    }

    private static class JAsset {

        private String hash;

        private void download(File file) throws IOException {
            URL url = this.getURL();
            file.getParentFile().mkdirs();
            TXLoaderCore.LOGGER.info("Downloading {} to {}", url, file);
            FileUtils.copyURLToFile(url, file, 2000, 10000);
        }

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
