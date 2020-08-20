package plugin.gpgs.v2;

import android.content.Intent;
import androidx.annotation.NonNull;

import com.ansca.corona.CoronaLua;
import com.google.android.gms.games.Games;
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

import static com.google.android.gms.games.video.Videos.CAPTURE_OVERLAY_STATE_CAPTURE_STARTED;
import static com.google.android.gms.games.video.Videos.CAPTURE_OVERLAY_STATE_CAPTURE_STOPPED;
import static com.google.android.gms.games.video.Videos.CAPTURE_OVERLAY_STATE_DISMISSED;
import static com.google.android.gms.games.video.Videos.CAPTURE_OVERLAY_STATE_SHOWN;

class Videos {

	Videos(LuaState L) {
		L.newTable(); // plugin.gpgs.v2.videos
		Utils.setJavaFunctionAsField(L, "isSupported", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return isSupported(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "isModeAvailable", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return isModeAvailable(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "loadCapabilities", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return loadCapabilities(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getState", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getState(L);
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
		L.setField(-2, "videos");
	}
	private VideosClient.OnCaptureOverlayStateListener stateListener;
	private VideosClient getClient(){
		return Games.getVideosClient(Connector.getContext(), Connector.getSignInAccount());
	}

	//region Lua functions
	// plugin.gpgs.v2.videos.isSupported()
	private int isSupported(LuaState L) {
		Utils.debugLog("videos.isSupported()");
		if (Utils.checkConnection()) {
			Task<Boolean> isSuppported = getClient().isCaptureSupported();
			while (!isSuppported.isSuccessful()) {
				int k = 0;
			}
			L.pushBoolean(isSuppported.getResult());
		} else {
			L.pushNil();
		}
		return 1;
	}

	// plugin.gpgs.v2.videos.isModeAvailable(params)
	// params.mode *
	// params.listener
	private int isModeAvailable(LuaState L) {
		Utils.debugLog("videos.isModeAvailable()");
		final String name = "isModeAvailable";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
				.string("mode")
				.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String mode = params.getStringNotNull("mode");
			final Integer luaListener = params.getListener("listener");
			Integer captureMode = Utils.captureModeToInt(mode);
			if (captureMode != null && VideoConfiguration.isValidCaptureMode(captureMode ,true)) {
				getClient().isCaptureAvailable(captureMode).addOnCompleteListener(new OnCompleteListener<Boolean>() {
					@Override
					public void onComplete(@NonNull Task<Boolean> task) {
						Hashtable<Object, Object> event = Utils.newEvent(name);
						boolean isError = !task.isSuccessful();
						event.put("isError", isError);
						if (isError) {
							event.put("errorCode", Utils.getErrorCode(task.getException()));
							event.put("errorMessage", task.getException().getLocalizedMessage());
						} else {
							event.put("isAvailable", task.getResult());
						}
						Utils.dispatchEvent(luaListener, event, true);
					}
				});
			}
		}
		return 0;
	}

	// plugin.gpgs.v2.videos.loadCapabilities(listener)
	private int loadCapabilities(LuaState L) {
		Utils.debugLog("videos.loadCapabilities()");
		final String name = "loadCapabilities";
		int luaListener = CoronaLua.REFNIL;
		int initListenerIndex = 1;
		if (CoronaLua.isListener(L, initListenerIndex, name)) {
			luaListener = CoronaLua.newRef(L, initListenerIndex);
		}
		else {
			if (L.isTable(1)) {
				Utils.errorLog(name + " must receive listener parameter or null, got " + L.typeName(1));
				return 0;
			}
		}
		final Integer listener = luaListener;
		if (Utils.checkConnection()) {
			getClient().getCaptureCapabilities().addOnCompleteListener(new OnCompleteListener<VideoCapabilities>() {
				@Override
				public void onComplete(@NonNull Task<VideoCapabilities> task) {
					Hashtable<Object, Object> event = Utils.newEvent(name);
					boolean isError = !task.isSuccessful();
					event.put("isError", isError);
					if (isError) {
						event.put("errorCode", Utils.getErrorCode(task.getException()));
						event.put("errorMessage", task.getException().getLocalizedMessage());
					} else {
						VideoCapabilitiesBridge capabilities = new VideoCapabilitiesBridge(task.getResult());
						event.put("capabilities", capabilities);
					}
					Utils.dispatchEvent(listener, event, true);
				}
			});
		}
		return 0;
	}

	// plugin.gpgs.v2.videos.getState(listener)
	private int getState(final LuaState L) {
		Utils.debugLog("videos.getState()");
		final String name = "getState";
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
		final int taskListener = luaListener;
		if (Utils.checkConnection()) {
			getClient().getCaptureState().addOnCompleteListener(new OnCompleteListener<CaptureState>() {
				@Override
				public void onComplete(@NonNull Task<CaptureState> task) {
					Hashtable<Object, Object> event = Utils.newEvent(name);
					boolean isError = !task.isSuccessful();
					event.put("isError", isError);
					if (isError) {
						event.put("errorCode", Utils.getErrorCode(task.getException()));
						event.put("errorMessage", task.getException().getLocalizedMessage());
					} else {
						CaptureState s = task.getResult();
						String mode;
						switch (s.getCaptureMode()) {
							case VideoConfiguration.CAPTURE_MODE_FILE:
								mode = "file";
								break;
							case VideoConfiguration.CAPTURE_MODE_STREAM:
								mode = "stream";
								break;
							default:
								mode = "unknown";
						}
						event.put("mode", mode);
						String quality;
						switch (s.getCaptureQuality()) {
							case VideoConfiguration.QUALITY_LEVEL_SD:
								quality = "sd";
								break;
							case VideoConfiguration.QUALITY_LEVEL_HD:
								quality = "hd";
								break;
							case VideoConfiguration.QUALITY_LEVEL_FULLHD:
								quality = "fullhd";
								break;
							case VideoConfiguration.QUALITY_LEVEL_XHD:
								quality = "xhd";
								break;
							default:
								quality = "unknown";
						}
						event.put("quality", quality);
						event.put("isCapturing", s.isCapturing());
						event.put("isOverlayVisible", s.isOverlayVisible());
						event.put("isPaused", s.isPaused());
					}
					Utils.dispatchEvent(taskListener, event, true);
				}
			});
		}
		return 0;
	}

	// plugin.gpgs.v2.videos.show(listener)
	int show(final LuaState L) {
		Utils.debugLog("videos.show()");
		final String name = "show";
		Integer luaListener = CoronaLua.REFNIL;
		int listenerIndex = 1;
		if (CoronaLua.isListener(L, listenerIndex, name)) {
			luaListener = CoronaLua.newRef(L, listenerIndex);
		}
		else {
			if (L.isTable(1)){
				Utils.errorLog(name + " must receive listener parameter or null, got "+ L.typeName(1));
				return 0;
			}
		}
		final Integer listener = luaListener;
		if (Utils.checkConnection()) {
			getClient().getCaptureOverlayIntent().addOnCompleteListener(new OnCompleteListener<Intent>() {
				@Override
				public void onComplete(@NonNull Task<Intent> task) {
					if (task.isSuccessful()){
						Utils.startActivity(task.getResult(), name, listener);
					}
				}
			});
		}
		return 0;
	}

	// plugin.gpgs.v2.videos.setListener(listener)
	private int setListener(LuaState L) {
		Utils.debugLog("videos.setListener()");
		final String name = "video";
		int luaListener = CoronaLua.REFNIL;
		int initListenerIndex = 1;
		if (CoronaLua.isListener(L, initListenerIndex, name)) {
			luaListener = CoronaLua.newRef(L, initListenerIndex);
		} else {
			if (L.isTable(1)) {
				Utils.errorLog(name + " must receive listener parameter or null, got " + L.typeName(1));
				return 0;
			}
		}
		final int fLuaListener = luaListener;
		if (Utils.checkConnection()) {
			stateListener = new VideosClient.OnCaptureOverlayStateListener() {
				@Override
				public void onCaptureOverlayStateChanged(int overlayState) {
					String phase;
					switch (overlayState) {
						case CAPTURE_OVERLAY_STATE_CAPTURE_STARTED:
							phase = "started";
							break;
						case CAPTURE_OVERLAY_STATE_CAPTURE_STOPPED:
							phase = "stopped";
							break;
						case CAPTURE_OVERLAY_STATE_DISMISSED:
							phase = "dismissed";
							break;
						case CAPTURE_OVERLAY_STATE_SHOWN:
							phase = "shown";
							break;
						default:
							phase = "unknown";
					}

					Hashtable<Object, Object> event = Utils.newEvent(name);
					event.put("phase", phase);
					event.put("isError", false);
					Utils.dispatchEvent(fLuaListener, event);
				}
			};
			getClient().registerOnCaptureOverlayStateChangedListener(stateListener);
		}
		return 0;
	}

	// plugin.gpgs.v2.videos.removeListener()
	private int removeListener(LuaState L) {
		Utils.debugLog("videos.removeListener()");
		if (Utils.checkConnection()) {
			getClient().unregisterOnCaptureOverlayStateChangedListener(stateListener);
		}
		return 0;
	}
	//endregion

	//region VideoCapabilitiesBridge
	private class VideoCapabilitiesBridge implements LuaUtils.LuaPushable {
		private VideoCapabilities capabilities;

		VideoCapabilitiesBridge(VideoCapabilities capabilities) {
			this.capabilities = capabilities;
		}

		public void push(LuaState L) {
			Hashtable<Object, Object> c = new Hashtable<>();
			c.put("isCameraSupported", capabilities.isCameraSupported());
			c.put("isMicSupported", capabilities.isMicSupported());
			c.put("isWriteStorageSupported", capabilities.isWriteStorageSupported());

			Utils.pushHashtable(L, c); // capabilities

			Utils.setJavaFunctionAsField(L, "supportsCapture", supportsCapture);
		}

		// capabilities.supportsCapture(params)
		// params.mode
		// params.quality
		private JavaFunction supportsCapture = new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				if (!L.isTable(1)){
					Utils.errorLog("supportsCapture must receive table parameter, got "+ L.typeName(1));
					return 0;
				}
				Scheme scheme = new Scheme()
					.string("mode")
					.string("quality");

				Table params = new Table(L, 1).parse(scheme);
				String mode = params.getString("mode");
				String quality = params.getString("quality");
				Integer captureMode = Utils.captureModeToInt(mode);
				Integer qualityLevel = null;
				switch (quality) {
					case "sd":
						qualityLevel = VideoConfiguration.QUALITY_LEVEL_SD;
						break;
					case "hd":
						qualityLevel = VideoConfiguration.QUALITY_LEVEL_HD;
						break;
					case "fullhd":
						qualityLevel = VideoConfiguration.QUALITY_LEVEL_FULLHD;
						break;
					case "xhd":
						qualityLevel = VideoConfiguration.QUALITY_LEVEL_XHD;
						break;
				}

				if ((captureMode != null) && (qualityLevel != null)) {
					L.pushBoolean(capabilities.isFullySupported(captureMode, qualityLevel));
				} else if (captureMode != null) {
					L.pushBoolean(capabilities.supportsCaptureMode(captureMode));
				} else if (qualityLevel != null) {
					L.pushBoolean(capabilities.supportsQualityLevel(qualityLevel));
				} else {
					L.pushNil();
				}
				return 1;
			}
		};
	}
	//endregion
}
