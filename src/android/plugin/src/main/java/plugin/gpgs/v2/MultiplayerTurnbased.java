package plugin.gpgs.v2;

import android.app.Activity;
import android.content.Intent;

import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaLua;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.Games;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;

import java.util.ArrayList;
import java.util.Hashtable;

import plugin.gpgs.v2.LuaUtils.Scheme;
import plugin.gpgs.v2.LuaUtils.Table;

class MultiplayerTurnbased {

	MultiplayerTurnbased(LuaState L) {
		L.newTable(); // plugin.gpgs.v2.multiplayer.turnbased
		Utils.setJavaFunctionAsField(L, "load", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return deprecated(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "cancel", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return deprecated(L);
			}
		});
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
		Utils.setJavaFunctionAsField(L, "finish", new JavaFunction() {
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
		Utils.setJavaFunctionAsField(L, "rematch", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return deprecated(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "send", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return deprecated(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getMatch", new JavaFunction() {
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
		Utils.setJavaFunctionAsField(L, "setListener", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return deprecated(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "removeListener", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return deprecated(L);
			}
		});
		L.setField(-2, "turnbased");
	}

	private int deprecated(LuaState L) {
		LuaUtils.errorLog("Multiplayer is no longer supported");
		return 0;
	}
	//endregion
}
