package mod.omo.discs.mixin;

import mod.omo.discs.ThrowableDiscsMod;
import mod.omo.discs.access.ItemAccessor;
import mod.omo.discs.entity.DiscEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicDiscItem.class)
public abstract class MusicDiscItemMixin extends Item {
	public MusicDiscItemMixin(Settings settings) {
		super(settings);
	}

	@Inject(at = @At("TAIL"), method = "<init>")
	private void init(int comparatorOutput, SoundEvent sound, Settings settings, CallbackInfo ci) {
		ItemAccessor.setMaxDamage(this, 225);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack itemStack = user.getStackInHand(hand);
		user.setCurrentHand(hand);
		return TypedActionResult.consume(itemStack);
	}

	@Override
	public int getMaxUseTime(ItemStack stack) {
		return 72000;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.SPEAR;
	}

	@Override
	public void onStoppedUsing(ItemStack inventoryStack, World world, LivingEntity user, int remainingUseTicks) {
		float f = BowItem.getPullProgress(this.getMaxUseTime(inventoryStack) - remainingUseTicks)/2f;
		ItemStack discStack = inventoryStack.copy();



		discStack.damage(1, user, e -> {
			user.sendToolBreakStatus(user.getActiveHand());
			inventoryStack.decrement(1);
		});

		if (!discStack.isEmpty()) {
			user.playSound(ThrowableDiscsMod.DISC_THROW, 1, 1);

			DiscEntity disc = new DiscEntity(discStack, user, world);
			disc.setPunch(EnchantmentHelper.getLevel(Enchantments.PUNCH, discStack));
			if (EnchantmentHelper.getLevel(Enchantments.FLAME, discStack) > 0) {
				disc.setOnFireFor(100);
			}
			disc.setPos(user.getX(), user.getEyeY(), user.getZ());
			disc.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, f * 3.0F, 1.0F);
			if (world.spawnEntity(disc)) {
				if (user instanceof PlayerEntity player && player.isCreative()) {
					disc.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
				} else {
					inventoryStack.decrement(1);
				}
			}
		}
	}

	@Override
	public boolean isUsedOnRelease(ItemStack stack) {
		return true;
	}

	@Override
	public int getEnchantability() {
		return 7;
	}
}
