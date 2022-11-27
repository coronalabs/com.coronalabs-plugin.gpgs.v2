package plugin.gpgs.v2;

import android.content.Intent;
import androidx.annotation.NonNull;

import com.ansca.corona.CoronaLua;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.VideosClient;
import com.google.android.gms.games.video.CaptureState;
import com.google.android.gms.games.video.VideoCapabilities;
import com.google.android.gms.games.video.VideoConfiguration;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;

import java.util.Hashtable;

import plugin.gpgs.v2.LuaUtils.Scheme;
import plugin.gpgs.v2.LuaUtils.Table;


class Videos {
	//This is no longer supported by Google :(

	Videos(LuaState L) {
		L.newTable(); // plugin.gpgs.v2.videos
		Utils.setJavaFunctionAsField(L, "isSupported", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return notSupportedWarning(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "isModeAvailable", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return notSupportedWarning(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "loadCapabilities", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return notSupportedWarning(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getState", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return notSupportedWarning(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "show", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return notSupportedWarning(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "setListener", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return notSupportedWarning(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "removeListener", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return notSupportedWarning(L);
			}
		});
		L.setField(-2, "videos");
	}


	//region Lua functions
	//Since videos are no longer supported, print a warning and return false
	private int notSupportedWarning(LuaState L) {
		LuaUtils.errorLog("Videos are no longer supported");
		L.pushBoolean(false);
		return 1;
	}

	//endregion
}
