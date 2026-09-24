package com.danilfb123.aasgranate.api;

/**
 * Общий контракт для любой сущности, у которой есть объёмное дымовое облако
 * (как у M18Entity / Rdg2Entity). SmokeStageRenderer рендерит ЛЮБУЮ сущность,
 * реализующую этот интерфейс, а не только гранаты — это и есть точка
 * расширения, которой пользуется API (см. AasSmokeApi и AasSmokeSourceEntity).
 *
 * Реализуя этот интерфейс у себя (или используя готовую AasSmokeSourceEntity
 * через AasSmokeApi), сторонний мод получает тот же самый объёмный дым,
 * что и у дымовых шашек, без копирования шейдеров/рендера.
 */
public interface SmokeCloudEmitter {

    /** Идёт ли сейчас дым (облако раскрывается / держится / затухает). */
    boolean isSmoking();

    /**
     * 0..1 — насколько плотное облако прямо сейчас (растёт GROW, держится 1.0
     * на HOLD, падает до 0 на FADE). partialTick — для интерполяции между тиками.
     */
    float getSmokeDensity(float partialTick);

    /** 0..1 — насколько облако раскрылось по объёму (используется для радиуса/высоты). */
    float getSmokeGrowth(float partialTick);

    /** Сырой счётчик тиков дымления (для анимации шума в шейдере/пуфах), или -1, если дыма нет. */
    float getSmokeTicks(float partialTick);

    /** Целевой (максимальный) радиус облака в блоках. */
    float getSmokeRadius();

    /** Целевая (максимальная) высота облака в блоках. */
    float getSmokeHeight();
}
