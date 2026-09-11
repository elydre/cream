package com.example.items;

import com.example.ModItems;

import net.minecraft.world.item.Item;

public class floppyDisk extends Item {
    public floppyDisk() {
        super(new Item.Properties().setId(ModItems.keyOfItem("floppy_disk")).stacksTo(1));
    }
}
