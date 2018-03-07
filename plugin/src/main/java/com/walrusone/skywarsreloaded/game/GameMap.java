package com.walrusone.skywarsreloaded.game;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import com.google.common.collect.Iterables;
import com.walrusone.skywarsreloaded.managers.MatchManager;
import com.walrusone.skywarsreloaded.managers.PlayerStat;
import com.walrusone.skywarsreloaded.managers.WorldManager;
import com.walrusone.skywarsreloaded.matchevents.AnvilRainEvent;
import com.walrusone.skywarsreloaded.matchevents.ArrowRainEvent;
import com.walrusone.skywarsreloaded.matchevents.ChestRefillEvent;
import com.walrusone.skywarsreloaded.matchevents.CrateDropEvent;
import com.walrusone.skywarsreloaded.matchevents.DeathMatchEvent;
import com.walrusone.skywarsreloaded.matchevents.DisableRegenEvent;
import com.walrusone.skywarsreloaded.matchevents.EnderDragonEvent;
import com.walrusone.skywarsreloaded.matchevents.HealthDecayEvent;
import com.walrusone.skywarsreloaded.matchevents.MatchEvent;
import com.walrusone.skywarsreloaded.matchevents.MobSpawnEvent;
import com.walrusone.skywarsreloaded.matchevents.WitherEvent;
import com.walrusone.skywarsreloaded.menus.ArenaMenu;
import com.walrusone.skywarsreloaded.menus.ArenasMenu;
import com.walrusone.skywarsreloaded.menus.gameoptions.ChestOption;
import com.walrusone.skywarsreloaded.menus.gameoptions.GameOption;
import com.walrusone.skywarsreloaded.menus.gameoptions.HealthOption;
import com.walrusone.skywarsreloaded.menus.gameoptions.KitVoteOption;
import com.walrusone.skywarsreloaded.menus.gameoptions.ModifierOption;
import com.walrusone.skywarsreloaded.menus.gameoptions.TimeOption;
import com.walrusone.skywarsreloaded.menus.gameoptions.WeatherOption;
import com.walrusone.skywarsreloaded.menus.gameoptions.objects.CoordLoc;
import com.walrusone.skywarsreloaded.menus.gameoptions.objects.GameKit;
import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.enums.MatchState;
import com.walrusone.skywarsreloaded.game.cages.Cage;
import com.walrusone.skywarsreloaded.game.cages.CageType;
import com.walrusone.skywarsreloaded.game.cages.CubeCage;
import com.walrusone.skywarsreloaded.game.cages.DomeCage;
import com.walrusone.skywarsreloaded.game.cages.PyramidCage;
import com.walrusone.skywarsreloaded.game.cages.SphereCage;
import com.walrusone.skywarsreloaded.game.cages.StandardCage;
import com.walrusone.skywarsreloaded.utilities.Messaging;
import com.walrusone.skywarsreloaded.utilities.Party;
import com.walrusone.skywarsreloaded.utilities.Util;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import java.util.ArrayList;

public class GameMap {
 
	static {
		new ArenasMenu();
	}
	private static ArrayList<GameMap> arenas;

	private ArrayList<Crate> crates = new ArrayList<Crate>();
	private boolean forceStart;
	private boolean allowFallDamage;
	private boolean allowRegen;
    private boolean thunder;
    private String winner = "";
    private int strikeCounter;
    private int nextStrike;
    private MatchState matchState;
    private ArrayList<TeamCard> teamCards;
    private int teamSize;

    private ArrayList<UUID> spectators = new ArrayList<UUID>();
    private String name;
    private int timer;
    private int restartTimer = -1;
    private int minPlayers;
    private GameKit kit;
    private Cage cage;
    private String currentTime;
    private String currentHealth;
    private String currentChest;
    private String currentWeather;
    private String currentModifier;
    private KitVoteOption kitVoteOption;
    private ChestOption chestOption;
    private HealthOption healthOption;
    private TimeOption timeOption;
    private WeatherOption weatherOption;
    private ModifierOption modifierOption;
	private ArrayList<CoordLoc> chests;
	private String displayName;
	private String designedBy;
	private ArrayList<SWRSign> signs;
	private Scoreboard scoreboard;
	private Objective objective;
	private boolean registered;
	private String arenakey;
	private GameQueue joinQueue;
	private boolean inEditing = false;
	private CoordLoc spectateSpawn;
	private ArrayList<CoordLoc> deathMatchSpawns;
	private boolean legacy = false;
	private ArrayList<MatchEvent> events = new ArrayList<MatchEvent>();
	private ArrayList<String> deathMatchWaiters = new ArrayList<String>();
	private ArrayList<String> anvils = new ArrayList<String>();
		
    public GameMap(final String name) {
    	this.name = name;
    	this.matchState = MatchState.OFFLINE;
    	teamCards = new ArrayList<TeamCard>();
    	deathMatchSpawns = new ArrayList<CoordLoc>();
    	signs = new ArrayList<SWRSign>();
    	chests = new ArrayList<CoordLoc>();
    	loadArenaData();
        this.thunder = false;
        allowRegen = true;
        timer = SkyWarsReloaded.getCfg().getWaitTimer();
        joinQueue = new GameQueue(this);
        arenakey = name + "menu";
        if (SkyWarsReloaded.getCfg().kitVotingEnabled()) {
			kitVoteOption = new KitVoteOption(this, name + "kitVote");
        }
        chestOption = new ChestOption(this, name + "chest");
        healthOption = new HealthOption(this, name + "health");
        timeOption =  new TimeOption(this, name + "time");
        weatherOption = new WeatherOption(this, name + "weather");
        modifierOption = new ModifierOption(this, name + "modifier");
        if (legacy) {
        	 boolean loaded = loadWorldForScanning(name);
             if (loaded) {
             	ChunkIterator();
      			SkyWarsReloaded.getWM().deleteWorld(name);
      			saveArenaData();
             }
        }
        loadEvents();
        if (registered) {
        	registerMap();
        }
        new ArenaMenu(arenakey, this);
    }

