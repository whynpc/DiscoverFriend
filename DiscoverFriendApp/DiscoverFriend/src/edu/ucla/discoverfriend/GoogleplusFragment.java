package edu.ucla.discoverfriend;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

public class GoogleplusFragment extends Fragment implements
		ConnectionCallbacks, OnConnectionFailedListener, OnClickListener {

	private static final int RC_SIGN_IN = 0;

	private GoogleApiClient mGoogleApiClient;

	private View mContentView;

	private SignInButton signinButton;

	private boolean mSignInClicked;
	private boolean mIntentInProgress;
	private ConnectionResult mConnectionResult;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		inflater.inflate(R.layout.googleplus, null);
		mContentView = inflater.inflate(R.layout.googleplus, null);

		signinButton = (SignInButton) mContentView.findViewById(R.id.sign_in_button);
		signinButton.setOnClickListener(this);

		mGoogleApiClient = new GoogleApiClient.Builder(
				mContentView.getContext()).addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this).addApi(Plus.API, null)
				.addScope(Plus.SCOPE_PLUS_LOGIN).build();

		return mContentView;
	}

	@Override
	public void onConnectionSuspended(int cause) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStart() {
		super.onStart();
		mGoogleApiClient.connect();
		Log.d("discfr", "googleapiclient connected");
	}

	@Override
	public void onStop() {
		super.onStop();

		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
	}

	public void onActivityResult(int requestCode, int responseCode,
			Intent intent) {
		if (requestCode == RC_SIGN_IN) {
			if (responseCode != Activity.RESULT_OK) {
				mSignInClicked = false;
			}

			mIntentInProgress = false;

			if (!mGoogleApiClient.isConnecting()) {
				mGoogleApiClient.connect();
			}
		}
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		mSignInClicked = false;
		Toast.makeText(mContentView.getContext(), "User is connected!",
				Toast.LENGTH_LONG).show();
	}

	private void resolveSignInError() {
		if (mConnectionResult.hasResolution()) {
			
		}
	}

	public void onConnectionFailed(ConnectionResult result) {
		if (!mIntentInProgress) {
			// Store the ConnectionResult so that we can use it later when the
			// user clicks
			// 'sign-in'.
			mConnectionResult = result;

			if (mSignInClicked) {
				// The user has already clicked 'sign-in' so we attempt to
				// resolve all
				// errors until the user is signed in, or they cancel.
				resolveSignInError();
			}
		}
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.sign_in_button
				&& !mGoogleApiClient.isConnecting()) {
			mSignInClicked = true;
			//resolveSignInErrors();
		}

	}

}
