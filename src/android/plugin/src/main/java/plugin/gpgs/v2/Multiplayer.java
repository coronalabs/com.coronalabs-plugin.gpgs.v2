package plugin.gpgs.v2;

import com.google.android.gms.games.Games;

import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;

import java.util.Hashtable;

class Multiplayer {
	MultiplayerInvitations invitations;
	MultiplayerRealtime realtime;


	Multiplayer(LuaState L) {
		L.newTable(); // plugin.gpgs.v2.multiplayer

		Utils.setJavaFunctionAsField(L, "getLimits", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getLimits(L);
			}
		});

		invitations = new MultiplayerInvitations(L);
		realtime = new MultiplayerRealtime(L);
		new MultiplayerTurnbased(L);

		L.setField(-2, "multiplayer");
	}

	// plugin.gpgs.v2.multiplayer.getLimits()
	static private int getLimits(LuaState L) {
		LuaUtils.errorLog("Multiplayer is no longer supported");
		return 0;
	}
}
