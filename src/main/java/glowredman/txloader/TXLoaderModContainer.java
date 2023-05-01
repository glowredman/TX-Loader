package glowredman.txloader;

import java.io.File;
import java.util.Collections;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.versioning.VersionParser;
import net.minecraftforge.fml.common.versioning.VersionRange;

public class TXLoaderModContainer extends DummyModContainer {

    public TXLoaderModContainer() {
        super(new ModMetadata());
        String version = TXLoaderModContainer.class.getPackage().getImplementationVersion();
        ModMetadata md = this.getMetadata();
        md.authorList = Collections.singletonList("glowredman");
        md.credits = "portablejim (Additional Resources mod)";
        md.description = "Loads official/custom assets";
        md.modId = "txloader";
        md.name = "TX Loader";
        md.updateJSON = "https://files.data-hole.de/mods/txloader/updates.json";
        md.url = "https://github.com/glowredman/TX-Loader";
        md.version = version == null ? "version not found" : version;
    }

    @Override
    public VersionRange acceptableMinecraftVersionRange() {
        return VersionParser.parseRange("[1.12.2]");
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
        event.registerServerCommand(new CommandTX());
    }
}
