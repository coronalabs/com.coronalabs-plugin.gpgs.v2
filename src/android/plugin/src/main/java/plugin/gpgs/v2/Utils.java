package plugin.gpgs.v2;

import android.app.Activity;
import android.content.Intent;

import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaEnvironment;
import com.ansca.corona.CoronaRuntime;
import com.ansca.corona.CoronaRuntimeTask;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.games.Game;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayerLevel;
import com.google.android.gms.games.PlayerLevelInfo;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.video.VideoConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;



abstract class Utils extends LuaUtils {
	static int clamp(int value, int min, int max) {
		if (value > max) {
			return max;
		} else if (value < min) {
			return min;
		} else {
			return value;
		}
	}

	static boolean checkConnection() {
		if (Connector.isConnected()) {
			return true;
		} else {
			log("Not connected");
			return false;
		}
	}

	static String resultCodeToString(int resultCode) {
		switch (resultCode) {
			case Activity.RESULT_OK:
				return "ok";
			case Activity.RESULT_CANCELED:
				return "canceled";
			case GamesActivityResultCodes.RESULT_APP_MISCONFIGURED:
				return "app misconfigured";
			case GamesActivityResultCodes.RESULT_LICENSE_FAILED:
				return "license failed";
			case GamesActivityResultCodes.RESULT_NETWORK_FAILURE:
				return "network failure";
			case GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED:
				return "reconnect required";
			case GamesActivityResultCodes.RESULT_SIGN_IN_FAILED:
				return "sign in failed";

			default:
				return String.valueOf(resultCode);
		}
	}

	static String errorCodeToString(int errorCode) {
		switch (errorCode) {
			case ConnectionResult.DEVELOPER_ERROR:
				return "developer error";
			case ConnectionResult.INTERNAL_ERROR:
				return "internal error";
			case ConnectionResult.INVALID_ACCOUNT:
				return "invalid account";
			case ConnectionResult.LICENSE_CHECK_FAILED:
				return "license check failed";
			case ConnectionResult.NETWORK_ERROR:
				return "network error";
			case ConnectionResult.RESOLUTION_REQUIRED:
				return "resolution required";
			case ConnectionResult.SERVICE_DISABLED:
				return "service disabled";
			case ConnectionResult.SERVICE_INVALID:
				return "service invalid";
			case ConnectionResult.SERVICE_MISSING:
				return "service missing";
			case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
				return "service version_update_required";
			case ConnectionResult.SIGN_IN_REQUIRED:
				return "sign in required";
			case ConnectionResult.SUCCESS:
				return "success";
			default:
				return "Unknown error code (" + errorCode + ")";
		}
	}
	static int getCollection(boolean friendsOnly) {
		if (friendsOnly) {
			return LeaderboardVariant.COLLECTION_FRIENDS;
		} else {
			return LeaderboardVariant.COLLECTION_PUBLIC;
		}
	}
	static String statusCodeToString(int statusCode) {
		/*
		switch (statusCode) {
			case STATUS_OK:
				return "ok";
			case STATUS_CLIENT_RECONNECT_REQUIRED:
				return "client reconnect required";
			case STATUS_REAL_TIME_CONNECTION_FAILED:
				return "realtime connection failed";
			case STATUS_MULTIPLAYER_DISABLED:
				return "multiplayer disabled";
			case STATUS_INTERNAL_ERROR:
				return "internal error";
			default:
				return "Unknown error code (" + statusCode + ")";
		}

		 */
		return "";
	}

	static class AvailabilityResult {
		boolean isError;
		String errorMessage;
		int code;
	}

