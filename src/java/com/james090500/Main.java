package com.james090500;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.UUID;
import java.util.logging.Level;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class Main extends Plugin implements Listener {
	
	@Override
	public void onEnable() {
		getProxy().getPluginManager().registerListener(this, this);
	}

	@EventHandler
	public void preLogin(PreLoginEvent event) {
		//Check player is logging in through DragonProxy
		String playerIp = ((InetSocketAddress) event.getConnection().getSocketAddress()).getAddress().toString().replace("/", "");
		if(playerIp.equals("127.0.0.1") || playerIp.equals("192.168.129.10")) {
			
			//Set online mode to false for that player
			event.getConnection().setOnlineMode(false);
			
			//See if we can get a uuid for the bedrock player (useful for allowing players to continue using their java account)
			//Security risk if gamer tags is already taken.
			String uuid = getUUID(event.getConnection().getName());					
			if(uuid != null) {
				UUID finalUUID = UUID.fromString(uuid.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
				event.getConnection().setUniqueId(finalUUID);
			} else {
				UUID dummyUUID = UUID.nameUUIDFromBytes(event.getConnection().getName().getBytes());
				event.getConnection().setUniqueId(dummyUUID);
			}
		}		
	}
	
	private String getUUID(String username) {
		try {
			//Send request
			URL apiUrl = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);			
			HttpURLConnection con = (HttpURLConnection) apiUrl.openConnection();
			con.setRequestMethod("GET");
			
			//Standard Response
			if(con.getResponseCode() == 200) {
				//Read response
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
	
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				
				//Get JSON Response
				JsonObject json = new Gson().fromJson(response.toString(), JsonObject.class);
				
				//Check "id" (uuid) exists in response
				if(json.get("id") == null) {
					getLogger().log(Level.WARNING, "An API Error has occured: " + json.get("errorMessage").getAsString());
					return null;
				}
				
				//Return uuid
				String uuid = json.get("id").getAsString();
				return uuid;
			//To many requests
			} else if(con.getResponseCode() == 429) {
				getLogger().log(Level.WARNING, "An API Error has occured: TooManyRequestsException");
				return null;
			//No user found
			} else if(con.getResponseCode() == 204) {				
				return null;
			}
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}
}