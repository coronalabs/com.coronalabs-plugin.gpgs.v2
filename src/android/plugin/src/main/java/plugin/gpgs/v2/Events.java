package plugin.gpgs.v2;

import android.net.Uri;
import androidx.annotation.NonNull;

import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.EventsClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.event.Event;
import com.google.android.gms.games.event.EventBuffer;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;

import java.util.ArrayList;
import java.util.Hashtable;

import plugin.gpgs.v2.LuaUtils.Scheme;
import plugin.gpgs.v2.LuaUtils.Table;

class Events {

	Events(LuaState L) {
		L.newTable(); // plugin.gpgs.v2.events
		Utils.setJavaFunctionAsField(L, "load", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return load(L);
			}
		});
		Utils.setJavaFunctionAsField(L, "increment", new JavaFunction() {
			@Override
			public int invoke(LuaState L) {
				return increment(L);
			}
		});
		L.setField(-2, "events");
	}

	/**
	 * Returns EventsClient instance
	 */
	private EventsClient getClient(){
		return PlayGames.getEventsClient(Connector.getActivity());
	}

	//region Lua functions
	// plugin.gpgs.v2.events.load(params)
	// params.eventId
	// params.eventIds
	// params.reload
	// params.listener
	private int load(LuaState L) {
		Utils.debugLog("events.load()");
		final String name = "load";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
				.string("eventId")
				.table("eventIds")
				.string("eventIds.#")
				.bool("reload")
				.listener("listener", name);

			Table params = new Table(L, 1).parse(scheme);
			String eventId = params.getString("eventId");
			Hashtable<Object, Object> eventIds = params.getTable("eventIds");
			Boolean reload = params.getBoolean("reload", false);
			final Integer luaListener = params.getListener("listener");
			ArrayList<String> eventIdArray = null;
			if ((eventIds != null) && (eventIds.values().size() > 0)) {
				eventIdArray = new ArrayList<>();
				for (Object o : eventIds.values()) {
					eventIdArray.add((String) o);
				}
			} else if (eventId != null) {
				eventIdArray = new ArrayList<>();
				eventIdArray.add(eventId);
			}
			Task<AnnotatedData<EventBuffer>> events;
			if (eventIdArray == null) {
				events = getClient().load(reload);
			} else {
				events = getClient().loadByIds(reload, eventIdArray.toArray(new String[eventIdArray.size()]));
			}
			events.addOnCompleteListener(new OnCompleteListener<AnnotatedData<EventBuffer>>() {
				@Override
				public void onComplete(@NonNull Task<AnnotatedData<EventBuffer>> task) {
					try {
						Hashtable<Object, Object> event = Utils.newEvent(name);
						boolean isError = !task.isSuccessful();
						event.put("isError", isError);
						if (isError) {
							event.put("errorCode", Utils.getErrorCode(task.getException()));
							event.put("errorMessage", task.getException().getLocalizedMessage());
						} else {
							Hashtable<Object, Object> events = new Hashtable<>();

							int i = 1;
							EventBuffer eventBuffer = task.getResult().get();
							if (eventBuffer != null) {
								for (Event e : eventBuffer) {
									Hashtable<Object, Object> events_event = new Hashtable<>();
									Uri iconUri = e.getIconImageUri();
									Utils.put(events_event, "description", e.getDescription());
									events_event.put("id", e.getEventId());
									Utils.put(events_event, "formattedSteps", e.getFormattedValue());
									if (iconUri != null) {
										events_event.put("imageUri", iconUri.toString());
									} else {
										events_event.put("imageUri", "");
									}
									events_event.put("name", e.getName());
									events_event.put("player", Utils.playerToHashtable(e.getPlayer()));
									events_event.put("steps", e.getValue());
									events_event.put("isVisible", e.isVisible());
									events.put(i++, events_event);
								}
							}
							eventBuffer.release();

							event.put("events", events);
						}
						Utils.dispatchEvent(luaListener, event, true);
					} catch(Throwable ignore) {}
				}
			});
		}
		return 0;
	}

	// plugin.gpgs.v2.events.increment(params)
	// params.eventId *
	// params.steps
	private int increment(LuaState L) {
		Utils.debugLog("events.increment()");
		final String name = "increment";
		if (!L.isTable(1)){
			Utils.errorLog(name + " must receive table parameter, got "+ L.typeName(1));
			return 0;
		}
		if (Utils.checkConnection()) {
			Scheme scheme = new Scheme()
				.string("eventId")
				.number("steps");

			Table params = new Table(L, 1).parse(scheme);
			String eventId = params.getStringNotNull("eventId");
			int steps = params.getLong("steps", 1).intValue();
			getClient().increment(eventId, steps);
		}
		return 0;
	}
	//endregion
}
