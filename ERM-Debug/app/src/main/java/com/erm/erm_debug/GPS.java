package com.erm.erm_debug;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import java.io.IOException;

/* Clase Main del software.*/
public class GPS extends ActionBarActivity implements LocationListener {
    // Location list, PROBABLEMENTE Hay que eliminarla
    List<Map<String, String>> locationList = new ArrayList<>();
    public static final int MY_PERMISSIONS = 0;

    // Inputs gráficos.
    private Switch mySwitch;
    private TextView samplesSize;
    private TextView locationSize;
    private TextView matchedSize;
    private TextView lat;
    private TextView lng;
    private TextView txtGps;
    private TextView datetime;
    private Switch switch_ubicacion;

    public String muestraBT;
    public double muestraGPSlng;
    public double muestraGPSlat;
    public long muestraGPStime;
    // Gps.
    public LocationManager handle;
    private String provider;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    //BluetoothDevice mmDevice;
    //OutputStream mmOutputStream;
    //InputStream mmInputStream;
    //Thread workerThread;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;

    SamplesManager samplesManager = new SamplesManager();

    // CONSTANTES
    private static final int MIN_TIME_COORDS = 15;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client2;

    /* Método que se ejecuta al iniciar la aplicación. */
    protected void onCreate(Bundle savedInstanceState) {
        // Inicialización.
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);
        // Inputs
        txtGps = (TextView) findViewById(R.id.proveedor);
        mySwitch = (Switch) findViewById(R.id.switch_ubicacion);
        lat = (TextView) findViewById(R.id.lat);
        lng = (TextView) findViewById(R.id.lng);
        datetime = (TextView) findViewById(R.id.datetime);
        locationSize = (TextView) findViewById(R.id.locationsSize);
        samplesSize = (TextView) findViewById(R.id.SamplesSize);
        matchedSize = (TextView) findViewById(R.id.MatchedSize);
        samplesManager = new SamplesManager();
        samplesManager.defaultList();


        try {
            AssetManager assets = getAssets();
            samplesManager.readSamplesFile(assets);
        } catch (IOException e) {
            e.printStackTrace();
        }

        AssetManager assets = getAssets();
        /*try {
            //readChordinatesFile(assets);
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    setEstadoSwitch(isChecked);
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client2 = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    /* Se ejecuta al mover el Switch, inicializa o termina el servicio de captura según el estado del Switch.*/
    void setEstadoSwitch(boolean switchStatus) throws ParseException, IOException {
        if (switchStatus) {
            IniciarServicio();
        } else {
            PararServicio();
        }
    }

    /* Inicia el servicio de captura de coordenadas.
     * Inicia la conexión Bluetooth */
    public void IniciarServicio() throws IOException {

        /* Verifica si el dispositivo es compatible con Bluetooth. */
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "El dispositivo no es compatible con Bluetooth.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "El dispositivo es compatible con Bluetooth.", Toast.LENGTH_SHORT).show();
        }

