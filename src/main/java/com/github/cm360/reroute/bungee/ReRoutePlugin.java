package com.github.cm360.reroute.bungee;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import com.github.cm360.reroute.bungee.hook.InitialHandlerHook;

import eu.software4you.bungeecord.plugin.ExtendedProxyPlugin;
import eu.software4you.common.collection.Pair;
import eu.software4you.transform.HookInjector;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class ReRoutePlugin extends ExtendedProxyPlugin implements Listener {

	private Map<PendingConnection, Pair<String, Long>> pendingRoutes;
	private Map<UUID, Pair<String, Long>> confirmedRoutes;
	
	@Override
    public void onLoad() {
        HookInjector.hook(new InitialHandlerHook(this));
    }
	
	@Override
	public void onEnable() {
		pendingRoutes = new HashMap<PendingConnection, Pair<String, Long>>();
		confirmedRoutes = new HashMap<UUID, Pair<String, Long>>();
		getProxy().getPluginManager().registerListener(this, this);
	}
	
	@Override
	public void onDisable() {
		getProxy().getPluginManager().unregisterListener(this);
	}
	
	public synchronized void cleanOldRoutes() {
		long checkTime = System.currentTimeMillis();
		long allowedDifference = 30000; // 30 seconds in ms
		pendingRoutes = pendingRoutes.entrySet().stream()
				.filter(e -> (e.getValue().getSecond() - checkTime <= allowedDifference))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		confirmedRoutes = confirmedRoutes.entrySet().stream()
				.filter(e -> (e.getValue().getSecond() - checkTime <= allowedDifference))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}
	
	public synchronized void requestReroute(PendingConnection connection, String destinationName) {
		pendingRoutes.put(connection, new Pair<String, Long>(destinationName, System.currentTimeMillis()));
		getLogger().info(String.format("The connection from '%s' has requested to be rerouted to %s.", connection.getSocketAddress(), destinationName));
	}
	
	@EventHandler
	public synchronized void onLoginEvent(LoginEvent event) {
		cleanOldRoutes();
		PendingConnection connection = event.getConnection();
		Pair<String, Long> routeRequest = pendingRoutes.remove(connection);
		if (routeRequest != null) {
			String destinationName = routeRequest.getFirst();
			getLogger().info(String.format("%s's next connection will attempt to be rerouted to %s.", connection.getName(), destinationName));
			confirmedRoutes.put(event.getConnection().getUniqueId(), new Pair<String, Long>(destinationName, System.currentTimeMillis()));
		}
	}
	
	@EventHandler
	public void onServerConnect(ServerConnectEvent event) {
		ProxiedPlayer eventPlayer = event.getPlayer();
		UUID eventPlayerId = eventPlayer.getUniqueId();
		// Reroute player if requested
		Pair<String, Long> routeRequest = confirmedRoutes.remove(eventPlayerId);
		if (routeRequest != null) {
			String destinationName = routeRequest.getFirst();
			ServerInfo destination = getProxy().getServerInfo(destinationName);
			if (destination == null) {
				getLogger().warning(String.format("%s can not be rerouted to %s, as this server cannot be found!", eventPlayer.getName(), destinationName));
			} else {
				getLogger().info(String.format("%s is being rerouted to %s.", eventPlayer.getName(), destinationName));
				event.setTarget(destination);
			}
		}
	}

}
