package glowredman.txloader;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.versioning.VersionParser;
import cpw.mods.fml.common.versioning.VersionRange;
import java.io.File;
import java.util.Collections;

public class TXLoaderModContainer extends DummyModContainer {

    public TXLoaderModContainer() {
        super(new ModMetadata());
        ModMetadata md = this.getMetadata();
        md.authorList = Collections.singletonList("glowredman");
        md.credits = "portablejim (Additional Resources mod)";
        md.description = "Loads official/custom assets";
        md.modId = "txloader";
        md.name = "TX Loader";
        md.url = "https://github.com/glowredman/TX-Loader";
        md.version = "GRADLETOKEN_VERSION";
    }

    @Override
    public VersionRange acceptableMinecraftVersionRange() {
        return VersionParser.parseRange("[1.7.10]");
    }

    @Override
    public Class<?> getCustomResourcePackClass() {
        return TXResourcePack.Normal.class;
    }

    @Override
    public File getSource() {
        return TXLoaderCore.modFile;
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }

    @Subscribe
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new TXCommand());
    }
}
