package mod.omo.discs.entity;

import mod.omo.discs.DiscAttacks;
import mod.omo.discs.ThrowableDiscsMod;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import static net.minecraft.block.JukeboxBlock.HAS_RECORD;

public class DiscEntity extends PersistentProjectileEntity {
    private static final TrackedData<ItemStack> DISC_STACK = DataTracker.registerData(DiscEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);

    private boolean dealtDamage;
    private boolean isSculkDisc;
    private boolean isInWall;
    private Vec3d returnVelocity;
    public int returnTimer;

    public DiscEntity(ItemStack itemStack, Entity owner, World world) {
        super(ThrowableDiscsMod.DISC_ENTITY_TYPE, world);
        this.dataTracker.set(DISC_STACK, itemStack);
        this.setNoGravity(true);
        setOwner(owner);

        if (itemStack.getItem() == Items.MUSIC_DISC_5) {
            setNoClip(true);
        }
    }

    public DiscEntity(EntityType<DiscEntity> discEntityEntityType, World world) {
        super(discEntityEntityType, world);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(DISC_STACK, new ItemStack(Items.MUSIC_DISC_CAT));
    }

    @Override
    protected ItemStack asItemStack() {
        ItemStack stack = this.dataTracker.get(DISC_STACK);
        isSculkDisc = stack.getItem().equals(Items.MUSIC_DISC_5);
        return stack;
    }

    public void tick() {
        // Change the landing sound based on what block the disc is about to collide with
        BlockPos touchingPos = this.getBlockPos().down();
        BlockState touching = this.world.getBlockState(touchingPos);
        setSound(touching.getSoundGroup().getPlaceSound());

        // Glowing effects for disc 5
        isInWall = !getBlockStateAtPos().isAir();
        setGlowing(isSculkDisc && isInWall);

        // If the disc has been thrown for long enough or been in the ground for a few ticks, disable hitbox and try to return
        if (this.inGroundTime > 4 || age > 30) {
            this.dealtDamage = true;
        }

        Entity owner = this.getOwner();
        if (this.dealtDamage) {
            // If the owner isn't alive, drop this as an item in the world
            if (!this.isOwnerAlive()) {
                if (!this.world.isClient && this.pickupType == PickupPermission.ALLOWED) {
                    this.dropStack(this.asItemStack(), 0.1F);
                }

                this.discard();
            }
            // If the owner is alive, try to insert into a empty jukebox
            else if (touching.getBlock() instanceof JukeboxBlock jukeboxBlock && !touching.get(HAS_RECORD)) {
                jukeboxBlock.setRecord(this, world, touchingPos, touching, asItemStack());
                world.syncWorldEvent(null, 1010, touchingPos, Item.getRawId(asItemStack().getItem()));

                this.discard();
            }
        }

        // Horizontal movement speed for calculating gravity, and sound effects
        float speed = (float) Vec3d.ZERO.distanceTo(getVelocity().multiply(1,0,1))*3;

        Item discItem = asItemStack().getItem();
        int throwDistance = getThrowDistance(discItem);

        // If it's still flying through the air
        if (!inGround) {
            returnTimer++;

            // If it's been flying for too long, start applying gravity
            if (returnTimer <= throwDistance) {
                double g = Math.min((-speed+2)/5.0 - 1, 0)/getGravity(discItem);
                if (submergedInWater) g = -0.06;
                this.setVelocity(this.getVelocity().add(0.0, g, 0.0));
            }

            // After 4 seconds (and the disc isn't in a wall, so it doesn't get stuck), start applying gravity and fall to the ground
            if (returnTimer > 80 && !isInWall) {
                returnTimer = -9999;
                returnVelocity = null;
                setNoClip(false);
            } else
            // If the disc is still moving, play a sound depending on the type and speed of the disc
            if (returnTimer % (Math.floor(Math.max(((-speed + 5)*2), 4))) == 0) {
                playSound(isGlowing() ? SoundEvents.BLOCK_SCULK_CHARGE : getSpinSound(discItem), 2f, random.nextBetween(9, 13)/10f + (throwDistance > 30 ? 0.5f : 0));
            }

            // Make disc 5 deal AOE damage
            if (isSculkDisc && isNoClip()) {
                dealSplashDamage(4f, false, getBoundingBox().expand(0.5).expand(0,1,0));
            }
        }
        // When grounded, set to a neutral state
        else {
            // Do slam attack when first hitting the ground
            doSlamAttack();

            returnTimer = 0;
            returnVelocity = null;
        }

        // If the disc has been thrown for X ticks, and hasn't tried to return to its owner yet, do that
        if (returnTimer > throwDistance && returnVelocity == null) {
            flyTowardsOwner();
            setVelocity(getVelocity().multiply(isNoClip() ? 0 : 0.4f).add(returnVelocity));
        }

        // If the disc is flying back towards it's owner
        if (returnVelocity != null) {

            // Constantly redirect towards the owner, making it harder to ose
            if (isSculkDisc) {
                flyTowardsOwner();
            }

            // If it's close enough to it's owner, allow for pickup
            if (owner.getEyePos().distanceTo(getPos()) < 3) setNoClip(true);

            // Approach the owner's position
            this.setVelocity(this.getVelocity().multiply(0.98).add(returnVelocity));
        }


        super.tick();
    }

