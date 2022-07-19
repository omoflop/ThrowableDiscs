package mod.omo.discs.enchantment;

import net.minecraft.item.ItemStack;
import net.minecraft.item.MusicDiscItem;
import org.moon.enchantmenttweaker.lib.EnchantmentModification;

import java.util.Optional;

public class DiscEnchantmentModification implements EnchantmentModification {

    @Override
    public boolean isApplicableTo(ItemStack stack, Optional<Integer> level) {
        return stack.getItem() instanceof MusicDiscItem;
    }
}
