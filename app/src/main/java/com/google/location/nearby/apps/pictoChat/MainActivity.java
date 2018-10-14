package com.google.location.nearby.apps.pictoChat;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate.Status;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.nearby.messages.MessagesClient;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = "PictoChat";
  private static final Strategy STRATEGY = Strategy.P2P_STAR;


  // Our handle to Nearby Connections
  private ConnectionsClient connectionsClient;
  //

  private String opponentEndpointId;
  private String opponentName;
  private String Message;
  private String Tag;

  private Button disconnectButton;
  private Button findButton;
  private Button SendBtn;
  private Button MsgBtn;
  private Button DrawBtn;
  private Button eraserbtn;
  private Button brushbtn;
  private Button clearBtn;


  private TextView Name;
  private String codeName;
  private TextView opponentText;
  private TextView HostStatus;
  private EditText UserMsg;
  private DrawingView Drawing;
  private SeekBar BrushSize;


  private ListView MessageList;
  private ArrayList<item> MsgArray;
  ArrayAdapter Adapter;
  private ProgressBar loading;

  private boolean AmHost;
  private boolean MsgDraw; //false = message mode, true = draw mode
  private boolean Connected;
  private boolean Primed; //false = don't know msg type, true = ready
  private boolean receievedType; //false = message, true = drawing

  HashMap<String,String> map = new HashMap<>();

  //////////////////////////////////////ON CREATE///////////////////////////////////////////////////
  @Override
  protected void onCreate(@Nullable Bundle bundle) {
    super.onCreate(bundle);
    setContentView(R.layout.activity_main);
    AllViews();

    // Intent Handling
    Intent intent = getIntent();
    codeName = intent.getStringExtra("username");
    AmHost = intent.getBooleanExtra("amHost",false);
    Name.setText(codeName);

    // Listview handling
    MsgArray= new ArrayList<item>();
    Adapter = new MyAdapter(this,MsgArray);
    MessageList.setAdapter(Adapter);
    //
    MessageMode(View.INVISIBLE);
    initialViews();

    connectionsClient = Nearby.getConnectionsClient(this);

    if(AmHost){
      HostStatus.setVisibility(View.VISIBLE);
      HostStatus.setText(R.string.Hosting);
      startAdvertising();
      findButton.setVisibility(View.INVISIBLE);
      findButton.setEnabled(false);
    }
    else{
      HostStatus.setVisibility(View.INVISIBLE);
      disconnectButton.setVisibility(View.INVISIBLE);
      disconnectButton.setEnabled(false);
    }
    resetGame();

    MsgDraw = false;
    Primed = false;

    BrushSize.setOnSeekBarChangeListener( new OnSeekBarChangeListener(){
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            Drawing.setEraserSize(progress);
            Drawing.setPenSize(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    });

  }
    //////////////////////////////////*Life Cycle Functions *//////////////////////////////////////
    @Override
    protected void onStop() {
        //connectionsClient.stopDiscovery();
        //connectionsClient.stopAllEndpoints();
        //resetGame();
        //Toast.makeText(getApplicationContext(),"OnStop",Toast.LENGTH_SHORT).show();
        Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        //Toast.makeText(getApplicationContext(),"OnPause",Toast.LENGTH_SHORT).show();
        super.onPause();
    }


    ///////////////////////////////*Connection Functions*///////////////////////////////////////////
    // Callbacks for receiving payloads
    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    String receivedMsg = new String(payload.asBytes(), UTF_8);
                    if(!Primed){
                        if(receivedMsg.equals("txt")){
                            Primed = true;
                            receievedType = false;
                            Log.i(TAG,"Primed for message");
                            return;
                        }
                        else{
                            //Toast.makeText(getApplicationContext(),"DRAWING",Toast.LENGTH_SHORT).show();
                            Primed = true;
                            receievedType = true;
                            Log.i(TAG, "Primed for Drawing");
                            return;
                        }
                    }
                    else{
                        if(!receievedType){
                            if(AmHost){
                                String name = map.get(endpointId);
                                String text = getString(R.string.message,name,receivedMsg);
                                ListUpdateString(text);
                                Log.i(TAG,"Host Sending to Client");
                                for (String values: map.keySet()) {
                                    Tag = "txt";
                                    connectionsClient.sendPayload(values,Payload.fromBytes(Tag.getBytes()));
                                    //connectionsClient.sendPayload(values,Payload.fromBytes("".getBytes()));
                                    connectionsClient.sendPayload(values, Payload.fromBytes(text.getBytes()));
                                }
                            }
                            else{
                                Log.i(TAG,"Client received message");
                                ListUpdateString(receivedMsg);
                            }
                        }
                        else{
                            byte[] receivedDrawing = payload.asBytes();
                            if(AmHost){
                                //String name = map.get(endpointId);
                                //ListUpdateString(name);
                                Log.i(TAG,"Host sending Picture");
                                ListUpdateDrawing(receivedDrawing);
                                for (String values: map.keySet()) {
                                    Tag = "drawing";
                                    connectionsClient.sendPayload(values,Payload.fromBytes(Tag.getBytes()));
                                    //connectionsClient.sendPayload(values,Payload.fromBytes("".getBytes()));
                                    connectionsClient.sendPayload(values, Payload.fromBytes(receivedDrawing));
                                }
                            }
                            else{
                                Log.i(TAG,"Client received drawing");
                                ListUpdateDrawing(receivedDrawing);
                            }
                        }
                        Primed = false;
                    }
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    //if (update.getStatus() == Status.SUCCESS && myChoice != null && opponentChoice != null) {
                    //finishRound();
                    //}
                }
            };


    // Callbacks for finding other devices
    private final EndpointDiscoveryCallback endpointDiscoveryCallback1 =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    Log.i(TAG, "onEndpointFound: endpoint found, connecting");
                    connectionsClient.requestConnection(codeName, endpointId, connectionLifecycleCallback1);
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    Log.i(TAG,"onEndpointFound: endpoint lost");
                    //connectionsClient.requestConnection(codeName, endpointId, connectionLifecycleCallback);
                }
            };

    // Callbacks for connections to other devices.
  private final ConnectionLifecycleCallback connectionLifecycleCallback1 =
          new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
              Log.i(TAG, "onConnectionInitiated: accepting connection");
              connectionsClient.acceptConnection(endpointId, payloadCallback);
              opponentName = connectionInfo.getEndpointName();
              //String connectedTo = getString(R.string.toastConnected,opponentName);
              map.put(endpointId,opponentName);
              //ListUpdateString(connectedTo);
              //for (String values: map.keySet()) {
                  //connectionsClient.sendPayload(values, Payload.fromBytes(connectedTo.getBytes()));
              //}
              //
              findButton.setVisibility(View.INVISIBLE);
              findButton.setEnabled(false);
              disconnectButton.setVisibility(View.VISIBLE);
              disconnectButton.setEnabled(true);

              // Update: Drawing
              UserMsg.setVisibility(View.VISIBLE);
              Drawing.setVisibility(View.INVISIBLE);
              MsgBtn.setVisibility(View.INVISIBLE);
              DrawBtn.setVisibility(View.VISIBLE);
              eraserbtn.setVisibility(View.INVISIBLE);
              loading.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onConnectionResult(String endpointId, ConnectionResolution result) {
              if (result.getStatus().isSuccess()) {
                Log.i(TAG, "onConnectionResult: connection successful");

                if(!AmHost){
                  connectionsClient.stopDiscovery();
                }
                opponentEndpointId = endpointId;
                setOpponentName(opponentName);
                Connected = true;

              } else {
                Log.i(TAG, "onConnectionResult: connection failed");
              }
              MessageMode(View.VISIBLE);
            }

            @Override
            public void onDisconnected(String endpointId) {
              Log.i(TAG, "onDisconnected: disconnected from the opponent");
              resetGame();
              if(!AmHost){
                  Toast.makeText(getApplicationContext(),"Disconnected from Host",Toast.LENGTH_SHORT).show();
                  finish();
              }
            }
          };

  /** Finds an opponent to play the game with using Nearby Connections. */

  /** Disconnects from the opponent and reset the UI. */
  public void disconnect(View view) {
    if(AmHost){
        connectionsClient.stopAllEndpoints();
        connectionsClient.stopDiscovery();
        finish();
    }
    else{
        connectionsClient.disconnectFromEndpoint(opponentEndpointId);
        connectionsClient.stopDiscovery();
        finish();
    }
  }

