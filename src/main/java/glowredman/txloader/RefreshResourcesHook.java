package glowredman.txloader;

import java.util.List;

import net.minecraft.client.resources.IResourcePack;

public class RefreshResourcesHook {

    public static void insertForcePack(List<IResourcePack> resourcePackList) {
        resourcePackList.add(new TXResourcePack.Force());
    }

}
