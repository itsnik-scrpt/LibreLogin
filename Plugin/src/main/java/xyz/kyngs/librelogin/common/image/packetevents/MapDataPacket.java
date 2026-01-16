/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.image.packetevents;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.netty.buffer.ByteBuf;

/**
 * Packet for sending map data to the client.
 * Supports Minecraft 1.13+ through 1.21.11
 *
 * @author kyngs
 */
public class MapDataPacket extends PacketWrapper<MapDataPacket> {
    private int mapID;
    private byte scale;
    private boolean locked;
    private boolean trackingPosition;
    private int columns;
    private int rows;
    private int posX;
    private int posZ;
    private byte[] data;

    public MapDataPacket(int mapID, byte scale, int columns, int rows, int posX, int posZ, byte[] data) {
        super(PacketType.Play.Server.MAP_DATA);
        this.mapID = mapID;
        this.scale = scale;
        this.columns = columns;
        this.rows = rows;
        this.posX = posX;
        this.posZ = posZ;
        this.data = data;
        this.trackingPosition = false;
        this.locked = false;
    }

    @Override
    public void read() {
        // Not needed for sending-only packet
    }

    @Override
    public void write() {
        ByteBuf buffer = getBuffer();
        int protocol = getServerVersion().getProtocolVersion();

        // Write map ID (VarInt)
        writeVarInt(mapID);

        // Write scale
        buffer.writeByte(scale);

        // Write locked and tracking position (order changed in 1.17+)
        if (protocol < 755) { // < 1.17
            buffer.writeBoolean(trackingPosition);
            if (protocol >= 477) { // >= 1.14
                buffer.writeBoolean(locked);
            }
        } else { // >= 1.17
            buffer.writeBoolean(locked);
            buffer.writeBoolean(trackingPosition);
        }

        // Write icon count (always 0 for our use case)
        if (protocol < 755 || trackingPosition) {
            writeVarInt(0); // No icons
        }

        // Write map data
        buffer.writeByte(columns);
        
        if (columns > 0) {
            buffer.writeByte(rows);
            buffer.writeByte(posX);
            buffer.writeByte(posZ);
            writeVarInt(data.length);
            buffer.writeBytes(data);
        }
    }

    @Override
    public void copy(MapDataPacket wrapper) {
        this.mapID = wrapper.mapID;
        this.scale = wrapper.scale;
        this.locked = wrapper.locked;
        this.trackingPosition = wrapper.trackingPosition;
        this.columns = wrapper.columns;
        this.rows = wrapper.rows;
        this.posX = wrapper.posX;
        this.posZ = wrapper.posZ;
        this.data = wrapper.data;
    }

    // Helper method to write VarInt
    private void writeVarInt(int value) {
        ByteBuf buffer = getBuffer();
        while (true) {
            if ((value & ~0x7F) == 0) {
                buffer.writeByte(value);
                return;
            }
            buffer.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }
}
