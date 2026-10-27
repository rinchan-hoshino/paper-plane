package proof;
import java.nio.file.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.service.MixinService;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public final class Apply {
    public static void main(String[] args) throws Exception {
        System.setProperty("mixin.service", "proof.Service");
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.SERVER);
        var transformer = ((Service) MixinService.getService()).transformer();
        com.llamalad7.mixinextras.MixinExtrasBootstrap.init();
        Mixins.addConfiguration("native-proof.mixins.json");
        for (String name : args) {
            try (var in = ClassLoader.getSystemResourceAsStream(name.replace('.', '/') + ".class")) {
                if (in == null) throw new AssertionError(name);
                var bytes = in.readAllBytes();
                var transformed = transformer.transformClassBytes(name, name, bytes);
                if (java.util.Arrays.equals(bytes, transformed)) throw new AssertionError("Mixin did not transform " + name);
                var node = new ClassNode();
                new ClassReader(transformed).accept(node, 0);
                int hooks = 0;
                for (var method : node.methods) if (method.name.contains("paperPlane$")) hooks++;
                if (hooks == 0) throw new AssertionError("No paperPlane hook in " + name);
                Path output = Path.of(System.getProperty("proof.output", "."), name.replace('.', '/') + ".class");
                Files.createDirectories(output.getParent()); Files.write(output, transformed);
                System.out.println("APPLIED " + name + " hooks=" + hooks);
            }
        }
    }
}
