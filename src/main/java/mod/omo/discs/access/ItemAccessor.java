package mod.omo.discs.access;

import net.minecraft.item.Item;

public interface ItemAccessor {
    void setMaxDamage(int maxDamage);

    static void setMaxDamage(Item i, int maxDamage) {
        ((ItemAccessor)(i)).setMaxDamage(maxDamage);
    }
}