public void findPeople(View view){
      startDiscovery();
}


  /** Starts looking for other players using Nearby Connections. */
  private void startDiscovery() {
    // Note: Discovery may fail. To keep this demo simple, we don't handle failures.
      Log.i(TAG,"Starting discovery");
    connectionsClient.startDiscovery(
        getPackageName(), endpointDiscoveryCallback1, new DiscoveryOptions(STRATEGY));
      loading.setVisibility(View.VISIBLE);
      //add something here to set visibility of gif to yes
  }

  /** Broadcasts our presence using Nearby Connections so other players can find us. */
  private void startAdvertising() {
    connectionsClient.startAdvertising(
        codeName, getPackageName(), connectionLifecycleCallback1, new AdvertisingOptions(STRATEGY));
      Log.i(TAG, "starting Advertise");
      loading.setVisibility(View.VISIBLE);
      //add something here to set visibility of gif to yes

  }


///////////////////////////////////*Basic UI Handling Functions*///////////////////////////////////
  /** Wipes all game state and updates the UI accordingly. */
  private void resetGame() {
    opponentEndpointId = null;
    opponentName = null;
    setOpponentName(getString(R.string.no_opponent));
  }

  //SENDING MESSAGE FUNCTION
  public void SendMsg(View view){
      if(!MsgDraw){
          Message = UserMsg.getText().toString();
          UserMsg.setText("");
          Tag = "txt";
          if(AmHost){
              String text = getString(R.string.message,codeName,Message);
              for (String values: map.keySet()) {
                  connectionsClient.sendPayload(opponentEndpointId,Payload.fromBytes(Tag.getBytes()));
                  connectionsClient.sendPayload(values, Payload.fromBytes(text.getBytes()));
              }
              ListUpdateString(text);
              Log.i(TAG,"Host Send");
          }
          else{
              connectionsClient.sendPayload(opponentEndpointId,Payload.fromBytes(Tag.getBytes()));
              connectionsClient.sendPayload(opponentEndpointId, Payload.fromBytes(Message.getBytes()));
              //ListUpdateString(text);
              //connectionsClient.sendPayload(opponentEndpointId,Payload.fromBytes(Message.getBytes()));
              Log.i(TAG,"Client Send");
          }
      }
      else{
          Tag = "draw";
          Log.i(TAG,"Sending a drawing");
          byte[] DrawnArray = Drawing.ImageStream();
          //ListUpdateDrawing(DrawnArray);
          if(AmHost){
              for (String values:map.keySet()){
                  connectionsClient.sendPayload(opponentEndpointId,Payload.fromBytes(Tag.getBytes()));
                  connectionsClient.sendPayload(opponentEndpointId, Payload.fromBytes(DrawnArray));
              }
          }
          else{
              connectionsClient.sendPayload(opponentEndpointId,Payload.fromBytes(Tag.getBytes()));
              connectionsClient.sendPayload(opponentEndpointId, Payload.fromBytes(DrawnArray));
          }

      }
      Tag = "";
  }

  public void drawMode(View view){
      InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
      inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
      UserMsg.setVisibility(View.INVISIBLE);
      Drawing.setVisibility(View.VISIBLE);
      MsgBtn.setVisibility(View.VISIBLE);
      DrawBtn.setVisibility(View.INVISIBLE);
      eraserbtn.setVisibility(View.VISIBLE);
      brushbtn.setVisibility(View.INVISIBLE);
      BrushSize.setVisibility(View.VISIBLE);
      clearBtn.setVisibility(View.VISIBLE);
      MsgDraw=true;

  }

  public void msgMode(View view){
      InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
      inputMethodManager.showSoftInputFromInputMethod(view.getWindowToken(),0);
      UserMsg.setVisibility(View.VISIBLE);
      Drawing.setVisibility(View.INVISIBLE);
      MsgBtn.setVisibility(View.INVISIBLE);
      DrawBtn.setVisibility(View.VISIBLE);
      eraserbtn.setVisibility(View.INVISIBLE);
      brushbtn.setVisibility(View.INVISIBLE);
      BrushSize.setVisibility(View.INVISIBLE);
      clearBtn.setVisibility(View.INVISIBLE);
      MsgDraw = false;
  }

  private void AllViews(){
      disconnectButton = findViewById(R.id.disconnect);
      SendBtn = findViewById(R.id.sendBtn);
      findButton = findViewById(R.id.disconnect2);
      MessageList = findViewById(R.id.MessageList);
      HostStatus = findViewById(R.id.HostStatus);
      opponentText = findViewById(R.id.opponent_name);
      Name = findViewById(R.id.name);
      UserMsg = findViewById(R.id.UserMsg);
      MsgBtn = findViewById(R.id.messageBtn);
      DrawBtn = findViewById(R.id.drawBtn);
      Drawing = findViewById(R.id.drawingTable);
      eraserbtn = findViewById(R.id.eraseBtn);
      brushbtn = findViewById(R.id.brushBtn);
      BrushSize = findViewById(R.id.BrushSize);
      clearBtn = findViewById(R.id.clearBtn);
      loading = findViewById(R.id.loading);
      //initialize gif here

  }

  private void initialViews(){ //when before connecting to shit
      MsgBtn.setVisibility(View.INVISIBLE);
      DrawBtn.setVisibility(View.INVISIBLE);
      Drawing.setVisibility(View.INVISIBLE);
      eraserbtn.setVisibility(View.INVISIBLE);
      brushbtn.setVisibility(View.INVISIBLE);
      BrushSize.setVisibility(View.INVISIBLE);
      clearBtn.setVisibility(View.INVISIBLE);
      loading.setVisibility(View.INVISIBLE);
  }

  private void MessageMode(int mode){ //when chat room
      UserMsg.setVisibility(mode);
      SendBtn.setVisibility(mode);
      loading.setVisibility(View.INVISIBLE);

  }

  public void EraserBtn(View view){
      Drawing.initializeEraser();
      eraserbtn.setVisibility(View.INVISIBLE);
      brushbtn.setVisibility(View.VISIBLE);
  }

  public void btnBrush (View view){
      Drawing.initializePen();
      eraserbtn.setVisibility(View.VISIBLE);
      brushbtn.setVisibility(View.INVISIBLE);
  }

  public void clearBtn(View view){
      Drawing.clearImage();
  }

  private void ListUpdateString(String msg){
      item msghave = new item(msg);
      MsgArray.add(msghave);
      Adapter.notifyDataSetChanged();
      MessageList.smoothScrollByOffset(MsgArray.size() - 1 );
  }
  private void ListUpdateDrawing(byte[] msg){
      item msghave = new item(msg);
      MsgArray.add(msghave);
      Adapter.notifyDataSetChanged();
      MessageList.smoothScrollByOffset(MsgArray.size() - 1);
  }

  private void setOpponentName(String opponentName) {
    opponentText.setText(getString(R.string.opponent_name, opponentName));
  }

}