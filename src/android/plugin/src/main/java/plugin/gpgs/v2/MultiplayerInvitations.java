package plugin.gpgs.v2;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaLua;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.InvitationBuffer;
import com.google.android.gms.games.multiplayer.InvitationCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;

import java.util.Hashtable;

import plugin.gpgs.v2.LuaUtils.Scheme;
import plugin.gpgs.v2.LuaUtils.Table;

import static com.google.android.gms.games.multiplayer.Multiplayer.EXTRA_INVITATION;
import static com.google.android.gms.games.multiplayer.Multiplayer.EXTRA_TURN_BASED_MATCH;
import static com.google.android.gms.games.multiplayer.Multiplayer.SORT_ORDER_MOST_RECENT_FIRST;
import static com.google.android.gms.games.multiplayer.Multiplayer.SORT_ORDER_SOCIAL_AGGREGATION;

class MultiplayerInvitations {

	private InvitationCallback callback;
	MultiplayerInvitations(LuaState L) {
		L.newTable(); // plugin.gpgs.v2.multiplayer.invitations
		Utils.setJavaFunctionAsField(L, "load", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return load(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "decline", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return decline(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "dismiss", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return dismiss(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "show", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return show(L, false);
			}
		});
		Utils.setJavaFunctionAsField(L, "setListener", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return setListener(L, false);
			}
		});
		Utils.setJavaFunctionAsField(L, "removeListener", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return removeListener(L);
			}
		});
		L.setField(-2, "invitations");
	}

	//region Lua functions
	// plugin.gpgs.v2.multiplayer.invitations.load(params)
	// params.mostRecentFirst
	// params.listener
	private int load(LuaState L) {
		Utils.debugLog("multiplayer.invitations.load()");
		final String name = "load";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
				.bool("mostRecentFirst")
				.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			Boolean mostRecentFirst = params.getBoolean("mostRecentFirst", false);
			final Integer luaListener = params.getListener("listener");
			Multiplayer.InvitationsClient().
					loadInvitations(mostRecentFirst ? SORT_ORDER_MOST_RECENT_FIRST : SORT_ORDER_SOCIAL_AGGREGATION).
					addOnCompleteListener(new OnCompleteListener<AnnotatedData<InvitationBuffer>>() {
						@Override
						public void onComplete(@NonNull Task<AnnotatedData<InvitationBuffer>> task) {
							Hashtable<Object, Object> event = Utils.newEvent(name);
							boolean isError = !task.isSuccessful();
							event.put("isError", isError);
							if (isError) {
								event.put("errorCode", Utils.getErrorCode(task.getException()));
								event.put("errorMessage", task.getException().getLocalizedMessage());
							} else {
								Hashtable<Object, Object> invitations = new Hashtable<>();

								int i = 1;
								InvitationBuffer invitationBuffer = task.getResult().get();
								for (Invitation invitation : invitationBuffer) {
									invitations.put(i++, Utils.invitationToHashtable(invitation));
								}
								invitationBuffer.release();

								event.put("invitations", invitations);
							}
							Utils.dispatchEvent(luaListener, event, true);
						}
					});
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.invitations.decline(invitationId)
	private int decline(LuaState L) {
		Utils.debugLog("multiplayer.invitations.decline()");
		if (!L.isString(1)){
			Utils.errorLog("decline must receive string parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			String invitationId = L.checkString(1);
			Multiplayer.RealTimeClient().declineInvitation(invitationId);
			Multiplayer.TurnBasedClient().declineInvitation(invitationId);
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.invitations.dismiss(invitationId)
	private int dismiss(LuaState L) {
		Utils.debugLog("multiplayer.invitations.dismiss()");
		if (!L.isString(1)){
			Utils.errorLog("dismiss must receive string parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			String invitationId = L.checkString(1);
			Multiplayer.RealTimeClient().dismissInvitation(invitationId);
			Multiplayer.TurnBasedClient().dismissInvitation(invitationId);
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.invitations.show(listener)
	int show(LuaState L, final boolean isLegacy) {
		Utils.debugLog("multiplayer.invitations.show()");
		final String name = "show";
		int luaListener;
		int initListenerIndex = 1;
		if (CoronaLua.isListener(L, initListenerIndex, name)) {
			luaListener = CoronaLua.newRef(L, initListenerIndex);
		}
		else {
			Utils.errorLog(name + " must receive listener parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Task<Intent> intent = Multiplayer.InvitationsClient().getInvitationInboxIntent();
			final int fLuaListener = luaListener;
			final CoronaActivity.OnActivityResultHandler resultHandler = new CoronaActivity.OnActivityResultHandler() {
				@Override
				public void onHandleActivityResult(CoronaActivity activity, int requestCode, int resultCode, Intent intent) {
					activity.unregisterActivityResultHandler(this);
					Hashtable<Object, Object> event = Utils.newEvent(name);

					if (!isLegacy) {
						boolean isError = resultCode != Activity.RESULT_OK;
						event.put("isError", isError);
						if (isError) {
							event.put("errorCode", resultCode);
							event.put("errorMessage", Utils.resultCodeToString(resultCode));
						}
						if (intent != null) {
							if (intent.hasExtra(EXTRA_INVITATION)) {
								Invitation invitation = intent.getParcelableExtra(EXTRA_INVITATION);
								event.put("invitation", Utils.invitationToHashtable(invitation));
							} else if (intent.hasExtra(EXTRA_TURN_BASED_MATCH)) {
								com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch match = intent.getParcelableExtra(EXTRA_TURN_BASED_MATCH);
								MultiplayerTurnbased.matches.put(match.getMatchId(), match);
								event.put("matchId", match.getMatchId());
							}
						}
					} else {
						event.put("type", "invitations");
						Hashtable<Object, Object> data = new Hashtable<>();
						boolean isError = (resultCode != Activity.RESULT_OK) && (resultCode != Activity.RESULT_CANCELED);
						data.put("isError", isError);
						boolean isCancelled = resultCode == Activity.RESULT_CANCELED;
						data.put("phase", isCancelled ? "cancelled" : "selected");
						if (!isError && !isCancelled && intent != null && intent.hasExtra(EXTRA_INVITATION)) {
							Invitation invitation = intent.getParcelableExtra(EXTRA_INVITATION);
							data.put("roomID", invitation.getInvitationId()); // It is in fact invitation id, not room id, same mechanics in the old plugin.
						}
						event.put("data", data);
					}

					Utils.dispatchEvent(fLuaListener, event, true);
				}
			};
			intent.addOnCompleteListener(new OnCompleteListener<Intent>() {
				@Override
				public void onComplete(@NonNull Task<Intent> task) {
					if (task.isSuccessful())
						Utils.startActivity(task.getResult(), resultHandler);
				}
			});
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.invitations.setListener(listener)
	private int setListener(LuaState L, final boolean isLegacy) {
		Utils.debugLog("multiplayer.invitations.setListener()");
		final String name = "invitation";
		int luaListener;
		int initListenerIndex = 1;
		if (CoronaLua.isListener(L, initListenerIndex, name)) {
			luaListener = CoronaLua.newRef(L, initListenerIndex);
		}
		else {
			Utils.errorLog(name + " must receive listener parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			final int fLuaListener = luaListener;
			callback = new InvitationCallback() {
				public void onInvitationReceived(Invitation invitation) {
					Hashtable<Object, Object> event = Utils.newEvent(name);
					if (!isLegacy) {
						event.put("isError", false);
						event.put("phase", "received");
						event.put("invitation", Utils.invitationToHashtable(invitation));
					} else {
						event.put("type", "setInvitationReceivedListener");
						Hashtable<Object, Object> data = new Hashtable<>();
						data.put("roomID", invitation.getInvitationId());
						data.put("alias", invitation.getInviter().getDisplayName());
						data.put("playerID", invitation.getInviter().getPlayer().getPlayerId());
						event.put("data", data);
					}
					Utils.dispatchEvent(fLuaListener, event);
				}

				public void onInvitationRemoved(String invitationId) {
					if (!isLegacy) {
						Hashtable<Object, Object> event = Utils.newEvent(name);
						event.put("isError", false);
						event.put("phase", "removed");
						event.put("invitationId", invitationId);
						Utils.dispatchEvent(fLuaListener, event);
					}
				}
			};
			Multiplayer.InvitationsClient().registerInvitationCallback(callback);
		}
		return 0;
	}

	// plugin.gpgs.v2.multiplayer.invitations.removeListener()
	private int removeListener(LuaState L) {
		Utils.debugLog("multiplayer.invitations.removeListener()");
		if (Utils.checkConnection()) {
			Multiplayer.InvitationsClient().unregisterInvitationCallback(callback);
		}
		return 0;
	}
	//endregion

	//region Legacy compatibility
	private String[] actions = new String[]{"setInvitationReceivedListener"};

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
		if (action.equals("setInvitationReceivedListener")) {
			L.getField(1, "listener");
			L.remove(1);
			return setListener(L, true);
		}
		return 0;
	}
	//endregion
}
