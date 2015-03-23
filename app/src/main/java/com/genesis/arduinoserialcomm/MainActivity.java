package com.genesis.arduinoserialcomm;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends ActionBarActivity {

    private static UsbSerialPort sPort = null;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    private UsbManager usbManager;

    private Button refreshDevices,pushData,stop,backward;

    private Spinner deviceList;

    private NumberPicker pushML;

    private List<UsbSerialPort> list;

    private ArrayAdapter<UsbSerialPort> arrayAdap;

    private Integer dataOut = 0;

    private TextView feedback;

    private ScrollView scroller;

    private String dataString = "";

    private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {
        @Override
        public void onNewData(final byte[] data) {

            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.updateReceivedData(data);
                }
            });

        }

        @Override
        public void onRunError(Exception e) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        scroller = (ScrollView) findViewById(R.id.scroll);
        stop = (Button) findViewById(R.id.button3);
        backward = (Button) findViewById(R.id.button4);
        feedback = (TextView) findViewById(R.id.textView);
        pushData = (Button) findViewById(R.id.button2);
        pushML = (NumberPicker) findViewById(R.id.numberPicker);
        refreshDevices = (Button) findViewById(R.id.button);
        deviceList = (Spinner) findViewById(R.id.spinner);
        list = new ArrayList<UsbSerialPort>();
        arrayAdap = new ArrayAdapter<UsbSerialPort>(this,android.R.layout.simple_list_item_1, list);
        deviceList.setAdapter(arrayAdap);
        pushML.setMaxValue(10);
        pushML.setMinValue(0);
        pushML.setWrapSelectorWheel(false);
        pushML.setFocusable(false);
        pushML.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        pushML.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {

                dataOut = newVal;

            }
        });

        pushData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sPort != null)
                {
                    final byte[] datas = dataOut.toString().getBytes();
                    try {
                        sPort.write(datas, 100);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else
                    Toast.makeText(getApplicationContext(), "Port not open", Toast.LENGTH_LONG).show();
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(sPort != null)
                {
                    Integer s = 30;
                    final byte[] datas = s.toString().getBytes();
                    try {
                        sPort.write(datas, 100);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        });

        backward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(sPort != null)
                {
                    Integer s = 20;
                    final byte[] datas = s.toString().getBytes();
                    try {
                        sPort.write(datas, 100);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        });

        deviceList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                sPort = list.get(position);

                if (sPort == null) {
                } else {
                    final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

                    UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
                    if (connection == null) {
                        return;
                    }

                    try {
                        sPort.open(connection);
                        sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                        sPort.setRTS(true);
                        sPort.setDTR(true);
                        Toast.makeText(getApplicationContext(), "PORT OPENED", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        try {
                            sPort.close();
                        } catch (IOException e2) {
                            // Ignore.
                        }
                        sPort = null;
                        return;
                    }
                }
                onDeviceStateChange();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        getUsbDevices();

        refreshDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getUsbDevices();

            }
        });

    }


    @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (sPort != null) {
            try {
                sPort.setRTS(false);
                sPort.setDTR(false);
                sPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            sPort = null;
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private void getUsbDevices()
    {
        new AsyncTask<Void,Void,List<UsbSerialPort>>(){

            protected List<UsbSerialPort> doInBackground(Void... params) {
                SystemClock.sleep(1000);

                final List<UsbSerialDriver> drivers =
                        UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);

                final List<UsbSerialPort> result = new ArrayList<UsbSerialPort>();
                for (final UsbSerialDriver driver : drivers) {
                    final List<UsbSerialPort> ports = driver.getPorts();
                    result.addAll(ports);
                }

                return result;
            }

            @Override
            protected void onPostExecute(List<UsbSerialPort> result) {
                list.clear();
                list.addAll(result);
                arrayAdap.notifyDataSetChanged();
            }

        }.execute((Void) null);
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sPort != null) {
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private void updateReceivedData(final byte[] data) {

        final String message = HexDump.dumpHexString(data) + "\n";
        feedback.append(message.substring(message.indexOf("$")+1));
        //Toast.makeText(getApplicationContext(), message.substring(message.indexOf("$") + 1).toString(), Toast.LENGTH_SHORT).show();
        scroller.scrollTo(0, feedback.getBottom());

    }

}
