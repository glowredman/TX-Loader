package glowredman.txloader;

import java.util.List;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class TXMinecraftTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!"net.minecraft.client.Minecraft".equals(transformedName)) {
            return basicClass;
        }
        return transformMinecraft(basicClass);
    }

    private static byte[] transformMinecraft(byte[] basicClass) {
        TXLoaderCore.LOGGER.info("Transforming net.minecraft.client.Minecraft");

        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(basicClass);
        classReader.accept(classNode, 0);

        // find refreshResources() method
        MethodNode targetMethod = null;
        final String refreshResourcesName = TXLoaderCore.SRGmappings ? "func_110436_a" : "refreshResources";
        final String refreshResourcesDesc = "()V";

        for (MethodNode method : classNode.methods) {
            if (method.name.equals(refreshResourcesName) && method.desc.equals(refreshResourcesDesc)) {
                targetMethod = method;
                break;
            }
        }

        if (targetMethod == null) throw new RuntimeException("Could not find method refreshResources()!");

        // find first invocation of IReloadableResourceManager.reloadResources()
        AbstractInsnNode targetInsn = null;
        final String reloadResourcesName = TXLoaderCore.SRGmappings ? "func_110541_a" : "reloadResources";
        final String reloadResourcesDesc = "(Ljava/util/List;)V";

        for (AbstractInsnNode ain : targetMethod.instructions.toArray()) {
            if (ain instanceof MethodInsnNode) {
                MethodInsnNode min = (MethodInsnNode) ain;
                if (min.name.equals(reloadResourcesName) && min.desc.equals(reloadResourcesDesc)) {
                    targetInsn = ain;
                    break;
                }
            }
        }

        if (targetInsn == null)
            throw new RuntimeException("Could not find invocation of reloadResources() in method refreshResources()!");

        // insert new instructions
        InsnList insertForcePackInsnList = new InsnList();
        insertForcePackInsnList.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "glowredman/txloader/TXMinecraftTransformer",
                "insertForcePack",
                "(Ljava/util/List;)V",
                false));
        insertForcePackInsnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
        targetMethod.instructions.insertBefore(targetInsn, insertForcePackInsnList);

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void insertForcePack(List resourcePackList) {
        resourcePackList.add(new TXResourcePack.Force());
    }
}
