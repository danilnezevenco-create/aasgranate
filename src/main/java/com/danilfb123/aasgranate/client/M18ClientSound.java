package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.M18Entity;
import net.minecraft.client.Minecraft;

/**
 * Клиентская точка входа для запуска зацикленного звука шашки.
 * Вызывается из M18Entity через DistExecutor, чтобы класс не грузился на сервере.
 */
public final class M18ClientSound {

    private M18ClientSound() {}

    public static void start(M18Entity grenade) {
        Minecraft.getInstance().getSoundManager().play(new M18SmokeSoundInstance(grenade));
    }
}
