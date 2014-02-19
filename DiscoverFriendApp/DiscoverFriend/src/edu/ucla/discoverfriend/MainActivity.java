package edu.ucla.discoverfriend;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

public class MainActivity extends FragmentActivity {

	private static final String TAG = "MainActivity";

	private Handler mUpdateHandler;

	NsdHelper mNsdHelper;
	Connection mConnection;
	private MainFragment mainFragment;

	private Button discoverServicesButton;
	private TextView textView1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState == null) {
			// Add the fragment on initial activity setup
			mainFragment = new MainFragment();
			getSupportFragmentManager().beginTransaction().add(android.R.id.content, mainFragment).commit();
		} else {
			// Or set the fragment from restored state info
			mainFragment = (MainFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
		}

		try {
			PackageInfo info = getPackageManager().getPackageInfo(
					"com.facebook.samples.loginhowto", 
					PackageManager.GET_SIGNATURES);
			for (Signature signature : info.signatures) {
				MessageDigest md = MessageDigest.getInstance("SHA");
				md.update(signature.toByteArray());
				Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
			}
		} catch (NameNotFoundException e) {

		} catch (NoSuchAlgorithmException e) {

		}

		setContentView(R.layout.activity_main);

		textView1 = (TextView) findViewById(R.id.textView1);


		Session.openActiveSession(this, true, new Session.StatusCallback() {

			// callback when session changes state
			@Override
			public void call(Session session, SessionState state, Exception exception) {
				if (session.isOpened()) {

					// make request to the /me API
					Request.newMeRequest(session, new Request.GraphUserCallback() {

						// callback after Graph API response with user object
						@Override
						public void onCompleted(GraphUser user, Response response) {
							if (user != null) {
								textView1.setText(user.getName());
							}
						}
					}).executeAsync();
				}
			}
		});

		mUpdateHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				//String chatLine = msg.getData().getString("msg");
				//addChatLine(chatLine);
			}
		};

		mConnection = new Connection(mUpdateHandler);

		mNsdHelper = new NsdHelper(this);
		mNsdHelper.initializeNsd();

		discoverServicesButton = (Button) findViewById(R.id.discoverServices);
		discoverServicesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//clickDiscover(v);
			}
		});

	}

	public void clickAdvertise(View v) {
		// Register service
		if(mConnection.getLocalPort() > -1) {
			mNsdHelper.registerService(mConnection.getLocalPort());
		} else {
			Log.d(TAG, "ServerSocket isn't bound.");
		}
	}

	public void clickDiscover(View v) {
		mNsdHelper.discoverServices();
	}

	public void clickConnect(View v) {
		NsdServiceInfo service = mNsdHelper.getChosenServiceInfo();
		if (service != null) {
			Log.d(TAG, "Connecting.");
			mConnection.connectToServer(service.getHost(),
					service.getPort());
		} else {
			Log.d(TAG, "No service to connect to!");
		}
	}

	public void clickSend(View v) {
		/*EditText messageView = (EditText) this.findViewById(R.id.chatInput);
		if (messageView != null) {
			String messageString = messageView.getText().toString();
			if (!messageString.isEmpty()) {
				mConnection.sendMessage(messageString);
			}
			messageView.setText("");
		}*/
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		return true;
	}

	@Override
	protected void onPause() {
		if (mNsdHelper != null) {
			mNsdHelper.tearDown();
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mNsdHelper != null) {
			mNsdHelper.registerService(mConnection.getLocalPort());
			mNsdHelper.discoverServices();
		}
	}

	@Override
	protected void onDestroy() {
		mNsdHelper.tearDown();
		mConnection.tearDown();
		super.onDestroy();
	}

}
