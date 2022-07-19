package mod.omo.discs;

import mod.omo.discs.entity.DiscEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class ThrowableDiscsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ThrowableDiscsMod.DISC_ENTITY_TYPE, DiscEntityRenderer::new);
    }
}
