package plugin.gpgs.v2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;

import com.ansca.corona.CoronaActivity;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataBuffer;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;

import java.io.IOException;
import java.util.Hashtable;

import plugin.gpgs.v2.LuaUtils.Scheme;
import plugin.gpgs.v2.LuaUtils.Table;

import static com.google.android.gms.games.snapshot.Snapshots.DISPLAY_LIMIT_NONE;
import static com.google.android.gms.games.snapshot.Snapshots.EXTRA_SNAPSHOT_METADATA;
import static com.google.android.gms.games.snapshot.Snapshots.EXTRA_SNAPSHOT_NEW;
import static com.google.android.gms.games.snapshot.Snapshots.RESOLUTION_POLICY_HIGHEST_PROGRESS;
import static com.google.android.gms.games.snapshot.Snapshots.RESOLUTION_POLICY_LAST_KNOWN_GOOD;
import static com.google.android.gms.games.snapshot.Snapshots.RESOLUTION_POLICY_LONGEST_PLAYTIME;
import static com.google.android.gms.games.snapshot.Snapshots.RESOLUTION_POLICY_MANUAL;
import static com.google.android.gms.games.snapshot.Snapshots.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED;

class Snapshots {

	Hashtable<String, Snapshot> snapshotHashtable = new Hashtable<>();

