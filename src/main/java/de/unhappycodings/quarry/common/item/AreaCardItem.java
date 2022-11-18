package de.unhappycodings.quarry.common.item;

import de.unhappycodings.quarry.Quarry;
import de.unhappycodings.quarry.common.blocks.QuarryBlock;
import de.unhappycodings.quarry.common.container.AreaCardContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AreaCardItem extends Item implements MenuProvider {

    public AreaCardItem() {
        super(new Item.Properties().stacksTo(1).tab(Quarry.creativeTab));
    }

    public static void writePos(CompoundTag nbt, BlockPos pos) {
        nbt.putInt("x", pos.getX());
        nbt.putInt("y", pos.getY());
        nbt.putInt("z", pos.getZ());
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        if (stack.getOrCreateTag().contains("pos1")) {
            String pos = stack.getOrCreateTag().get("pos1").getAsString().replace("{", "").replace("}", "").replace(",", " ");
            tooltipComponents.add(new TranslatableComponent("item.quarry.areacard.text.box").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            if (stack.getOrCreateTag().contains("lastBlock")) {
                int blocksMined = stack.getOrCreateTag().getInt("lastBlock");
                tooltipComponents.add(new TranslatableComponent("item.quarry.areacard.text.mined").append(" " + blocksMined).setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            }
            tooltipComponents.add(new TranslatableComponent("item.quarry.areacard.text.from").append(" " + pos).setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        }
        if (stack.getOrCreateTag().contains("pos2")) {
            String pos = stack.getOrCreateTag().get("pos2").getAsString().replace("{", "").replace("}", "").replace(",", " ");
            tooltipComponents.add(new TranslatableComponent("item.quarry.areacard.text.to").append(" " + pos).setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        }
        for (int i = 0; i <= 6; i++) {
            if (stack.getOrCreateTag().getCompound("Filters").getBoolean(String.valueOf(i))) {
                tooltipComponents.add(new TranslatableComponent("item.quarry.areacard.text.filters_active").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));
                break;
            }
        }
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }

    @SuppressWarnings("ConstantConditions")
    @NotNull
    @Override
    public InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.CONSUME;
        Player player = context.getPlayer();
        ItemStack item = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof QuarryBlock || context.getHand() != InteractionHand.MAIN_HAND)
            return InteractionResult.CONSUME;
        if (!item.getOrCreateTag().contains("pos1") || item.getOrCreateTag().contains("pos2")) {
            item = new ItemStack(item.getItem());
            CompoundTag posTag = new CompoundTag();
            writePos(posTag, pos);
            item.getOrCreateTag().put("pos1", posTag);
            player.sendMessage(new TranslatableComponent("message.quarry.savedfirst").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)), Util.NIL_UUID);
        } else {
            if (!item.getOrCreateTag().contains("pos2")) {
                CompoundTag posTag = new CompoundTag();
                writePos(posTag, pos);
                item.getOrCreateTag().put("pos2", posTag);
                player.sendMessage(new TranslatableComponent("message.quarry.savedsecond").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)), Util.NIL_UUID);
            }
        }
        player.setItemSlot(EquipmentSlot.MAINHAND, item);
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        for (int i = 1; i < 3; i++) {
            CompoundTag tag = stack.getOrCreateTag().getCompound("pos" + i);
            if (!tag.contains("x") && !tag.contains("y") && !tag.contains("z")) {
                tag.putInt("x", 0);
                tag.putInt("y", 0);
                tag.putInt("z", 0);
                stack.getOrCreateTag().put("pos" + i, tag);
            }
        }
        return stack;
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level pLevel, @NotNull Player pPlayer, @NotNull InteractionHand pUsedHand) {
        if (!pLevel.isClientSide && pUsedHand == InteractionHand.MAIN_HAND) {
            NetworkHooks.openGui((ServerPlayer) pPlayer, this);
        }
        return super.use(pLevel, pPlayer, pUsedHand);
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return new TextComponent("Area Card");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pInventory, Player pPlayer) {
        return new AreaCardContainer(pContainerId, pInventory, pPlayer.getOnPos(), pPlayer.getLevel());
    }
}
