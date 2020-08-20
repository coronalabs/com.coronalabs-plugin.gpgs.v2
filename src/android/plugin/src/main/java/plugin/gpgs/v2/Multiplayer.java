package plugin.gpgs.v2;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.InvitationsClient;
import com.google.android.gms.games.RealTimeMultiplayerClient;
import com.google.android.gms.games.TurnBasedMultiplayerClient;
import com.google.android.gms.tasks.Task;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;

import java.util.Hashtable;

import static com.google.android.gms.games.multiplayer.Multiplayer.MAX_RELIABLE_MESSAGE_LEN;
import static com.google.android.gms.games.multiplayer.Multiplayer.MAX_UNRELIABLE_MESSAGE_LEN;

class Multiplayer {
	MultiplayerInvitations invitations;
	MultiplayerRealtime realtime;
	public static RealTimeMultiplayerClient RealTimeClient() {
		return Games.getRealTimeMultiplayerClient(Connector.getContext(), Connector.getSignInAccount());
	}
	public static TurnBasedMultiplayerClient TurnBasedClient() {
		return Games.getTurnBasedMultiplayerClient(Connector.getContext(), Connector.getSignInAccount());
	}
	public static InvitationsClient InvitationsClient() {
		return Games.getInvitationsClient(Connector.getContext(), Connector.getSignInAccount());
	}

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
		Utils.debugLog("multiplayer.getLimits()");
		if (Utils.checkConnection()) {
			Hashtable<Object, Object> limits = new Hashtable<>();
			limits.put("reliablePayloadSize", MAX_RELIABLE_MESSAGE_LEN);
			limits.put("unreliablePayloadSize", MAX_UNRELIABLE_MESSAGE_LEN);
			Task<Integer> maxMatchDataSize = TurnBasedClient().getMaxMatchDataSize();
			while (!maxMatchDataSize.isSuccessful()){
				int k = 0;
			}
			limits.put("matchPayloadSize", maxMatchDataSize.getResult());
			Utils.pushHashtable(L, limits);
		} else {
			L.pushNil();
		}
		return 0;
	}
}
