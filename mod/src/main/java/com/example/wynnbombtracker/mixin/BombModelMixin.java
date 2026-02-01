package com.example.wynnbombtracker.mixin;

import com.example.wynnbombtracker.BombTrackerClient;
import com.wynntils.models.worlds.BombModel;
import com.wynntils.models.worlds.type.BombInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BombModel.class)
public class BombModelMixin {

    @Inject(method = "addBombFromChat", at = @At("RETURN"), remap = false)
    private void onAddBomb(String user, String bomb, String server, CallbackInfoReturnable<BombInfo> cir) {
        BombInfo info = cir.getReturnValue();
        if (info != null) {
            BombTrackerClient.onBombDetected(user, bomb, server, info);
        }
    }
}
