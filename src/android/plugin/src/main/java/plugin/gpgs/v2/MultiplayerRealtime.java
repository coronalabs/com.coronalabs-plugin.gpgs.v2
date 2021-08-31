package plugin.gpgs.v2;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.NonNull;

import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaLua;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.games.Games;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import plugin.gpgs.v2.LuaUtils.Scheme;
import plugin.gpgs.v2.LuaUtils.Table;



class MultiplayerRealtime {


	MultiplayerRealtime(LuaState L) {
		L.newTable(); // plugin.gpgs.v2.multiplayer.realtime
		Utils.setJavaFunctionAsField(L, "create", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return deprecated(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "join", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return deprecated(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "leave", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return deprecated(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "sendReliably", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return deprecated(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "sendUnreliably", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return deprecated(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getRoom", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return deprecated(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "showSelectPlayers", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return deprecated(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "show", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return deprecated(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "setListeners", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return deprecated(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "removeListeners", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return deprecated(L);
			}
		});
		L.setField(-2, "realtime");
	}


	private int deprecated(LuaState L) {
		LuaUtils.errorLog("Multiplayer is no longer supported");
		return 0;
	}

	//For Legacy compatibility
	boolean hasAction() {
		return true;
	}
	int request(LuaState L) {
		LuaUtils.errorLog("Multiplayer is no longer supported");
		return 0;
	}

	//endregion
}