	private void loadEvents() {
		File dataDirectory = SkyWarsReloaded.get().getDataFolder();
        File mapDataDirectory = new File(dataDirectory, "mapsData");

        if (!mapDataDirectory.exists() && !mapDataDirectory.mkdirs()) {
        	return;
        }

        File mapFile = new File(mapDataDirectory, name + ".yml");
        FileConfiguration fc = YamlConfiguration.loadConfiguration(mapFile);
       	events.add(new DisableRegenEvent(this, fc.getBoolean("events.DisableRegenEvent.enabled")));
       	events.add(new HealthDecayEvent(this, fc.getBoolean("events.HealthDecayEvent.enabled")));
       	events.add(new EnderDragonEvent(this, fc.getBoolean("events.EnderDragonEvent.enabled")));
       	events.add(new WitherEvent(this, fc.getBoolean("events.WitherEvent.enabled")));
       	events.add(new MobSpawnEvent(this, fc.getBoolean("events.MobSpawnEvent.enabled")));
        events.add(new ChestRefillEvent(this, fc.getBoolean("events.ChestRefillEvent.enabled")));
       	events.add(new DeathMatchEvent(this, fc.getBoolean("events.DeathMatchEvent.enabled")));
       	events.add(new ArrowRainEvent(this, fc.getBoolean("events.ArrowRainEvent.enabled")));
        events.add(new AnvilRainEvent(this, fc.getBoolean("events.AnvilRainEvent.enabled")));
       	events.add(new CrateDropEvent(this, fc.getBoolean("events.CrateDropEvent.enabled")));
	}

	public void update() {
		updateArenasManager();
		this.updateArenaManager();
        this.updateSigns();
        this.sendBungeeUpdate();
        if (this.isRegistered()) {
            this.updateScoreboard();
        }
        if (SkyWarsReloaded.getIC().has("joinmenu")) {
            SkyWarsReloaded.getIC().getMenu("joinmenu").update();
        }
	}
   
	/*Player Handling Methods*/
	
	public boolean addPlayers(final Player player) {
		if (Util.get().isBusy(player.getUniqueId())) {
			return false;
		}
		boolean result = false;
		PlayerStat ps = PlayerStat.getPlayerStats(player.getUniqueId());
		Collections.shuffle(teamCards);
		TeamCard reserved = null;
		for (TeamCard tCard: teamCards) {
			if (tCard.getFullCount() > 0) {
				reserved = tCard.sendReservation(player, ps);
				break;
			}
		}
		if (reserved != null) {
			result = reserved.joinGame(player);
		}
    	this.update();
    	return result;
    }
	
	public boolean addPlayers(final Party party) {
		TeamCard team = null;
		Map<TeamCard, ArrayList<Player>> players = new HashMap<TeamCard, ArrayList<Player>>();
		if (teamSize == 1) {
			for (UUID uuid: party.getMembers()) {
				Player player = Bukkit.getPlayer(uuid);
				if (Util.get().isBusy(uuid)) {
					party.sendPartyMessage(new Messaging.MessageFormatter().setVariable("player", player.getName()).format("party.memberbusy"));
				} else {
					PlayerStat ps = PlayerStat.getPlayerStats(uuid);
					Collections.shuffle(teamCards);
					
					if (teamSize == 1) {
						for (TeamCard tCard: teamCards) {
							if (tCard.getFullCount() > 0) {
								TeamCard reserve = tCard.sendReservation(player, ps);
								this.update();
								if (reserve != null) {
									if (players.get(reserve) == null) {
										players.put(reserve, new ArrayList<Player>());
									}
									players.get(reserve).add(player);
								}
							}
						}
					}
				}
			}
		} else {
			for (TeamCard tCard: teamCards) {
				if (tCard.getFullCount() > party.getSize()) {
					for (int i = 0; i < party.getSize(); i++) {
						Player player = Bukkit.getPlayer(party.getMembers().get(i));
						PlayerStat ps = PlayerStat.getPlayerStats(player.getUniqueId());
						TeamCard reserve = tCard.sendReservation(player, ps);
						if (reserve != null) {
							if (players.get(reserve) == null) {
								players.put(reserve, new ArrayList<Player>());
							}
							players.get(reserve).add(player);
						}
						team = reserve;
					}
					this.update();
				}
			}
		}

		boolean result = false;
		if (teamSize == 1 && players.size() == party.getSize()) {
			for (TeamCard tCard: players.keySet()) {
				result = tCard.joinGame(players.get(tCard).get(0));
			}
			this.update();
			return result;
		} else if (teamSize > 1 && team != null && players.get(team).size() == party.getSize()) {
			for (int i = 0; i < players.get(team).size(); i++) {
				result = team.joinGame(players.get(team).get(i));
			}
			this.update();
			return result;
		} else {
			for (ArrayList<Player> play: players.values()) {
				for (Player player: play) {
					PlayerCard pCard = this.getPlayerCard(player);
					pCard.reset();
				}
			}
		}
    	this.update();
    	return false;
	}

	public boolean removePlayer(final UUID uuid) {
		boolean result = false;
		for (TeamCard tCard: teamCards) {
			result = tCard.removePlayer(uuid);
		}
    	this.update();
    	return result;
    }
 
    public ArrayList<Player> getAlivePlayers() {
    	ArrayList<Player> alivePlayers = new ArrayList<Player>();
    	for (TeamCard tCard: teamCards) {
    		for (PlayerCard pCard: tCard.getPlayers()) {
        		if (pCard.getPlayer() != null) {
            		if (!mapContainsDead(pCard.getPlayer().getUniqueId())) {
                		alivePlayers.add(pCard.getPlayer());
            		}
        		}
        	}
    	}
    	return alivePlayers;
    }
    
