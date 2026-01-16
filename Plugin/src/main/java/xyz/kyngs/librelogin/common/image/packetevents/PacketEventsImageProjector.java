/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.image.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetHeldSlot;
import xyz.kyngs.librelogin.api.image.ImageProjector;
import xyz.kyngs.librelogin.common.AuthenticLibreLogin;
import xyz.kyngs.librelogin.common.image.AuthenticImageProjector;

import java.awt.image.BufferedImage;

public class PacketEventsImageProjector<P, S> extends AuthenticImageProjector<P, S> implements ImageProjector<P> {

    public PacketEventsImageProjector(AuthenticLibreLogin<P, S> plugin) {
        super(plugin);
    }

    @Override
    public void enable() {
        // PacketEvents doesn't require module registration
    }

    /**
     * <b>This implementation only really renders pure black and everything else as transparent. Shouldn't be used for anything else than a QR code.</b>
     *
     * @param image  The image to render.
     * @param player The player to render the image to.
     */
    @Override
    public void project(BufferedImage image, P player) {
        var uuid = platformHandle.getUUIDForPlayer(player);
        User user = PacketEvents.getAPI().getPlayerManager().getUser(uuid);
        
        if (user == null) {
            return;
        }

        int protocol = user.getClientVersion().getProtocolVersion();

        // Create filled map item
        ItemStack.Builder itemBuilder = ItemStack.builder()
                .type(ItemTypes.FILLED_MAP)
                .amount(1);

        // Add map NBT data for 1.17+
        if (protocol >= 755) { // 1.17+
            NBTCompound nbt = new NBTCompound();
            nbt.setTag("map", 0);
            itemBuilder.nbt(nbt);
        }

        ItemStack item = itemBuilder.build();

        // Send item to hotbar slot 0 (inventory slot 36)
        WrapperPlayServerSetSlot setSlot = new WrapperPlayServerSetSlot(
                0, // Window ID (0 for player inventory)
                0, // State ID
                36, // Slot (hotbar slot 0 is inventory slot 36)
                item
        );
        user.sendPacket(setSlot);

        // Set held slot to 0
        WrapperPlayServerSetHeldSlot setHeldSlot = new WrapperPlayServerSetHeldSlot(0);
        user.sendPacket(setHeldSlot);

        // Resize image if needed
        if (image.getWidth() != 128 || image.getHeight() != 128) {
            var resized = new BufferedImage(128, 128, image.getType());
            var graphics = resized.createGraphics();
            graphics.drawImage(image, 0, 0, 128, 128, 0, 0, image.getWidth(), image.getHeight(), null);
            graphics.dispose();
            image = resized;
        }

        // Convert image to map data
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        byte[] data = new byte[pixels.length];

        for (int i = 0; i < pixels.length; i++) {
            data[i] = (byte) (pixels[i] == -16777216 ? 116 : 56);
        }

        // Send map data packet
        MapDataPacket mapDataPacket = new MapDataPacket(0, (byte) 0, 128, 128, 0, 0, data);
        user.sendPacket(mapDataPacket);
    }

    @Override
    public boolean canProject(P player) {
        var uuid = platformHandle.getUUIDForPlayer(player);
        User user = PacketEvents.getAPI().getPlayerManager().getUser(uuid);
        
        if (user == null) {
            return false;
        }

        int protocol = user.getClientVersion().getProtocolVersion();
        
        // Support 1.13 (393) through 1.21.11 (768)
        return protocol >= 393 && protocol <= 768;
    }
}
