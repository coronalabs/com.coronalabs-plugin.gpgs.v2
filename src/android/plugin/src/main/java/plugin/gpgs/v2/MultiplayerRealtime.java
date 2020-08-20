package plugin.gpgs.v2;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaLua;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.RealTimeMultiplayerClient;
import com.google.android.gms.games.multiplayer.realtime.OnRealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateCallback;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import plugin.gpgs.v2.LuaUtils.Scheme;
import plugin.gpgs.v2.LuaUtils.Table;

import static com.google.android.gms.games.multiplayer.Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS;
import static com.google.android.gms.games.multiplayer.Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS;

class MultiplayerRealtime extends RoomStatusUpdateCallback implements OnRealTimeMessageReceivedListener {
	private int luaMessageListener = CoronaLua.REFNIL;
	private int luaPeerListener = CoronaLua.REFNIL;
	private int luaRoomListener = CoronaLua.REFNIL;
	private boolean isLegacyMessageListener = false;
	private boolean isLegacyRoomListener = false;
	static Hashtable<String, Room> rooms = new Hashtable<>();

	private RoomUpdateCallback roomUpdateCallback = new RoomUpdateCallback() {
		@Override
		public void onRoomCreated(int statusCode, @Nullable Room room) {
			if (!isLegacyRoomListener) {
				Hashtable<Object, Object> event = Utils.newEvent("room");
				event.put("phase", "created");
				boolean isError = statusCode != CommonStatusCodes.SUCCESS;
				event.put("isError", isError);
				if (isError) {
					event.put("errorCode", statusCode);
					event.put("errorMessage", Utils.statusCodeToString(statusCode));
				} else {
					rooms.put(room.getRoomId(), room);
					event.put("roomId", room.getRoomId());
				}
				Utils.dispatchEvent(luaRoomListener, event);
			} else {
				Hashtable<Object, Object> event = Utils.newLegacyEvent("createRoom");
				Hashtable<Object, Object> data = new Hashtable<>();
				data.put("isError", statusCode != CommonStatusCodes.SUCCESS);
				data.put("roomID", room.getRoomId());
				event.put("data", data);
				Utils.dispatchEvent(luaRoomListener, event);
			}
		}

		@Override
		public void onJoinedRoom(int statusCode, @Nullable Room room) {
			if (!isLegacyRoomListener) {
				Hashtable<Object, Object> event = Utils.newEvent("room");
				event.put("phase", "joined");
				boolean isError = statusCode != CommonStatusCodes.SUCCESS;
				event.put("isError", isError);
				if (isError) {
					event.put("errorCode", statusCode);
					event.put("errorMessage", Utils.statusCodeToString(statusCode));
				} else {
					rooms.put(room.getRoomId(), room);
					event.put("roomId", room.getRoomId());
				}
				Utils.dispatchEvent(luaRoomListener, event);
			} else {
				Hashtable<Object, Object> event = Utils.newLegacyEvent("joinRoom");
				Hashtable<Object, Object> data = new Hashtable<>();
				data.put("isError", statusCode != CommonStatusCodes.SUCCESS);
				data.put("roomID", room.getRoomId());
				event.put("data", data);
				Utils.dispatchEvent(luaRoomListener, event);
			}
		}

		@Override
		public void onLeftRoom(int statusCode, @NonNull String roomId) {
			if (!isLegacyRoomListener) {
				Hashtable<Object, Object> event = Utils.newEvent("room");
				event.put("phase", "left");
				boolean isError = statusCode != CommonStatusCodes.SUCCESS;
				event.put("isError", isError);
				if (isError) {
					event.put("errorCode", statusCode);
					event.put("errorMessage", Utils.statusCodeToString(statusCode));
				} else {
					rooms.remove(roomId);
					event.put("roomId", roomId);
				}
				Utils.dispatchEvent(luaRoomListener, event);
			} else {
				Hashtable<Object, Object> event = Utils.newLegacyEvent("leaveRoom");
				Hashtable<Object, Object> data = new Hashtable<>();
				data.put("isError", statusCode != CommonStatusCodes.SUCCESS);
				data.put("roomID", roomId);
				event.put("data", data);
				Utils.dispatchEvent(luaRoomListener, event);
			}
		}

		@Override
		public void onRoomConnected(int statusCode, @Nullable Room room) {
			if (!isLegacyRoomListener) {
				Hashtable<Object, Object> event = Utils.newEvent("room");
				event.put("phase", "connected");
				boolean isError = statusCode != CommonStatusCodes.SUCCESS;
				event.put("isError", isError);
				if (isError) {
					event.put("errorCode", statusCode);
					event.put("errorMessage", Utils.statusCodeToString(statusCode));
				} else {
					rooms.put(room.getRoomId(), room);
					event.put("roomId", room.getRoomId());
				}
				Utils.dispatchEvent(luaRoomListener, event);
			} else {
				Hashtable<Object, Object> event = Utils.newLegacyEvent("connectedRoom");
				Hashtable<Object, Object> data = new Hashtable<>();
				data.put("isError", statusCode != CommonStatusCodes.SUCCESS);
				data.put("roomID", room.getRoomId());
				event.put("data", data);
				Utils.dispatchEvent(luaRoomListener, event);
			}
		}
	};