	static AvailabilityResult isGooglePlayServicesAvailable() {
		AvailabilityResult availabilityResult = new AvailabilityResult();
		availabilityResult.isError = false;
		availabilityResult.errorMessage = "";

		CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
		if (activity == null) {
			availabilityResult.isError = true;
			availabilityResult.errorMessage = "Google Play Services require alive CoronaActivity";
			return availabilityResult;
		}

		int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity);
		availabilityResult.code = result;
		if (result != ConnectionResult.SUCCESS) {
			availabilityResult.isError = true;
		}
		switch(result) {
			case ConnectionResult.SERVICE_MISSING:
				availabilityResult.errorMessage = "Google Play Services are missing";
				break;
			case ConnectionResult.SERVICE_UPDATING:
				availabilityResult.errorMessage = "Google Play Services are updating";
				break;
			case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
				availabilityResult.errorMessage = "Google Play Services require an update";
				break;
			case ConnectionResult.SERVICE_DISABLED:
				availabilityResult.errorMessage = "Google Play Services are disabled";
				break;
			case ConnectionResult.SERVICE_INVALID:
				availabilityResult.errorMessage = "Google Play Services are invalid";
				break;
		}
		return availabilityResult;
	}

	static String timeSpanToString(int timeSpan) {
		switch (timeSpan) {
			case LeaderboardVariant.TIME_SPAN_DAILY:
				return "daily";
			case LeaderboardVariant.TIME_SPAN_WEEKLY:
				return "weekly";
			default:
				return "all time";
		}
	}

	static int getTimeSpan(String timeSpan) {
		switch (timeSpan) {
			case "daily":
				return LeaderboardVariant.TIME_SPAN_DAILY;
			case "weekly":
				return LeaderboardVariant.TIME_SPAN_WEEKLY;
			default:
				return LeaderboardVariant.TIME_SPAN_ALL_TIME;
		}
	}

	static Hashtable<Object, Object> playerLevelToHashtable(PlayerLevel l) {
		Hashtable<Object, Object> level = new Hashtable<>();
		level.put("number", l.getLevelNumber());
		level.put("maxXp", l.getMaxXp());
		level.put("minXp", l.getMinXp());
		return level;
	}

	static Hashtable<Object, Object> playerToHashtable(Player p) {
		Hashtable<Object, Object> player = new Hashtable<>();
		player.put("id", p.getPlayerId());
		player.put("name", p.getDisplayName());
		put(player, "title", p.getTitle());
		if (p.getHiResImageUri() != null) {
			player.put("largeImageUri", p.getHiResImageUri().toString());
		}
		if (p.getIconImageUri() != null) {
			player.put("smallImageUri", p.getIconImageUri().toString());
		}
		if (p.getBannerImageLandscapeUri() != null) {
			player.put("landscapeBannerUri", p.getBannerImageLandscapeUri().toString());
		}
		if (p.getBannerImagePortraitUri() != null) {
			player.put("portraitBannerUri", p.getBannerImagePortraitUri().toString());
		}
		if (p.getLastPlayedWithTimestamp() != Player.TIMESTAMP_UNKNOWN) {
			player.put("lastMultiplayerTimestamp", String.valueOf(p.getLastPlayedWithTimestamp()));
		}
		PlayerLevelInfo l = p.getLevelInfo();
		if (l != null) {
			Hashtable<Object, Object> level = new Hashtable<>();
			level.put("current", playerLevelToHashtable(l.getCurrentLevel()));
			level.put("next", playerLevelToHashtable(l.getNextLevel()));
			level.put("isMax", l.isMaxLevel());
			level.put("xp", l.getCurrentXpTotal());
			level.put("lastLevelUpTimestamp", String.valueOf(l.getLastLevelUpTimestamp()));
			player.put("level", level);
		}
		return player;
	}

	static Hashtable<Object, Object> leaderboardScoreToHashtable(LeaderboardScore s) {
		Hashtable<Object, Object> score = new Hashtable<>();
		score.put("rank", s.getRank());
		score.put("score", s.getRawScore());
		score.put("formattedRank", s.getDisplayRank());
		score.put("formattedScore", s.getDisplayScore());
		put(score, "tag", s.getScoreTag());
		score.put("timestamp", String.valueOf(s.getTimestampMillis()));

		Player p = s.getScoreHolder();
		if (p != null) {
			score.put("player", playerToHashtable(p));
		} else {
			Hashtable<Object, Object> player = new Hashtable<>();
			player.put("name", s.getScoreHolderDisplayName());
			if (s.getScoreHolderHiResImageUri() != null) {
				player.put("largeImageUri", s.getScoreHolderHiResImageUri().toString());
			}
			if (s.getScoreHolderIconImageUri() != null) {
				player.put("smallImageUri", s.getScoreHolderIconImageUri().toString());
			}
			score.put("player", player);
		}

		return score;
	}

	static Hashtable<Object, Object> gameToHashtable(Game g) {
		Hashtable<Object, Object> game = new Hashtable<>();
		put(game, "areSnapshotsEnabled", g.areSnapshotsEnabled());
		put(game, "achievementCount", g.getAchievementTotalCount());
		put(game, "id", g.getApplicationId());
		put(game, "description", g.getDescription());
		put(game, "developerName", g.getDeveloperName());
		put(game, "name", g.getDisplayName());
		putAsString(game, "featuredImageUri", g.getFeaturedImageUri());
		putAsString(game, "largeImageUri", g.getHiResImageUri());
		putAsString(game, "smallImageUri", g.getIconImageUri());
		put(game, "leaderboardCount", g.getLeaderboardCount());
		put(game, "primaryCategory", g.getPrimaryCategory());
		put(game, "secondaryCategory", g.getSecondaryCategory());
		put(game, "themeColor", g.getThemeColor());
		put(game, "hasGamepadSupport", g.hasGamepadSupport());
		return game;
	}

	static Hashtable<Object, Object> snapshotMetadataToHashtable(SnapshotMetadata m) {
		Hashtable<Object, Object> metadata = new Hashtable<>();
		if (m.getCoverImageUri() != null) {
			put(metadata, "imageAspectRatio", m.getCoverImageAspectRatio());
			putAsString(metadata, "imageUri", m.getCoverImageUri());
		}
		put(metadata, "description", m.getDescription());
		put(metadata, "deviceName", m.getDeviceName());
		put(metadata, "game", gameToHashtable(m.getGame()));
		put(metadata, "timestamp", String.valueOf(m.getLastModifiedTimestamp()));
		put(metadata, "player", playerToHashtable(m.getOwner()));
		if (m.getPlayedTime() != SnapshotMetadata.PLAYED_TIME_UNKNOWN) {
			put(metadata, "playedTime", m.getPlayedTime());
		}
		if (m.getProgressValue() != SnapshotMetadata.PROGRESS_VALUE_UNKNOWN) {
			put(metadata, "progress", m.getProgressValue());
		}
		put(metadata, "id", m.getSnapshotId());
		//put(metadata, "title", m.getTitle());
		put(metadata, "name", m.getUniqueName());
		put(metadata, "hasChangePending", m.hasChangePending());

		return metadata;
	}





	static Integer captureModeToInt(String mode) {
		if (mode != null) {
			switch (mode) {
				case "file":
					return VideoConfiguration.CAPTURE_MODE_FILE;
				case "stream":
					return VideoConfiguration.CAPTURE_MODE_STREAM;
				default:
					return VideoConfiguration.CAPTURE_MODE_UNKNOWN;
			}
		}
		return null;
	}

	static int getErrorCode(Exception ex){
		int status = CommonStatusCodes.DEVELOPER_ERROR;
		if (ex instanceof ApiException) {
			status = ((ApiException) ex).getStatusCode();
		}
		return status;
	}




	static Hashtable<Object, Object> listToHashtable(List<String> list) {
		int i = 1;
		Hashtable<Object, Object> hashtable = new Hashtable<>();
		for (Object o : list) {
			hashtable.put(i++, o);
		}
		return hashtable;
	}

	static Hashtable<Object, Object> newLegacyEvent(String name) {
		Hashtable<Object, Object> event = newEvent(name);
		event.put("type", name);
		return event;
	}

	static void startActivity(final Intent intent) {
		startActivity(intent, null, null);
	}

	static void startActivity(final Intent intent, final String name, final Integer luaListener) {
		CoronaActivity.OnActivityResultHandler resultHandler = new CoronaActivity.OnActivityResultHandler() {
			@Override
			public void onHandleActivityResult(CoronaActivity activity, int requestCode, int resultCode, Intent intent) {
				activity.unregisterActivityResultHandler(this);
				if ((luaListener != null) && (name != null)) {
					Hashtable<Object, Object> event = newEvent(name);

					boolean isError = resultCode != Activity.RESULT_OK;
					event.put("isError", isError);
					if (isError) {
						event.put("errorCode", resultCode);
						event.put("errorMessage", resultCodeToString(resultCode));
						if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
							activity.getRuntimeTaskDispatcher().send(new CoronaRuntimeTask() {
								public void executeUsing(CoronaRuntime runtime) {
									Connector.getInstance().signOut();
								}
							});
						}
					}
					dispatchEvent(luaListener, event, true);
				}
			}
		};
		startActivity(intent, resultHandler);
	}

	static void startActivity(final Intent intent, final CoronaActivity.OnActivityResultHandler resultHandler) {
		CoronaActivity.OnActivityResultHandler resultHandlerWrapper = new CoronaActivity.OnActivityResultHandler() {
			@Override
			public void onHandleActivityResult(CoronaActivity activity, int requestCode, int resultCode, Intent intent) {
				if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
					activity.getRuntimeTaskDispatcher().send(new CoronaRuntimeTask() {
						public void executeUsing(CoronaRuntime runtime) {
							Connector.getInstance().signOut();
						}
					});
				}
				resultHandler.onHandleActivityResult(activity, requestCode, resultCode, intent);
			}
		};

		final CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
		if (activity != null) {
			final int requestCode = activity.registerActivityResultHandler(resultHandlerWrapper);
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					activity.startActivityForResult(intent, requestCode);
				}
			});
		}
	}
}
