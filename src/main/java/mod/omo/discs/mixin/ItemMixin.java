package mod.omo.discs.mixin;

import mod.omo.discs.access.ItemAccessor;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Item.class)
public class ItemMixin implements ItemAccessor {

    @Shadow @Mutable @Final
    public int maxDamage;

    @Override
    public void setMaxDamage(int maxDamage) {
        this.maxDamage = maxDamage;
    }
}
