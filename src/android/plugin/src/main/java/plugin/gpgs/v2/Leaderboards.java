package plugin.gpgs.v2;

import android.content.Intent;
import androidx.annotation.NonNull;

import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.leaderboard.Leaderboard;
import com.google.android.gms.games.leaderboard.LeaderboardBuffer;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardScoreBuffer;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.ScoreSubmissionData;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;
import com.naef.jnlua.LuaType;

import java.util.Hashtable;

import plugin.gpgs.v2.LuaUtils.Scheme;
import plugin.gpgs.v2.LuaUtils.Table;

class Leaderboards {

	Leaderboards(LuaState L) {
		L.newTable(); // plugin.gpgs.v2.leaderboards
		Utils.setJavaFunctionAsField(L, "load", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return load(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "loadScores", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return loadScores(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "submit", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return submit(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "show", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return show(L);
			}
		});
		L.setField(-2, "leaderboards");
	}

	private LeaderboardsClient getClient(){
		return Games.getLeaderboardsClient(Connector.getContext(), Connector.getSignInAccount());
	}

	//region Lua functions
	// plugin.gpgs.v2.leaderboards.load(params)
	// params.leaderboardId
	// params.reload
	// params.listener
	private int load(LuaState L) {
		Utils.debugLog("leaderboards.load()");
		final String name = "load";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
					.string("leaderboardId")
					.bool("reload")
					.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String leaderboardId = params.getString("leaderboardId");
			Boolean reload = params.getBoolean("reload", false);
			final Integer luaListener = params.getListener("listener");
			Task<AnnotatedData<Leaderboard>> annotatedDataTaskLeaderBoard = null;
			Task<AnnotatedData<LeaderboardBuffer>> annotatedDataTaskLeaderBoardBuffer = null;
			if (leaderboardId != null) {
				annotatedDataTaskLeaderBoard = getClient().loadLeaderboardMetadata(leaderboardId, reload);
			} else {
				annotatedDataTaskLeaderBoardBuffer = getClient().loadLeaderboardMetadata(reload);
			}
			if (annotatedDataTaskLeaderBoard != null) {
				annotatedDataTaskLeaderBoard.addOnCompleteListener(new
						LeaderboardMetadataOnCompleteListener<AnnotatedData<Leaderboard>>(name, luaListener));
			}
			if (annotatedDataTaskLeaderBoardBuffer != null){
				annotatedDataTaskLeaderBoardBuffer.addOnCompleteListener(new
						LeaderboardMetadataOnCompleteListener<AnnotatedData<LeaderboardBuffer>>(name, luaListener));
			}
		}
		return 0;
	}

