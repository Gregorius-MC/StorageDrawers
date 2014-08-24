package com.jaquadro.minecraft.storagedrawers.block.tile;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;

public class TileEntityDrawers extends TileEntity
{
    private class DrawerData {
        public Item item;
        public int meta;
        public int count;
        public NBTTagCompound attrs;

        public DrawerData () {
            reset();
        }

        public void writeToNBT (NBTTagCompound tag) {
            if (item != null) {
                tag.setShort("Item", (short) Item.getIdFromItem(item));
                tag.setShort("Meta", (short) meta);
                tag.setInteger("Count", count);

                if (attrs != null)
                    tag.setTag("Tags", attrs);
            }
        }

        public void readFromNBT (NBTTagCompound tag) {
            if (tag.hasKey("Item")) {
                item = Item.getItemById(tag.getShort("Item"));
                meta = tag.getShort("Meta");
                count = tag.getInteger("Count");

                if (tag.hasKey("Tags"))
                    attrs = tag.getCompoundTag("Tags");
            }
        }

        public void reset () {
            item = null;
            meta = 0;
            count = 0;
            attrs = null;
        }

        public int maxCapacity () {
            if (item == null)
                return 0;

            return item.getItemStackLimit(null) * stackCapacity();
        }

        public int remainingCapacity () {
            if (item == null)
                return 0;

            return maxCapacity() - count;
        }

        public int stackCapacity () {
            return drawerCount * drawerCapacity;
        }
    }

    private int direction;
    private int drawerCount = 2;
    private int drawerCapacity = 1;
    private int level = 1;

    private DrawerData[] data;

    public TileEntityDrawers () {
        setDrawerCount(2);
    }

    public int getDirection () {
        return direction;
    }

    public void setDirection (int direction) {
        this.direction = direction % 6;
    }

    public void setDrawerCount (int count) {
        drawerCount = count;

        data = new DrawerData[drawerCount];
        for (int i = 0; i < data.length; i++)
            data[i] = new DrawerData();
    }

    public void setDrawerCapacity (int stackCount) {
        drawerCapacity = stackCount;
    }

    public int getSlotCount () {
        return data.length;
    }

    public Item getItem (int slot) {
        return data[slot].item;
    }

    public int getItemMeta (int slot) {
        return data[slot].meta;
    }

    public ItemStack getSingleItemStack (int slot) {
        if (data[slot].item == null)
            return null;

        ItemStack stack = new ItemStack(data[slot].item, 1, data[slot].meta);
        stack.setTagCompound(data[slot].attrs);

        return stack;
    }

    public NBTTagCompound getItemAttrs (int slot) {
        return data[slot].attrs;
    }

    public ItemStack takeItemsFromSlot (int slot, int count) {
        if (data[slot].item == null)
            return null;

        ItemStack stack = new ItemStack(data[slot].item, 1, data[slot].meta);
        stack.stackSize = Math.min(stack.getMaxStackSize(), count);
        stack.setTagCompound(data[slot].attrs);

        data[slot].count -= stack.stackSize;
        if (data[slot].count == 0)
            data[slot].reset();

        return stack;
    }

    public int putItemsIntoSlot (int slot, ItemStack stack, int count) {
        if (data[slot].item == null) {
            data[slot].item = stack.getItem();
            data[slot].meta = stack.getItemDamage();
            data[slot].attrs = stack.getTagCompound();
        }

        if (data[slot].item != stack.getItem() || data[slot].meta != stack.getItemDamage())
            return 0;

        if ((data[slot].attrs == null || stack.getTagCompound() == null) && data[slot].attrs != stack.getTagCompound())
            return 0;
        else if (data[slot].attrs != null && !data[slot].attrs.equals(stack.getTagCompound()))
            return 0;

        int countAdded = Math.min(data[slot].remainingCapacity(), stack.stackSize);
        countAdded = Math.min(countAdded, count);

        data[slot].count += countAdded;
        stack.stackSize -= countAdded;

        return countAdded;
    }

    @Override
    public void readFromNBT (NBTTagCompound tag) {
        super.readFromNBT(tag);

        direction = tag.getByte("Dir");
        drawerCapacity = tag.getByte("Cap");
        level = tag.getByte("Lev");

        NBTTagList slots = tag.getTagList("Slots", Constants.NBT.TAG_COMPOUND);
        drawerCount = slots.tagCount();
        data = new DrawerData[slots.tagCount()];

        for (int i = 0; i < data.length; i++) {
            NBTTagCompound slot = slots.getCompoundTagAt(i);
            data[i] = new DrawerData();
            data[i].readFromNBT(slot);
        }
    }

    @Override
    public void writeToNBT (NBTTagCompound tag) {
        super.writeToNBT(tag);

        tag.setByte("Dir", (byte)direction);
        tag.setByte("Cap", (byte)drawerCapacity);
        tag.setByte("Lev", (byte)level);

        NBTTagList slots = new NBTTagList();
        for (int i = 0; i < data.length; i++) {
            NBTTagCompound slot = new NBTTagCompound();
            data[i].writeToNBT(slot);
            slots.appendTag(slot);
        }

        tag.setTag("Slots", slots);
    }

    @Override
    public Packet getDescriptionPacket () {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);

        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 5, tag);
    }

    @Override
    public void onDataPacket (NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
        getWorldObj().func_147479_m(xCoord, yCoord, zCoord); // markBlockForRenderUpdate
    }
}