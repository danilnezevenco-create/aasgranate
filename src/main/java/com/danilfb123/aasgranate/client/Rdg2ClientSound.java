package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.Rdg2Entity;
import net.minecraft.client.Minecraft;

/**
 * Клиентская точка входа для запуска зацикленного звука шашки.
 * Вызывается из Rdg2Entity через DistExecutor, чтобы класс не грузился на сервере.
 */
public final class Rdg2ClientSound {

    private Rdg2ClientSound() {}

    public static void start(Rdg2Entity grenade) {
        Minecraft.getInstance().getSoundManager().play(new Rdg2SmokeSoundInstance(grenade));
    }
}