        // Si el dispositivo es compatible con Bluetooth, Habilita la conexión.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        // Busca entre todos los dispositivos pareados con el teléfono al arduino (HC-06) y lo establece como dispositivo por defecto.
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("HC-06")) {
                    mmDevice = device;
                    Toast.makeText(this, mmDevice.getName(), Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }

        // Configura e inicia la conexión con el Dispositivo Arduino.
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();
        beginListenForData();

        //int time = (int) (System.currentTimeMillis());
        //String timeString = Integer.toString(time);
        Calendar ca = Calendar.getInstance();

        SimpleDateFormat df = new SimpleDateFormat("yyyy,MM,dd,HH,mm,ss");
        String formattedDate = df.format(ca.getTime());
        System.out.println("INICIO:" + formattedDate);
        write(formattedDate);

        //Toast.makeText(this, "Iniciado", Toast.LENGTH_SHORT).show();
        // Configura el Handler para la captura de coordenadas GPS.
        handle = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        // Assume thisActivity is the current activity
        int internetPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET);
        int finePermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int networkPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_NETWORK_STATE);
        int coarsePermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        // Si no tiene los permisos
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Pedir permisos
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE
                    },
                    MY_PERMISSIONS);
        } else {
            provider = handle.getBestProvider(c, true);
            txtGps.setText("Proveedor:" + provider);

            handle.requestLocationUpdates(provider, 5000, 0, this);
            Location location = handle.getLastKnownLocation(provider);
            this.onLocationChanged(location);
            MuestraPosicionActual(location);
        }

        //txtGps.setText(String.valueOf(permissionCheck));


    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "dado", Toast.LENGTH_SHORT).show();
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    Toast.makeText(this, "denegado", Toast.LENGTH_SHORT).show();

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    void beginListenForData() {
        System.out.println("begin");
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        //System.out.println("meh");
                        if (bytesAvailable > 0) {
                            //System.out.println(bytesAvailable);
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                //System.out.println(delimiter);
                                if (b == delimiter)

                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    //System.out.println("AAAA");

                                    handler.post(new Runnable() {
                                        public void run() {
                                            locationSize.setText(data);
                                            muestraBT = data;
                                            //System.out.println("llegó");
                                            //System.out.println("nueva BT:" + muestraBT);
                                            try {
                                                unirMuestras();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }

                                            //data = null;
                                            //Toast.makeText(this, data, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    //System.out.println("ELSE");
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        System.out.println("STOP");
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    public void MuestraPosicionActual(Location location) {

        if (location == null) {
            lat.setText("Lat Desconocida");
            lng.setText("Lng Desconocida");
        } else {
            lat.setText(String.valueOf(location.getLatitude()));
            lng.setText(String.valueOf(location.getLongitude()));
        }
    }


    public void PararServicio() throws ParseException {
        handle.removeUpdates(this);
        lat.setText("Detenido");
        lng.setText("Detenido");
        Toast.makeText(this, "OFF", Toast.LENGTH_SHORT).show();
        //samplesManager.makeMatch(locationList);
    }

    public void unirMuestras() throws IOException {
        System.out.println(muestraBT);
        List<String> inputBT = Arrays.asList(muestraBT.split(","));

        String tmp = inputBT.get(0);
        int muestraBTTime = Integer.parseInt(tmp);
        muestraBTTime = muestraBTTime + 86400;
        System.out.println("BTTIME:" + muestraBTTime);
        long resultado = muestraBTTime - muestraGPStime;

        String time = String.valueOf(muestraBTTime);
        long timestampLong = Long.parseLong(time)*1000;
        Date d = new Date(timestampLong);
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        //int year = c.get(Calendar.YEAR);
        //int month = c.get(Calendar.MONTH);
        //int date = c.get(Calendar.DATE);
        //System.out.println(year +"-"+month+"-"+date);
        //Calendar ca = Calendar.getInstance();

        // NECEISTO GENERAR EL YY-MM-DD del timestampt gps o bluethooth

        //System.out.println("CA" + ca.getTime());
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = df.format(c.getTime());
        //Date date = new Date(muestraBTTime);



        //System.out.println("Timestampt BT:" + inputBT.get(0));

        // lat, lng, date, value
        String params = String.valueOf(muestraGPSlat) + "|" + String.valueOf(muestraGPSlng) + "|" + formattedDate + "|" + inputBT.get(1) ;
        System.out.println(params);

        if(Math.abs(resultado) < 30 && resultado > 0) {
            this.makeRequest(params);
            System.out.println("resulado positivo: " + resultado);
        } else {
            this.makeRequest(params);
            System.out.println("resulado negativo: " + resultado);
        }

        //System.out.println(inputBT.get(0));

        //System.out.println(muestraGPStime);
    }

    public void write(String s) throws IOException {
        mmOutputStream.write(s.getBytes());
        System.out.println("hola soy la app");
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) {
            lat.setText("Lat Desconocida");
            lng.setText("Lng Desconocida");
        } else {

            muestraGPStime = System.currentTimeMillis() / 1000;
            muestraGPSlat = location.getLatitude();
            muestraGPSlng = location.getLongitude();

            System.out.println("nueva GPS:" + muestraGPStime);


            lat.setText("asdas");
            // Guardamos las coordenadas en una matriz.
            Map<String, String> map = new HashMap<>();
            map.put("lat", String.valueOf(location.getLatitude()));
            map.put("lng", String.valueOf(location.getLongitude()));

            // Se añade la fecha a la matriz en formato yyyy-MM.dd HH:mm:ss.
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate = df.format(c.getTime());
            map.put("datetime", formattedDate);

            // Agregamos la matriz a nuestra lista que contiene todas las coordenadas.
            locationList.add(map);

            // Si hay alguna muestra y hay 10 o más coordenadas se hace el match.
            //System.out.println(String.valueOf(samplesManager.getSizeOfSamples()) + " wait " + String.valueOf(locationList.size()));
            if (samplesManager.getSizeOfSamples() > 0 && locationList.size() >= 1) {
                /*try {
                    //samplesManager.makeMatch(locationList);
                } catch (ParseException e) {
                    e.printStackTrace();
                }*/
            }
            // Si hay muchas coordenadas y aún no hay samples se limpia la lista de coordenadas.
            else if (locationList.size() >= 50) {
                Toast.makeText(this, "Limpiando exceso de coordenadas", Toast.LENGTH_SHORT).show();
                this.clearList();
            }

            //System.out.println(String.valueOf(locationList.size()));
            if (samplesManager.getSizeOfMatchedSamples() > 10) {
                Toast.makeText(this, "Enviando a servidor", Toast.LENGTH_SHORT).show();
                //this.makeRequest("hola");
                locationList.clear();
            }
            lat.setText(String.valueOf(location.getLatitude()));
            lng.setText(String.valueOf(location.getLongitude()));
            datetime.setText(formattedDate);


            //System.out.println(locationList.size());
            locationSize.setText(String.valueOf("cant. coordenadas: " + locationList.size()));
            samplesSize.setText(String.valueOf("cant. muestras: " + samplesManager.getSizeOfSamples()));
            matchedSize.setText(String.valueOf("cant. matched: " + samplesManager.getSizeOfMatchedSamples()));
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    OkHttpClient client = new OkHttpClient();

    String get(String url, String json) throws IOException {
        //System.out.println(json);
        //RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }

    String bowlingJson() {

        //JSONArray json = new JSONArray(locationList);

        Map<String, List> map = new HashMap<>();
        map.put("samples", samplesManager.getMatchedSamples());

        JSONObject json = new JSONObject(map);
        return json.toString();


    }

    public void makeRequest(String params) throws IOException {

        String json = this.bowlingJson();
        String response = this.get("http://104.236.92.253/api/savesamples/" + params, json);
        System.out.println("?????????sss????????????");
        System.out.println("http://104.236.92.253/api/savesamples/" + params);
        System.out.println(response);
        //Toast.makeText(this, response, Toast.LENGTH_SHORT).show();
    }

    // Se limpia la lista de coordenadas. en el futuro la idea es eliminar solo las
    // mas antiguas progresivamente.
    public void clearList() {
        locationList.clear();

    }

    public void readChordinatesFile(AssetManager assets) throws IOException {

        InputStream is = assets.open("chordinates.csv");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] RowData = line.split(",");
                //System.out.println(RowData[0] + RowData[1] + "noe gomez");
                //date = RowData[0];
                //value = RowData[1];
                // do something with "data" and "value"

                Map<String, String> map = new HashMap<>();
                map.put("lat", String.valueOf(RowData[0]));
                map.put("lng", String.valueOf(RowData[1]));
                map.put("datetime", String.valueOf(RowData[2]));

                locationList.add(map);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("GPS Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client2.connect();
        AppIndex.AppIndexApi.start(client2, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client2, getIndexApiAction());
        client2.disconnect();
    }
}