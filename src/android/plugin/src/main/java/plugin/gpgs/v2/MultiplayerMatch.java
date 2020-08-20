package plugin.gpgs.v2;

import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;

import java.util.Hashtable;

class MultiplayerMatch {
	private TurnBasedMatch match;

	MultiplayerMatch(LuaState L, TurnBasedMatch match) {
		this.match = match;

		L.newTable(); // match
		Utils.setJavaFunctionAsField(L, "canRematch", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return canRematch(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getAvailableAutoMatchSlots", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getAvailableAutoMatchSlots(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getPayload", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getPayload(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getMainOpponentParticipantId", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getMainOpponentParticipantId(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getLastUpdatedTimestamp", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getLastUpdatedTimestamp(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getLastUpdaterParticipantId", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getLastUpdaterParticipantId(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getParticipant", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getParticipant(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getParticipantId", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getParticipantId(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getParticipantIds", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getParticipantIds(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getParticipantStatus", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getParticipantStatus(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getPendingParticipantId", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getPendingParticipantId(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getPreviousPayload", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getPreviousPayload(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getRematchId", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getRematchId(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getStatus", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getStatus(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getTurnStatus", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getTurnStatus(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getVersion", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getVersion(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "isLocallyModified", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return isLocallyModified(L);
			}
		});

		L.pushString(String.valueOf(match.getCreationTimestamp()));
		L.setField(-2, "timestamp");

		L.pushString(match.getCreatorId());
		L.setField(-2, "creatorId");

		L.pushString(match.getDescription());
		L.setField(-2, "description");

		L.pushString(match.getMatchId());
		L.setField(-2, "id");

		L.pushInteger(match.getMatchNumber());
		L.setField(-2, "matchNumber");

		if (match.getVariant() != TurnBasedMatch.MATCH_VARIANT_DEFAULT) {
			L.pushInteger(match.getVariant());
			L.setField(-2, "variant");
		}
	}

	//region Lua functions
	// match.canRematch()
	private int canRematch(LuaState L) {
		Utils.debugLog("match.canRematch()");
		if (Utils.checkConnection()) {
			L.pushBoolean(match.canRematch());
			return 1;
		}
		return 0;
	}

	// match.getAvailableAutoMatchSlots()
	private int getAvailableAutoMatchSlots(LuaState L) {
		Utils.debugLog("match.getAvailableAutoMatchSlots()");
		if (Utils.checkConnection()) {
			L.pushInteger(match.getAvailableAutoMatchSlots());
			return 1;
		}
		return 0;
	}

	// match.getPayload()
	private int getPayload(LuaState L) {
		Utils.debugLog("match.getPayload()");
		if (Utils.checkConnection()) {
			byte[] payload = match.getData();
			if (payload != null) {
				L.pushString(payload);
				return 1;
			}
		}
		return 0;
	}

	// match.getMainOpponentParticipantId()
	private int getMainOpponentParticipantId(LuaState L) {
		Utils.debugLog("match.getMainOpponentParticipantId()");
		if (Utils.checkConnection()) {
			String participantId = match.getDescriptionParticipantId();
			if (participantId != null) {
				L.pushString(participantId);
			} else {
				L.pushNil();
			}
			return 1;
		}
		return 0;
	}

	// match.getLastUpdatedTimestamp()
	private int getLastUpdatedTimestamp(LuaState L) {
		Utils.debugLog("match.getLastUpdatedTimestamp()");
		if (Utils.checkConnection()) {
			L.pushString(String.valueOf(match.getLastUpdatedTimestamp()));
			return 1;
		}
		return 0;
	}

	// match.getLastUpdaterParticipantId()
	private int getLastUpdaterParticipantId(LuaState L) {
		Utils.debugLog("match.getLastUpdaterParticipantId()");
		if (Utils.checkConnection()) {
			String participantId = match.getLastUpdaterId();
			if (participantId != null) {
				L.pushString(participantId);
				return 1;
			}
		}
		return 0;
	}

	// match.getParticipant(participantId)
	private int getParticipant(LuaState L) {
		Utils.debugLog("match.getParticipant()");
		if (!L.isString(1)){
			Utils.errorLog("getParticipant must receive String parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			String participantId = L.checkString(1);
			Utils.pushHashtable(L, Utils.participantToHashtable(match.getParticipant(participantId)));
			return 1;
		}
		return 0;
	}

	// match.getParticipantId(playerId)
	private int getParticipantId(LuaState L) {
		Utils.debugLog("match.getParticipantId()");
		if (!L.isString(1)){
			Utils.errorLog("getParticipantId must receive String parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			String playerId = L.checkString(1);
			match.getParticipantId(playerId);
			return 1;
		}
		return 0;
	}

	// match.getParticipantIds()
	private int getParticipantIds(LuaState L) {
		Utils.debugLog("match.getParticipantIds()");
		if (Utils.checkConnection()) {
			Hashtable<Object, Object> participantIds = new Hashtable<>();
			int i = 1;
			for (String participantId : match.getParticipantIds()) {
				participantIds.put(i++, participantId);
			}
			Utils.pushHashtable(L, participantIds);
			return 1;
		}
		return 0;
	}

	// match.getParticipantStatus(participantId)
	private int getParticipantStatus(LuaState L) {
		Utils.debugLog("match.getParticipantStatus()");
		if (!L.isString(1)){
			Utils.errorLog("getParticipantStatus must receive String parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			String participantId = L.checkString(1);
			String status = "";
			switch (match.getParticipantStatus(participantId)) {
				case Participant.STATUS_INVITED:
					status = "invited";
					break;
				case Participant.STATUS_JOINED:
					status = "joined";
					break;
				case Participant.STATUS_DECLINED:
					status = "declined";
					break;
				case Participant.STATUS_LEFT:
					status = "left";
					break;
				case Participant.STATUS_NOT_INVITED_YET:
					status = "not invited yet";
					break;
			}
			L.pushString(status);
			return 1;
		}
		return 0;
	}

	// match.getPendingParticipantId()
	private int getPendingParticipantId(LuaState L) {
		Utils.debugLog("match.getPendingParticipantId()");
		if (Utils.checkConnection()) {
			String participantId = match.getPendingParticipantId();
			if (participantId != null) {
				L.pushString(participantId);
			} else {
				L.pushNil();
			}
			return 1;
		}
		return 0;
	}

	// match.getPreviousPayload()
	private int getPreviousPayload(LuaState L) {
		Utils.debugLog("match.getPreviousPayload()");
		if (Utils.checkConnection()) {
			byte[] payload = match.getPreviousMatchData();
			if (payload != null) {
				L.pushString(payload);
				return 1;
			}
		}
		return 0;
	}

	// match.getRematchId()
	private int getRematchId(LuaState L) {
		Utils.debugLog("match.getRematchId()");
		if (Utils.checkConnection()) {
			String matchId = match.getRematchId();
			if (matchId != null) {
				L.pushString(matchId);
			} else {
				L.pushNil();
			}
			return 1;
		}
		return 0;
	}

	// match.getStatus()
	private int getStatus(LuaState L) {
		Utils.debugLog("match.getStatus()");
		if (Utils.checkConnection()) {
			String status = "";
			switch (match.getStatus()) {
				case TurnBasedMatch.MATCH_STATUS_ACTIVE:
					status = "active";
					break;
				case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
					status = "automatching";
					break;
				case TurnBasedMatch.MATCH_STATUS_CANCELED:
					status = "canceled";
					break;
				case TurnBasedMatch.MATCH_STATUS_COMPLETE:
					status = "completed";
					break;
				case TurnBasedMatch.MATCH_STATUS_EXPIRED:
					status = "expired";
					break;
			}
			L.pushString(status);
			return 1;
		}
		return 0;
	}

	// match.getTurnStatus()
	private int getTurnStatus(LuaState L) {
		Utils.debugLog("match.getTurnStatus()");
		if (Utils.checkConnection()) {
			String status = "";
			switch (match.getTurnStatus()) {
				case TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE:
					status = "completed";
					break;
				case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
					status = "invited";
					break;
				case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
					status = "my turn";
					break;
				case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
					status = "their turn";
					break;
			}
			L.pushString(status);
			return 1;
		}
		return 0;
	}

	// match.getVersion()
	private int getVersion(LuaState L) {
		Utils.debugLog("match.getVersion()");
		if (Utils.checkConnection()) {
			L.pushInteger(match.getVersion());
			return 1;
		}
		return 0;
	}

	// match.isLocallyModified()
	private int isLocallyModified(LuaState L) {
		Utils.debugLog("match.isLocallyModified()");
		if (Utils.checkConnection()) {
			L.pushBoolean(match.isLocallyModified());
			return 1;
		}
		return 0;
	}
	//endregion
}