    private void doSlamAttack() {
        int s = EnchantmentHelper.getLevel(ThrowableDiscsMod.SLAM, asItemStack());
        if (returnTimer > 0 && s > 0) {
            dealSplashDamage(6f + s*4f, true, getBoundingBox().expand(s*0.8, s*0.8, s*0.8));
            for(int i = 0; i < s*48; i++) {
                float x = (float) (getX() + random.nextBetween(-s, s)*0.8f);
                float y = (float) (getY() + random.nextBetween(-s, s)*0.3f);
                float z = (float) (getZ() + random.nextBetween(-s, s)*0.8f);
                world.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, world.getBlockState(new BlockPos(x, getY()-1, z))), x, y, z, (Math.random()-0.5d)/4.0, (Math.random()-0.5d)/4.0, (Math.random()-0.5d)/4.0);
            }
            playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 1.2f, random.nextBetween(11, 15)/10f);
        }

    }

    // Sets the return velocity to face the owner
    private void flyTowardsOwner() {
        returnVelocity = ownerDirection().multiply(0.07);
    }

    public Vec3d ownerDirection() {
        return getOwner().getEyePos().subtract(this.getPos()).normalize();
    }

    // Deals damage in an area around the disc
    private void dealSplashDamage(float f, boolean friendlyFire, Box aoe) {
        for(Entity e : world.getOtherEntities(this, aoe)) {
            if (e instanceof LivingEntity livingEntity && (!friendlyFire || !livingEntity.getUuid().equals(getOwner().getUuid()))) {
                livingEntity.damage(DamageSource.thrownProjectile(this, getOwner()), f + getAttackDamage(asItemStack(), livingEntity));
            }
        }
    }

    public float getAttackDamage(ItemStack itemStack) {
        float f = (float) ((EnchantmentHelper.getLevel(Enchantments.POWER, itemStack) * 1.6) + 0.5);

        Entity owner = getOwner();
        Item discItem = itemStack.getItem();
        if (owner != null) {
            if (discItem.equals(Items.MUSIC_DISC_BLOCKS)) {
                float blastResistance = owner.getSteppingBlockState().getBlock().getBlastResistance();
                f += Math.max(Math.round(((blastResistance - 1) / (blastResistance + 22)) * 15), 0);
            } else if (discItem.equals(Items.MUSIC_DISC_MELLOHI) && owner.getY() > getY()) {
                f += 4f;
            } else if (discItem.equals(Items.MUSIC_DISC_STRAD) && getY() < owner.getY()) {
                f += 4f;
            }
        }

        return f;
    }

    private static double getGravity(Item i) {
        return i == Items.MUSIC_DISC_FAR ? 100.0 : 35.0;
    }

    private static SoundEvent getSpinSound(Item i) {
        if (i == Items.MUSIC_DISC_PIGSTEP) {
            return SoundEvents.ENTITY_PIG_AMBIENT;
        }

        if (i == Items.MUSIC_DISC_CAT) {
            return SoundEvents.ENTITY_CAT_AMBIENT;
        }

        return ThrowableDiscsMod.DISC_SPIN;
    }

    private static int getThrowDistance(Item i) {
        return i == Items.MUSIC_DISC_FAR ? 60 : 30;
    }

    private boolean isOwnerAlive() {
        Entity entity = this.getOwner();
        if (entity == null || !entity.isAlive()) {
            return false;
        } else {
            return !(entity instanceof ServerPlayerEntity) || !entity.isSpectator();
        }
    }

    protected EntityHitResult getEntityCollision(Vec3d currentPosition, Vec3d nextPosition) {
        return this.dealtDamage ? null : super.getEntityCollision(currentPosition, nextPosition);
    }

    protected void onEntityHit(EntityHitResult entityHitResult) {
        Entity hitEntity = entityHitResult.getEntity();

        // Does 4 hearts by default
        float f = 8.0F;
        if (hitEntity instanceof LivingEntity livingEntity) {
            f += getAttackDamage(asItemStack());
        }

        Entity owner = this.getOwner();
        DamageSource damageSource = DamageSource.thrownProjectile(this, owner == null ? this : owner);

        // If the entity can take the damage
        if (hitEntity.damage(damageSource, f)) {

            dealtDamage = true;

            // Make it not work on endermen
            if (hitEntity.getType() == EntityType.ENDERMAN) {
                return;
            }

            // Do enchantment shenanigans when thrown by living things/players
            if (hitEntity instanceof LivingEntity livingHitEntity) {
                if (owner instanceof LivingEntity livingOwner) {
                    EnchantmentHelper.onUserDamaged(livingHitEntity, livingOwner);
                    EnchantmentHelper.onTargetDamaged(livingOwner, livingHitEntity);
                }

                // Make flame work
                if (isOnFire()) {
                    hitEntity.setOnFireFor(3);
                }

                // Make it instantly start returning to the owner
                returnTimer = 60;
                this.onHit(livingHitEntity);
            }

            DiscAttacks.attack(owner, hitEntity, this, asItemStack());
        }

        // Bounce off the target
        this.setVelocity(this.getVelocity().multiply(-0.01, -0.1, -0.01));
    }

    public boolean tryPickup(PlayerEntity player) {
        return (dealtDamage) && this.isOwner(player) && player.getInventory().insertStack(this.asItemStack());
    }

    protected SoundEvent getHitSound() {
        return SoundEvents.ITEM_TRIDENT_HIT_GROUND;
    }

    public void onPlayerCollision(PlayerEntity player) {
        if (this.isOwner(player) || this.getOwner() == null) {
            super.onPlayerCollision(player);
        }

    }

    @Override
    public boolean doesRenderOnFire() {
        return false;
    }
}
