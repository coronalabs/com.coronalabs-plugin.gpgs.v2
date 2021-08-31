package plugin.gpgs.v2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import android.view.View;
import android.widget.RelativeLayout;

import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaActivity.OnActivityResultHandler;
import com.ansca.corona.CoronaEnvironment;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.drive.DriveScopes;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

class Connector implements OnActivityResultHandler {
	interface SignInListener {
		enum SignInResult {
			SUCCESS,
			FAILED,
			CANCELED
		}
		void onSignIn(SignInResult result);
		void onSignIn(SignInResult result, Integer errorCode, String errorMessage);
		void onSignOut();
	}

	private SignInListener signInListener;
	private GoogleSignInOptions _signInOptions;
	private GoogleSignInClient _signInClient;
	private GoogleSignInClient getSignInClient() {
		if(_signInOptions==null) {
			_signInOptions = new GoogleSignInOptions.
					Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).
					requestScopes(new Scope(DriveScopes.DRIVE_FILE)).
					requestScopes(new Scope(DriveScopes.DRIVE_APPDATA)).
					requestScopes(Games.SCOPE_GAMES_LITE).
					build();
		}
		if(_signInClient==null) {
			_signInClient = GoogleSignIn.getClient(getContext(), _signInOptions);
		}
		return _signInClient;
	}

	private int expectedRequestCode; // Request code we use when invoking other Activities to complete the sign-in flow
	private boolean isConnecting = false;

	private static final Connector instance = new Connector(); // Singleton
	private Connector() {}
	static Connector getInstance() {
		return instance;
	}

	static boolean isConnected() {
		return getContext() != null && getSignInAccount() != null;
	}

	static boolean isAuthenticated() {
		return isConnected() && getSignInAccount() != null;
	}

	Connector connector = this;
    static Context getContext() {
        return CoronaEnvironment.getApplicationContext();
    }

    static GoogleSignInAccount getSignInAccount() {
        return GoogleSignIn.getLastSignedInAccount(getContext());
    }

    void setContext (final CoronaActivity activity, SignInListener listener) {
        signInListener = listener;
		expectedRequestCode = activity.registerActivityResultHandler(this);

//		// This view has to be added, otherwise GoogleApiClient won't find a view to show popups
//		final View popupView = new View(activity);
//		activity.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if( activity.getRuntimeTaskDispatcher().isRuntimeAvailable() ) {
//                    RelativeLayout relativeLayout = new RelativeLayout(activity);
//                    relativeLayout.addView(popupView);
//                    activity.getOverlayView().addView(relativeLayout);
//                }
//            }
//        });
    }


	 String getServerAuthCode(String serverId) {
    	_signInOptions = new GoogleSignInOptions.
				Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).
				requestServerAuthCode(serverId).
				requestScopes(new Scope(DriveScopes.DRIVE_APPDATA)).
				requestScopes(new Scope(DriveScopes.DRIVE_FILE)).
				requestScopes(Games.SCOPE_GAMES_LITE).
				requestEmail().
				build();
		_signInClient = null;

		return isConnected() ? getSignInAccount().getServerAuthCode() : "";
	}

	void signIn(boolean userInitiated) {
		if (isConnected()) {
			signInListener.onSignIn(SignInListener.SignInResult.SUCCESS);
		} else if (!isConnecting) { // We don't have a pending connection resolution, so start a new one
			connect(userInitiated);
		}
	}

	void signOut() {
		if (isConnected()) {
			getSignInClient().signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
				@Override
				public void onComplete(@NonNull Task<Void> task) {
					signInListener.onSignOut();
				}
			});
		} else {
			signInListener.onSignOut();
		}
	}

	private void connect(boolean userInitiated) {
		isConnecting = true;
		if(userInitiated) {
			Activity activity = CoronaEnvironment.getCoronaActivity();
            if(activity!=null) {
                activity.startActivityForResult(getSignInClient().getSignInIntent(), expectedRequestCode);
            }
		} else {
			getSignInClient().silentSignIn().addOnCompleteListener(new OnCompleteListener<GoogleSignInAccount>() {
				@Override
				public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
					isConnecting = false;
					if (task.isSuccessful()) {
						signInListener.onSignIn(SignInListener.SignInResult.SUCCESS);
					} else {
						signInListener.onSignIn(SignInListener.SignInResult.FAILED);
					}
				}
			});
		}
	}

	private void disconnect() {
		isConnecting = false;
		if (isConnected()) {
			getSignInClient().signOut();
		}
	}


	@Override
	public void onHandleActivityResult(CoronaActivity activity, int requestCode, int resultCode, Intent intent) {
		if (requestCode != expectedRequestCode) { // Request code was not meant for us
			return;
		}
		Utils.debugLog("Activity Result code:" + Utils.resultCodeToString(resultCode));
		isConnecting = false;
		// We're coming back from an activity that was launched to resolve a connection problem. For example, a sign-in UI.
		switch (resultCode) {
			case Activity.RESULT_OK:
				signInListener.onSignIn(SignInListener.SignInResult.SUCCESS);
				break;
			case GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED:
				signInListener.onSignIn(SignInListener.SignInResult.FAILED, null, "Reconnect Required");
				break;
			case Activity.RESULT_CANCELED:
				disconnect();
				signInListener.onSignIn(SignInListener.SignInResult.CANCELED);
				break;
			case GamesActivityResultCodes.RESULT_APP_MISCONFIGURED:
				disconnect();
				signInListener.onSignIn(SignInListener.SignInResult.FAILED, null, "Bad Configuration");
				Utils.errorLog("This application is misconfigured!");
				break;
			default:
				disconnect();
				signInListener.onSignIn(SignInListener.SignInResult.FAILED, resultCode, Utils.resultCodeToString(resultCode));
				break;
		}
	}
	//endregion
}
