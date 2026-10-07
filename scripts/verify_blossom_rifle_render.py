"""Check Blossom Rifle reload and actual ordinary/emissive geometry using this module's actual GeckoLib (Python + JDK 25).

Run from any directory: python scripts/verify_blossom_rifle_render.py
"""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
ANIMATION = ROOT / "src/main/resources/assets/jeg/geckolib/animations/item/blossom_rifle.animation.json"
INIT = """allprojects { afterEvaluate {
    tasks.register('writeBlossomCheckClasspath') { doLast {
        file(project.property('blossomClasspath')).text = sourceSets.client.runtimeClasspath.asPath
    } }
} }
"""
JAVA = r"""
import java.nio.file.*;
import com.geckolib.loading.definition.animation.ActorAnimations;
import com.geckolib.loading.math.MathParser;
import com.geckolib.cache.animation.Animation;
import com.geckolib.animation.AnimationProcessor;
import com.geckolib.animation.state.*;
import com.geckolib.animation.object.LoopType;

class BlossomReloadCheck {

    static void checkGlow(Path path) throws Exception {
        var model = com.geckolib.loading.definition.geometry.Geometry.GSON
                .fromJson(Files.readString(path), com.geckolib.loading.definition.geometry.Geometry.class)
                .bake(net.minecraft.resources.Identifier.fromNamespaceAndPath("jeg", "item/gun/blossom_rifle"));
        var silencer = model.getBone("silencer").orElseThrow();
        var snapshot = BoneSnapshot.create(silencer);
        var pass = new GlowPass(model);
        var draw = Class.forName("ttv.migami.jeg.client.render.gun.layer.GunAttachmentLayer")
                .getDeclaredMethod("renderGlowBone", com.geckolib.cache.model.GeoBone.class,
                        com.geckolib.renderer.base.RenderPassInfo.class,
                        com.mojang.blaze3d.vertex.VertexConsumer.class, int.class);
        draw.setAccessible(true);
        snapshot.apply();
        int visible = vertices(silencer, pass, draw, true);
        if (visible <= 0) throw new AssertionError("Silencer flowers must have visible glow geometry");
        snapshot.skipRender(true).skipChildrenRender(true).apply();
        if (vertices(silencer, pass, draw, false) != 0)
            throw new AssertionError("Hidden silencer subtree emitted ordinary geometry");
        int hiddenGlow = vertices(silencer, pass, draw, true);
        if (hiddenGlow != 0) throw new AssertionError("Hidden silencer subtree emitted " + hiddenGlow + " glow vertices");
        int body = vertices(model.getBone("gun_body").orElseThrow(), pass, draw, true);
        if (body <= 0) throw new AssertionError("Gun body flowers lost their glow");
        snapshot.skipRender(false).skipChildrenRender(false).apply();
        if (vertices(silencer, pass, draw, true) != visible)
            throw new AssertionError("Restored silencer lost its glow geometry");
        System.out.println("Blossom glow passed: hidden ordinary/glow vertices=0, restored=" + visible + ", body=" + body);
    }
    static int vertices(com.geckolib.cache.model.GeoBone bone, GlowPass pass,
                        java.lang.reflect.Method draw, boolean glow) throws Exception {
        int[] count = {0};
        var buffer = (com.mojang.blaze3d.vertex.VertexConsumer) java.lang.reflect.Proxy.newProxyInstance(
                BlossomReloadCheck.class.getClassLoader(),
                new Class<?>[]{com.mojang.blaze3d.vertex.VertexConsumer.class}, (proxy, method, args) -> {
                    if (method.getName().equals("addVertex")) count[0]++;
                    return method.getReturnType() == com.mojang.blaze3d.vertex.VertexConsumer.class ? proxy : null;
                });
        if (glow) draw.invoke(null, bone, pass, buffer, 0x00F000F0);
        else bone.positionAndRender(pass, buffer, 0x00F000F0, 0, -1);
        return count[0];
    }
    static class GlowPass extends com.geckolib.renderer.base.RenderPassInfo<com.geckolib.renderer.base.GeoRenderState> {
        GlowPass(com.geckolib.cache.model.BakedGeoModel model) {
            super(null, () -> new java.util.HashMap<>(), new com.mojang.blaze3d.vertex.PoseStack(), model, null, true);
        }
        @Override public int packedOverlay() { return 0; }
        @Override public int renderColor() { return -1; }
    }
    static Animation reload;
    static float value(int bone, double time, AnimationPoint.Transform transform, AnimationPoint.Axis axis) {
        AnimationPoint point = AnimationPoint.createFor(reload, null, LoopType.PLAY_ONCE, time);
        ControllerState state = new ControllerState(point, null, -1, 0, false, null, null, null);
        return AnimationProcessor.findAnimationPointValue(null, state, point, null, bone, transform, axis, null);
    }
    static void equal(float actual, float expected, String label) {
        if (!Float.isFinite(actual) || Math.abs(actual - expected) > 0.0001F)
            throw new AssertionError(label + ": " + actual + " != " + expected);
    }
    public static void main(String[] args) throws Exception {
        var file = ActorAnimations.GSON.fromJson(Files.readString(Path.of(args[0])), ActorAnimations.class);
        reload = file.animations().get("reload").bake("reload", MathParser.create());
        int magazine = -1, bones = 0, samples = 0;
        for (int i = 0; i < reload.boneAnimations().length; i++) {
            String name = reload.boneAnimations()[i].boneName();
            if (!name.equals("magazine") && !name.matches("flower\\d*")) continue;
            if (name.equals("magazine")) magazine = i;
            bones++;
            for (double time = 0; time <= reload.length(); time += 0.005) {
                for (var axis : AnimationPoint.Axis.values()) {
                    float scale = value(i, time, AnimationPoint.Transform.SCALE, axis);
                    if (!Float.isFinite(scale) || scale < -0.00001F || scale > 1.00001F)
                        throw new AssertionError(name + " at " + time + " " + axis + ": " + scale);
                    samples++;
                }
            }
            for (double time : new double[]{0, 0.1, 0.3749})
                equal(value(i, time, AnimationPoint.Transform.SCALE, AnimationPoint.Axis.X), 1, name + " initial hold");
        }
        if (bones != 18 || magazine < 0) throw new AssertionError("Expected magazine and 17 flower bones");
        for (double time : new double[]{0, 0.1, 0.3749, 0.375})
            equal(value(magazine, time, AnimationPoint.Transform.TRANSLATION, AnimationPoint.Axis.Y), 0, "Magazine initial position");
        equal(value(magazine, 0.75, AnimationPoint.Transform.TRANSLATION, AnimationPoint.Axis.Y), -13, "Magazine drop");
        for (var axis : AnimationPoint.Axis.values()) {
            equal(value(magazine, 0.75, AnimationPoint.Transform.SCALE, axis), 1, "Before disappearance");
            equal(value(magazine, 0.7917, AnimationPoint.Transform.SCALE, axis), 0, "Disappeared");
            equal(value(magazine, 2.9167, AnimationPoint.Transform.SCALE, axis), 0, "Before insertion");
            equal(value(magazine, 3.0, AnimationPoint.Transform.SCALE, axis), 0.5F, "Insertion transition");
            equal(value(magazine, 3.0833, AnimationPoint.Transform.SCALE, axis), 1, "Inserted");
            equal(value(magazine, 4.7917, AnimationPoint.Transform.SCALE, axis), 1, "Reload complete");
        }
        checkGlow(Path.of(args[1]));
        System.out.println("Blossom reload passed: " + samples + " GeckoLib scale samples, initial hold, drop, disappearance and insertion");
    }
}
"""

with tempfile.TemporaryDirectory(prefix="jeg-blossom-") as directory:
    temporary = Path(directory)
    init = temporary / "classpath.init.gradle"
    init.write_text(INIT, encoding="utf-8")
    classpath = temporary / "classpath.txt"
    wrapper = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    subprocess.run([str(wrapper), "writeBlossomCheckClasspath", "--console=plain",
                    "--no-configuration-cache", "--init-script", str(init),
                    "-PblossomClasspath=" + str(classpath)], cwd=ROOT, check=True)
    source = temporary / "BlossomReloadCheck.java"
    source.write_text(JAVA, encoding="utf-8")
    arguments = temporary / "java.args"
    arguments.write_text("\n".join('"' + value.replace("\\", "/") + '"'
                                  for value in ["-cp", classpath.read_text(), str(source), str(ANIMATION),
                                                str(ROOT / "src/main/resources/assets/jeg/geckolib/models/item/gun/blossom_rifle.geo.json")]),
                         encoding="utf-8")
    subprocess.run(["java", "@" + str(arguments)], cwd=ROOT, check=True)
