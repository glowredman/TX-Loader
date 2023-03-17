package glowredman.txloader;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.io.filefilter.DirectoryFileFilter;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ModContainer;

public class TXResourcePack implements IResourcePack {

    private final String name;
    private final Path dir;

    private TXResourcePack(String name, Path dir) {
        this.name = name;
        this.dir = dir;
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        return new FileInputStream(this.getResourcePath(location).toFile());
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        return Files.exists(this.getResourcePath(location));
    }

    @Override
    public Set<String> getResourceDomains() {
        if (TXLoaderCore.isRemoteReachable) {
            RemoteHandler.getAssets();
        }

        File[] subDirs = this.dir.toFile().listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
        Set<String> resourceDomains = new HashSet<>();
        for (File f : subDirs) {
            resourceDomains.add(f.getName());
        }
        return resourceDomains;
    }

    @Override
    @Nullable
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer metadataSerializer, String metadataSectionName) throws IOException {
        return null;
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        return null;
    }

    @Override
    public String getPackName() {
        return this.name;
    }

    private Path getResourcePath(ResourceLocation rl) {
        return this.dir.resolve(rl.getResourceDomain()).resolve(rl.getResourcePath());
    }

    public static class Normal extends TXResourcePack {

        public Normal(ModContainer modContainer) {
            super("TX Loader Resources", TXLoaderCore.resourcesDir.toPath());
            TXLoaderCore.resourcesDir.mkdir();
        }
    }

    public static class Force extends TXResourcePack {

        public Force() {
            super("TX Loader Forced Resources", TXLoaderCore.forceResourcesDir.toPath());
            TXLoaderCore.forceResourcesDir.mkdir();
        }
    }
}