	private RoomConfig.Builder builder = RoomConfig.builder(roomUpdateCallback);

	MultiplayerRealtime(LuaState L) {
		L.newTable(); // plugin.gpgs.v2.multiplayer.realtime
		Utils.setJavaFunctionAsField(L, "create", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return create(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "join", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return join(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "leave", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return leave(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "sendReliably", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return sendReliably(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "sendUnreliably", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return sendUnreliably(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getRoom", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getRoom(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "showSelectPlayers", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return showSelectPlayers(L, false);
			}
		});
		Utils.setJavaFunctionAsField(L, "show", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return show(L, false);
			}
		});
		Utils.setJavaFunctionAsField(L, "setListeners", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return setListeners(L, false);
			}
		});
		Utils.setJavaFunctionAsField(L, "removeListeners", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return removeListeners(L);
			}
		});
		L.setField(-2, "realtime");
	}

	private RealTimeMultiplayerClient getRealTimeMultiplayerClient(){
		return Games.getRealTimeMultiplayerClient(Connector.getContext(), Connector.getSignInAccount());
	}

	//region Lua functions
	// plugin.gpgs.v2.multiplayer.realtime.create(params)
	// params.playerId
	// params.playerIds
	// params.automatch.minPlayers
	// params.automatch.maxPlayers
	// params.automatch.exclusionBits
	// params.automatch.variant
	private int create(LuaState L) {
		Utils.debugLog("multiplayer.realtime.create()");
		if (!L.isTable(1)){
			Utils.errorLog("create must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
				.string("playerId")
				.table("playerIds")
				.string("playerIds.#")
				.table("automatch")
				.number("automatch.minPlayers")
				.number("automatch.maxPlayers")
				.number("automatch.exclusionBits")
				.number("automatch.variant");

			Table params = new Table(L, 1).parse(scheme);
			String playerId = params.getString("playerId");
			Hashtable<Object, Object> playerIds = params.getTable("playerIds");
			Integer minPlayers = params.getInteger("automatch.minPlayers");
			Integer maxPlayers = params.getInteger("automatch.maxPlayers");
			Long exclusionBits = params.getLong("automatch.exclusionBits", 0);
			Integer variant = params.getInteger("automatch.variant", Room.ROOM_VARIANT_DEFAULT);

			ArrayList<String> playerIdArray = new ArrayList<>();
			if ((playerIds != null) && (playerIds.values().size() > 0)) {
				for (Object o : playerIds.values()) {
					playerIdArray.add((String) o);
				}
			} else if (playerId != null) {
				playerIdArray.add(playerId);
			}

			if (playerIdArray.size() > 0) {
				builder.addPlayersToInvite(playerIdArray);
			}
			if ((minPlayers != null) && (maxPlayers != null)) {
				builder.setAutoMatchCriteria(RoomConfig.createAutoMatchCriteria(minPlayers, maxPlayers, exclusionBits));
			}
			builder.setVariant(variant);
			builder.setOnMessageReceivedListener(this);
			builder.setRoomStatusUpdateCallback(this);
			getRealTimeMultiplayerClient().create(builder.build());
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.realtime.join(invitationId)
	private int join(LuaState L) {
		Utils.debugLog("multiplayer.realtime.join()");
		if (!L.isString(1)){
			Utils.errorLog("join must receive string parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			String invitationId = L.checkString(1);
			builder.setInvitationIdToAccept(invitationId);
			builder.setOnMessageReceivedListener(this);
			builder.setRoomStatusUpdateCallback(this);
			getRealTimeMultiplayerClient().join(builder.build());
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.realtime.leave(roomId)
	private int leave(LuaState L) {
		Utils.debugLog("multiplayer.realtime.leave()");
		if (!L.isString(1)){
			Utils.errorLog("leave must receive string parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			String roomId = L.checkString(1);
			getRealTimeMultiplayerClient().leave(builder.build(), roomId);
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.realtime.sendReliably(params)
	// params.roomId *
	// params.participantId
	// params.participantIds
	// params.payload *
	// params.listener
	private int sendReliably(LuaState L) {
		Utils.debugLog("multiplayer.realtime.sendReliably()");
		final String name = "sendReliably";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
				.string("roomId")
				.string("participantId")
				.table("participantIds")
				.string("participantIds.#")
				.byteArray("payload")
				.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String roomId = params.getStringNotNull("roomId");
			String participantId = params.getString("participantId");
			Hashtable<Object, Object> participantIds = params.getTable("participantIds");
			byte[] payload = params.getByteArrayNotNull("payload");
			Integer luaListener = params.getListener("listener");

			ArrayList<String> participantIdArray = new ArrayList<>();
			if ((participantIds != null) && (participantIds.values().size() > 0)) {
				for (Object o : participantIds.values()) {
					participantIdArray.add((String) o);
				}
			} else if(participantId != null) {
				participantIdArray.add(participantId);
			}

			for (String p : participantIdArray) {
				Integer luaParticipantListener = CoronaLua.REFNIL;
				if (luaListener != null) {
					L.rawGet(LuaState.REGISTRYINDEX, luaListener);
					luaParticipantListener = CoronaLua.newRef(L, -1); // Duplicate listener ref for each participant so we can safely delete it later.
					L.pop(1);
				}
				final Integer fLuaParticipantListener = luaParticipantListener;
				getRealTimeMultiplayerClient().sendReliableMessage(payload, roomId, p, new RealTimeMultiplayerClient.ReliableMessageSentCallback() {
					public void onRealTimeMessageSent(int statusCode, int tokenId, String recipientParticipantId) {
						Hashtable<Object, Object> event = Utils.newEvent(name);
						boolean isError = statusCode != CommonStatusCodes.SUCCESS;
						event.put("isError", isError);
						if (isError) {
							event.put("errorCode", statusCode);
							event.put("errorMessage", Utils.statusCodeToString(statusCode));
						}
						event.put("tokenId", tokenId);
						event.put("participantId", recipientParticipantId);
						Utils.dispatchEvent(fLuaParticipantListener, event, true);
					}
				});
			}
			if (luaListener != null) {
				CoronaLua.deleteRef(L, luaListener);
			}
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.realtime.sendUnreliably(params)
	// params.roomId *
	// params.participantId
	// params.participantIds
	// params.payload *
	private int sendUnreliably(LuaState L) {
		Utils.debugLog("multiplayer.realtime.sendUnreliably()");
		if (!L.isTable(1)){
			Utils.errorLog("sendUnreliably must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
				.string("roomId")
				.string("participantId")
				.table("participantIds")
				.string("participantIds.#")
				.byteArray("payload");

			Table params = new Table(L, 1).parse(scheme);
			String roomId = params.getStringNotNull("roomId");
			String participantId = params.getString("participantId");
			Hashtable<Object, Object> participantIds = params.getTable("participantIds");
			byte[] payload = params.getByteArrayNotNull("payload");

			ArrayList<String> participantIdArray = null;
			if ((participantIds != null) && (participantIds.values().size() > 0)) {
				participantIdArray = new ArrayList<>();
				for (Object o : participantIds.values()) {
					participantIdArray.add((String) o);
				}
			} else if(participantId != null) {
				participantIdArray = new ArrayList<>();
				participantIdArray.add(participantId);
			}

			if (participantIdArray != null) {
				getRealTimeMultiplayerClient().sendUnreliableMessage(payload, roomId, participantIdArray);
			} else {
				getRealTimeMultiplayerClient().sendUnreliableMessageToOthers(payload, roomId);
			}
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.realtime.getRoom(roomId)
	private int getRoom(LuaState L) {
		Utils.debugLog("multiplayer.realtime.getRoom()");
		if (!L.isString(1)){
			Utils.errorLog("getRoom must receive string parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			String roomId = L.checkString(1);
			Room room = rooms.get(roomId);
			if (room != null) {
				new MultiplayerRoom(L, room);
			} else {
				L.pushNil();
			}
		}
		return 1;
	}

	// plugin.gpgs.v2.multiplayer.realtime.showSelectPlayers(params)
	// params.playerId
	// params.playerIds
	// params.minPlayers
	// params.maxPlayers
	// params.allowAutomatch
	// params.listener
	int showSelectPlayers(LuaState L, final boolean isLegacy) {
		Utils.debugLog("multiplayer.realtime.showSelectPlayers()");
		final String name = "showSelectPlayers";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
				.string("playerId")
				.table("playerIds")
				.string("playerIds.#")
				.number("minPlayers")
				.number("maxPlayers")
				.bool("allowAutomatch")
				.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String playerId = params.getString("playerId");
			Hashtable<Object, Object> playerIds = params.getTable("playerIds");
			int minPlayers = params.getInteger("minPlayers", 1);
			int maxPlayers = params.getInteger("maxPlayers", 8); // GPGS maximum
			boolean allowAutomatch = params.getBoolean("allowAutomatch", false);
			final Integer luaListener = params.getListener("listener");

			ArrayList<String> playerIdArray = null;
			if ((playerIds != null) && (playerIds.values().size() > 0)) {
				playerIdArray = new ArrayList<>();
				for (Object o : playerIds.values()) {
					playerIdArray.add((String) o);
				}
			} else if(playerId != null) {
				playerIdArray = new ArrayList<>();
				playerIdArray.add(playerId);
			}
			final ArrayList<String> finalArray = playerIdArray;
			final CoronaActivity.OnActivityResultHandler resultHandler = new CoronaActivity.OnActivityResultHandler() {
				@Override
				public void onHandleActivityResult(CoronaActivity activity, int requestCode, int resultCode, Intent intent) {
					activity.unregisterActivityResultHandler(this);
					Hashtable<Object, Object> event = Utils.newEvent(name);

					if (!isLegacy) {
						boolean isError = resultCode != Activity.RESULT_OK;
						event.put("isError", isError);
						if (isError) {
							event.put("errorCode", resultCode);
							event.put("errorMessage", Utils.resultCodeToString(resultCode));
						}
						if (intent != null) {
							if (intent.hasExtra(Games.EXTRA_PLAYER_IDS)) {
								ArrayList<String> playerIds = intent.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
								event.put("playerIds", Utils.listToHashtable(playerIds));
							}
							Hashtable<Object, Object> automatch = new Hashtable<>();
							if (intent.hasExtra(EXTRA_MIN_AUTOMATCH_PLAYERS)) {
								automatch.put("minPlayers", intent.getIntExtra(EXTRA_MIN_AUTOMATCH_PLAYERS, 0));
							}
							if (intent.hasExtra(EXTRA_MAX_AUTOMATCH_PLAYERS)) {
								automatch.put("maxPlayers", intent.getIntExtra(EXTRA_MAX_AUTOMATCH_PLAYERS, 0));
							}
							if (automatch.size() > 0) {
								event.put("automatch", automatch);
							}
						}
					} else {
						event.put("type", "selectPlayers");
						Hashtable<Object, Object> data = new Hashtable<>();
						if (intent != null) {
							if (intent.hasExtra(Games.EXTRA_PLAYER_IDS)) {
								ArrayList<String> playerIds = intent.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
								data = Utils.listToHashtable(playerIds);
							}
							if (intent.hasExtra(EXTRA_MIN_AUTOMATCH_PLAYERS)) {
								data.put("minAutoMatchPlayers", intent.getIntExtra(EXTRA_MIN_AUTOMATCH_PLAYERS, 0));
							}
							if (intent.hasExtra(EXTRA_MAX_AUTOMATCH_PLAYERS)) {
								data.put("maxAutoMatchPlayers", intent.getIntExtra(EXTRA_MAX_AUTOMATCH_PLAYERS, 0));
							}
						}
						data.put("phase", resultCode == Activity.RESULT_OK ? "selected" : "cancelled");
						event.put("data", data);
					}

					Utils.dispatchEvent(luaListener, event, true);
				}
			};
			getRealTimeMultiplayerClient().getSelectOpponentsIntent(minPlayers, maxPlayers, allowAutomatch).addOnCompleteListener(new OnCompleteListener<Intent>() {
				@Override
				public void onComplete(@NonNull Task<Intent> task) {
					Intent intent = task.getResult();
					if (finalArray != null) {
						intent.putExtra(Games.EXTRA_PLAYER_IDS, finalArray);
					}
					Utils.startActivity(intent, resultHandler);
				}
			});
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.realtime.show(params)
	// params.roomId *
	// params.minPlayersRequired
	// params.listener
	int show(LuaState L, final boolean isLegacy) {
		Utils.debugLog("multiplayer.realtime.show()");
		final String name = "show";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
					.string("roomId")
					.number("minPlayersRequired")
					.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String roomId = params.getStringNotNull("roomId");
			int minPlayersRequired = params.getInteger("minPlayersRequired", Integer.MAX_VALUE);
			final Integer luaListener = params.getListener("listener");

			Room room = rooms.get(roomId);
			if (room != null) {
				final CoronaActivity.OnActivityResultHandler resultHandler = new CoronaActivity.OnActivityResultHandler() {
					@Override
					public void onHandleActivityResult(CoronaActivity activity, int requestCode, int resultCode, Intent intent) {
						activity.unregisterActivityResultHandler(this);
						Hashtable<Object, Object> event = Utils.newEvent(name);
						event.put("type", "waitingRoom");
						Hashtable<Object, Object> data = new Hashtable<>();
						if (intent != null) {
							if (intent.hasExtra(Games.EXTRA_PLAYER_IDS)) {
								ArrayList<String> playerIds = intent.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
								data = Utils.listToHashtable(playerIds);
							}
							if (intent.hasExtra(EXTRA_MIN_AUTOMATCH_PLAYERS)) {
								data.put("minAutoMatchPlayers", intent.getIntExtra(EXTRA_MIN_AUTOMATCH_PLAYERS, 0));
							}
							if (intent.hasExtra(EXTRA_MAX_AUTOMATCH_PLAYERS)) {
								data.put("maxAutoMatchPlayers", intent.getIntExtra(EXTRA_MAX_AUTOMATCH_PLAYERS, 0));
							}
						}
						boolean isError = (resultCode != Activity.RESULT_OK) && (resultCode != Activity.RESULT_CANCELED);
						data.put("isError", isError);
						boolean isCancelled = resultCode == Activity.RESULT_CANCELED;
						data.put("phase", isCancelled ? "cancelled" : "selected");
						Utils.dispatchEvent(luaListener, event, true);
					}
				};
				getRealTimeMultiplayerClient().getWaitingRoomIntent(room, minPlayersRequired).addOnCompleteListener(new OnCompleteListener<Intent>() {
					@Override
					public void onComplete(@NonNull Task<Intent> task) {
						Intent intent = task.getResult();
						if (!isLegacy) {
							Utils.startActivity(intent, name, luaListener);
						} else {
							Utils.startActivity(intent, resultHandler);
						}
					}
				});
			}
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.realtime.setListeners(params)
	// params.message
	// params.peer
	// params.room
	private int setListeners(LuaState L, boolean isLegacy) {
		Utils.debugLog("multiplayer.realtime.setListeners()");
		if (!L.isTable(1)){
			Utils.errorLog("setListeners must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
				.listener("message", "message")
				.listener("peer", "peer")
				.listener("room", "room");

			Table params = new Table(L, 1).parse(scheme);
			Integer luaListener = params.getListener("message");
			if (luaListener != null) {
				luaMessageListener = luaListener;
				isLegacyMessageListener = isLegacy;
			}
			luaListener = params.getListener("peer");
			if (luaListener != null) {
				luaPeerListener = luaListener;
			}
			luaListener = params.getListener("room");
			if (luaListener != null) {
				luaRoomListener = luaListener;
				isLegacyRoomListener = isLegacy;
			}
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.realtime.removeListeners()
	private int removeListeners(LuaState L) {
		Utils.debugLog("multiplayer.realtime.removeListeners()");
		if (luaMessageListener != CoronaLua.REFNIL) {
			CoronaLua.deleteRef(L, luaMessageListener);
			luaMessageListener = CoronaLua.REFNIL;
		}
		if (luaPeerListener != CoronaLua.REFNIL) {
			CoronaLua.deleteRef(L, luaPeerListener);
			luaPeerListener = CoronaLua.REFNIL;
		}
		if (luaRoomListener != CoronaLua.REFNIL) {
			CoronaLua.deleteRef(L, luaRoomListener);
			luaRoomListener = CoronaLua.REFNIL;
		}
		return 0;
	}
	//endregion

	//region RealTimeMessageReceivedListener, RoomUpdateListener, RoomStatusUpdateListener
	public void onRealTimeMessageReceived(RealTimeMessage message) {
		Hashtable<Object, Object> event;
		if (!isLegacyMessageListener) {
			event = Utils.newEvent("message");
			event.put("isError", false);
			event.put("payload", message.getMessageData());
			event.put("participantId", message.getSenderParticipantId());
			event.put("isReliable", message.isReliable());
		} else {
			event = Utils.newLegacyEvent("setMessageReceivedListener");
			Hashtable<Object, Object> data = new Hashtable<>();
			data.put("message", message.getMessageData());
			data.put("participantId", message.getSenderParticipantId());
			event.put("data", data);
		}
		Utils.dispatchEvent(luaMessageListener, event);
	}

	public void	onConnectedToRoom(Room room) {
		Hashtable<Object, Object> event = Utils.newEvent("peer");
		event.put("phase", "connected to room");
		event.put("isError", false);
		rooms.put(room.getRoomId(), room);
		event.put("roomId", room.getRoomId());
		Utils.dispatchEvent(luaPeerListener, event);
	}

	public void	onDisconnectedFromRoom(Room room) {
		Hashtable<Object, Object> event = Utils.newEvent("peer");
		event.put("phase", "disconnected from room");
		event.put("isError", false);
		rooms.put(room.getRoomId(), room);
		event.put("roomId", room.getRoomId());
		Utils.dispatchEvent(luaPeerListener, event);
	}

	public void	onP2PConnected(String participantId) {
		Hashtable<Object, Object> event = Utils.newEvent("peer");
		event.put("phase", "P2P connected");
		event.put("isError", false);
		event.put("participantId", participantId);
		Utils.dispatchEvent(luaPeerListener, event);
	}

	public void	onP2PDisconnected(String participantId) {
		Hashtable<Object, Object> event = Utils.newEvent("peer");
		event.put("phase", "P2P disconnected");
		event.put("isError", false);
		event.put("participantId", participantId);
		Utils.dispatchEvent(luaPeerListener, event);
	}

	public void	onPeerDeclined(Room room, List<String> participantIds) {
		if (!isLegacyRoomListener) {
			Hashtable<Object, Object> event = Utils.newEvent("peer");
			event.put("phase", "peer declined");
			event.put("isError", false);
			rooms.put(room.getRoomId(), room);
			event.put("roomId", room.getRoomId());
			event.put("participantIds", participantIds);
			Utils.dispatchEvent(luaPeerListener, event);
		} else {
			Hashtable<Object, Object> event = Utils.newLegacyEvent("peerDeclinedInvitation");
			Hashtable<Object, Object> data = Utils.listToHashtable(participantIds);
			data.put("isError", false);
			data.put("roomID", room.getRoomId());
			event.put("data", data);
			Utils.dispatchEvent(luaRoomListener, event);
		}
	}

	public void	onPeerInvitedToRoom(Room room, List<String> participantIds) {
		Hashtable<Object, Object> event = Utils.newEvent("peer");
		event.put("phase", "peer invited to room");
		event.put("isError", false);
		rooms.put(room.getRoomId(), room);
		event.put("roomId", room.getRoomId());
		event.put("participantIds", participantIds);
		Utils.dispatchEvent(luaPeerListener, event);
	}

	public void	onPeerJoined(Room room, List<String> participantIds) {
		if (!isLegacyRoomListener) {
			Hashtable<Object, Object> event = Utils.newEvent("peer");
			event.put("phase", "peer joined");
			event.put("isError", false);
			rooms.put(room.getRoomId(), room);
			event.put("roomId", room.getRoomId());
			event.put("participantIds", participantIds);
			Utils.dispatchEvent(luaPeerListener, event);
		} else {
			Hashtable<Object, Object> event = Utils.newLegacyEvent("peerAcceptedInvitation");
			Hashtable<Object, Object> data = Utils.listToHashtable(participantIds);
			data.put("isError", false);
			data.put("roomID", room.getRoomId());
			event.put("data", data);
			Utils.dispatchEvent(luaRoomListener, event);
		}
	}

	public void	onPeerLeft(Room room, List<String> participantIds) {
		if (!isLegacyRoomListener) {
			Hashtable<Object, Object> event = Utils.newEvent("peer");
			event.put("phase", "peer left");
			event.put("isError", false);
			rooms.put(room.getRoomId(), room);
			event.put("roomId", room.getRoomId());
			event.put("participantIds", participantIds);
			Utils.dispatchEvent(luaPeerListener, event);
		} else {
			Hashtable<Object, Object> event = Utils.newLegacyEvent("peerLeftRoom");
			Hashtable<Object, Object> data = Utils.listToHashtable(participantIds);
			data.put("isError", false);
			data.put("roomID", room.getRoomId());
			event.put("data", data);
			Utils.dispatchEvent(luaRoomListener, event);
		}
	}

	public void	onPeersConnected(Room room, List<String> participantIds) {
		Hashtable<Object, Object> event = Utils.newEvent("peer");
		event.put("phase", "peers connected");
		event.put("isError", false);
		rooms.put(room.getRoomId(), room);
		event.put("roomId", room.getRoomId());
		event.put("participantIds", participantIds);
		Utils.dispatchEvent(luaPeerListener, event);
	}

	public void	onPeersDisconnected(Room room, List<String> participantIds) {
		if (!isLegacyRoomListener) {
			Hashtable<Object, Object> event = Utils.newEvent("peer");
			event.put("phase", "peers disconnected");
			event.put("isError", false);
			rooms.put(room.getRoomId(), room);
			event.put("roomId", room.getRoomId());
			event.put("participantIds", participantIds);
			Utils.dispatchEvent(luaPeerListener, event);
		} else {
			Hashtable<Object, Object> event = Utils.newLegacyEvent("peerDisconnectedFromRoom");
			Hashtable<Object, Object> data = Utils.listToHashtable(participantIds);
			data.put("isError", false);
			data.put("roomID", room.getRoomId());
			event.put("data", data);
			Utils.dispatchEvent(luaRoomListener, event);
		}
	}

	public void	onRoomAutoMatching(Room room) {
		Hashtable<Object, Object> event = Utils.newEvent("peer");
		event.put("phase", "room automatching");
		event.put("isError", false);
		rooms.put(room.getRoomId(), room);
		event.put("roomId", room.getRoomId());
		Utils.dispatchEvent(luaPeerListener, event);
	}

	public void	onRoomConnecting(Room room) {
		Hashtable<Object, Object> event = Utils.newEvent("peer");
		event.put("phase", "room connecting");
		event.put("isError", false);
		rooms.put(room.getRoomId(), room);
		event.put("roomId", room.getRoomId());
		Utils.dispatchEvent(luaPeerListener, event);
	}
	//endregion

	//region Legacy compatibility
	private String[] actions = new String[]{"createRoom", "joinRoom", "leaveRoom", "setRoomListener", "sendMessage", "setMessageReceivedListener"};

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
			case "createRoom":
				// Legacy params
				L.getField(1, "playerIDs");
				Hashtable<Object, Object> playerIds = CoronaLua.toHashtable(L, -1);
				L.pop(1);

				L.getField(1, "minAutoMatchPlayers");
				Integer minPlayers = L.toInteger(-1);
				L.pop(1);

				L.getField(1, "maxAutoMatchPlayers");
				Integer maxPlayers = L.toInteger(-1);
				L.pop(1);

				L.getField(1, "listener");
				L.remove(1);

				// New params
				L.newTable(0, 1);
				L.pushValue(1);
				L.setField(-2, "room");
				L.remove(1);
				setListeners(L, true);

				// New params
				L.newTable(0, 3);

				L.newTable(0, 2); // automatch

				L.pushInteger(minPlayers);
				L.setField(-2, "minPlayers");

				L.pushInteger(maxPlayers);
				L.setField(-2, "maxPlayers");

				L.setField(-3, "automatch");

				CoronaLua.pushHashtable(L, playerIds);
				L.setField(-2, "playerIds");

				L.remove(1); // remove playerIds
				return create(L);
			case "joinRoom": {
				// Legacy params
				L.getField(1, "roomID");
				String roomId = L.toString(-1);
				L.pop(1);

				L.getField(1, "listener");
				L.remove(1);

				// New params
				L.newTable(0, 1);
				L.pushValue(1);
				L.setField(-2, "room");
				L.remove(1);
				setListeners(L, true);
				L.remove(1);

				// New argument
				L.pushString(roomId);
				return join(L);
			} case "leaveRoom": {
				// Legacy params
				L.getField(1, "roomID");
				String roomId = L.toString(-1);
				L.pop(1);

				L.getField(1, "listener");
				L.remove(1);

				// New params
				L.newTable(0, 1);
				L.pushValue(1);
				L.setField(-2, "room");
				L.remove(1);
				setListeners(L, true);

				// New argument
				L.pushString(roomId);
				return leave(L);
			} case "setRoomListener":
				L.getField(1, "listener");
				L.setField(1, "room");
				return setListeners(L, true);
			case "sendMessage":
				// Legacy params
				L.getField(1, "playerIDs");
				Hashtable<Object, Object> participantIds = CoronaLua.toHashtable(L, -1);
				L.pop(1);

				L.getField(1, "roomID");
				String roomId = L.toString(-1);
				L.pop(1);

				L.getField(1, "message");
				String payload = L.toString(-1);
				L.pop(1);

				L.getField(1, "reliable");
				Boolean isReliable = L.toBoolean(-1);
				L.pop(1);

				L.getField(1, "listener");
				L.remove(1);

				// New params
				L.newTable(0, 4);

				CoronaLua.pushHashtable(L, participantIds);
				L.setField(-2, "participantIds");

				L.pushValue(2);
				L.setField(-2, "listener");
				L.remove(1);

				L.pushString(roomId);
				L.setField(-2, "roomId");

				L.pushString(payload);
				L.setField(-2, "payload");

				if (isReliable) {
					return sendReliably(L);
				} else {
					return sendUnreliably(L);
				}
			case "setMessageReceivedListener":
				L.getField(1, "listener");
				L.setField(1, "message");
				return setListeners(L, true);
			default:
				return 0;
		}
	}
	//endregion
}
