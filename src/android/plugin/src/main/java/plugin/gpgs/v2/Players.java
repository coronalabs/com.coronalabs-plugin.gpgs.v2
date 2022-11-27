package plugin.gpgs.v2;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaLua;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayerBuffer;
import com.google.android.gms.games.PlayerStatsClient;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.games.stats.PlayerStats;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;

import java.util.ArrayList;
import java.util.Hashtable;

import plugin.gpgs.v2.LuaUtils.Scheme;
import plugin.gpgs.v2.LuaUtils.Table;

import static com.google.android.gms.games.PlayersClient.EXTRA_PLAYER_SEARCH_RESULTS;

class Players {

	Players(LuaState L) {
		L.newTable(); // plugin.gpgs.v2.players
		Utils.setJavaFunctionAsField(L, "load", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return load(L, false);
			}
		});
		Utils.setJavaFunctionAsField(L, "loadStats", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return loadStats(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "showCompare", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return showCompare(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "showSearch", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return showSearch(L);
			}
		});
		L.setField(-2, "players");
	}

	private PlayersClient getClient(){
		return PlayGames.getPlayersClient(Connector.getActivity());
	}

	private PlayerStatsClient getStatsClient(){
		return PlayGames.getPlayerStatsClient(Connector.getActivity());
	}

	//region Lua functions
	// plugin.gpgs.v2.players.load(params)
	// params.playerId
	// params.playerIds
	// params.source = "connected", "invitable", "recentlyPlayedWith"
	// params.limit
	// params.reload
	// params.listener
	private int load(LuaState L, boolean isLegacy) {
		Utils.debugLog("players.load()");
		String name = "loadPlayers";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
					.string("playerId")
					.table("playerIds")
					.string("playerIds.#")
					.string("source")
					.number("limit")
					.bool("reload")
					.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String playerId = params.getString("playerId");
			Hashtable<Object, Object> playerIds = params.getTable("playerIds");
			String source = params.getString("source");
			Long limit = params.getLong("limit");
			Boolean reload = params.getBoolean("reload", false);
			Integer luaListener = params.getListener("listener");
			int pageSize = 25;
			if ((limit != null) && (limit < pageSize)) {
				pageSize = limit.intValue();
			}
			ArrayList<String> playerIdArray = null;
			if ((playerIds != null) && (playerIds.values().size() > 0)) {
				playerIdArray = new ArrayList<>();
				for (Object o : playerIds.values()) {
					playerIdArray.add((String) o);
				}
			}
			if (playerIdArray != null) {
				playerId = playerIdArray.remove(0);
				if (playerId != null) {
					getClient().loadPlayer(playerId, reload).addOnCompleteListener(new LoadPlayersCompleteListener<AnnotatedData<Player>>(name, luaListener));
				} else {
					getClient().loadRecentlyPlayedWithPlayers(pageSize, reload).addOnCompleteListener(new LoadPlayersCompleteListener<AnnotatedData<PlayerBuffer>>(name, luaListener));
				}
			} else {
				if (playerId == null) {
					getClient().getCurrentPlayer().addOnCompleteListener(new LoadPlayersCompleteListener<Player>(name, luaListener));
				}
			}
		}
		return 0;
	}

	// plugin.gpgs.v2.players.loadStats(params)
	// params.reload
	// params.listener
	private int loadStats(LuaState L) {
		Utils.debugLog("players.loadStats()");
		final String name = "loadStats";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
				.bool("reload")
				.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			final Boolean reload = params.getBoolean("reload", false);
			final Integer luaListener = params.getListener("listener");
			getStatsClient().loadPlayerStats(reload).addOnCompleteListener(new OnCompleteListener<AnnotatedData<PlayerStats>>() {
				@Override
				public void onComplete(@NonNull Task<AnnotatedData<PlayerStats>> result) {
					try {
						Hashtable<Object, Object> event = Utils.newEvent(name);
						boolean isError = !result.isSuccessful();
						event.put("isError", isError);
						if (isError) {
							event.put("errorCode", Utils.getErrorCode(result.getException()));
							event.put("errorMessage", result.getException().getLocalizedMessage());
						} else {
							Hashtable<Object, Object> stats = new Hashtable<>();
							PlayerStats s = result.getResult().get();
							if (s.getAverageSessionLength() != PlayerStats.UNSET_VALUE) {
								stats.put("averageSessionLength", s.getAverageSessionLength());
							}
							if (s.getDaysSinceLastPlayed() != PlayerStats.UNSET_VALUE) {
								stats.put("daysSinceLastPlayed", s.getDaysSinceLastPlayed());
							}
							if (s.getNumberOfPurchases() != PlayerStats.UNSET_VALUE) {
								stats.put("numberOfPurchases", s.getNumberOfPurchases());
							}
							if (s.getNumberOfSessions() != PlayerStats.UNSET_VALUE) {
								stats.put("numberOfSessions", s.getNumberOfSessions());
							}
							if (s.getSessionPercentile() != PlayerStats.UNSET_VALUE) {
								stats.put("sessionPercentile", s.getSessionPercentile());
							}
							if (s.getSpendPercentile() != PlayerStats.UNSET_VALUE) {
								stats.put("spendPercentile", s.getSpendPercentile());
							}
							event.put("stats", stats);
						}
						Utils.dispatchEvent(luaListener, event, true);
					} catch(Throwable ignore){}
				}
			});
		}
		return 0;
	}

	// plugin.gpgs.v2.players.showCompare(params)
	// params.playerId *
	// params.listener
	private int showCompare(LuaState L) {
		Utils.debugLog("players.showCompare()");
		final String name = "showCompare";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
				.string("playerId")
				.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String playerId = params.getStringNotNull("playerId");
			final Integer luaListener = params.getListener("listener");
			getClient().loadPlayer(playerId).addOnCompleteListener(new OnCompleteListener<AnnotatedData<Player>>() {
				@Override
				public void onComplete(@NonNull Task<AnnotatedData<Player>> task) {
					Hashtable<Object, Object> event = Utils.newEvent(name);
					boolean isError = !task.isSuccessful();
					event.put("isError", isError);
					if (isError) {
						event.put("errorCode", Utils.getErrorCode(task.getException()));
						event.put("errorMessage", task.getException().getLocalizedMessage());
					} else {
						Task<Intent> intentTask = getClient().getCompareProfileIntent(task.getResult().get());
						while (!intentTask.isSuccessful()) {
							int k = 0;
						}
						Utils.startActivity(intentTask.getResult(), name, luaListener);
					}
				}
			});
		}
		return 0;
	}

	// plugin.gpgs.v2.players.showSearch(listener)
	private int showSearch(LuaState L) {
		Utils.debugLog("players.showSearch()");
		final String name = "showSearch";
		int listenerIndex = 1;
		int luaListener;
		if (CoronaLua.isListener(L, listenerIndex, name)) {
			luaListener = CoronaLua.newRef(L, listenerIndex);
		} else {
			Utils.errorLog(name + " must receive listener parameter, got " + L.typeName(1));
			return 0;
		}
		final int listener = luaListener;
		if (Utils.checkConnection()) {
			final CoronaActivity.OnActivityResultHandler resultHandler = new CoronaActivity.OnActivityResultHandler() {
				@Override
				public void onHandleActivityResult(CoronaActivity activity, int requestCode, int resultCode, Intent intent) {
					try {
						activity.unregisterActivityResultHandler(this);
						Hashtable<Object, Object> event = Utils.newEvent(name);
						boolean isError = resultCode != Activity.RESULT_OK;
						event.put("isError", isError);
						if (isError) {
							event.put("errorCode", resultCode);
							event.put("errorMessage", Utils.resultCodeToString(resultCode));
						} else if (intent != null) {
							Parcelable p = intent.getParcelableArrayListExtra(EXTRA_PLAYER_SEARCH_RESULTS).get(0);
							event.put("player", Utils.playerToHashtable((Player) p));
						}
						Utils.dispatchEvent(listener, event, true);
					} catch (Throwable ignore) {}
				}
			};
			getClient().getPlayerSearchIntent().addOnCompleteListener(new OnCompleteListener<Intent>() {
				@Override
				public void onComplete(@NonNull Task<Intent> task) {
					try {
						Utils.startActivity(task.getResult(), resultHandler);
					} catch (Throwable ignore) {}
				}
			});
		}
		return 0;
	}
	//endregion

	//region CompleteListeners
	private class LoadPlayersCompleteListener<T> implements OnCompleteListener<T> {
		private String name;
		private Integer luaListener;
		private Hashtable<Object, Object> players = new Hashtable<>();

		LoadPlayersCompleteListener(String name, Integer luaListener) {
			this.name = name;
			this.luaListener = luaListener;
		}

		@Override
		public void onComplete(@NonNull Task<T> task) {
			try {
				boolean isError = !task.isSuccessful();
				Hashtable<Object, Object> event = Utils.newEvent(name);
				event.put("isError", isError);
				if (isError) {
					event.put("errorCode", Utils.getErrorCode(task.getException()));
					event.put("errorMessage", task.getException().getLocalizedMessage());
				} else {
					T result = task.getResult();
					int i = players.size() + 1;
					if (result instanceof Player) {
						players.put(i++, Utils.playerToHashtable((Player) result));
					}
					if (result instanceof AnnotatedData<?>) {
						try {
							AnnotatedData<PlayerBuffer> annotatedData = (AnnotatedData<PlayerBuffer>) result;
							PlayerBuffer buffer = annotatedData.get();
							for (Player p : buffer) {
								players.put(i++, Utils.playerToHashtable(p));
							}
							buffer.release();
						} catch (Exception ignored) {
						}
						try {
							AnnotatedData<Player> player = (AnnotatedData<Player>) result;
							players.put(i++, Utils.playerToHashtable(player.get()));
						} catch (Exception ignored) {
						}
					}
				}
				event.put("players", players);
				Utils.dispatchEvent(luaListener, event, true);
			} catch(Throwable ignore){}
		}
	}
	//endregion

	//region Legacy compatibility
	private String[] actions = new String[]{"loadLocalPlayer", "loadPlayers", "loadFriends"};

	boolean hasAction(String action) {
		for (String a : actions) {
			if (action.equals(a)) {
				return true;
			}
		}
		return false;
	}

	// gameNetwork.request(action, ...)
	int request(LuaState L, String action) {
		switch (action) {
			case "loadLocalPlayer":
				return load(L, true);
			case "loadPlayers":
				L.getField(1, "playerIDs");
				L.setField(1, "playerIds");
				return load(L, true);
			case "loadFriends":
				L.pushString("invitable");
				L.setField(1, "source");
				L.pushBoolean(true);
				L.setField(1, "reload");
				return load(L, true);
			default:
				return 0;
		}
	}
	//endregion
}
