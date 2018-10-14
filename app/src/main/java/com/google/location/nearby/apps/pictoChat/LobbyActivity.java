package com.google.location.nearby.apps.pictoChat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.nearby.Nearby;

import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;

import com.google.android.gms.nearby.connection.Strategy;

public class LobbyActivity extends AppCompatActivity {

    //Permissions are determined here - should be done
    //Have a connector that will check if there is a host already - need to test
    //start intent whether or not user is a hosting
    //Enter Username? - done

    private static final String TAG = "PictoChat";
    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    public boolean lobby = true;
    //

    private static final String[] REQUIRED_PERMISSIONS =
            new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    private ConnectionsClient connectionsClient;

    public int Broadcasts = 0;
    private String username;

    //public TextView BroadcastNum;
    private TextView Prompt;
    private Button HostBtn;
    private Button ClientBtn;
    private Button UserAccepted;
    private EditText UserInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainlobby); //Start the lobby activity
        /*
        TextView title = (TextView) findViewById(R.id.Title);
        Typeface typeface = Typeface.createFromAsset(getAssets(),"fonts/Pictochat.tff");
        title.setTypeface(typeface);
        */
        connectionsClient = Nearby.getConnectionsClient(this);
        //startDiscovery(); //begin checking if there are any broadcasts
        initiation(); //begin initializing all items in layout
    }



    ////////////////////////////* Connection Handling Functions */////////////////////////////////

    /////////////////////////////*Permission handling functions *//////////////////////////////////

    /** Returns true if the app was granted all the permissions. Otherwise, returns false. */
    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;

    }
    /** Handles user acceptance (or denial) of our permission request. */
    @CallSuper
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        recreate();
    }

    ///////////////////////////*life cycle functions *//////////////////////////////////////////
    @Override
    protected void onStart() {
        super.onStart();

        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }
    }

    ///////////////////////////* Private App Functions *////////////////////////////////////////


    private void initiation(){
        //BroadcastNum = findViewById(R.id.Broadcasts);
        //BroadcastNum.setText(getString(R.string.Advertising,Broadcasts));

        HostBtn = findViewById(R.id.hostStart);
        ClientBtn = findViewById(R.id.clientStart);
        HostBtn.setVisibility(View.INVISIBLE);  //hide the buttons
        ClientBtn.setVisibility(View.INVISIBLE); //hide the buttons
        HostBtn.setEnabled(false);
        ClientBtn.setEnabled(false);

        UserAccepted = findViewById(R.id.accept);
        Prompt = findViewById(R.id.Prompt1);
        UserInput = findViewById(R.id.username);


    }

    public void UserInputted(View view){
        HostBtn.setVisibility(View.VISIBLE);  //show the buttons
        ClientBtn.setVisibility(View.VISIBLE); //show the buttons
        HostBtn.setEnabled(true);
        ClientBtn.setEnabled(true);
        //
        Prompt.setText(R.string.Prompt2);
        //
        UserAccepted.setVisibility(View.INVISIBLE);
        UserAccepted.setEnabled(false);
        UserInput.setVisibility(View.INVISIBLE);
        //
        username = UserInput.getText().toString();
        if (username.isEmpty()){
            //Toast.makeText(getApplicationContext(),"Random Generating Username",Toast.LENGTH_SHORT).show();
            username = CodenameGenerator.generate();
        }
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    protected void onPause() {
        connectionsClient.stopDiscovery();
        Toast.makeText(getApplicationContext(),"Stop Discovery",Toast.LENGTH_SHORT).show();
        Broadcasts = 0;
        super.onPause();
    }

    @Override
    protected void onResume() {
        //startDiscovery();
        super.onResume();
    }

    public void ClientStart(View view){
        Intent intent = new Intent(LobbyActivity.this, MainActivity.class);
        intent.putExtra("amHost", false);
        intent.putExtra("username",username);
        startActivity(intent);
    }

    public void HostStart(View view){
        Intent intent = new Intent(LobbyActivity.this, MainActivity.class);
        intent.putExtra("amHost",true);
        intent.putExtra("username",username);
        startActivity(intent);
    }

}
