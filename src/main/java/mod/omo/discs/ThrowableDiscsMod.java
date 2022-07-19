package mod.omo.discs;

import mod.omo.discs.enchantment.DiscEnchantmentModification;
import mod.omo.discs.enchantment.SlamEnchantment;
import mod.omo.discs.entity.DiscEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.moon.enchantmenttweaker.EnchantmentModifications;
import org.moon.enchantmenttweaker.lib.EnchantmentModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ThrowableDiscsMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("throwable_discs");

	public static final EntityType<DiscEntity> DISC_ENTITY_TYPE = Registry.register(Registry.ENTITY_TYPE, new Identifier("throwable_discs:disc_entity"), FabricEntityTypeBuilder.<DiscEntity>create().entityFactory(DiscEntity::new).dimensions(EntityDimensions.fixed(0.5f, 0.5f)).trackedUpdateRate(20).build());
	public static final SoundEvent DISC_SPIN = new SoundEvent(new Identifier("throwable_discs:disc_spin"));
	public static final SoundEvent DISC_THROW = new SoundEvent(new Identifier("throwable_discs:disc_throw"));

	public static final SlamEnchantment SLAM = Registry.register(Registry.ENCHANTMENT, new Identifier("throwable_discs:slam"), new SlamEnchantment());

	@Override
	public void onInitialize() {
		Registry.register(Registry.SOUND_EVENT, DISC_SPIN.getId(), DISC_SPIN);
		Registry.register(Registry.SOUND_EVENT, DISC_THROW.getId(), DISC_THROW);

		EnchantmentModifications.add(Enchantments.POWER, new DiscEnchantmentModification());
		EnchantmentModifications.add(Enchantments.PUNCH, new DiscEnchantmentModification());
		EnchantmentModifications.add(Enchantments.FLAME, new DiscEnchantmentModification());

		EnchantmentModifications.add(SLAM, new EnchantmentModification() {
			@Override
			public boolean isNotApplicableTo(ItemStack stack, Optional<Integer> level) {
				return !(stack.getItem() instanceof MusicDiscItem disc) || disc.equals(Items.MUSIC_DISC_5);
			}
		});

	}
}
