/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.ucla.discoverfriend;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import edu.ucla.discoverfriend.DeviceListFragment.DeviceActionListener;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener, GroupInfoListener {

	protected static final int CHOOSE_FILE_RESULT_CODE = 20;
	private static final int SOCKET_TIMEOUT = 5000;
	private View mContentView = null;
	private WifiP2pDevice device;
	private WifiP2pInfo info;
	ProgressDialog progressDialog = null;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		mContentView = inflater.inflate(R.layout.device_detail, null);
		mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				
				
				WifiP2pConfig config = new WifiP2pConfig();
				config.deviceAddress = device.deviceAddress;
				config.wps.setup = WpsInfo.PBC;
				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
				progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
						"Connecting to :" + device.deviceAddress, true, true
						//                        new DialogInterface.OnCancelListener() {
						//
						//                            @Override
						//                            public void onCancel(DialogInterface dialog) {
						//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
						//                            }
						//                        }
				);
				((DeviceActionListener) getActivity()).connect(config);

			}
		});

		mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						((DeviceActionListener) getActivity()).disconnect();
					}
				});

		mContentView.findViewById(R.id.btn_start_server).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// Server feature that prepares the CustomNetworkPacket
						// intent to send to the client.
						TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
						statusText.setText("Sending (to device): CNP");
						Log.d(MainActivity.TAG, "Intent----------- ");
						
						Intent serviceIntent = new Intent(getActivity(), DataTransferService.class);
						serviceIntent.setAction(DataTransferService.ACTION_SEND_DATA);
						serviceIntent.putExtra(DataTransferService.EXTRAS_DATA, ((MainActivity) getActivity()).getCnp());
						getActivity().startService(serviceIntent);
					}
				});

		return mContentView;
	}

	/**
	 * Unused.
	 */
	@Override
	public void onConnectionInfoAvailable(final WifiP2pInfo info) {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		this.info = info;
		this.getView().setVisibility(View.VISIBLE);

		// The owner IP is now known.
		TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
		view.setText(getResources().getString(R.string.group_owner_text)
				+ ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
						: getResources().getString(R.string.no)));

		// InetAddress from WifiP2pInfo struct.
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText("CONNECTION INFO: Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

		if (info.groupFormed && info.isGroupOwner) {
			mContentView.findViewById(R.id.btn_start_server).setVisibility(View.VISIBLE);
			((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources().getString(R.string.server_text));
		}
		else if (info.groupFormed) {
			FacebookFragment fragment = (FacebookFragment) getFragmentManager().findFragmentById(R.id.frag_facebook);
			new ClientAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text), info.groupOwnerAddress.getHostAddress(), fragment.getUid()).execute();
		}

		// Hide the connect button.
		mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
	}
	

	/**
	 * Note: Legacy devices connected through WiFi do not enter through this
	 * flow. Although the peer list includes them in group.getClientList(),
	 * the PeerListListener only reflects changes in connections made by
	 * WiFi Direct compatible devices.
	 */
	@Override
	public void onGroupInfoAvailable(WifiP2pGroup group) {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		this.getView().setVisibility(View.VISIBLE);
		
		// The owner IP is now known.
		TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
		view.setText(getResources().getString(R.string.group_owner_text)
				+ ((group.isGroupOwner() == true) ? getResources().getString(R.string.yes)
						: getResources().getString(R.string.no)));
		
		// InetAddress from WifiP2pInfo struct.
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText("Group Owner IP - " + group.getOwner().deviceAddress);
		
		if (group.isGroupOwner()) {
			// Provides a passphrase for legacy devices to use to connect to
			// this device, which serves as an AP.
			view = (TextView) mContentView.findViewById(R.id.group_passphrase);
			view.setText("GO Passphrase - " + group.getPassphrase());
		}
		
		// Group owner acts as server. Here, we enable the button corresponding
		// to sending the message containing two Bloom Filters and one
		// certificate to (one) client.
		if (group.isGroupOwner() && !group.getClientList().isEmpty()) {
			mContentView.findViewById(R.id.btn_start_server).setVisibility(View.VISIBLE);
			((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources().getString(R.string.server_text));
		}
		
		// Client prepares to accept the message passed by the server.
		else if (!group.isGroupOwner()) {
			((TextView) mContentView.findViewById(R.id.status_text)).setText("Attempting to retreive packet for " + SOCKET_TIMEOUT + " ms");
			FacebookFragment fragment = (FacebookFragment) getFragmentManager().findFragmentById(R.id.frag_facebook);
			new ClientAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text), group.getOwner().deviceAddress, fragment.getUid()).execute();
		}
		
		else {
			((TextView) mContentView.findViewById(R.id.status_text)).setText("No other devices connected.");
		}

		// Hide the connect button.
		mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
		
	}

	/**
	 * Updates the UI with device data
	 * 
	 * @param device the device to be displayed
	 */
	public void showDetails(WifiP2pDevice device) {
		this.device = device;
		this.getView().setVisibility(View.VISIBLE);
		TextView view = (TextView) mContentView.findViewById(R.id.device_address);
		view.setText(device.deviceAddress);
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText(device.toString());

	}

	/**
	 * Clears the UI fields after a disconnect or direct mode disable operation.
	 */
	public void resetViews() {
		mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
		TextView view = (TextView) mContentView.findViewById(R.id.device_address);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.group_owner);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.status_text);
		view.setText(R.string.empty);
		mContentView.findViewById(R.id.btn_start_server).setVisibility(View.GONE);
		this.getView().setVisibility(View.GONE);
	}
	
	/**
	 * A simple client socket that accepts connection and writes some data on
	 * the stream.
	 */
	public static class ClientAsyncTask extends AsyncTask<Void, Void, CustomNetworkPacket> {
		
		private Context context;
		private TextView statusText;
		private String host;
		private String uid;

		/**
		 * @param context
		 * @param statusText
		 */
		public ClientAsyncTask(Context context, View statusText, String host, String uid) {
			this.context = context;
			this.statusText = (TextView) statusText;
			this.host = host;
			this.uid = uid;
		}

		@Override
		protected CustomNetworkPacket doInBackground(Void... params) {
			Socket socket = new Socket();
			int port = 8988;

			try {
				Log.d(MainActivity.TAG, "Opening client socket - ");
				socket.bind(null);
				socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

				Log.d(MainActivity.TAG, "Client socket - " + socket.isConnected());
				
				InputStream inputstream = socket.getInputStream();
				ObjectInputStream ois = new ObjectInputStream(inputstream);
				CustomNetworkPacket cnp = (CustomNetworkPacket) ois.readObject();

				return cnp;
			} catch (IOException e) {
                Log.e(MainActivity.TAG, e.getMessage());
                return null;
            } catch (ClassNotFoundException e) {
            	Log.e(MainActivity.TAG, e.getMessage());
                return null;
			} finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }
		}

		/*
		 * (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(CustomNetworkPacket result) {
			if (result != null) {
				statusText.setText(result.getCf());
				
				// Check if current user is social network friends with sender
				if (result.getBf().mightContain(uid)) {
					// Prompt user to act accordingly.
				}
				else {
					// Ignore and don't respond.
				}
			}

		}

		/*
		 * (non-Javadoc)
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			statusText.setText("Opening a client socket");
		}

	}


}