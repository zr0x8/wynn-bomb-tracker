package com.wynntils.models.worlds;

import com.wynntils.models.worlds.type.BombInfo;

public class BombModel {
    public static final ActiveBombContainer BOMBS = new ActiveBombContainer();

    public static class ActiveBombContainer {
        public java.util.Collection<BombInfo> getBombs() {
            return java.util.Collections.emptyList();
        }
    }

    // stub
    @SuppressWarnings("unused")
    private BombInfo addBombFromChat(String user, String bomb, String server) {
        return null;
    }
}
