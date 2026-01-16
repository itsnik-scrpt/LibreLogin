/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.bungeecord;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.bungee.BungeePacketEventsBuilder;
import net.byteflux.libby.BungeeLibraryManager;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import xyz.kyngs.librelogin.api.provider.LibreLoginProvider;

public class BungeeCordBootstrap extends Plugin implements LibreLoginProvider<ProxiedPlayer, ServerInfo> {

    private BungeeCordLibreLogin libreLogin;

    @Override
    public void onLoad() {
        // Initialize PacketEvents
        PacketEvents.setAPI(BungeePacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings()
                .checkForUpdates(false)
                .debug(false);
        PacketEvents.getAPI().load();

        var libraryManager = new BungeeLibraryManager(this);

        getLogger().info("Loading libraries...");

        libraryManager.configureFromJSON();

        libreLogin = new BungeeCordLibreLogin(this);
    }

    @Override
    public void onEnable() {
        PacketEvents.getAPI().init();
        libreLogin.enable();
    }

    @Override
    public void onDisable() {
        libreLogin.disable();
        PacketEvents.getAPI().terminate();
    }

    @Override
    public BungeeCordLibreLogin getLibreLogin() {
        return libreLogin;
    }

}
