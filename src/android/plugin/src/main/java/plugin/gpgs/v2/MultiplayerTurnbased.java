package plugin.gpgs.v2;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaLua;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.TurnBasedMultiplayerClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.ParticipantResult;
import com.google.android.gms.games.multiplayer.turnbased.LoadMatchesResponse;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchUpdateCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;

import java.util.ArrayList;
import java.util.Hashtable;

import plugin.gpgs.v2.LuaUtils.Scheme;
import plugin.gpgs.v2.LuaUtils.Table;

import static com.google.android.gms.games.multiplayer.Multiplayer.EXTRA_INVITATION;
import static com.google.android.gms.games.multiplayer.Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS;
import static com.google.android.gms.games.multiplayer.Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS;
import static com.google.android.gms.games.multiplayer.Multiplayer.EXTRA_TURN_BASED_MATCH;
import static com.google.android.gms.games.multiplayer.Multiplayer.SORT_ORDER_MOST_RECENT_FIRST;
import static com.google.android.gms.games.multiplayer.Multiplayer.SORT_ORDER_SOCIAL_AGGREGATION;

class MultiplayerTurnbased {
	static Hashtable<String, TurnBasedMatch> matches = new Hashtable<>();

	MultiplayerTurnbased(LuaState L) {
		L.newTable(); // plugin.gpgs.v2.multiplayer.turnbased
		Utils.setJavaFunctionAsField(L, "load", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return load(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "cancel", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return cancel(L);
			}
		});
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
		Utils.setJavaFunctionAsField(L, "finish", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return finish(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "leave", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return leave(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "rematch", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return rematch(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "send", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return send(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getMatch", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getMatch(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "showSelectPlayers", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return showSelectPlayers(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "show", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return show(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "setListener", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return setListener(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "removeListener", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return removeListener(L);
			}
		});
		L.setField(-2, "turnbased");
	}

	private TurnBasedMultiplayerClient getClient(){
		return Games.getTurnBasedMultiplayerClient(Connector.getContext(), Connector.getSignInAccount());
	}

	private class TurnBasedMatchOnCompleteListener implements OnCompleteListener<TurnBasedMatch>{
		private String name;
		private Integer luaListener;

		TurnBasedMatchOnCompleteListener(String name, Integer luaListener) {
			this.name = name;
			this.luaListener = luaListener;
		}
		@Override
		public void onComplete(@NonNull Task<TurnBasedMatch> task) {
			Hashtable<Object, Object> event = Utils.newEvent(name);
			boolean isError = !task.isSuccessful();
			event.put("isError", isError);
			if (isError) {
				event.put("errorCode", Utils.getErrorCode(task.getException()));
				event.put("errorMessage", task.getException().getLocalizedMessage());
			} else {
				TurnBasedMatch match = task.getResult();
				matches.put(match.getMatchId(), match);
				event.put("matchId", match.getMatchId());
			}
			Utils.dispatchEvent(luaListener, event, true);
		}
	}

	//region Lua functions
	// plugin.gpgs.v2.multiplayer.turnbased.load(params)
	// params.matchId
	// params.filters
	// params.mostRecentFirst
	// params.listener
	private int load(LuaState L) {
		Utils.debugLog("multiplayer.turnbased.load()");
		final String name = "load";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
				.string("matchId")
				.table("filters")
				.string("filters.#")
				.bool("mostRecentFirst")
				.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String matchId = params.getString("matchId");
			Hashtable<Object, Object> filters = params.getTable("filters");
			Boolean mostRecentFirst = params.getBoolean("mostRecentFirst", false);
			final Integer luaListener = params.getListener("listener");

			if (matchId != null) {
				getClient().loadMatch(matchId).addOnCompleteListener(new OnCompleteListener<AnnotatedData<TurnBasedMatch>>() {
					@Override
					public void onComplete(@NonNull Task<AnnotatedData<TurnBasedMatch>> task) {
						Hashtable<Object, Object> event = Utils.newEvent(name);
						boolean isError = !task.isSuccessful();
						event.put("isError", isError);
						if (isError) {
							event.put("errorCode", Utils.getErrorCode(task.getException()));
							event.put("errorMessage", task.getException().getLocalizedMessage());
						} else {
							TurnBasedMatch match = task.getResult().get();
							matches.put(match.getMatchId(), match);
							event.put("matchId", match.getMatchId());
						}
						Utils.dispatchEvent(luaListener, event, true);
					}
				});
			} else {
				getClient().loadMatchesByStatus(mostRecentFirst ? SORT_ORDER_MOST_RECENT_FIRST : SORT_ORDER_SOCIAL_AGGREGATION,
						Utils.filtersToMatchTurnStatuses(filters)).addOnCompleteListener(new OnCompleteListener<AnnotatedData<LoadMatchesResponse>>() {
					@Override
					public void onComplete(@NonNull Task<AnnotatedData<LoadMatchesResponse>> task) {
						Hashtable<Object, Object> event = Utils.newEvent(name);
						boolean isError = !task.isSuccessful();
						event.put("isError", isError);
						if (isError) {
							event.put("errorCode", Utils.getErrorCode(task.getException()));
							event.put("errorMessage", task.getException().getLocalizedMessage());
						} else {
							LoadMatchesResponse response = task.getResult().get();

							Hashtable<Object, Object> completed = new Hashtable<>();
							int i = 1;
							for (TurnBasedMatch m : response.getCompletedMatches()) {
								matches.put(m.getMatchId(), m);
								completed.put(i++, m.getMatchId());
							}
							event.put("completed", completed);

							Hashtable<Object, Object> myTurn = new Hashtable<>();
							i = 1;
							for (TurnBasedMatch m : response.getMyTurnMatches()) {
								matches.put(m.getMatchId(), m);
								myTurn.put(i++, m.getMatchId());
							}
							event.put("myTurn", myTurn);

							Hashtable<Object, Object> theirTurn = new Hashtable<>();
							i = 1;
							for (TurnBasedMatch m : response.getTheirTurnMatches()) {
								matches.put(m.getMatchId(), m);
								theirTurn.put(i++, m.getMatchId());
							}
							event.put("theirTurn", theirTurn);

							Hashtable<Object, Object> invitations = new Hashtable<>();
							i = 1;
							for (Invitation _i : response.getInvitations()) {
								invitations.put(i++, Utils.invitationToHashtable(_i));
							}
							event.put("invitations", invitations);

							response.release();
						}
						Utils.dispatchEvent(luaListener, event, true);
					}
				});
			}
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.turnbased.cancel(params)
	// params.matchId *
	// params.listener
	private int cancel(LuaState L) {
		Utils.debugLog("multiplayer.turnbased.cancel()");
		final String name = "cancel";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
				.string("matchId")
				.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String matchId = params.getStringNotNull("matchId");
			final Integer luaListener = params.getListener("listener");

			getClient().cancelMatch(matchId).addOnCompleteListener(new OnCompleteListener<String>() {
				@Override
				public void onComplete(@NonNull Task<String> task) {
					Hashtable<Object, Object> event = Utils.newEvent(name);
					boolean isError = !task.isSuccessful();
					event.put("isError", isError);
					if (isError) {
						event.put("errorCode", Utils.getErrorCode(task.getException()));
						event.put("errorMessage", task.getException().getLocalizedMessage());
					} else {
						event.put("matchId", task.getResult());
					}
					Utils.dispatchEvent(luaListener, event, true);
				}
			});
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.turnbased.create(params)
	// params.playerId
	// params.playerIds
	// params.automatch.minPlayers
	// params.automatch.maxPlayers
	// params.automatch.exclusionBits
	// params.automatch.variant
	// params.listener
	private int create(LuaState L) {
		Utils.debugLog("multiplayer.realtime.create()");
		String name = "create";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
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
				.number("automatch.variant")
				.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String playerId = params.getString("playerId");
			Hashtable<Object, Object> playerIds = params.getTable("playerIds");
			Integer minPlayers = params.getInteger("automatch.minPlayers");
			Integer maxPlayers = params.getInteger("automatch.maxPlayers");
			Long exclusionBits = params.getLong("automatch.exclusionBits", 0);
			Integer variant = params.getInteger("automatch.variant", TurnBasedMatch.MATCH_VARIANT_DEFAULT);
			Integer luaListener = params.getListener("listener");

			ArrayList<String> playerIdArray = new ArrayList<>();
			if ((playerIds != null) && (playerIds.values().size() > 0)) {
				for (Object o : playerIds.values()) {
					playerIdArray.add((String) o);
				}
			} else if (playerId != null) {
				playerIdArray.add(playerId);
			}

			TurnBasedMatchConfig.Builder builder = TurnBasedMatchConfig.builder();
			if (playerIdArray.size() > 0) {
				builder.addInvitedPlayers(playerIdArray);
			}
			if ((minPlayers != null) && (maxPlayers != null)) {
				builder.setAutoMatchCriteria(TurnBasedMatchConfig.createAutoMatchCriteria(minPlayers, maxPlayers, exclusionBits));
			}
			builder.setVariant(variant);
			getClient().createMatch(builder.build()).addOnCompleteListener(new TurnBasedMatchOnCompleteListener(name, luaListener));
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.turnbased.join(params)
	// params.invitationId *
	// params.listener
	private int join(LuaState L) {
		Utils.debugLog("multiplayer.turnbased.join()");
		String name = "join";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
				.string("invitationId")
				.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String invitationId = params.getStringNotNull("invitationId");
			Integer luaListener = params.getListener("listener");
			getClient().acceptInvitation(invitationId).addOnCompleteListener(new TurnBasedMatchOnCompleteListener(name, luaListener));
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.turnbased.finish(params)
	// params.matchId *
	// params.payload
	// params.results
	// params.listener
	private int finish(LuaState L) {
		Utils.debugLog("multiplayer.turnbased.finish()");
		final String name = "finish";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
				.string("matchId")
				.byteArray("payload")
				.table("results")
				.string("results.#.participantId")
				.string("results.#.result")
				.number("results.#.placing")
				.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String matchId = params.getStringNotNull("matchId");
			byte[] payload = params.getByteArray("payload");
			Hashtable<Object, Object> results = params.getTable("results");
			final Integer luaListener = params.getListener("listener");

			ArrayList<ParticipantResult> participantResults = Utils.participantResultsToArrayList(results);

			Task<TurnBasedMatch> match;
			if (payload != null) {
				match = getClient().finishMatch(matchId, payload, participantResults);
			} else {
				match = getClient().finishMatch(matchId);
			}
			match.addOnCompleteListener(new OnCompleteListener<TurnBasedMatch>() {
				@Override
				public void onComplete(@NonNull Task<TurnBasedMatch> task) {
					Hashtable<Object, Object> event = Utils.newEvent(name);
					boolean isError = !task.isSuccessful();
					event.put("isError", isError);
					if (isError) {
						event.put("errorCode", Utils.getErrorCode(task.getException()));
						event.put("errorMessage", task.getException().getLocalizedMessage());
					} else {
						TurnBasedMatch match = task.getResult();
						matches.put(match.getMatchId(), match);
						event.put("matchId", match.getMatchId());
					}
					Utils.dispatchEvent(luaListener, event, true);
				}
			});
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.turnbased.leave(params)
	// params.matchId *
	// params.pendingParticipantId
	// params.pendingAutomatch
	// params.listener
	private int leave(LuaState L) {
		Utils.debugLog("multiplayer.turnbased.leave()");
		final String name = "leave";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
				.string("matchId")
				.string("pendingParticipantId")
				.bool("isPendingAutomatch")
				.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			final String matchId = params.getStringNotNull("matchId");
			String pendingParticipantId = params.getString("pendingParticipantId");
			Boolean isPendingAutomatch = params.getBoolean("isPendingAutomatch");
			final Integer luaListener = params.getListener("listener");

			Task<Void> leaveMatchTask;
			if ((pendingParticipantId == null) && (isPendingAutomatch == null)) {
				leaveMatchTask = getClient().leaveMatch(matchId);
			} else {
				if ((isPendingAutomatch != null) && isPendingAutomatch) {
					leaveMatchTask = getClient().leaveMatchDuringTurn(matchId, null);
				} else {
					leaveMatchTask = getClient().leaveMatchDuringTurn(matchId, pendingParticipantId);
				}
			}
			leaveMatchTask.addOnCompleteListener(new OnCompleteListener<Void>() {
				@Override
				public void onComplete(@NonNull Task<Void> task) {
					Hashtable<Object, Object> event = Utils.newEvent(name);
					boolean isError = !task.isSuccessful();
					event.put("isError", isError);
					if (isError) {
						event.put("errorCode", Utils.getErrorCode(task.getException()));
						event.put("errorMessage", task.getException().getLocalizedMessage());
					} else {
						event.put("matchId", matchId);
					}
					Utils.dispatchEvent(luaListener, event, true);
				}
			});
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.turnbased.rematch(params)
	// params.matchId *
	// params.listener
	private int rematch(LuaState L) {
		Utils.debugLog("multiplayer.turnbased.rematch()");
		String name = "rematch";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
				.string("matchId")
				.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String matchId = params.getStringNotNull("matchId");
			Integer luaListener = params.getListener("listener");
			getClient().rematch(matchId).addOnCompleteListener(new TurnBasedMatchOnCompleteListener(name, luaListener));
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.turnbased.send(params)
	// params.matchId *
	// params.payload *
	// params.pendingParticipantId
	// params.pendingAutomatch
	// params.results
	// params.listener
	private int send(LuaState L) {
		Utils.debugLog("multiplayer.turnbased.send()");
		final String name = "send";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
				.string("matchId")
				.byteArray("payload")
				.string("pendingParticipantId")
				.bool("isPendingAutomatch")
				.table("results")
				.string("results.#.participantId")
				.string("results.#.result")
				.number("results.#.placing")
				.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String matchId = params.getStringNotNull("matchId");
			byte[] payload = params.getByteArrayNotNull("payload");
			String pendingParticipantId = params.getString("pendingParticipantId");
			Boolean isPendingAutomatch = params.getBoolean("isPendingAutomatch");
			Hashtable<Object, Object> results = params.getTable("results");
			final Integer luaListener = params.getListener("listener");

			ArrayList<ParticipantResult> participantResults = Utils.participantResultsToArrayList(results);

			Task<TurnBasedMatch> matchTask = null;
			if (participantResults != null) {
				if ((isPendingAutomatch != null) && isPendingAutomatch) {
					matchTask = getClient().takeTurn(matchId, payload, null, participantResults);
				} else if (pendingParticipantId != null) {
					matchTask = getClient().takeTurn(matchId, payload, pendingParticipantId, participantResults);
				}
			} else {
				if ((isPendingAutomatch != null) && isPendingAutomatch) {
					matchTask = getClient().takeTurn(matchId, payload, null);
				} else if (pendingParticipantId != null) {
					Utils.log("sent");
					matchTask = getClient().takeTurn(matchId, payload, pendingParticipantId);
				}
			}
			if (matchTask != null) {
				matchTask.addOnCompleteListener(new OnCompleteListener<TurnBasedMatch>() {
					@Override
					public void onComplete(@NonNull Task<TurnBasedMatch> task) {
						Hashtable<Object, Object> event = Utils.newEvent(name);
						boolean isError = !task.isSuccessful();
						event.put("isError", isError);
						if (isError) {
							event.put("errorCode", Utils.getErrorCode(task.getException()));
							event.put("errorMessage", task.getException().getLocalizedMessage());
						} else {
							TurnBasedMatch match = task.getResult();
							matches.put(match.getMatchId(), match);
							event.put("matchId", match.getMatchId());
						}
						Utils.dispatchEvent(luaListener, event, true);
					}
				});
			}
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.turnbased.getMatch(matchId)
	private int getMatch(LuaState L) {
		Utils.debugLog("multiplayer.turnbased.getMatch()");
		if (!L.isString(1)){
			Utils.errorLog("getMatch must receive string parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			String matchId = L.checkString(1);
			TurnBasedMatch match = matches.get(matchId);
			if (match != null) {
				new MultiplayerMatch(L, match);
			} else {
				L.pushNil();
			}
		}
		return 1;
	}

	// plugin.gpgs.v2.multiplayer.turnbased.show(listener)
	private int show(LuaState L) {
		Utils.debugLog("multiplayer.turnbased.show()");
		final String name = "show";
		int luaListener = CoronaLua.REFNIL;
		int initListenerIndex = 1;
		if (CoronaLua.isListener(L, initListenerIndex, name)) {
			luaListener = CoronaLua.newRef(L, initListenerIndex);
		}
		else {
			if (L.isTable(1)){
				Utils.errorLog(name + " must receive listener parameter or null, got "+ L.typeName(1));
				return 0;
			}
		}
		final int fLuaListener = luaListener;
		if (Utils.checkConnection()) {
			final CoronaActivity.OnActivityResultHandler resultHandler = new CoronaActivity.OnActivityResultHandler() {
				@Override
				public void onHandleActivityResult(CoronaActivity activity, int requestCode, int resultCode, Intent intent) {
					activity.unregisterActivityResultHandler(this);
					Hashtable<Object, Object> event = Utils.newEvent(name);
					boolean isError = resultCode != Activity.RESULT_OK;
					event.put("isError", isError);
					if (isError) {
						event.put("errorCode", resultCode);
						event.put("errorMessage", Utils.resultCodeToString(resultCode));
					}
					if (intent != null) {
						if (intent.hasExtra(EXTRA_INVITATION)) {
							Invitation invitation = intent.getParcelableExtra(EXTRA_INVITATION);
							event.put("invitation", Utils.invitationToHashtable(invitation));
						} else if (intent.hasExtra(EXTRA_TURN_BASED_MATCH)) {
							TurnBasedMatch match = intent.getParcelableExtra(EXTRA_TURN_BASED_MATCH);
							matches.put(match.getMatchId(), match);
							event.put("matchId", match.getMatchId());
						}
					}
					Utils.dispatchEvent(fLuaListener, event, true);
				}
			};
			getClient().getInboxIntent().addOnCompleteListener(new OnCompleteListener<Intent>() {
				@Override
				public void onComplete(@NonNull Task<Intent> task) {
					Intent intent = task.getResult();
					Utils.startActivity(intent, resultHandler);
				}
			});
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.turnbased.showSelectPlayers(params)
	// params.playerId
	// params.playerIds
	// params.minPlayers
	// params.maxPlayers
	// params.allowAutomatch
	// params.listener
	private int showSelectPlayers(LuaState L) {
		Utils.debugLog("multiplayer.turnbased.showSelectPlayers()");
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
					Utils.dispatchEvent(luaListener, event, true);
				}
			};
			getClient().getSelectOpponentsIntent(minPlayers, maxPlayers, allowAutomatch).addOnCompleteListener(new OnCompleteListener<Intent>() {
				@Override
				public void onComplete(@NonNull Task<Intent> task) {
					if (task.isSuccessful()) {
						Intent intent = task.getResult();
						if (finalArray != null) {
							intent.putExtra(Games.EXTRA_PLAYER_IDS, finalArray);
						}
						Utils.startActivity(intent, resultHandler);
					}
				}
			});
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.turnbased.setListener(listener)
	private int setListener(LuaState L) {
		Utils.debugLog("multiplayer.turnbased.setListener()");
		final String name = "match";
		int luaListener = CoronaLua.REFNIL;
		int initListenerIndex = 1;
		if (CoronaLua.isListener(L, initListenerIndex, name)) {
			luaListener = CoronaLua.newRef(L, initListenerIndex);
		}
		else
		{
			if (L.isTable(1)){
				Utils.errorLog(name + " must receive listener parameter or null, got "+ L.typeName(1));
				return 0;
			}
		}
		final int fLuaListener = luaListener;
		if (Utils.checkConnection()) {
			getClient().registerTurnBasedMatchUpdateCallback(new TurnBasedMatchUpdateCallback() {
				@Override
				public void onTurnBasedMatchReceived(@NonNull TurnBasedMatch match) {
					matches.put(match.getMatchId(), match);
					Hashtable<Object, Object> event = Utils.newEvent(name);
					event.put("phase", "received");
					event.put("isError", false);
					event.put("matchId", match.getMatchId());
					Utils.dispatchEvent(fLuaListener, event);
				}

				@Override
				public void onTurnBasedMatchRemoved(@NonNull String matchId) {
					matches.remove(matchId);
					Hashtable<Object, Object> event = Utils.newEvent(name);
					event.put("phase", "removed");
					event.put("isError", false);
					event.put("matchId", matchId);
					Utils.dispatchEvent(fLuaListener, event);
				}
			});
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.turnbased.removeListener()
	private int removeListener(LuaState L) {
		Utils.debugLog("multiplayer.turnbased.removeListener()");
		if (Utils.checkConnection()) {
			final String name = "match";
			int luaListener = CoronaLua.REFNIL;
			int initListenerIndex = 1;
			if (CoronaLua.isListener(L, initListenerIndex, name)) {
				luaListener = CoronaLua.newRef(L, initListenerIndex);
			}
			final int fLuaListener = luaListener;
			getClient().unregisterTurnBasedMatchUpdateCallback(new TurnBasedMatchUpdateCallback() {
				@Override
				public void onTurnBasedMatchReceived(@NonNull TurnBasedMatch match) {
					matches.put(match.getMatchId(), match);
					Hashtable<Object, Object> event = Utils.newEvent(name);
					event.put("phase", "received");
					event.put("isError", false);
					event.put("matchId", match.getMatchId());
					Utils.dispatchEvent(fLuaListener, event);
				}

				@Override
				public void onTurnBasedMatchRemoved(@NonNull String matchId) {
					matches.remove(matchId);
					Hashtable<Object, Object> event = Utils.newEvent(name);
					event.put("phase", "removed");
					event.put("isError", false);
					event.put("matchId", matchId);
					Utils.dispatchEvent(fLuaListener, event);
				}
			});
		}
		return 0;
	}
	//endregion
}
