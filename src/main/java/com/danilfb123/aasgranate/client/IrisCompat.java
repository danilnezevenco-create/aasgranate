package com.danilfb123.aasgranate.client;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.util.Optional;
import org.slf4j.Logger;

/** Определяет, активны ли шейдеры Iris / Oculus / OptiFine. */
final class IrisCompat {

    private static final Logger LOGGER = LogUtils.getLogger();

    // У Iris/Oculus для 1.20.1 API может лежать в разных пакетах.
    private static final String[] API_CLASSES = {
            "net.irisshaders.iris.api.v0.IrisApi",
            "net.coderbot.iris.api.v0.IrisApi"
    };
    private static final String[] IRIS_CLASSES = {
            "net.irisshaders.iris.Iris",
            "net.coderbot.iris.Iris"
    };

    private static final Object IRIS_API;
    private static final Method IRIS_IN_USE;
    private static final Method IRIS_CURRENT_PACK;
    private static final Method OPTIFINE_SHADERS;

    static {
        Object api = null;
        Method inUse = null;
        for (String name : API_CLASSES) {
            try {
                Class<?> c = Class.forName(name);
                api = c.getMethod("getInstance").invoke(null);
                inUse = c.getMethod("isShaderPackInUse"); // метод интерфейса, не реализации
                LOGGER.info("[aasgranate] Iris API found: {}", name);
                break;
            } catch (Throwable ignored) {
                api = null;
                inUse = null;
            }
        }
        IRIS_API = api;
        IRIS_IN_USE = inUse;

        Method pack = null;
        for (String name : IRIS_CLASSES) {
            try {
                pack = Class.forName(name).getMethod("getCurrentPack");
                LOGGER.info("[aasgranate] Iris class found: {}", name);
                break;
            } catch (Throwable ignored) {}
        }
        IRIS_CURRENT_PACK = pack;

        Method of = null;
        try {
            of = Class.forName("net.optifine.Config").getMethod("isShaders");
        } catch (Throwable ignored) {}
        OPTIFINE_SHADERS = of;

        if (IRIS_IN_USE == null && IRIS_CURRENT_PACK == null && OPTIFINE_SHADERS == null)
            LOGGER.info("[aasgranate] No Iris/Oculus/OptiFine detected");
    }

    private static long lastCheck;
    private static boolean cached;
    private static boolean logged;

    private IrisCompat() {}

    static boolean shadersActive() {
        long now = System.nanoTime();
        if (now - lastCheck > 500_000_000L) {
            boolean value = compute();
            if (!logged || value != cached) {
                LOGGER.info("[aasgranate] shader pack active = {}", value);
                logged = true;
            }
            cached = value;
            lastCheck = now;
        }
        return cached;
    }

    private static boolean compute() {
        if (IRIS_IN_USE != null) {
            try {
                return (boolean) IRIS_IN_USE.invoke(IRIS_API);
            } catch (Throwable ignored) {}
        }
        if (IRIS_CURRENT_PACK != null) {
            try {
                Object r = IRIS_CURRENT_PACK.invoke(null);
                if (r instanceof Optional<?> opt) return opt.isPresent();
            } catch (Throwable ignored) {}
        }
        if (OPTIFINE_SHADERS != null) {
            try {
                return (boolean) OPTIFINE_SHADERS.invoke(null);
            } catch (Throwable ignored) {}
        }
        return false;
    }
}