	// plugin.gpgs.v2.leaderboards.loadScores(params)
	// params.leaderboardId *
	// params.position = "single", "top", "centered"
	// params.timeSpan = "all time", "daily", "weekly"
	// params.friendsOnly
	// params.limit
	// params.reload
	// params.listener
	private int loadScores(LuaState L) {
		Utils.debugLog("leaderboards.loadScores()");
		final String name = "loadScores";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
					.string("leaderboardId")
					.string("position")
					.string("timeSpan")
					.bool("friendsOnly")
					.number("limit")
					.bool("reload")
					.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String leaderboardId = params.getStringNotNull("leaderboardId");
			String position = params.getString("position", "top");
			String timeSpan = params.getString("timeSpan", "all time");
			Boolean friendsOnly = params.getBoolean("friendsOnly", false);
			int limit = Utils.clamp(params.getLong("limit", 25).intValue(), 1, 25);
			Boolean reload = params.getBoolean("reload", false);
			final Integer luaListener = params.getListener("listener");
			switch (position) {
				case "single":
					getClient().loadCurrentPlayerLeaderboardScore(leaderboardId, Utils.getTimeSpan(timeSpan), Utils.getCollection(friendsOnly)).
							addOnCompleteListener(new OnCompleteListener<AnnotatedData<LeaderboardScore>>() {
								@Override
								public void onComplete(@NonNull Task<AnnotatedData<LeaderboardScore>> task) {
									Hashtable<Object, Object> event = Utils.newEvent(name);
									boolean isError = !task.isSuccessful();
									event.put("isError", isError);
									if (isError) {
										event.put("errorCode", Utils.getErrorCode(task.getException()));
										event.put("errorMessage", task.getException().getLocalizedMessage());
									} else {
										Hashtable<Object, Object> scores = new Hashtable<>();
										LeaderboardScore score = task.getResult().get();
										if (score != null) {
											scores.put(1, Utils.leaderboardScoreToHashtable(score));
										}
										event.put("scores", scores);
									}
									Utils.dispatchEvent(luaListener, event, true);
								}
							});
					break;
				case "centered":
				case "top":
					getClient().loadPlayerCenteredScores(leaderboardId, Utils.getTimeSpan(timeSpan), Utils.getCollection(friendsOnly), limit, reload).
							addOnCompleteListener(new OnCompleteListener<AnnotatedData<LeaderboardsClient.LeaderboardScores>>() {
								@Override
								public void onComplete(@NonNull Task<AnnotatedData<LeaderboardsClient.LeaderboardScores>> task) {
									Hashtable<Object, Object> event = Utils.newEvent(name);
									boolean isError = !task.isSuccessful();
									event.put("isError", isError);
									if (isError) {
										event.put("errorCode", Utils.getErrorCode(task.getException()));
										event.put("errorMessage", task.getException().getLocalizedMessage());
									} else {
										Hashtable<Object, Object> scores = new Hashtable<>();

										int i = 1;
										LeaderboardScoreBuffer scoreBuffer = task.getResult().get().getScores();
										for (LeaderboardScore s : scoreBuffer) {
											scores.put(i++, Utils.leaderboardScoreToHashtable(s));
										}
										scoreBuffer.release();

										event.put("scores", scores);
									}
									Utils.dispatchEvent(luaListener, event, true);
								}
							});
					break;
			}
		}
		return 0;
	}

	// plugin.gpgs.v2.leaderboards.submit(params)
	// params.leaderboardId *
	// params.score *
	// params.tag
	// params.listener
	private int submit(LuaState L) {
		Utils.debugLog("leaderboards.submit()");
		final String name = "submit";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
					.string("leaderboardId")
					.number("score")
					.string("tag")
					.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String leaderboardId = params.getStringNotNull("leaderboardId");
			Long score = params.getLongNotNull("score");
			String tag = params.getString("tag");
			final Integer luaListener = params.getListener("listener");
			if (tag != null) {
				// Only URL safe characters are allowed. Length limit is 64.
				tag = tag.replaceAll("[^A-Za-z0-9_\\.\\-]", "");
				tag = tag.substring(0, Math.min(tag.length(), 64));
			}
			if (luaListener != null) {
				Task<ScoreSubmissionData> task;
				if (tag != null) {
					task = getClient().submitScoreImmediate(leaderboardId, score, tag);
				} else {
					task = getClient().submitScoreImmediate(leaderboardId, score);
				}
				task.addOnCompleteListener(new OnCompleteListener<ScoreSubmissionData>() {
					@Override
					public void onComplete(@NonNull Task<ScoreSubmissionData> task) {
						Hashtable<Object, Object> event = Utils.newEvent(name);
						boolean isError = !task.isSuccessful();
						event.put("isError", isError);
						if (isError) {
							event.put("errorCode", Utils.getErrorCode(task.getException()));
							event.put("errorMessage", task.getException().getLocalizedMessage());
						} else {
							ScoreSubmissionData d = task.getResult();
							event.put("playerId", d.getPlayerId());
							event.put("leaderboardId", d.getLeaderboardId());

							Hashtable<Object, Object> scores = new Hashtable<>();
							for (int timeSpan : new Integer[]{LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.TIME_SPAN_DAILY, LeaderboardVariant.TIME_SPAN_WEEKLY}) {
								ScoreSubmissionData.Result r = d.getScoreResult(timeSpan);
								if (r != null) {
									Hashtable<Object, Object> score = new Hashtable<>();
									score.put("score", String.valueOf(r.rawScore));
									score.put("formattedScore", r.formattedScore);
									score.put("isNewBest", r.newBest);
									Utils.put(score, "tag", r.scoreTag);
									scores.put(Utils.timeSpanToString(timeSpan), score);
								}
							}

							event.put("scores", scores);
						}
						Utils.dispatchEvent(luaListener, event, true);
					}
				});
			} else {
				if (tag != null) {
					getClient().submitScore(leaderboardId, score, tag);
				} else {
					getClient().submitScore(leaderboardId, score);
				}
			}
		}
		return 0;
	}

	// plugin.gpgs.v2.leaderboards.show([params])
	// params.leaderboardId
	// params.timeSpan
	// params.friendsOnly
	int show(LuaState L) {
		Utils.debugLog("leaderboards.show()");
		final String name = "show";
		if (Utils.checkConnection()) {
			Task<Intent> intentTask = getClient().getAllLeaderboardsIntent();
			if (L.isTable(1)) {
				Scheme scheme = new Scheme()
						.string("leaderboardId")
						.string("timeSpan")
						.bool("friendsOnly")
						.listener("listener", name);

				Table params = new Table(L, 1).parse(scheme);
				String leaderboardId = params.getString("leaderboardId");
				String timeSpan = params.getString("timeSpan", "all time"); // "weekly", "daily"
				boolean friendsOnly = params.getBoolean("friendsOnly", false);
				if (leaderboardId != null) {
					intentTask = getClient().getLeaderboardIntent(leaderboardId, Utils.getTimeSpan(timeSpan), Utils.getCollection(friendsOnly));
				}
			}
			intentTask.addOnCompleteListener(new OnCompleteListener<Intent>() {
				@Override
				public void onComplete(@NonNull Task<Intent> task) {
					if (task.isSuccessful()){
						Utils.startActivity(task.getResult());
					}
				}
			});
		}
		return 0;
	}
	//endregion

	//region OnCompleteListeners

	private class LeaderboardMetadataOnCompleteListener<T> implements OnCompleteListener<T> {
		private String name;
		private Integer luaListener;
		private T buffer;

		LeaderboardMetadataOnCompleteListener(String name, Integer luaListener) {
			this.name = name;
			this.luaListener = luaListener;
		}

		@Override
		public void onComplete(@NonNull Task<T> task) {
			Hashtable<Object, Object> event = Utils.newEvent(name);

			boolean isError = !task.isSuccessful();
			event.put("isError", isError);
			if (isError) {
				event.put("errorCode", Utils.getErrorCode(task.getException()));
				event.put("errorMessage", task.getException().getLocalizedMessage());
			} else {
				Hashtable<Object, Object> leaderboards = new Hashtable<>();
				int i = 1;
				buffer = task.getResult();
				if (buffer instanceof LeaderboardBuffer) {
					for (Leaderboard l : ((LeaderboardBuffer) buffer)) {
						Hashtable<Object, Object> leaderboard = new Hashtable<>();

						leaderboard.put("id", l.getLeaderboardId());
						leaderboard.put("name", l.getDisplayName());
						leaderboard.put("imageUri", l.getIconImageUri());
						leaderboard.put("scoreOrder", l.getScoreOrder() == Leaderboard.SCORE_ORDER_LARGER_IS_BETTER ? "larger is better" : "smaller is better");

						leaderboards.put(i++, leaderboard);
					}
					((LeaderboardBuffer) buffer).release();
					event.put("leaderboards", leaderboards);
				}
				if (buffer instanceof Leaderboard){
					Hashtable<Object, Object> leaderboard = new Hashtable<>();
					Leaderboard l = (Leaderboard) buffer;
					leaderboard.put("id", l.getLeaderboardId());
					leaderboard.put("name", l.getDisplayName());
					leaderboard.put("imageUri", l.getIconImageUri());
					leaderboard.put("scoreOrder", l.getScoreOrder() == Leaderboard.SCORE_ORDER_LARGER_IS_BETTER ? "larger is better" : "smaller is better");

					leaderboards.put(i++, leaderboard);
					event.put("leaderboards", leaderboards);
				}
			}
			Utils.dispatchEvent(luaListener, event, true);
		}
	}
	//endregion

	//region Legacy compatibility
	private String[] actions = new String[]{"setHighScore", "loadLeaderboardCategories", "loadScores"};

	boolean hasAction(String action) {
		for (String a : actions) {
			if (action.equals(a)) {
				return true;
			}
		}
		return false;
	}

	// gameNetwork.request(action, ...)
	int request(LuaState L, String action) {
		switch (action) {
			case "setHighScore": {
				// Legacy params
				L.getField(1, "localPlayerScore");

				L.getField(-1, "category");
				String leaderboardId = L.toString(-1);
				L.pop(1);

				L.getField(-1, "value");
				Integer score = L.toInteger(-1);
				L.pop(1);

				L.pop(1); // localPlayerScore table

				L.getField(1, "listener");
				L.remove(1);

				// New params
				L.newTable(0, 3);

				L.pushString(leaderboardId);
				L.setField(-2, "leaderboardId");

				L.pushInteger(score);
				L.setField(-2, "score");

				L.pushValue(1);
				L.setField(-2, "listener");

				L.remove(1); // remove original listener
				return submit(L);
			} case "loadLeaderboardCategories":
				L.pushBoolean(true);
				L.setField(1, "reload");
				return load(L);
			case "loadScores":
				// Legacy params
				L.getField(1, "leaderboard");

				L.getField(-1, "category");
				String leaderboardId = L.toString(-1);
				L.pop(1);

				L.getField(-1, "playerScope");
				String playerScope = L.toString(-1);
				L.pop(1);

				L.getField(-1, "timeScope");
				String timeScope = L.toString(-1);
				L.pop(1);

				L.getField(-1, "range");
				int limit = 25;
				if (L.type(-1) == LuaType.TABLE) {
					L.pushInteger(1);
					L.next(-2);
					if (L.type(-1) == LuaType.NUMBER) {
						limit = L.toInteger(-1);
					}
					L.pop(2);
				}
				L.pop(1);

				L.getField(-1, "playerCentered");
				boolean playerCentered = false;
				if (L.type(-1) == LuaType.BOOLEAN) {
					playerCentered = L.toBoolean(-1);
				}
				L.pop(1);

				L.pop(1); // leaderboard table

				L.getField(1, "listener");
				L.remove(1);

				// New params
				L.newTable(0, 3);

				L.pushString(leaderboardId);
				L.setField(-2, "leaderboardId");

				String position = playerCentered ? "centered" : "top";
				L.pushString(position);
				L.setField(-2, "position");

				String timeSpan = "all time";
				if (timeScope != null) {
					if (timeScope.equals("Week")) {
						timeSpan = "weekly";
					} else if (timeScope.equals("Today")) {
						timeSpan = "daily";
					}
				}
				L.pushString(timeSpan);
				L.setField(-2, "timeSpan");

				boolean friendsOnly = (playerScope != null) && (playerScope.equals("FriendsOnly"));
				L.pushBoolean(friendsOnly);
				L.setField(-2, "friendsOnly");

				L.pushInteger(limit);
				L.setField(-2, "limit");

				L.pushBoolean(true);
				L.setField(-2, "reload");

				L.pushValue(1);
				L.setField(-2, "listener");

				L.remove(1); // remove original listener
				return loadScores(L);
			default:
				return 0;
		}
	}
}
