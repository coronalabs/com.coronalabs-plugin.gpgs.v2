package plugin.gpgs.v2;

import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;

import java.util.Hashtable;

class MultiplayerRoom {
	private Room room;

	MultiplayerRoom(LuaState L, Room room) {
		this.room = room;

		L.newTable(); // room
		Utils.setJavaFunctionAsField(L, "getAutoMatchWaitEstimateSeconds", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getAutomatchWaitEstimateSeconds(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getStatus", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getStatus(L);
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

		L.pushString(String.valueOf(room.getCreationTimestamp()));
		L.setField(-2, "timestamp");

		L.pushString(room.getCreatorId());
		L.setField(-2, "creatorId");

		if (room.getDescription() != null) {
			L.pushString(room.getDescription());
			L.setField(-2, "description");
		}

		L.pushString(room.getRoomId());
		L.setField(-2, "id");

		if (room.getVariant() != Room.ROOM_VARIANT_DEFAULT) {
			L.pushInteger(room.getVariant());
			L.setField(-2, "variant");
		}
	}

	//region Lua functions
	// room.getAutomatchWaitEstimateSeconds()
	private int getAutomatchWaitEstimateSeconds(LuaState L) {
		Utils.debugLog("room.getAutomatchWaitEstimateSeconds()");
		if (Utils.checkConnection()) {
			L.pushInteger(room.getAutoMatchWaitEstimateSeconds());
			return 1;
		}
		return 0;
	}

	// room.getStatus()
	private int getStatus(LuaState L) {
		Utils.debugLog("room.getStatus()");
		if (Utils.checkConnection()) {
			String status = "";
			switch (room.getStatus()) {
				case Room.ROOM_STATUS_INVITING:
					status = "inviting";
					break;
				case Room.ROOM_STATUS_ACTIVE:
					status = "active";
					break;
				case Room.ROOM_STATUS_AUTO_MATCHING:
					status = "automatching";
					break;
				case Room.ROOM_STATUS_CONNECTING:
					status = "connecting";
					break;
			}
			L.pushString(status);
			return 1;
		}
		return 0;
	}

	// room.getParticipant(participantId)
	private int getParticipant(LuaState L) {
		Utils.debugLog("room.getParticipant()");
		if (!L.isString(1)){
			Utils.errorLog("getParticipant must receive string parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			String participantId = L.checkString(1);
			Utils.pushHashtable(L, Utils.participantToHashtable(room.getParticipant(participantId)));
			return 1;
		}
		return 0;
	}

	// room.getParticipantId(playerId)
	private int getParticipantId(LuaState L) {
		Utils.debugLog("room.getParticipantId()");
		if (!L.isString(1)){
			Utils.errorLog("getParticipantId must receive string parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			String playerId = L.checkString(1);
			L.pushString(room.getParticipantId(playerId));
			return 1;
		}
		return 0;
	}

	// room.getParticipantIds()
	private int getParticipantIds(LuaState L) {
		Utils.debugLog("room.getParticipantIds()");
		if (Utils.checkConnection()) {
			Hashtable<Object, Object> participantIds = new Hashtable<>();
			int i = 1;
			for (String participantId : room.getParticipantIds()) {
				participantIds.put(i++, participantId);
			}
			Utils.pushHashtable(L, participantIds);
			return 1;
		}
		return 0;
	}

	// room.getParticipantStatus(participantId)
	private int getParticipantStatus(LuaState L) {
		Utils.debugLog("room.getParticipantStatus()");
		if (!L.isString(1)){
			Utils.errorLog("getParticipantStatus must receive string parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			String participantId = L.checkString(1);
			String status = null;
			switch (room.getParticipantStatus(participantId)) {
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
			}
			L.pushString(status);
			return 1;
		}
		return 0;
	}
	//endregion
}
