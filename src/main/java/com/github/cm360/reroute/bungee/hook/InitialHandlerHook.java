package com.github.cm360.reroute.bungee.hook;

import java.util.logging.Level;

import com.github.cm360.reroute.bungee.ReRoutePlugin;

import eu.software4you.transform.Callback;
import eu.software4you.transform.Hook;
import eu.software4you.transform.HookPoint;
import eu.software4you.transform.Hooks;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.protocol.packet.Handshake;

@Hooks("net.md_5.bungee.connection.InitialHandler")
public class InitialHandlerHook {
	
	private ReRoutePlugin plugin;
	
	public InitialHandlerHook(ReRoutePlugin plugin) {
		this.plugin = plugin;
	}

	@Hook(value = "handle(Lnet/md_5/bungee/protocol/packet/Handshake;)V", at = HookPoint.HEAD)
	public void hook_handle(Handshake handshake, Callback<?> cb) {
		try {
			String host = handshake.getHost();
			if (host.contains("\0")) {
				String[] hostSplit = host.split("\0");
				if (hostSplit[1].equals("ReRoute-Fabric"))
					plugin.requestReroute((PendingConnection) cb.self(), hostSplit[2]);
			}
		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "Error processing handshake packet", e);
		}
	}

}
