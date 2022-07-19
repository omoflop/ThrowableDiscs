package mod.omo.discs;

import mod.omo.discs.entity.DiscEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;

public class DiscAttacks {
    private static final HashMap<Item, Attack> MUSIC_DISC_ATTACKS = new HashMap<>();

    static {
        // Sends targets flying towards attacker
        register(Items.MUSIC_DISC_MALL, (owner, target, disc, stack) -> target.setVelocity(disc.ownerDirection().multiply(1.2)));

        // Swaps attacker and target's positions, also makes attacker pickup disc
        register(Items.MUSIC_DISC_OTHERSIDE, (owner, target, disc, stack) -> {
            Vec3d temp = target.getPos();
            Vec3d pos2 = owner.getPos();
            target.teleport(pos2.x, pos2.y, pos2.z);
            owner.teleport(temp.x, temp.y, temp.z);
            target.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1);
            if (owner instanceof PlayerEntity player) {
                if (disc.tryPickup(player)) {
                    disc.discard();
                }
            }
        });

        // Give targets 3 seconds of slowness 1
        register(Items.MUSIC_DISC_STAL, (owner, target, disc, stack) -> {
            if (target instanceof LivingEntity living) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 0));
            }
        });
    }

    public static void register(Item disc, Attack attack) {
        assert disc instanceof MusicDiscItem;
        MUSIC_DISC_ATTACKS.put(disc, attack);
    }

    public interface Attack {
        void run(Entity owner, Entity target, DiscEntity disc, ItemStack stack);
    }

    public static void attack(Entity owner, Entity target, DiscEntity disc, ItemStack stack) {
        Item item = stack.getItem();
        if (MUSIC_DISC_ATTACKS.containsKey(item)) {
            MUSIC_DISC_ATTACKS.get(item).run(owner, target, disc, stack);
        }
    }
}
