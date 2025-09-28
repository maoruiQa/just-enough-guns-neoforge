package ttv.migami.jeg.network;

import com.mrcrayfish.framework.api.FrameworkAPI;
import com.mrcrayfish.framework.api.network.FrameworkNetwork;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.network.message.*;

public class PacketHandler
{
    private static FrameworkNetwork playChannel;

    public static void init()
    {
        playChannel = FrameworkAPI.createNetworkBuilder(new ResourceLocation(Reference.MOD_ID, "play"), 1)
                .registerPlayMessage("c2s_aim", C2SMessageAim.class, C2SMessageAim.CODEC, C2SMessageAim::handle, PacketFlow.SERVERBOUND)
                .registerPlayMessage("c2s_reload", C2SMessageReload.class, C2SMessageReload.CODEC, C2SMessageReload::handle, PacketFlow.SERVERBOUND)
                .registerPlayMessage("c2s_shoot", C2SMessageShoot.class, C2SMessageShoot.CODEC, C2SMessageShoot::handle, PacketFlow.SERVERBOUND)
                .registerPlayMessage("c2s_pre_fire_sound", C2SMessagePreFireSound.class, C2SMessagePreFireSound.CODEC, C2SMessagePreFireSound::handle, PacketFlow.SERVERBOUND)
                .registerPlayMessage("c2s_burst", C2SMessageBurst.class, C2SMessageBurst.CODEC, C2SMessageBurst::handle, PacketFlow.SERVERBOUND)
                .registerPlayMessage("c2s_unload", C2SMessageUnload.class, C2SMessageUnload.CODEC, C2SMessageUnload::handle, PacketFlow.SERVERBOUND)
                .registerPlayMessage("s2c_stun_grenade", S2CMessageStunGrenade.class, S2CMessageStunGrenade.CODEC, S2CMessageStunGrenade::handle, PacketFlow.CLIENTBOUND)
                .registerPlayMessage("s2c_smoke_grenade", S2CMessageSmokeGrenade.class, S2CMessageSmokeGrenade.CODEC, S2CMessageSmokeGrenade::handle, PacketFlow.CLIENTBOUND)
                .registerPlayMessage("c2s_craft", C2SMessageCraft.class, C2SMessageCraft.CODEC, C2SMessageCraft::handle, PacketFlow.SERVERBOUND)
                .registerPlayMessage("s2c_bullet_trail", S2CMessageBulletTrail.class, S2CMessageBulletTrail.CODEC, S2CMessageBulletTrail::handle, PacketFlow.CLIENTBOUND)
                .registerPlayMessage("c2s_attachments", C2SMessageAttachments.class, C2SMessageAttachments.CODEC, C2SMessageAttachments::handle, PacketFlow.SERVERBOUND)
                .registerPlayMessage("s2c_update_guns", S2CMessageUpdateGuns.class, S2CMessageUpdateGuns.CODEC, S2CMessageUpdateGuns::handle, PacketFlow.CLIENTBOUND)
                .registerPlayMessage("s2c_blood", S2CMessageBlood.class, S2CMessageBlood.CODEC, S2CMessageBlood::handle, PacketFlow.CLIENTBOUND)
                .registerPlayMessage("s2c_explosive_ammo", S2CMessageExplosiveAmmo.class, S2CMessageExplosiveAmmo.CODEC, S2CMessageExplosiveAmmo::handle, PacketFlow.CLIENTBOUND)
                .registerPlayMessage("c2s_shooting", C2SMessageShooting.class, C2SMessageShooting.CODEC, C2SMessageShooting::handle, PacketFlow.SERVERBOUND)
                .registerPlayMessage("c2s_first_person_reload", C2SMessageFirstPersonReload.class, C2SMessageFirstPersonReload.CODEC, C2SMessageFirstPersonReload::handle, PacketFlow.SERVERBOUND)
                .registerPlayMessage("s2c_gun_sound", S2CMessageGunSound.class, S2CMessageGunSound.CODEC, S2CMessageGunSound::handle, PacketFlow.CLIENTBOUND)
                .registerPlayMessage("s2c_projectile_hit_block", S2CMessageProjectileHitBlock.class, S2CMessageProjectileHitBlock.CODEC, S2CMessageProjectileHitBlock::handle, PacketFlow.CLIENTBOUND)
                .registerPlayMessage("s2c_laser", S2CMessageLaser.class, S2CMessageLaser.CODEC, S2CMessageLaser::handle, PacketFlow.CLIENTBOUND)
                .registerPlayMessage("s2c_projectile_hit_entity", S2CMessageProjectileHitEntity.class, S2CMessageProjectileHitEntity.CODEC, S2CMessageProjectileHitEntity::handle, PacketFlow.CLIENTBOUND)
                .registerPlayMessage("s2c_remove_projectile", S2CMessageRemoveProjectile.class, S2CMessageRemoveProjectile.CODEC, S2CMessageRemoveProjectile::handle, PacketFlow.CLIENTBOUND)
                .registerPlayMessage("c2s_leftover_ammo", C2SMessageLeftOverAmmo.class, C2SMessageLeftOverAmmo.CODEC, C2SMessageLeftOverAmmo::handle, PacketFlow.SERVERBOUND)
                .registerPlayMessage("c2s_melee", C2SMessageMelee.class, C2SMessageMelee.CODEC, C2SMessageMelee::handle, PacketFlow.SERVERBOUND)
                .registerPlayMessage("c2s_inspect_gun", C2SMessageInspectGun.class, C2SMessageInspectGun.CODEC, C2SMessageInspectGun::handle, PacketFlow.SERVERBOUND)
                .registerPlayMessage("c2s_casing", C2SMessageCasing.class, C2SMessageCasing.CODEC, C2SMessageCasing::handle, PacketFlow.SERVERBOUND)
                .registerPlayMessage("c2s_gun_unjammed", C2SMessageGunUnjammed.class, C2SMessageGunUnjammed.CODEC, C2SMessageGunUnjammed::handle, PacketFlow.SERVERBOUND)
                .registerPlayMessage("c2s_flashlight", C2SMessageFlashlight.class, C2SMessageFlashlight.CODEC, C2SMessageFlashlight::handle, PacketFlow.SERVERBOUND)
                .registerPlayMessage("s2c_send_medal", S2CMessageSendMedal.class, S2CMessageSendMedal.CODEC, S2CMessageSendMedal::handle, PacketFlow.CLIENTBOUND)
                .registerPlayMessage("s2c_send_kill_medal", S2CMessageSendKillMedal.class, S2CMessageSendKillMedal.CODEC, S2CMessageSendKillMedal::handle, PacketFlow.CLIENTBOUND)
                .registerPlayMessage("s2c_send_headshot", S2CMessageSendHeadshot.class, S2CMessageSendHeadshot.CODEC, S2CMessageSendHeadshot::handle, PacketFlow.CLIENTBOUND)
                .registerPlayMessage("c2s_toggle_medals", C2SMessageToggleMedals.class, C2SMessageToggleMedals.CODEC, C2SMessageToggleMedals::handle, PacketFlow.SERVERBOUND)
                .registerPlayMessage("c2s_charge_sync", C2SMessageChargeSync.class, C2SMessageChargeSync.CODEC, C2SMessageChargeSync::handle, PacketFlow.SERVERBOUND)
                .registerPlayMessage("c2s_overheat", C2SMessageOverheat.class, C2SMessageOverheat.CODEC, C2SMessageOverheat::handle, PacketFlow.SERVERBOUND)
                .registerPlayMessage("c2s_burn_player", C2SMessageBurnPlayer.class, C2SMessageBurnPlayer.CODEC, C2SMessageBurnPlayer::handle, PacketFlow.SERVERBOUND)
                .build();
    }

    public static FrameworkNetwork getPlayChannel()
    {
        return playChannel;
    }
}
