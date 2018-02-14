package com.tutsplus.nearbyconnections2;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import static android.content.ContentValues.TAG;

public class MainActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String SERVICE_ID = "UNIQUE_SERVICE_ID";

    private GoogleApiClient mGoogleApiClient;
    private String mEndpoint;

    private ArrayAdapter<String> mAdapter;
    private EditText mEditText;
    private ListView mListView;

    private PayloadCallback mPayloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String s, Payload payload) {
            addText(new String(payload.asBytes()));
        }

        @Override
        public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {}
    };

    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Nearby.Connections.acceptConnection(mGoogleApiClient, endpointId, mPayloadCallback);
                    mEndpoint = endpointId;
                    Nearby.Connections.stopDiscovery(mGoogleApiClient);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {}

                @Override
                public void onDisconnected(String endpointId) {}
            };

    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(
                        String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
                    if( discoveredEndpointInfo.getServiceId().equalsIgnoreCase(SERVICE_ID)) {
                        Nearby.Connections.requestConnection(
                                mGoogleApiClient,
                                "Droid",
                                endpointId,
                                mConnectionLifecycleCallback);
                    }

                }

                @Override
                public void onEndpointLost(String endpointId) {
                    addText("Disconnected");
                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        mGoogleApiClient = new GoogleApiClient.Builder(this, this, this)
                .addApi(Nearby.CONNECTIONS_API)
                .enableAutoManage(this, this)
                .build();
    }

    private void initViews() {
        mEditText = (EditText) findViewById(R.id.edittext);
        mListView = (ListView) findViewById(R.id.list);
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1);
        mListView.setAdapter(mAdapter);
    }

    private void startDiscovery() {
        Nearby.Connections.startDiscovery(
                mGoogleApiClient,
                SERVICE_ID,
                mEndpointDiscoveryCallback,
                new DiscoveryOptions(Strategy.P2P_STAR));
    }

    public void sendPayload(View v) {
        String text = mEditText.getText().toString();
        addText(text);
        mEditText.setText("");
        Nearby.Connections.sendPayload(mGoogleApiClient, mEndpoint, Payload.fromBytes(text.getBytes()));
    }

    private void addText(String text) {
        mAdapter.add(text);
        mAdapter.notifyDataSetChanged();
        mListView.post(new Runnable() {
            @Override
            public void run() {
                mListView.setSelection(mListView.getCount() - 1);
            }
        });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startDiscovery();
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Nearby.Connections.disconnectFromEndpoint(mGoogleApiClient, mEndpoint);
    }
}