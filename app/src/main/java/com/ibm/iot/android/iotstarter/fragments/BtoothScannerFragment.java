package com.ibm.iot.android.iotstarter.fragments;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.iot.android.iotstarter.IoTStarterApplication;
import com.ibm.iot.android.iotstarter.R;
import com.ibm.iot.android.iotstarter.iot.IoTClient;
import com.ibm.iot.android.iotstarter.utils.Constants;
import com.ibm.iot.android.iotstarter.utils.MessageFactory;
import com.ibm.iot.android.iotstarter.utils.MyIoTActionListener;
import com.ibm.iot.android.iotstarter.utils.MyIoTCallbacks;
import com.ibm.iot.android.iotstarter.views.DrawingView;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.w3c.dom.Text;

import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * { BtoothScannerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BtoothScannerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BtoothScannerFragment extends IoTStarterPagerFragment implements TextToSpeech.OnInitListener {
    private final static String TAG = BtoothScannerFragment.class.getName();

    // TODO: Rename and change types of parameters
    TextView txtScan;
    TextView txtScannerId;
    Button btnScan;
    Button btnGetPrevMsg;
    Button btnGetCurrentMsg;
    CheckBox chkboxAlarm;
    Spinner spinnerDeviceId;

    private boolean isSetAlarm = false;
    private int counter = 1;
    private int messageCounter = 0;
    private String prevMessage = "";

    private int MY_DATA_CHECK_CODE = 0;
    private TextToSpeech myTTS;
    //private OnFragmentInteractionListener mListener;

    public BtoothScannerFragment() {
        // Required empty public constructor
    }


    // TODO: Rename and change types and number of parameters
    public static BtoothScannerFragment newInstance() {
        BtoothScannerFragment fragment = new BtoothScannerFragment();
        //Bundle args = new Bundle();
        //args.putString(ARG_PARAM1, param1);
        //args.putString(ARG_PARAM2, param2);
        //fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);



        super.onCreate(savedInstanceState);

//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }

    }

    /*@Override
    public void onResume(){

    }*/
    /*@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        Log.d(TAG,".onCreateOptions() entered");
        getActivity().getMenuInflater().inflate(R.menu.menu, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }*/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_btooth_scanner, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        /*if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }*/
    }

    /*@Override
    public void onAttach(Context context) {
        super.onAttach(context);
        *//*if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }*//*
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //mListener = null;
    }*/

    @Override
    public void onResume(){
        Log.d(TAG, ".onResume() entered");

        super.onResume();
        app = (IoTStarterApplication) getActivity().getApplication();
        app.setCurrentRunningActivity(TAG);

        if(broadcastReceiver == null){
            Log.d(TAG, ".onResume() - Registering iotBroadcastReceiver");
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, ".onReceive() - Received intent for iotBroadcastReceiver");
                    processIntent(intent);
                }
            };
        }



        getActivity().getApplicationContext().registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.APP_ID + Constants.INTENT_IOT));

        initializeBtoothScannerActivity();


    }

    @Override
    public void onDestroy(){
        Log.d(TAG, ".onDestroy() entered");
        try{
            getActivity().getApplicationContext().unregisterReceiver(broadcastReceiver);
        }
        catch(IllegalArgumentException iae){

        }
        super.onDestroy();
    }

    @Override
    void updateViewStrings(){
        Log.d(TAG, ".updateViewStrings() entered");
        if(app.getDeviceId() != null){
            ((TextView) getActivity().findViewById(R.id.deviceIDIoT)).setText(app.getDeviceId());
        }
        else{
            ((TextView) getActivity().findViewById(R.id.deviceIDIoT)).setText("-");
        }

        if(app.getMsgString() != "" ){
            ((TextView) getActivity().findViewById(R.id.txtView_Message)).setText(app.getMsgString());
        }
        else{
            ((TextView) getActivity().findViewById(R.id.txtView_Message)).setText("");
        }

        //
        if(messageCounter == 0){
            processReceiveIntent();
            messageCounter++;
        }
    }

    private void processIntent(Intent intent){
        Log.d(TAG, ".processIntent() entered");

        updateViewStrings();

        String data = intent.getStringExtra(Constants.INTENT_DATA);
        assert data !=  null;
        if(data.equals(Constants.INTENT_DATA_PUBLISHED)){
            processPublishIntent();
        }
        else if(data.equals(Constants.INTENT_DATA_RECEIVED)){
            processReceiveIntent();
        }
        else if(data.equals(Constants.ACCEL_EVENT)){
            processAccelEvent();
        } else if (data.equals(Constants.COLOR_EVENT)) {

            Log.d(TAG, "Updating background color");
//            DrawingView drawingView = (DrawingView) getActivity().findViewById(R.id.drawing);
//            drawingView.setBackgroundColor(app.getColor());
        }
        else if (data.equals(Constants.ALERT_EVENT)){
            String message = intent.getStringExtra(Constants.INTENT_DATA_MESSAGE);
            new AlertDialog.Builder(getActivity())
                    .setTitle("Received alert")
                    .setMessage(message)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).show();
        }
    }

    private void processPublishIntent(){
        Log.v(TAG, ".processPublishIntent() entered");
        String publishedString = "Messages Published: 0";
        publishedString = publishedString.replace("0", Integer.toString(app.getPublishCount()));
        //((TextView) getActivity().findViewById(R.id.messagesPublishedView)).setText(publishedString);
    }

    private void processReceiveIntent() {
        Log.v(TAG, ".processReceiveIntent() entered");
        String receivedString = this.getString(R.string.messages_received);
        receivedString = receivedString.replace("0",Integer.toString(app.getReceiveCount()));
        String receivedMsgString = app.getMsgString();

        if(receivedMsgString != null) {
            if (receivedMsgString.contains("alarm")) {
                String receivedMsgScanValue = app.getScanValue();
                String receivedMsgToDeviceId = app.getToDeviceId();
                String currentDeviceId = app.getDeviceId();
                if (receivedMsgToDeviceId.equals(currentDeviceId)) {
                    ((TextView) getActivity().findViewById(R.id.txtView_Message)).setText(receivedMsgScanValue);
                }
            } else {
                ((TextView) getActivity().findViewById(R.id.txtView_Message)).setText(receivedMsgString);
                if(receivedMsgString.length()> 0) {
                    if(messageCounter == 0){
                        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MyPref", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("prevMessage",app.getSpeakMsgString());
                        editor.commit();
                        messageCounter++;
                    }
                    else {
                        speakWords(app.getSpeakMsgString());
                    }
                }
            }
        }
        //((TextView) getActivity().findViewById(R.id.messagesReceivedView)).setText(receivedString);
    }

    private void processAccelEvent() {
        Log.v(TAG, ".processAccelEvent()");
        float[] accelData = app.getAccelData();
        /*((TextView) getActivity().findViewById(R.id.accelX)).setText("x: " + accelData[0]);
        ((TextView) getActivity().findViewById(R.id.accelY)).setText("y: " + accelData[1]);
        ((TextView) getActivity().findViewById(R.id.accelZ)).setText("z: " + accelData[2]);*/
    }

    private void initializeBtoothScannerActivity(){
        Log.d(TAG,".initializeBtoothScannerFragment() entered");

        context = getActivity().getApplicationContext();

        updateViewStrings();

        btnScan = (Button) getActivity().findViewById(R.id.btnSendNodeRed);
        btnGetPrevMsg = (Button) getActivity().findViewById(R.id.btnGetPrevMsg);
        btnGetCurrentMsg = (Button) getActivity().findViewById(R.id.btnGetCurrentMsg);
        txtScan = (EditText) getActivity().findViewById(R.id.editTextScannedVal);
        chkboxAlarm = (CheckBox) getActivity().findViewById(R.id.checkBoxAlarm);
        spinnerDeviceId = (Spinner) getActivity().findViewById(R.id.spinnerDeviceIDs);

        //spinnerDeviceId.setEnabled(false);
        chkboxAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(chkboxAlarm.isChecked()){
                    spinnerDeviceId.setEnabled(true);
                }
                else{
                    spinnerDeviceId.setEnabled(false);
                }
            }
        });
        txtScan.setOnKeyListener(new View.OnKeyListener(){
             @Override
             public boolean onKey(View v, int keyCode, KeyEvent event){
                  if(keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN){
                      handleSendText();
                      txtScan.setText("");
                      return true;
                  }
                  return false;
             }
        });
        btnScan.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                handleSendText();
                txtScan.setText("");
            }
        });
        btnGetPrevMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MyPref", MODE_PRIVATE);
                String pref_prevMsg = sharedPreferences.getString("prevMessage", null);
                if(pref_prevMsg != null){
                    prevMessage = pref_prevMsg;
                    speakWords(prevMessage);
                }
                else{
                    speakWords("No previous message");
                }
            }
        });
        btnGetCurrentMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakWords(app.getSpeakMsgString());
            }
        });

        TextView txtMsg = (TextView) getActivity().findViewById(R.id.txtView_Message);
        //txtMsg.seton
       // DrawingView drawingView = (DrawingView) getActivity().findViewById(R.id.drawing);
        //drawingView.setContext(context);
    }

    private void handleSendText(){
        Log.d(TAG, ".handleSendText() entered");
        if(chkboxAlarm.isChecked() == true){
            isSetAlarm = true;
        }
        else{
            isSetAlarm = false;
        }
        if(app.getConnectionType() != Constants.ConnectionType.QUICKSTART){
            counter = app.getMsgCount();
            String msgToSend = "";
            String devId = app.getDeviceId();
            String messageData = "";
            msgToSend = txtScan.getText().toString();
            msgToSend = msgToSend.replace("\\n", "");
            msgToSend = msgToSend.replace("\\r", "");
            if(isSetAlarm == true){
                String toDeviceId = String.valueOf(spinnerDeviceId.getSelectedItem());
                messageData = MessageFactory.getTextMessage("alarm", msgToSend, devId, toDeviceId);
            }
            else{
                messageData = MessageFactory.getTextMessage(Integer.valueOf(devId), msgToSend);
            }

            try{
                MyIoTActionListener listener = new MyIoTActionListener(context, Constants.ActionStateStatus.PUBLISH);
                IoTClient ioTClient = IoTClient.getInstance(context);

                ioTClient.publishEvent(Constants.TEXT_EVENT,  "json", messageData,2,false,listener);
                //
                messageCounter = 0;
                int count = app.getPublishCount();
                app.setPublishCount(++count);

                //String runningActivity = app.getCurrentRunningActivity();
                //if(runningActivity != null && runningActivity.equals(IoTPagerFragment.class.getName())){
                    Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_IOT);
                    actionIntent.putExtra(Constants.INTENT_DATA,Constants.INTENT_DATA_PUBLISHED);
                    context.sendBroadcast(actionIntent);
                //}
            }
            catch (MqttException e){

            }
        }
    }

    public int getCounter() { return counter; }

    private void speakWords(String speech){
        myTTS.speak(speech, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == MY_DATA_CHECK_CODE){
            myTTS = new TextToSpeech(context, this);
        }
        else{
            Intent installTTSIntent = new Intent();
            installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
            startActivity(installTTSIntent);
        }
    }

    public void onInit(int initStatus){
        if(initStatus == TextToSpeech.SUCCESS){
            myTTS.setLanguage(Locale.US);
        }
        else if (initStatus == TextToSpeech.ERROR) {
            Toast.makeText(context, "Sorry! Text To Speech failed...", Toast.LENGTH_LONG).show();
        }
    }
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    /*public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }*/
}
