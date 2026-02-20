package ttv.migami.jeg.client.render.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.HumanoidArm;

public final class HandRenderInvoker {
    private HandRenderInvoker() {}

    public static boolean renderHand(
            Minecraft minecraft,
            AbstractClientPlayer player,
            PoseStack poseStack,
            Object bufferSource,
            int packedLight,
            HumanoidArm arm,
            boolean sleeveVisible
    ) {
        try {
            Object dispatcher = minecraft.getEntityRenderDispatcher();
            Object renderer = invokeBest(dispatcher, new String[] { "getPlayerRenderer" }, player);
            if (renderer == null) {
                renderer = invokeBest(dispatcher, new String[] { "getRenderer" }, player);
            }
            if (renderer == null) {
                return false;
            }

            Object skin = resolveSkinTexture(player);
            if (skin == null) {
                return false;
            }

            String methodName = arm == HumanoidArm.LEFT ? "renderLeftHand" : "renderRightHand";
            Method[] methods = renderer.getClass().getMethods();
            for (Method method : methods) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }

                int count = method.getParameterCount();
                if (count != 5 && count != 6) {
                    continue;
                }

                Object[] args = count == 5
                        ? new Object[] { poseStack, bufferSource, packedLight, skin, sleeveVisible }
                        : new Object[] { poseStack, bufferSource, packedLight, skin, sleeveVisible, player };
                if (!matches(method.getParameterTypes(), args)) {
                    continue;
                }

                method.invoke(renderer, args);
                return true;
            }
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static Object resolveSkinTexture(AbstractClientPlayer player) throws Exception {
        try {
            Object skin = player.getSkin();
            Method body = skin.getClass().getMethod("body");
            Object bodyValue = body.invoke(skin);
            Method texturePath = bodyValue.getClass().getMethod("texturePath");
            return texturePath.invoke(bodyValue);
        } catch (NoSuchMethodException ignored) {
            Method legacy = player.getClass().getMethod("getSkinTextureLocation");
            return legacy.invoke(player);
        }
    }

    private static Object invokeBest(Object target, String[] names, Object arg) throws Exception {
        for (String name : names) {
            for (Method method : target.getClass().getMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != 1) {
                    continue;
                }
                if (isParamCompatible(method.getParameterTypes()[0], arg)) {
                    return method.invoke(target, arg);
                }
            }
        }
        return null;
    }

    private static boolean matches(Class<?>[] paramTypes, Object[] args) {
        if (paramTypes.length != args.length) {
            return false;
        }
        for (int i = 0; i < paramTypes.length; i++) {
            if (!isParamCompatible(paramTypes[i], args[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isParamCompatible(Class<?> paramType, Object arg) {
        if (arg == null) {
            return !paramType.isPrimitive();
        }
        if (paramType.isPrimitive()) {
            return (paramType == int.class && arg instanceof Integer)
                    || (paramType == boolean.class && arg instanceof Boolean)
                    || (paramType == float.class && arg instanceof Float)
                    || (paramType == double.class && arg instanceof Double)
                    || (paramType == long.class && arg instanceof Long)
                    || (paramType == short.class && arg instanceof Short)
                    || (paramType == byte.class && arg instanceof Byte)
                    || (paramType == char.class && arg instanceof Character);
        }
        return paramType.isAssignableFrom(arg.getClass());
    }
}
