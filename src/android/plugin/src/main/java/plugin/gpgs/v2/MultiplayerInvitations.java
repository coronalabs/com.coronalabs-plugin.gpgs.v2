package plugin.gpgs.v2;

import android.app.Activity;
import android.content.Intent;

import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaLua;
import com.google.android.gms.games.AnnotatedData;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;

import java.util.Hashtable;

import plugin.gpgs.v2.LuaUtils.Scheme;
import plugin.gpgs.v2.LuaUtils.Table;


class MultiplayerInvitations {


	MultiplayerInvitations(LuaState L) {
		L.newTable(); // plugin.gpgs.v2.multiplayer.invitations
		Utils.setJavaFunctionAsField(L, "load", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return deprecated(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "decline", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return deprecated(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "dismiss", new JavaFunction() {
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
		L.setField(-2, "invitations");
	}
	private int deprecated(LuaState L) {
		LuaUtils.errorLog("Multiplayer is no longer supported");
		return 0;
	}

	//For Legacy compatibility
	boolean hasAction() {
		return true;
	}

	public int request(LuaState L) {
		LuaUtils.errorLog("Multiplayer is no longer supported");
		return 0;
	}
	//endregion
}