    public boolean mapContainsDead(UUID uuid) {
    	for(TeamCard tCard: teamCards) {
    		if (tCard.getDead().contains(uuid)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public ArrayList<Player> getAllPlayers() {
    	ArrayList<Player> allPlayers = new ArrayList<Player>();
    	for (TeamCard tCard: teamCards) {
    		for (PlayerCard pCard: tCard.getPlayers()) {
        		if (pCard.getPlayer() != null) {
                	allPlayers.add(pCard.getPlayer());
        		}
        	}
    	}
    	return allPlayers;
    }
    
    public boolean canAddPlayer() {
    	if (!(this.matchState == MatchState.WAITINGSTART && this.registered)) {
    		return false;
    	}
    	for (TeamCard tCard: teamCards) {
    		if (tCard.getFullCount() > 0) {
    			return true;
    		}
    	}
        return false;
    }
    
    public boolean canAddParty(Party party) {
    	if (!(this.matchState == MatchState.WAITINGSTART && this.registered)) {
    		return false;
    	}
    	if (teamSize == 1) {
    		int playerCount = getPlayerCount();
    		return playerCount + party.getSize()-1 < teamCards.size();
    	} else {
    		for (TeamCard tCard: teamCards) {
        		if (tCard.getFullCount() > party.getSize()) {
        			return true;
        		}
        	}
    	}
    	return false;
    }
    
	/*Map Handling Methods*/
	
    static {
        GameMap.arenas = new ArrayList<GameMap>();
    }
	
	public static GameMap getMap(final String mapName) {
    	shuffle();
    	for (final GameMap map : GameMap.arenas) {
            if (map.name.equals(ChatColor.stripColor(mapName))) {
                return map;
            }
        }
        return null;
    }
	
	public static boolean addMap(String name, boolean forceRegister, boolean startup) {
		GameMap gMap = new GameMap(name);
		arenas.add(gMap);
		return gMap.isRegistered();
	}
	
	public void removeMap() {
		unregister();
		File dataDirectory = new File (SkyWarsReloaded.get().getDataFolder(), "maps");
		File target = new File (dataDirectory, name);
		SkyWarsReloaded.getWM().deleteWorld(target);

        File mapDataDirectory = new File(dataDirectory, "mapsData");
        if (!mapDataDirectory.exists() && !mapDataDirectory.mkdirs()) {
        	return;
        }
        File mapFile = new File(mapDataDirectory, name + ".yml");
        mapFile.delete();
		arenas.remove(this);
	}
        
    public static void loadMaps() {
    	File mapFile = new File(SkyWarsReloaded.get().getDataFolder(), "maps.yml");
        if (mapFile.exists()) {
        	updateMapData();
        }
    	arenas.clear();
    	File dataDirectory = SkyWarsReloaded.get().getDataFolder();
		File maps = new File (dataDirectory, "maps");
		if (maps.exists() && maps.isDirectory()) {
			for (File map : maps.listFiles()) {
				if (map.isDirectory()) {
					addMap(map.getName(), true, true);
				} 
			}
		} else {
			SkyWarsReloaded.get().getLogger().info("Maps directory is missing or no Maps were found!");
		} 
    }
    
	private static void updateMapData() {
		 File mapFile = new File(SkyWarsReloaded.get().getDataFolder(), "maps.yml");       
	        if (mapFile.exists()) {
	            FileConfiguration storage = YamlConfiguration.loadConfiguration(mapFile);

	            if (storage.getConfigurationSection("maps") != null) {
	                for (String key: storage.getConfigurationSection("maps").getKeys(false)) {
	                	String name = key;
	                	String displayname = storage.getString("maps." + key + ".displayname");
	                	int minplayers = storage.getInt("maps." + key + ".minplayers");
	                	String creator = storage.getString("maps." + key + ".creator");
	                	List<String> signs = storage.getStringList("maps." + key + ".signs");
	                	boolean registered = storage.getBoolean("maps." + key + ".registered");
	                	
	            		File dataDirectory = SkyWarsReloaded.get().getDataFolder();
	                    File mapDataDirectory = new File(dataDirectory, "mapsData");

	                    if (!mapDataDirectory.exists() && !mapDataDirectory.mkdirs()) {
	                    	return;
	                    }

	                    File newMapFile = new File(mapDataDirectory, name + ".yml");
	                    copyDefaults(newMapFile);
	                    FileConfiguration fc = YamlConfiguration.loadConfiguration(newMapFile);
	                    fc.set("displayname", displayname);
	    	            fc.set("minplayers", minplayers);
	    	            fc.set("creator", creator);
	    	            fc.set("signs", signs);
	    	            fc.set("registered", registered);
	    	            fc.set("spectateSpawn", "0:95:0");
	    	            fc.set("deathMatchSpawns", null);
	    	            fc.set("legacy", true);
	    	            try {
							fc.save(newMapFile);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	                }
	            }
	            mapFile.delete();
	        }
	}
	
	public void saveArenaData() {
		File dataDirectory = SkyWarsReloaded.get().getDataFolder();
        File mapDataDirectory = new File(dataDirectory, "mapsData");

        if (!mapDataDirectory.exists() && !mapDataDirectory.mkdirs()) {
        	return;
        }

        File mapFile = new File(mapDataDirectory, name + ".yml");
        if (!mapFile.exists()) {
        	SkyWarsReloaded.get().getLogger().info("File doesn't exist!");
        	return;
        }
        copyDefaults(mapFile);
        FileConfiguration fc = YamlConfiguration.loadConfiguration(mapFile);
        fc.set("displayname", displayName);
        fc.set("minplayers", minPlayers);
        fc.set("creator", designedBy);
        fc.set("registered", registered);
        fc.set("spectateSpawn", spectateSpawn.getLocation());
        fc.set("cage", cage.getType().toString().toLowerCase());
        fc.set("teamSize", teamSize);
       
        List<String> spawns = new ArrayList<String>();
        for (TeamCard tCard: teamCards) {
        	spawns.add(tCard.getSpawn().getLocation());
        }
        fc.set("spawns", spawns);
        
        List<String> dSpawns = new ArrayList<String>();
        for (CoordLoc loc: deathMatchSpawns) {
        	dSpawns.add(loc.getLocation());
        }
        fc.set("deathMatchSpawns", dSpawns);
        
        List<String> stringSigns = new ArrayList<String>();
   	 	for (SWRSign s: signs) {
   	 		stringSigns.add(Util.get().locationToString(s.getLocation()));
   	 	}
   	 	fc.set("signs", stringSigns);

   	 	List<String> stringChests = new ArrayList<String>();
   	 	for (CoordLoc chest: chests) {
   	 		stringChests.add(chest.getLocation());
   	 	}
   	 	fc.set("chests", stringChests);
   	 	fc.set("legacy", null);
   	 	try {
			fc.save(mapFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void loadArenaData() {
		File dataDirectory = SkyWarsReloaded.get().getDataFolder();
        File mapDataDirectory = new File(dataDirectory, "mapsData");

        if (!mapDataDirectory.exists() && !mapDataDirectory.mkdirs()) {
        	return;
        }

        File mapFile = new File(mapDataDirectory, name + ".yml");
        copyDefaults(mapFile);
        
        FileConfiguration fc = YamlConfiguration.loadConfiguration(mapFile);
        displayName = fc.getString("displayname", name);
        designedBy = fc.getString("creator", "");
        registered = fc.getBoolean("registered", false);
        spectateSpawn = Util.get().getCoordLocFromString(fc.getString("spectateSpawn", "0:95:0"));
        legacy = fc.getBoolean("legacy");
        teamSize = fc.getInt("teamSize", 1);
     
        String cage = fc.getString("cage");
        CageType ct = CageType.matchType(cage.toUpperCase());
        setCage(ct);		
        
        List<String> spawns = fc.getStringList("spawns");
        List<String> dSpawns = fc.getStringList("deathMatchSpawns");
        List<String> stringSigns = fc.getStringList("signs");
        List<String> stringChests = fc.getStringList("chests");
       
        for (String spawn: spawns) {
        	addTeamCard(Util.get().getCoordLocFromString(spawn));
        }
        int def = 2;
        if (teamCards.size() > 4) {
        	def = teamCards.size()/2;
        }
        minPlayers = fc.getInt("minplayers", def);
        
        for (String dSpawn: dSpawns) {
        	deathMatchSpawns.add(Util.get().getCoordLocFromString(dSpawn));
        }
        
   	 	for (String s: stringSigns) {
   	 		signs.add(new SWRSign(name, Util.get().stringToLocation(s)));
   	 	}

   	 	for (String chest: stringChests) {
   	 		addChest(Util.get().getCoordLocFromString(chest));
   	 	}
   	 	try {
			fc.save(mapFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static World createNewMap(String mapName) {
    	World newWorld = SkyWarsReloaded.getWM().createEmptyWorld(mapName);
		if (newWorld == null) {
			return null;
		}
		newWorld.save();
		SkyWarsReloaded.getWM().unloadWorld(mapName);
		File dataDirectory = new File (SkyWarsReloaded.get().getDataFolder(), "maps");
		File target = new File (dataDirectory, mapName);
		SkyWarsReloaded.getWM().deleteWorld(target);
		File source = new File (SkyWarsReloaded.get().getServer().getWorldContainer().getAbsolutePath(), mapName);
		SkyWarsReloaded.getWM().copyWorld(source, target);
		SkyWarsReloaded.getWM().loadWorld(mapName);
		addMap(mapName, false, false);
		return SkyWarsReloaded.get().getServer().getWorld(mapName);
	}
	
	public boolean registerMap() {
		if (inEditing) {
			saveMap(null);
		}
    	if (teamCards.size() > 1) {
    		registered = true;
            refreshMap();
            SkyWarsReloaded.get().getLogger().info("Registered Map " + name + "!");
    	} else {
    		registered = false;
    		SkyWarsReloaded.get().getLogger().info("Could Not Register Map: " + name + " - Map must have at least 2 Spawn Points");
    	}
    	return registered;
	}
	
	public void unregister() {
		this.registered = false;
		saveArenaData();
		stopGameInProgress();
	}
	
	public void stopGameInProgress() {
		this.matchState = MatchState.OFFLINE;
		for (final UUID uuid: this.getSpectators()) {
    		final Player player = SkyWarsReloaded.get().getServer().getPlayer(uuid);
    		if (player != null) {
    			MatchManager.get().removeSpectator(this, player);
    		}
    	}
        for (final Player player : this.getAlivePlayers()) {
        	if (player != null) {
                MatchManager.get().playerLeave(player, DamageCause.CUSTOM, true, false);
        	}
        }
        SkyWarsReloaded.getWM().deleteWorld(this.getName());
	}
    
    public static boolean loadWorldForScanning(String name) {
        	File dataDirectory = SkyWarsReloaded.get().getDataFolder();
    		File maps = new File (dataDirectory, "maps");
    		
    			String root = SkyWarsReloaded.get().getServer().getWorldContainer().getAbsolutePath();
    			File rootDirectory = new File(root);
    			WorldManager wm = SkyWarsReloaded.getWM();
    			File source = new File(maps, name);
    			File target = new File(rootDirectory, name);
    			wm.copyWorld(source, target);
    			boolean mapExists = false;
    			if(target.isDirectory()) {			 
    				if(target.list().length > 0) {
    		 			mapExists = true;
    				}	 
    			}
    			if (mapExists) {
    				SkyWarsReloaded.getWM().deleteWorld(name);
    			}
    			
    			wm.copyWorld(source, target);
    			
    			boolean loaded = SkyWarsReloaded.getWM().loadWorld(name);
    			if(!loaded) {
    				SkyWarsReloaded.get().getLogger().info("Could Not Load Map: " + name);
    			}
    			return loaded;
	}

	public static ArrayList<GameMap> getMaps() {
		return new ArrayList<GameMap>(arenas);
	}
	
	public static ArrayList<GameMap> getPlayableArenas() {
		ArrayList<GameMap> sorted = new ArrayList<GameMap>();
		for (GameMap gMap: arenas) {
			if (gMap.isRegistered()) {
				sorted.add(gMap);
			}
		}
		Collections.sort(sorted, new GameMapComparator());
		return sorted;
	}
	
	public static ArrayList<GameMap> getSortedArenas() {
		ArrayList<GameMap> sorted = new ArrayList<GameMap>();
		for (GameMap gMap: arenas) {
			sorted.add(gMap);
		}
		Collections.sort(sorted, new GameMapComparator());
		return sorted;
	}
	
	public static boolean mapRegistered(String name) {
		for (GameMap gMap: arenas) {
			if (gMap.getName().equals(name)) {
				return gMap.isRegistered();
			}
		}
		return false;
	}
	   
	public boolean loadMap() {
			WorldManager wm = SkyWarsReloaded.getWM();
			String mapName = name;
			boolean mapExists = false;
	    	File dataDirectory = SkyWarsReloaded.get().getDataFolder();
			File maps = new File (dataDirectory, "maps");
			File source = new File(maps, name);
			String root = SkyWarsReloaded.get().getServer().getWorldContainer().getAbsolutePath();
			File rootDirectory = new File(root);
			File target = new File(rootDirectory, mapName);
			if(target.isDirectory()) {			 
				if(target.list().length > 0) {
		 			mapExists = true;
				}	 
			}
			if (mapExists) {
				SkyWarsReloaded.getWM().deleteWorld(mapName);
			}
			
			wm.copyWorld(source, target);
			
			boolean loaded = SkyWarsReloaded.getWM().loadWorld(mapName);
			
			if (loaded) {
				World world = SkyWarsReloaded.get().getServer().getWorld(mapName);
			    world.setAutoSave(false);
			    world.setThundering(false);
			    world.setStorm(false);
			    world.setDifficulty(Difficulty.NORMAL);
			    world.setSpawnLocation(2000, 0, 2000);
			    world.setTicksPerAnimalSpawns(1);
			    world.setTicksPerMonsterSpawns(1);
		        world.setGameRuleValue("doMobSpawning", "false");
		        world.setGameRuleValue("mobGriefing", "false");
		        world.setGameRuleValue("doFireTick", "false");
		        world.setGameRuleValue("showDeathMessages", "false");
		        cage.createSpawnPlatforms(this);
			}
			return loaded;
	}
	
	public void ChunkIterator() {
		World chunkWorld;
		chunkWorld = SkyWarsReloaded.get().getServer().getWorld(name);
		int mapSize = SkyWarsReloaded.getCfg().getMaxMapSize();
		int max1 = mapSize/2;
		int min1 = -mapSize/2;
		Block min = chunkWorld.getBlockAt(min1, 0, min1);
		Block max = chunkWorld.getBlockAt(max1, 0, max1);
		Chunk cMin = min.getChunk();
		Chunk cMax = max.getChunk();
		teamCards.clear();
		chests.clear();
		
		for(int cx = cMin.getX(); cx < cMax.getX(); cx++) {
			for(int cz = cMin.getZ(); cz < cMax.getZ(); cz++) {
		           Chunk currentChunk = chunkWorld.getChunkAt(cx, cz);
		           currentChunk.load(true);

		           for(BlockState te : currentChunk.getTileEntities()) {
		               	if(te instanceof Beacon){
			                  Beacon beacon = (Beacon) te;
			                  Block block = beacon.getBlock().getRelative(0, -1, 0);
			                  if(!block.getType().equals(Material.GOLD_BLOCK) && !block.getType().equals(Material.IRON_BLOCK) 
			                		  && !block.getType().equals(Material.DIAMOND_BLOCK)&& !block.getType().equals(Material.EMERALD_BLOCK)) {
				                  Location loc = beacon.getLocation();
				                  teamCards.add(new TeamCard(teamSize, new CoordLoc(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), this));
			                  }
			            } else if (te instanceof Chest) {
				                  Chest chest = (Chest) te;
				                  addChest(chest);
			            } 
		           }
		        }
	     }
		
	}
	
	public static void editMap(GameMap gMap, Player player) {
		gMap.unregister();
		gMap.setEditing(true);
		String worldName = gMap.getName();
		boolean loaded = false;
		for (World world: SkyWarsReloaded.get().getServer().getWorlds()) {
			if (world.getName().equals(worldName)) {
				loaded = true;
			}
		} 
		
		if (!loaded) {
			File dataDirectory = new File(SkyWarsReloaded.get().getDataFolder(), "maps");
			File source = new File (dataDirectory, worldName);
			File target = new File (SkyWarsReloaded.get().getServer().getWorldContainer().getAbsolutePath(), worldName);
			boolean mapExists = false;
			if(target.isDirectory()) {			 
				if(target.list().length > 0) {
		 			mapExists = true;
				}	 
			}
			if (mapExists) {
				SkyWarsReloaded.getWM().deleteWorld(worldName);
			}
			SkyWarsReloaded.getWM().copyWorld(source, target);
		}
		loaded = SkyWarsReloaded.getWM().loadWorld(worldName);
		if (loaded) {
			World editWorld = SkyWarsReloaded.get().getServer().getWorld(worldName);
			for (TeamCard tCard: gMap.getTeamCards()) {
				if (tCard.getSpawn() != null) {
					editWorld.getBlockAt(tCard.getSpawn().getX(), tCard.getSpawn().getY(), tCard.getSpawn().getZ()).setType(Material.DIAMOND_BLOCK);
				}
			}
			for (CoordLoc cl: gMap.getDeathMatchSpawns()) {
					editWorld.getBlockAt(cl.getX(), cl.getY(), cl.getZ()).setType(Material.EMERALD_BLOCK);
			}
			SkyWarsReloaded.get().getServer().getScheduler().scheduleSyncDelayedTask(SkyWarsReloaded.get(), new Runnable() {
				public void run() {
					player.teleport(new Location(editWorld, 0, 95, 0), TeleportCause.PLUGIN);
					player.setGameMode(GameMode.CREATIVE);
					player.setAllowFlight(true);
					player.setFlying(true);
				}
			}, 20);
		} else {
			player.sendMessage(new Messaging.MessageFormatter().format("error.map-fail-load"));
		}
	}

	
	public void refreshMap() {
		for (TeamCard tCard: teamCards) {
			tCard.reset();
		}
		thunder = false;
		forceStart = false;
		allowRegen = true;
        kit = null;
        restartTimer = -1;
        winner = "";
        deathMatchWaiters.clear();
		if (SkyWarsReloaded.getCfg().kitVotingEnabled()) {
			kitVoteOption.restore();
		}
		for (MatchEvent event: events) {
			event.reset();
		}
		healthOption.restore();
        chestOption.restore();
        timeOption.restore();
        weatherOption.restore();
        modifierOption.restore();
        SkyWarsReloaded.getWM().deleteWorld(name);
        this.loadMap();
        final GameMap gMap = this;
        if (SkyWarsReloaded.get().isEnabled()) {
            new BukkitRunnable() {
				@Override
				public void run() {
					matchState = MatchState.WAITINGSTART;
			        getScoreBoard();
			        MatchManager.get().start(gMap);
			        update();
				}
            }.runTaskLater(SkyWarsReloaded.get(), 40);
        }  
	}
	
	/*Inventories*/
	
	public void updateArenaManager() {
		if (SkyWarsReloaded.getIC().has(arenakey)) {
			SkyWarsReloaded.getIC().getMenu(arenakey).update();
		}
	}
	
	public static void openArenasManager(Player player) {
		if (player.hasPermission("sw.arenas")) {
			SkyWarsReloaded.getIC().show(player, "arenasmenu");
		}
	}
	
	public static void updateArenasManager() {
		if (SkyWarsReloaded.getIC().has("arenasmenu")) {
			SkyWarsReloaded.getIC().getMenu("arenasmenu").update();
		}
	}
	
	public void setKitVote(Player player, GameKit kit2) {
		for (TeamCard tCard: teamCards) {
			for (PlayerCard pCard: tCard.getPlayers()) {
				if (pCard.getPlayer() != null && pCard.getPlayer().equals(player)) {
					pCard.setKitVote(kit2);
					return;
				}
			}
		}
	}
   
    public GameKit getSelectedKit(Player player) {
    	for (TeamCard tCard: teamCards) {
        	for (PlayerCard pCard: tCard.getPlayers()) {
        		if (pCard != null) {
        			if (pCard.getPlayer() != null && pCard.getPlayer().equals(player)) {
        				return pCard.getKitVote();
        			}
        		}
        	}
    	}
    	return null;
    }
    
	/*Scoreboard Methods*/
	
	public void getScoreBoard() {
		if (scoreboard != null) {
            resetScoreboard();
        }
		ScoreboardManager manager = SkyWarsReloaded.get().getServer().getScoreboardManager();
		scoreboard = manager.getNewScoreboard();
        objective = scoreboard.registerNewObjective("info", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		updateScoreboard();
	}
	
	public void updateScoreboard() {
        if (objective != null) {
            objective.unregister();
        }
        if (scoreboard != null) {
        	objective = scoreboard.registerNewObjective("info", "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            String sb = "";
            if (matchState.equals(MatchState.WAITINGSTART)) {
            	sb = "scoreboards.waitboard.line";
            } else if (matchState.equals(MatchState.PLAYING)) {
            	sb = "scoreboards.playboard.line";
            } else if (matchState.equals(MatchState.ENDING)) {
            	sb = "scoreboards.endboard.line";
            	if (restartTimer == -1) {
            		startRestartTimer();
            	}
            }
            
            for (int i = 1; i < 17; i++) {
            	if (i == 1) {
        	        String title = getScoreboardLine(sb + i);
        	        if (title.length() > 32) {
        	        	title = title.substring(0, 31);
        	        }
        	        objective.setDisplayName(title);
        		} else {
        			String s = "";
        			if (getScoreboardLine(sb + i).length() == 0) {
        				for (int j = 0; j < i; j++) {
        					s = s + " ";
        				}
        			} else {
        				s = getScoreboardLine(sb + i);
        			}
        			if (!s.equalsIgnoreCase("remove")) {
        				if (s.length() > 40) {
        	    	        s = s.substring(0, 39);
        				}
            			Score score = objective.getScore(s);
        				score.setScore(17-i);
        			}
        		}
            }	
        }   
	}
	
	private void startRestartTimer() {
		restartTimer = SkyWarsReloaded.getCfg().getTimeAfterMatch();
		if (SkyWarsReloaded.get().isEnabled()) {
			new BukkitRunnable() {
				@Override
				public void run() {
					restartTimer--;
					if (restartTimer == 0) {
						this.cancel();
					}
					updateScoreboard();
				}
			}.runTaskTimer(SkyWarsReloaded.get(), 0, 20);
		}
	}
	

	
	private String getScoreboardLine(String lineNum) {
		return new Messaging.MessageFormatter()
				.setVariable("mapname", displayName)
				.setVariable("time", "" + Util.get().getFormattedTime(timer))
				.setVariable("players", "" + getAlivePlayers().size())
				.setVariable("maxplayers", "" + teamCards.size())
				.setVariable("winner", winner)
				.setVariable("restarttime", "" + restartTimer)
				.setVariable("chestvote", ChatColor.stripColor(currentChest))
				.setVariable("timevote", ChatColor.stripColor(currentTime))
				.setVariable("healthvote", ChatColor.stripColor(currentHealth))
				.setVariable("weathervote", ChatColor.stripColor(currentWeather))
				.setVariable("modifiervote", ChatColor.stripColor(currentModifier))
				.format(lineNum);
	}

    private void resetScoreboard() {
        if (objective != null) {
            objective.unregister();
        }
        
        if (scoreboard != null) {
            scoreboard = null;
        }
    }

	public Scoreboard getScoreboard() {
		return scoreboard;
	}
    
	/*Bungeemode Methods*/
	
	public void sendBungeeUpdate() {
		if (SkyWarsReloaded.getCfg().bungeeMode()) {
			String playerCount = "" + this.getAlivePlayers().size();
			String maxPlayers = "" + this.getMaxPlayers();
			String gameStarted = "" + this.matchState.toString();
			ArrayList<String> messages = new ArrayList<String>();
			messages.add("ServerUpdate");
			messages.add(SkyWarsReloaded.get().getServerName());
			messages.add(playerCount);
			messages.add(maxPlayers);
			messages.add(gameStarted);
			Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
			if (player != null) {
				SkyWarsReloaded.get().sendSWRMessage(player, SkyWarsReloaded.getCfg().getBungeeLobby(), messages);
			}
		}
	}	
	
    /*Sign Methods*/
	
	public void updateSigns() {
		for (SWRSign s : signs) {
			s.update();
		}
	}
	
	public List<SWRSign> getSigns() {
		return this.signs;
	}
	

	public boolean hasSign(Location loc) {
		for (SWRSign s: signs) {
			if (s.getLocation().equals(loc)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean removeSign(Location loc) {
		SWRSign sign = null;
		for (SWRSign s: signs) {
			if (s.getLocation().equals(loc)) {
				sign = s;
			}
		}
		if (sign != null) {
			signs.remove(sign);
			saveArenaData();
			updateSigns();
			return true;
		}
		return false;
	}
	
	public void addSign(Location loc) {
		signs.add(new SWRSign(name, loc));
		saveArenaData();
		updateSigns();
	}
	
	
	
	/*Getter and Setter Methods*/
        
    public String getDisplayName() {
    	return this.displayName;
    }
    
    public String getDesigner() {
    	return this.designedBy;
    }
    
    public ArrayList<CoordLoc> getChests(){
		return chests;
	}
	
	public boolean containsSpawns() {
		if (teamCards.size() >= 2) {
			return true;
		}
		return false;
	}
    
    public MatchState getMatchState() {
        return this.matchState;
    }
    
    public void setMatchState(final MatchState state) {
        this.matchState = state;
    }
    
    public int getPlayerCount() {
    	int count = 0;
    	for (TeamCard tCard: teamCards) {
    		for (PlayerCard pCard: tCard.getPlayers()) {
    			if (pCard.getPreElo() != -1) {
    				count++;
    			}
    		}
    	}
		return count;
    }
    
    public int getMinPlayers() {
    	if (minPlayers == 0) {
    		return teamCards.size();
    	}
    	return minPlayers;
    }
    
    public void setMinPlayers(int x) {
    	minPlayers = x;
    	saveArenaData();
    }
        
    public int getTimer() {
        return this.timer;
    }
    
    public void setTimer(final int lenght) {
        this.timer = lenght;
    }
    
    public GameKit getKit() {
    	return kit;
    }
          
    public String getName() {
        return this.name;
    }
       
	public static void shuffle() {
		Collections.shuffle(arenas);
	}
	
	public void setAllowFallDamage(boolean b) {
		allowFallDamage = b;
	}
   
	public boolean allowFallDamage() {
		return allowFallDamage;
	}

	public ArrayList<UUID> getDea() {
		ArrayList<UUID> dead = new ArrayList<UUID>();
		for (TeamCard tCard: teamCards) {
			dead.addAll(tCard.getDead());
		}
		return dead;
	}

	public ArrayList<UUID> getSpectators() {
		return spectators;
	}
	
	public boolean isThunder() {
		return thunder;
	}

	public void setNextStrike(int randomNum) {
		nextStrike = randomNum;
	}
	
	public int getNextStrike() {
		return nextStrike;
	}
	
	public void setStrikeCounter(int num) {
		strikeCounter = num;
	}
	
	public int getStrikeCounter() {
		return strikeCounter;
	}
	
	public int getMaxPlayers() {
		return teamCards.size();
	}
	
	public boolean isMatchStarted() {
		if (this.matchState == MatchState.WAITINGSTART) {
			return false;
		}
		return true;
	}
	
	public void setThunderStorm(boolean b) {
		this.thunder = b;
	}
	

	public ArrayList<PlayerCard> getPlayerCards() {
		ArrayList<PlayerCard> cards = new ArrayList<PlayerCard>();
		for (TeamCard tCard: teamCards) {
			cards.addAll(tCard.getPlayers());
		}
		return cards;
	}
	
	public PlayerCard getPlayerCard(Player player) {
		for (TeamCard tCard: teamCards) {
			for (PlayerCard pCard: tCard.getPlayers()) {
				if (pCard.getPlayer() != null && pCard.getPlayer().equals(player)) {
					return pCard;
				}
			}
		}
		return null;
	}
	
	public void setForceStart(boolean state) {
		forceStart = true;
	}
	
	public boolean getForceStart() {
		return forceStart;
	}

	public static GameMap getMapByDisplayName(String name) {
		for (GameMap gMap: arenas) {
			if (ChatColor.stripColor((ChatColor.translateAlternateColorCodes('&', gMap.getDisplayName()))).equalsIgnoreCase(name)) {
				return gMap;
			}
		}
		return null;
	}

	public void setAllowRegen(boolean b) {
		allowRegen = b;
	}

	public boolean allowRegen() {
		return allowRegen;
	}
	
	public void setWinner(String name) {
		winner = name;
	}
	
	public boolean isRegistered() {
		return registered;
	}

	public void setRegistered(boolean b) {
		registered = b;
		saveArenaData();
		update();
	}

	public void setCreator(String creator) {
		this.designedBy = creator;
		saveArenaData();
	}

	public void setDisplayName(String displayName2) {
		this.displayName = displayName2;
		saveArenaData();
	}

	public String getArenaKey() {
		return arenakey;
	}

	public void setCurrentChest(String voteString) {
		currentChest = voteString;
	}

	public void setCurrentModifier(String voteString) {
		currentModifier = voteString;
	}

	public void setCurrentTime(String voteString) {
		currentTime = voteString;	
	}
	
	public void setCurrentHealth(String voteString) {
		currentHealth = voteString;	
	}

	public void setCurrentWeather(String voteString) {
		currentWeather = voteString;
	}

	public GameOption getChestOption() {
		return chestOption;
	}

	public GameOption getTimeOption() {
		return timeOption;
	}

	public GameOption getWeatherOption() {
		return weatherOption;
	}

	public GameOption getModifierOption() {
		return modifierOption;
	}

	public void setKit(GameKit voted) {
		this.kit = voted;
	}

	public KitVoteOption getKitVoteOption() {
		return kitVoteOption;
	}

	public GameOption getHealthOption() {
		return healthOption;
	}

	public void setEditing(boolean b) {
		inEditing = b;
	}
	
	public boolean isEditing() {
		return inEditing;
	}
	
	private static void copyDefaults(File mapFile) {
        FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(mapFile);
		Reader defConfigStream = new InputStreamReader(SkyWarsReloaded.get().getResource("mapFile.yml"));
		if (defConfigStream != null) {
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
			playerConfig.options().copyDefaults(true);
			playerConfig.setDefaults(defConfig);
			try {
				playerConfig.save(mapFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public World getCurrentWorld() {
        World mapWorld;
		mapWorld = SkyWarsReloaded.get().getServer().getWorld(name);
		return mapWorld;
	}

	public void setSpectateSpawn(Location location) {
		spectateSpawn = new CoordLoc(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		saveArenaData();
	}

	public void addTeamCard(Location loc) {
		addTeamCard(new CoordLoc(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
		saveArenaData();
	}
	
	public void addTeamCard(CoordLoc loc) {
		teamCards.add(new TeamCard(teamSize, loc, this));
	}
	
	public boolean removeTeamCard(Location loc) {
		CoordLoc remove = new CoordLoc(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		TeamCard toRemove = null;
		for (TeamCard tCard: teamCards) {
			if (tCard.getSpawn().equals(remove)) {
				toRemove = tCard;
			}
		}
		if (toRemove != null) {
			teamCards.remove(toRemove);
			return true;
		}
		return false;
	}
	
	public void addDeathMatchSpawn(Location loc) {
		addDeathMatchSpawn(new CoordLoc(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
		saveArenaData();
	}
	
	public void addDeathMatchSpawn(CoordLoc loc) {
		deathMatchSpawns.add(loc);
	}
	
	public boolean removeDeathMatchSpawn(Location loc) {
		CoordLoc remove = new CoordLoc(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		boolean removed = deathMatchSpawns.remove(remove);
		return removed;
	}
	
	public boolean addChest(Chest chest) {
		InventoryHolder ih = chest.getInventory().getHolder();
        if (ih instanceof DoubleChest) {
        	DoubleChest dc = (DoubleChest) ih;
			Chest left = (Chest) dc.getLeftSide();
			Chest right = (Chest) dc.getRightSide();
			CoordLoc locLeft = new CoordLoc(left.getX(), left.getY(), left.getZ());
			CoordLoc locRight = new CoordLoc(right.getX(), right.getY(), right.getZ());
			if (!(chests.contains(locLeft) || chests.contains(locRight))) {
				addChest(locLeft);
				return true;
			}
        } else {
        	CoordLoc loc = new CoordLoc(chest.getX(), chest.getY(), chest.getZ());
            if (!chests.contains(loc)){
      	  		addChest(loc);
      	  		return true;
            }
        }
        return false;
	}
	
	public void addChest(CoordLoc loc) {
		chests.add(loc);
	}
	
	public void removeChest(Chest chest) {
		InventoryHolder ih = chest.getInventory().getHolder();
		if (ih instanceof DoubleChest) {
			DoubleChest dc = (DoubleChest) ih;
			Chest left = (Chest) dc.getLeftSide();
			Chest right = (Chest) dc.getRightSide();
			CoordLoc locLeft = new CoordLoc(left.getX(), left.getY(), left.getZ());
			CoordLoc locRight = new CoordLoc(right.getX(), right.getY(), right.getZ());
			if (chests.contains(locLeft)) {
				chests.remove(locLeft);
			}
			if (chests.contains(locRight)) {
				chests.remove(locRight);
			}
		} else {
			CoordLoc loc = new CoordLoc(chest.getX(), chest.getY(), chest.getZ());
			if (chests.contains(loc)) {
				chests.remove(loc);
			}
		}

	}

	public CoordLoc getSpectateSpawn() {
		return spectateSpawn;
	}

	public boolean saveMap(@Nullable Player mess) {
		Location respawn = SkyWarsReloaded.getCfg().getSpawn();
		for (World world: SkyWarsReloaded.get().getServer().getWorlds()) {
			if (world.getName().equals(name)) {
				World editWorld = SkyWarsReloaded.get().getServer().getWorld(world.getName());
				for (Player player: editWorld.getPlayers()) {
					player.teleport(respawn, TeleportCause.PLUGIN);
				}					
				editWorld.save();
				SkyWarsReloaded.getWM().unloadWorld(world.getName());
				File dataDirectory = new File (SkyWarsReloaded.get().getDataFolder(), "maps");
				File target = new File (dataDirectory, world.getName());
				SkyWarsReloaded.getWM().deleteWorld(target);
				File source = new File (SkyWarsReloaded.get().getServer().getWorldContainer().getAbsolutePath(), world.getName());
				SkyWarsReloaded.getWM().copyWorld(source, target);
				SkyWarsReloaded.getWM().deleteWorld(source);
				if (mess != null) {
					mess.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", world.getName()).format("maps.saved"));
					mess.sendMessage(new Messaging.MessageFormatter().format("maps.register-reminder"));
				}
				saveArenaData();
				return true;
			} 	
		}
		if (mess != null) {
			mess.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", name).format("error.map-not-in-edit"));
		}
		return false;
	}

	public ArrayList<MatchEvent> getEvents() {
		return events;
	}

	public  ArrayList<CoordLoc> getDeathMatchSpawns() {
		return deathMatchSpawns;
	}

	public void removeDMSpawnBlocks() {
		for (CoordLoc loc: deathMatchSpawns) {
			World world = getCurrentWorld();
			Location loca = new Location(world, loc.getX(), loc.getY(), loc.getZ());
			world.getBlockAt(loca).setType(Material.AIR);
		}
	}
	
	public void removeSpawnBlocks() {
		for (TeamCard tCard: teamCards) {
			World world = getCurrentWorld();
			CoordLoc loc = tCard.getSpawn();
			Location loca = new Location(world, loc.getX(), loc.getY(), loc.getZ());
			world.getBlockAt(loca).setType(Material.AIR);
		}
	}
	
	public ArrayList<String> getDeathMatchWaiters() {
	   	return deathMatchWaiters;
	}
	    
	public void addDeathMatchWaiter(Player player) {
	  	if (player != null) {
	       	deathMatchWaiters.add(player.getUniqueId().toString());
	   	}
	}
	    
	public void clearDeathMatchWaiters() {
		deathMatchWaiters.clear();	
	}

	public ArrayList<String> getAnvils() {
		return anvils;
	}
	
	public void addCrate(Location loc, int max) {
		crates.add(new Crate(loc, max));
	}
	
	public void removeCrates() {
		for (Crate crate: crates) {
			if (crate.getLocation() != null) {
				crate.getLocation().getWorld().getBlockAt(crate.getLocation()).setType(Material.AIR);
			}
		}
		crates.clear();
	}

	public ArrayList<Crate> getCrates() {
		return crates;
	}

	public Cage getCage() {
		return cage;
	}

	public void setCage(CageType next) {
        switch(next) {
        case CUBE: this.cage = new CubeCage();
        break;
        case DOME: this.cage = new DomeCage();
		break;
        case PYRAMID: this.cage = new PyramidCage();
		break;
        case SPHERE: this.cage = new SphereCage();
		break;
        case STANDARD: this.cage = new StandardCage();
		break;
		default: this.cage = new StandardCage();
        }
        saveArenaData();
	}

	public ArrayList<TeamCard> getTeamCards() {
		return teamCards;
	}

	public GameQueue getJoinQueue() {
		return joinQueue;
	}

	public TeamCard getTeamCard(Player player) {
		for (TeamCard tCard: teamCards) {
			if (tCard.containsPlayer(player.getUniqueId()) != null) {
				return tCard;
			}
		}
		return null;
	}

	public int getTeamCount() {
		int count = 0;
		for (TeamCard tCard: teamCards) {
			if (tCard.getPlayersSize() > 0) {
				count++;
			}
		}
		return count;
	}

	public int getTeamsOut() {
		int count = 0;
		for (TeamCard tCard: teamCards) {
			int numOfPlayers = tCard.getPlayersSize();
			if (numOfPlayers > 0 && tCard.isElmininated(numOfPlayers)) {
				count++;
			}
		}
		return count;
	}
	
	public int getTeamsleft() {
		return getTeamCount() - getTeamsOut();
	}

	public int getTeamSize() {
		return teamSize;
	}

	public TeamCard getWinningTeam() {
		for (TeamCard tCard: teamCards) {
			if (!tCard.isElmininated()) {
				return tCard;
			}
		}
		return null;
	}
}
