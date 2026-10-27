package proof;

import java.io.*;
import java.net.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.launch.platform.container.*;
import org.spongepowered.asm.mixin.transformer.*;

public final class Service extends MixinServiceAbstract implements IClassProvider, IClassBytecodeProvider {
    public String getName() { return "PaperPlaneNativeProof"; }
    public boolean isValid() { return true; }
    public MixinEnvironment.Phase getInitialPhase() { return MixinEnvironment.Phase.DEFAULT; }
    public IClassProvider getClassProvider() { return this; }
    public IClassBytecodeProvider getBytecodeProvider() { return this; }
    public ITransformerProvider getTransformerProvider() { return null; }
    public IClassTracker getClassTracker() { return null; }
    public IMixinAuditTrail getAuditTrail() { return null; }
    public Collection<String> getPlatformAgents() { return List.of(); }
    public IContainerHandle getPrimaryContainer() { return new ContainerHandleURI(new File(".").toURI()); }
    public InputStream getResourceAsStream(String name) { return ClassLoader.getSystemResourceAsStream(name); }
    public URL[] getClassPath() { return Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator)).map(p -> { try { return new File(p).toURI().toURL(); } catch (Exception e) {throw new RuntimeException(e);} }).toArray(URL[]::new); }
    public Class<?> findClass(String name) throws ClassNotFoundException { return findClass(name, false); }
    public Class<?> findClass(String name, boolean init) throws ClassNotFoundException { return Class.forName(name, init, ClassLoader.getSystemClassLoader()); }
    public Class<?> findAgentClass(String name, boolean init) throws ClassNotFoundException { return findClass(name, init); }
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException { return getClassNode(name, false); }
    public ClassNode getClassNode(String name, boolean transform) throws ClassNotFoundException, IOException { return getClassNode(name, transform, 0); }
    public ClassNode getClassNode(String name, boolean transform, int flags) throws ClassNotFoundException, IOException {
        try (var in = getResourceAsStream(name.replace('.', '/') + ".class")) {
            if (in == null) throw new ClassNotFoundException(name);
            var node = new ClassNode();
            new ClassReader(in).accept(node, flags);
            return node;
        }
    }
    public IMixinTransformer transformer() { return getInternal(IMixinTransformerFactory.class).createTransformer(); }
}