	Snapshots(LuaState L) {
		L.newTable(); // plugin.gpgs.v2.snapshots
		Utils.setJavaFunctionAsField(L, "load", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return load(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "open", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return open(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "save", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return save(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "discard", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return discard(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "delete", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return delete(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "resolveConflict", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return resolveConflict(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getSnapshot", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getSnapshot(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "getLimits", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return getLimits(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "show", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return show(L);
			}
		});
		L.setField(-2, "snapshots");
	}

	private SnapshotsClient getClient() {
		return Games.getSnapshotsClient(Connector.getContext(), Connector.getSignInAccount());
	}

	//region Lua functions
	// plugin.gpgs.v2.snapshots.load(params)
	// params.reload
	// params.listener
	private int load(LuaState L) {
		Utils.debugLog("snapshots.load()");
		final String name = "load";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
					.bool("reload")
					.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			Boolean reload = params.getBoolean("reload", false);
			final Integer luaListener = params.getListener("listener");
			getClient().load(reload).addOnCompleteListener(new OnCompleteListener<AnnotatedData<SnapshotMetadataBuffer>>() {
				@Override
				public void onComplete(@NonNull Task<AnnotatedData<SnapshotMetadataBuffer>> task) {
					Hashtable<Object, Object> event = Utils.newEvent(name);
					boolean isError = !task.isSuccessful();
					event.put("isError", isError);
					if (isError) {
						event.put("errorCode", Utils.getErrorCode(task.getException()));
						event.put("errorMessage", task.getException().getLocalizedMessage());
					} else {
						Hashtable<Object, Object> snapshots = new Hashtable<>();

						int i = 1;
						SnapshotMetadataBuffer snapshotMetadataBuffer = task.getResult().get();
						for (SnapshotMetadata s : snapshotMetadataBuffer) {
							snapshots.put(i++, Utils.snapshotMetadataToHashtable(s));
						}
						snapshotMetadataBuffer.release();

						event.put("snapshots", snapshots);
					}
					Utils.dispatchEvent(luaListener, event, true);
				}
			});
		}
		return 0;
	}

	// plugin.gpgs.v2.snapshots.open(params)
	// params.filename *
	// params.create
	// params.conflictPolicy
	// params.listener
	private int open(LuaState L) {
		Utils.debugLog("snapshots.open()");
		String name = "open";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
					.string("filename")
					.bool("create")
					.string("conflictPolicy")
					.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String filename = params.getStringNotNull("filename");
			Boolean create = params.getBoolean("create", false);
			String conflictPolicy = params.getString("conflictPolicy", "manual");
			Integer luaListener = params.getListener("listener");

			int conflictPolicyInt = RESOLUTION_POLICY_MANUAL;
			switch (conflictPolicy) {
				case "highest progress":
					conflictPolicyInt = RESOLUTION_POLICY_HIGHEST_PROGRESS;
					break;
				case "last known good":
					conflictPolicyInt = RESOLUTION_POLICY_LAST_KNOWN_GOOD;
					break;
				case "longest playtime":
					conflictPolicyInt = RESOLUTION_POLICY_LONGEST_PLAYTIME;
					break;
				case "most recently modified":
					conflictPolicyInt = RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED;
					break;
			}
			getClient().open(filename, create, conflictPolicyInt).addOnCompleteListener(new OpenSnapshotOnCompleteListener(name, luaListener));
		}
		return 0;
	}

	// plugin.gpgs.v2.snapshots.save(params)
	// params.snapshotId *
	// params.description
	// params.playedTime
	// params.progress
	// params.image.filename
	// params.image.baseDir
	// params.listener
	private int save(LuaState L) {
		Utils.debugLog("snapshots.save()");
		final String name = "save";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
					.string("snapshotId")
					.string("description")
					.number("playedTime")
					.number("progress")
					.table("image")
					.string("image.filename")
					.lightuserdata("image.baseDir")
					.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String snapshotId = params.getStringNotNull("snapshotId");
			String description = params.getString("description");
			Long playedTime = params.getLong("playedTime");
			Long progress = params.getLong("progress");
			String imageFilename = params.getString("image.filename");
			LuaUtils.LuaLightuserdata imageBaseDir = params.getLightuserdata("image.baseDir", Utils.Dirs.resourceDirectoryPointer);
			final Integer luaListener = params.getListener("listener");

			Snapshot snapshot = snapshotHashtable.get(snapshotId);
			if (snapshot != null) {
				SnapshotMetadataChange metadataChange = SnapshotMetadataChange.EMPTY_CHANGE;

				if ((description != null) || (playedTime != null) || (progress != null) || (imageFilename != null)) {
					SnapshotMetadataChange.Builder builder = new SnapshotMetadataChange.Builder();
					if (description != null) {
						builder.setDescription(description);
					}
					if (playedTime != null) {
						builder.setPlayedTimeMillis(playedTime);
					}
					if (progress != null) {
						builder.setProgressValue(progress);
					}
					if (imageFilename != null) {
						Bitmap image = Utils.getBitmap(L, imageFilename, imageBaseDir);
						if (image != null) {
							builder.setCoverImage(image);
						}
					}
					metadataChange = builder.build();
				}

				getClient().commitAndClose(snapshot, metadataChange).addOnCompleteListener(new OnCompleteListener<SnapshotMetadata>() {
					@Override
					public void onComplete(@NonNull Task<SnapshotMetadata> task) {
						Hashtable<Object, Object> event = Utils.newEvent(name);
						boolean isError = !task.isSuccessful();
						event.put("isError", isError);
						if (isError) {
							event.put("errorCode", Utils.getErrorCode(task.getException()));
							event.put("errorMessage", task.getException().getLocalizedMessage());
						} else {
							try {
								SnapshotMetadata result = task.getResult();
								snapshotHashtable.remove(result.getSnapshotId());
								event.put("snapshot", Utils.snapshotMetadataToHashtable(result));
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						}
						Utils.dispatchEvent(luaListener, event, true);
					}
				});
			} else {
				Utils.log("snapshot id not found: " + snapshotId);
			}
		}
		return 0;
	}

	// plugin.gpgs.v2.snapshots.discard(snapshotId)
	private int discard(LuaState L) {
		Utils.debugLog("snapshots.discard()");
		if (!L.isString(1)){
			Utils.errorLog("discard must receive String parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			String snapshotId = L.checkString(1);
			Snapshot snapshot = snapshotHashtable.get(snapshotId);
			if (snapshot != null) {
				getClient().discardAndClose(snapshot);
			} else {
				Utils.log("snapshot id not found: " + snapshotId);
			}
		}
		return 0;
	}

	// plugin.gpgs.v2.snapshots.delete(params)
	// params.snapshotId *
	// params.listener
	private int delete(LuaState L) {
		Utils.debugLog("snapshots.delete()");
		final String name = "delete";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
					.string("snapshotId")
					.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String snapshotId = params.getStringNotNull("snapshotId");
			final Integer luaListener = params.getListener("listener");
			Snapshot snapshot = snapshotHashtable.get(snapshotId);
			if (snapshot != null) {
				getClient().delete(snapshot.getMetadata()).addOnCompleteListener(new OnCompleteListener<String>() {
					@Override
					public void onComplete(@NonNull Task<String> task) {
						Hashtable<Object, Object> event = Utils.newEvent(name);
						boolean isError = !task.isSuccessful();
						event.put("isError", isError);
						if (isError) {
							event.put("errorCode", Utils.getErrorCode(task.getException()));
							event.put("errorMessage", task.getException().getLocalizedMessage());
						} else {
							String result = task.getResult();
							snapshotHashtable.remove(result);
							event.put("snapshotId", result);
						}
						Utils.dispatchEvent(luaListener, event, true);
					}
				});
			} else {
				Utils.log("snapshot id not found: " + snapshotId);
			}
		}
		return 0;
	}

	// plugin.gpgs.v2.snapshots.resolveConflict(params)
	// params.conflictId *
	// params.snapshotId *
	// params.listener
	private int resolveConflict(LuaState L) {
		Utils.debugLog("snapshots.resolveConflict()");
		String name = "resolveConflict";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
					.string("conflictId")
					.string("snapshotId")
					.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String conflictId = params.getStringNotNull("conflictId");
			String snapshotId = params.getStringNotNull("snapshotId");
			Integer luaListener = params.getListener("listener");
			Snapshot snapshot = snapshotHashtable.get(snapshotId);
			if (snapshot != null) {
				getClient().resolveConflict(conflictId, snapshot).addOnCompleteListener(new OpenSnapshotOnCompleteListener(name, luaListener));
			} else {
				Utils.log("snapshot id not found: " + snapshotId);
			}
		}
		return 0;
	}

	// plugin.gpgs.v2.snapshots.getSnapshot(snapshotId)
	private int getSnapshot(LuaState L) {
		Utils.debugLog("snapshots.getSnapshot()");
		if (!L.isString(1)){
			Utils.errorLog("getSnapshot must receive String parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			String snapshotId = L.checkString(1);
			Snapshot snapshot = snapshotHashtable.get(snapshotId);
			if (snapshot != null) {
				SnapshotBridge snapshotBridge = new SnapshotBridge(snapshot);
				snapshotBridge.push(L);
			} else {
				Utils.log("snapshot id not found: " + snapshotId);
				L.pushNil();
			}
		} else {
			L.pushNil();
		}
		return 1;
	}

	// plugin.gpgs.v2.snapshots.getLimits()
	private int getLimits(LuaState L) {
		Utils.debugLog("snapshots.getLimits()");
		if (Utils.checkConnection()) {
			Hashtable<Object, Object> limits = new Hashtable<>();
			limits.put("imageSize", getClient().getMaxCoverImageSize());
			limits.put("payloadSize", getClient().getMaxDataSize());
			Utils.pushHashtable(L, limits);
		} else {
			L.pushNil();
		}
		return 1;
	}

	// plugin.gpgs.v2.snapshots.show(params)
	// params.title *
	// params.disableAdd
	// params.disableDelete
	// params.limit
	// params.listener
	private int show(LuaState L) {
		Utils.debugLog("snapshots.show()");
		final String name = "show";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
					.string("title")
					.bool("disableAdd")
					.bool("disableDelete")
					.number("limit")
					.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String title = params.getStringNotNull("title");
			Boolean disableAdd = params.getBoolean("disableAdd", false);
			Boolean disableDelete = params.getBoolean("disableDelete", false);
			Integer limit = params.getInteger("limit", DISPLAY_LIMIT_NONE);
			final Integer luaListener = params.getListener("listener");
			final CoronaActivity.OnActivityResultHandler resultHandler = new CoronaActivity.OnActivityResultHandler() {
				@Override
				public void onHandleActivityResult(CoronaActivity activity, int requestCode, int resultCode, Intent intent) {
					activity.unregisterActivityResultHandler(this);
					if (luaListener != null) {
						Hashtable<Object, Object> event = Utils.newEvent(name);

						boolean isError = resultCode != Activity.RESULT_OK;
						event.put("isError", isError);
						if (isError) {
							event.put("errorCode", resultCode);
							event.put("errorMessage", Utils.resultCodeToString(resultCode));
						}
						if (intent != null) {
							if (intent.hasExtra(EXTRA_SNAPSHOT_METADATA)) {
								SnapshotMetadata snapshotMetadata = intent.getParcelableExtra(EXTRA_SNAPSHOT_METADATA);
								event.put("snapshot", Utils.snapshotMetadataToHashtable(snapshotMetadata));
							} else if (intent.hasExtra(EXTRA_SNAPSHOT_NEW)) {
								event.put("isNew", true);
							}
						}
						Utils.dispatchEvent(luaListener, event, true);
					}
				}
			};
			getClient().getSelectSnapshotIntent(title, !disableAdd, !disableDelete, limit).addOnCompleteListener(new OnCompleteListener<Intent>() {
				@Override
				public void onComplete(@NonNull Task<Intent> task) {
					Utils.startActivity(task.getResult(), resultHandler);
				}
			});
		}
		return 0;
	}
	//endregion

	//region OnCompleteListeners

	private class OpenSnapshotOnCompleteListener implements OnCompleteListener<SnapshotsClient.DataOrConflict<Snapshot>> {
		private String name;
		private Integer luaListener;

		OpenSnapshotOnCompleteListener(String name, Integer luaListener) {
			this.name = name;
			this.luaListener = luaListener;
		}

		@Override
		public void onComplete(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) {
			Hashtable<Object, Object> event = Utils.newEvent(name);
			boolean isError = !task.isSuccessful();
			event.put("isError", isError);
			if (isError) {
				event.put("errorCode", Utils.getErrorCode(task.getException()));
				event.put("errorMessage", task.getException().getLocalizedMessage());
			} else {
				if (task.getResult().isConflict()) {
					SnapshotsClient.SnapshotConflict conflict = task.getResult().getConflict();
					event.put("conflictId", conflict.getConflictId());
					snapshotHashtable.put(conflict.getConflictingSnapshot().getMetadata().getSnapshotId(), conflict.getConflictingSnapshot());
					event.put("conflictingSnapshotId", conflict.getConflictingSnapshot().getMetadata().getSnapshotId());
					snapshotHashtable.put(conflict.getSnapshot().getMetadata().getSnapshotId(), conflict.getSnapshot());
					event.put("snapshotId", conflict.getSnapshot().getMetadata().getSnapshotId());
				} else {
					Snapshot snapshot = task.getResult().getData();
					snapshotHashtable.put(snapshot.getMetadata().getSnapshotId(), snapshot);
					event.put("snapshotId", snapshot.getMetadata().getSnapshotId());
					event.put("snapshot", Utils.snapshotMetadataToHashtable(snapshot.getMetadata()));
				}
			}
			Utils.dispatchEvent(luaListener, event, true);
		}
	}

	//endregion

	//region SnapshotBridge
	private class SnapshotBridge {
		private Snapshot snapshot;

		SnapshotBridge(Snapshot snapshot) {
			this.snapshot = snapshot;
		}

		void push(LuaState L) {
			Utils.pushHashtable(L, Utils.snapshotMetadataToHashtable(snapshot.getMetadata())); // snapshot

			L.newTable(); // snapshot.contents
			Utils.setJavaFunctionAsField(L, "isClosed", isClosed);
			Utils.setJavaFunctionAsField(L, "modify", modify);
			Utils.setJavaFunctionAsField(L, "read", read);
			Utils.setJavaFunctionAsField(L, "write", write);
			L.setField(-2, "contents");
		}

		private JavaFunction isClosed = new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				L.pushBoolean(snapshot.getSnapshotContents().isClosed());
				return 1;
			}
		};

		private JavaFunction modify = new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				byte[] data = L.checkString(1).getBytes();
				int offset = L.checkInteger(2);
				L.pushBoolean(snapshot.getSnapshotContents().modifyBytes(offset, data, 0, data.length));
				return 1;
			}
		};

		private JavaFunction read = new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				byte[] data = null;
				try {
					data = snapshot.getSnapshotContents().readFully();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (data != null) {
					L.pushString(data);
				} else {
					L.pushNil();
				}
				return 1;
			}
		};

		private JavaFunction write = new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				if (!L.isString(1)){
					Utils.errorLog("write must receive String parameter, got "+ L.typeName(1));
					return 0;
				}
				String data = L.checkString(1);
				L.pushBoolean(snapshot.getSnapshotContents().writeBytes(data.getBytes()));
				return 1;
			}
		};
	}
	//endregion
}
