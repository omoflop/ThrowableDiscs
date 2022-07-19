package mod.omo.discs.entity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3f;

public class DiscEntityRenderer extends EntityRenderer<DiscEntity> {
    public DiscEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(DiscEntity entity) {
        return null;
    }

    @Override
    public void render(DiscEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        ItemStack item = entity.asItemStack();
        //MinecraftClient.getInstance().player.sendMessage(Text.literal(Registry.ITEM.getId(item.getItem()).toString()));
        matrices.push();

        matrices.multiply(Vec3f.NEGATIVE_X.getDegreesQuaternion(90));
        float spin = entity.returnTimer == 0 ? 0 : (entity.returnTimer+tickDelta)*16;
        matrices.multiply(Vec3f.NEGATIVE_Z.getDegreesQuaternion(entity.getYaw(tickDelta) + spin));
        matrices.translate(0, -0.15f, 0);

        MinecraftClient.getInstance().getItemRenderer().renderItem(item, ModelTransformation.Mode.GROUND, light, 0, matrices, vertexConsumers, 3);

        matrices.pop();
    }
}
