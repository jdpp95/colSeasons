package com.example.android.colseasons;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    public Spinner spinnerDia, spinnerMes, spinnerAño;
    public int dia, mes, año;
    public Calendar time, today;
    boolean monthFirst = true;
    boolean ini = true;
    double[] weights;

    public double latitud, longitud, altitud, porcentaje, avgTemp, hora, osD, osN, temperatura;
    public int seconds = 0;
    public ArrayList<Estacion> estaciones;
    public Estacion[] nearestS;

    TextView textAltitud, textDiff;

    private final int[] añoNormal = {31,28,31,30,31,30,31,31,30,31,30,31};
    private final int[] añoBisiesto = {31,29,31,30,31,30,31,31,30,31,30,31};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v("Debug","Marco");
        setContentView(R.layout.activity_main);
        Log.v("Debug","Polo");
        //getWindow().setSoftInputMode(
          //      WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
        } else {
            locationStart();
        }

        time = Calendar.getInstance();
        textAltitud = (TextView) findViewById(R.id.elevation);
        textDiff = (TextView) findViewById(R.id.diff);
        spinnerDia = (Spinner) findViewById(R.id.dia);
        spinnerMes = (Spinner) findViewById(R.id.mes);
        spinnerAño = (Spinner) findViewById(R.id.año);
        weights = new double[3];

        today = getDateForToday();
        año = today.get(Calendar.YEAR); spinnerAño.setSelection(2018-año); spinnerAño.setEnabled(false);
        mes = today.get(Calendar.MONTH); spinnerMes.setSelection(mes); spinnerMes.setEnabled(false);
        dia = time.get(Calendar.DATE);

        spinnerDia.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                dia = Integer.parseInt((String) spinnerDia.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                time = Calendar.getInstance();
                dia = time.get(Calendar.DATE);
            }
        });

        spinnerMes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mes = position + 1;

                int arrayRes;
                if (mes == 1 || mes == 3 || mes == 5 || mes == 7 || mes == 8 || mes == 10 || mes == 12)
                    arrayRes = R.array.days_31;
                else if (mes == 2)
                    if (año % 4 == 0 && (año % 100 != 0 || año % 400 == 0))
                        arrayRes = R.array.days_29;
                    else
                        arrayRes = R.array.days_28;
                else
                    arrayRes = R.array.days_30;

                ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(MainActivity.this, arrayRes, R.layout.support_simple_spinner_dropdown_item);

                spinnerDia.setAdapter(arrayAdapter);

                if(monthFirst)
                {
                    spinnerDia.setSelection(today.get(Calendar.DAY_OF_MONTH) - 1); spinnerDia.setEnabled(false);
                    monthFirst = false;
                }

                Log.v("Month changed","Month "+(mes+1));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                time = Calendar.getInstance();
                mes = time.get(Calendar.MONTH);
            }
        });

        spinnerAño.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                año = Integer.parseInt((String) spinnerAño.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                time = Calendar.getInstance();
                año = time.get(Calendar.YEAR);
            }
        });

        loadPlaces();
        nearestS = new Estacion[3];

        //spinnerDia.setSelection(6);

        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        /*
                        * tsnm
                        *           tsnm    OsD OsN
                        * Place 1   14.1    5.2 4.8
                        * Place 2   28.1    3.2 2.8
                        * Place 3   27.6    5.2 4.6
                        * Average   23.3    4.5 4.1
                         */
                        double temp[][] = new double[4][3];

                        /* hora = 12 + 35/60.0;
                        latitud = 4.6238;
                        longitud = -74.0828;
                        altitud = 2574;
                        */

                        if (seconds == 0) updateTime();
                        if (seconds == 5) nearestS = nearestStations();

                        if(nearestS == null)
                            Log.e("Error","Nearest stations error");
                        else {

                            seconds = (seconds + 1) % 10;
                            //tsnm[0,1,2] = values, tsnm[3] = weighted average
                            double weightsDist[] = new double[3];
                            double weightsAlt[] = new double[3];
                            double altDiff[] = new double[3];

                            double weightsDistSum = 0, weightsAltSum = 0;
                            for (int i = 0; i < 3; i++) {
                                float results[] = new float[3];
                                if (nearestS[i] != null) {
                                    temp[i][0] = nearestS[i].getTemperatura() + nearestS[i].getAltitud() / 180;
                                    temp[i][1] = nearestS[i].getOsD();
                                    temp[i][2] = nearestS[i].getOsN();

                                    Location.distanceBetween(latitud, longitud, nearestS[i].getLatitud(), nearestS[i].getLongitud(), results);
                                    weightsDist[i] = 1 / results[0]; // 1/Dist
                                    weightsDistSum += weightsDist[i];

                                    altDiff[i] = Math.abs(altitud - nearestS[i].getAltitud());
                                    weightsAlt[i] = 1 / altDiff[i];
                                    weightsAltSum += weightsAlt[i];
                                }
                            }

                            for(int i=0;i<3;i++) {
                                weights[i] = (weightsAlt[i]/weightsAltSum + weightsDist[i]/weightsDistSum)/2;
                                //temp[3][i] = 0;
                            }

                            //Normalize weights

                            for (int i = 0; i < 3; i++) {
                                /*
                                if (weightSum > 0)
                                    weights[i] /= weightSum;
                                    */

                                //Log.v("Distance to " + nearestS[i], formatNumber(weights[i] * 100, 0) + "% from "+ estaciones.size() +" stations.");
                                for(int j=0;j<3;j++)
                                    temp[3][j] += temp[i][j] * weights[i];
                            }

                        }

                        avgTemp = temp[3][0] - altitud/180; //Computes average temp for a place
                        osD = temp[3][1];
                        osN = temp[3][2];

                        updateTemperature(ini);
                        textAltitud.setText(formatNumber(altitud,0));
                    }
                });
            }
        };
        timer.schedule(timerTask, 1, 1000); //Runs this code every second
    }

    //Initializes the location
    private void locationStart() {
        LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Localizacion local = new Localizacion();
        local.setMainActivity(this);
        final boolean gpsEnabled = mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsEnabled) {
            Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(settingsIntent);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
            return;
        }
        mlocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, (LocationListener) local);
        mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) local);
    }

    public void loadPlaces() {
        String currentLine;

        estaciones = new ArrayList<>();

        try {
            InputStream graw = getResources().openRawResource(R.raw.estaciones);
            BufferedReader br2 = new BufferedReader(new InputStreamReader(graw));

            while ((currentLine = br2.readLine()) != null) {
                String tokens[] = currentLine.split("\t");

                Estacion estacion = new Estacion(
                        tokens[0],
                        tokens[1],
                        tokens[2],
                        Double.parseDouble(tokens[3]),
                        Double.parseDouble(tokens[4]),
                        Double.parseDouble(tokens[5]),
                        Double.parseDouble(tokens[6]),
                        Double.parseDouble(tokens[7]),
                        Double.parseDouble(tokens[8])
                );

                estaciones.add(estacion);
            }

            if (br2 != null)
                br2.close();

            if(graw != null)
                graw.close();

        } catch (Exception e) {
            Log.e("Error!!", "No se pudo leer el archivo de origen.");
        }
    }

    //Rounds a decimal number to a given number of decimals
    public String formatNumber(double number, int decimalPlaces) {
        BigDecimal bd = new BigDecimal(number);
        return bd.setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP).toString();
    }

    //Updates variables to current time (This should be updated every minute)
    public void updateTime() {
        time = Calendar.getInstance();
        hora = time.get(Calendar.HOUR_OF_DAY) + time.get(Calendar.MINUTE)/60.0 + time.get(Calendar.SECOND)/3600.0;
        porcentaje = hora/24.0;
    }

    //Updated every 10 seconds
    public Estacion[] nearestStations() {
        Estacion nearest[] = new Estacion[3];

        nearest[0] = null;
        nearest[1] = null;
        nearest[2] = null;

        double distances[] = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        double newDistance, bearing;

        for (Estacion x : estaciones) {
            float[] results = new float[3];
            Location.distanceBetween(latitud, longitud, x.getLatitud(), x.getLongitud(), results);
            newDistance = results[0];
            //Log.v("Distance data: ", "Place :" + x + ", distance: " + formatNumber(newDistance / 1000, 1) + "km, initial bearing: " + results[1] + ", final bearing: " + results[2]);

            //Algoritmo chevere

            int i=2;

            if(newDistance > distances[2])
                continue;
            else {
                while(i > 0 && newDistance < distances[i-1])
                    i--;
                //newDistance will replace position 'i' in array

            }

            for(int j=2; j>i; j--)
            {
                distances[j] = distances[j-1];
                nearest[j] = nearest[j-1];
            }

            distances[i] = newDistance;
            nearest[i] = x;

            //Log.v("Distances"," "+distances[0]+", "+distances[1]+", "+distances[2]);
        }

        return nearest;
    }

    //Colors background according to average temperature of the current date
    public void colorear(double temperatura) {
        int rgb[] = new int[3];
        final int[] FRIGID = {0, 0, 255};
        final int[] FREEZING = {0, 255, 255};
        final int[] CHILLY = {0, 192, 128};
        final int[] COLD = {0, 128, 0};
        final int[] COOL = {128, 192, 0};
        final int[] COMFORTABLE = {255, 255, 0};
        final int[] WARM = {255, 128, 0};
        final int[] HOT = {255, 0, 0};
        final int[] SWELTERING = {128, 0, 0};

        if (temperatura < 0) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = 255;
        } else if (temperatura < 21) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(255, FRIGID[i], 0, 21, temperatura) + 0.5);
        } else if (temperatura < 32) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(FRIGID[i], FREEZING[i], 21, 32, temperatura) + 0.5);
        } else if (temperatura < 41) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(FREEZING[i], CHILLY[i], 32, 41, temperatura) + 0.5);
        } else if (temperatura < 50) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(CHILLY[i], COLD[i], 40, 50, temperatura) + 0.5);
        } else if (temperatura < 60) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(COLD[i], COOL[i], 50, 60, temperatura) + 0.5);
        } else if (temperatura < 70) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(COOL[i], COMFORTABLE[i], 60, 70, temperatura) + 0.5);
        } else if (temperatura < 80) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(COMFORTABLE[i], WARM[i], 70, 80, temperatura) + 0.5);
        } else if (temperatura < 90) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(WARM[i], HOT[i], 80, 90, temperatura) + 0.5);
        } else if (temperatura < 100) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(HOT[i], SWELTERING[i], 90, 100, temperatura) + 0.5);
        } else if (temperatura < 125) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(SWELTERING[i], 0, 100, 125, temperatura) + 0.5);
        } else {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = 0;
        }

        TextView elevText = (TextView) findViewById(R.id.elevText);
        TextView dateText = (TextView) findViewById(R.id.dateText);
        TextView diffText = (TextView) findViewById(R.id.diffText);
        ScrollView main = (ScrollView) findViewById(R.id.main);

        TextView[] texts = {elevText, dateText, diffText, textAltitud, textDiff};

        if(altitud != 0)
            main.setBackgroundColor(Color.rgb(rgb[0],rgb[1],rgb[2]));
        else
            main.setBackgroundColor(Color.BLACK);

        for(int i=0; i<texts.length; i++)
            if (temperatura > 25 && temperatura < 35 || temperatura > 60 && temperatura < 80)
                texts[i].setTextColor(Color.BLACK);
            else
                texts[i].setTextColor(Color.WHITE);
    }

    public double transicion(double start1, double end1, double start2, double end2, double value2) {
        //128, 192, 20, 30, 23

        double proporcion = (value2 - start2) / (end2 - start2);

        return start1 + (end1 - start1) * proporcion;
    }

    //Updates seasonal temperature (Should be updated at least every minute)
    public void updateTemperature(boolean init) {

        Calendar today = Calendar.getInstance();

        today.set(año, mes - 1, dia);

        long seed = parseSeed(today);
        Log.v("Seed Value", String.valueOf(seed));

        Random rnd = new Random(seed);
        double oscilacionAnual = (avgTemp * -0.6485 + 28.712) * (osD + osN) / 10;
        double tempMediaAnual = (avgTemp * 5000 - 29571) / 4117;
        double todayTemp, tomorrowTemp;

        double dateDebug = today.get(Calendar.DAY_OF_YEAR);
        todayTemp = tempMediaAnual - Math.cos(2*Math.PI*((today.get(Calendar.DAY_OF_YEAR) / 365.25) - 1/16)) * oscilacionAnual/2
                + rnd.nextGaussian()*3;

        Calendar tomorrow = null;
        if(init) {
            int periodo = 0;

            double debug = today.get(Calendar.MONTH);
            periodo += 7*(today.get(Calendar.MONTH)/3);
            int diasem = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5)%7;
            periodo += diasem;

            tomorrow = randomDate((periodo + 1)%28,today.get(Calendar.YEAR));
        } else {
            tomorrow = (Calendar) today.clone();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        }

        seed = parseSeed(tomorrow);

        rnd.setSeed(seed);

        Log.v("Seed Value Tomorrow", String.valueOf(seed));

        tomorrowTemp = tempMediaAnual - Math.cos(2*Math.PI*((tomorrow.get(Calendar.DAY_OF_YEAR) / 365.25) - 1/16)) * oscilacionAnual/2
                + rnd.nextGaussian()*3;

        Log.v("Forecast","Today "+todayTemp+"\nTomorrow "+tomorrowTemp);

        temperatura = transicion(todayTemp, tomorrowTemp, 0, 1, porcentaje);

        if(altitud != 0)
            textDiff.setText(formatNumber(temperatura - avgTemp,1) + " °C");
         else
            textDiff.setText("-- °C");

        colorear(temperatura * 1.8 + 32);
    }

    public long parseSeed(Calendar time)
    {
        int day, month, year;
        day = time.get(Calendar.DATE);
        month = time.get(Calendar.MONTH) + 1;
        year = time.get(Calendar.YEAR);

        String semilla = "";
        semilla += year;
        if(month < 10)
            semilla += "0";

        semilla += month;

        if(day < 10)
            semilla += 0;

        semilla += day;

        long seed = Long.parseLong(semilla);

        return seed;
    }

    public boolean añoBisiesto(int año){
        return año%4 == 0 && (año%100 != 0 || año%400 == 0);
    }

    public Calendar getDateForToday(){
        final long millisInDay = 86400000;

        Calendar now = Calendar.getInstance();

        Calendar y1935 = Calendar.getInstance();
        y1935.set(1935,0,0);

        Calendar y2012 = Calendar.getInstance();
        y2012.set(2012,6,29);

        long millis1935 = y1935.getTimeInMillis();
        long millis2012 = y2012.getTimeInMillis();

        //System.out.println(millis1935/millisInDay+", "+millis2012/millisInDay);

        long millis = now.getTimeInMillis() - millis2012;
        int days = (int)((365.25/28)*millis/millisInDay);

        Calendar returnDate = (Calendar) y1935.clone();
        //returnDate.add(Calendar.YEAR, years);
        returnDate.add(Calendar.DAY_OF_YEAR, days);

        Log.v("Date",returnDate.get(Calendar.DAY_OF_MONTH)+"/"+(returnDate.get(Calendar.MONTH)+1)+"/"+returnDate.get(Calendar.YEAR));

        int periodo = 0;

        double debug = returnDate.get(Calendar.MONTH);
        periodo += 7*(returnDate.get(Calendar.MONTH)/3);
        int diasem = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5)%7;
        periodo += diasem;

        return randomDate(periodo, returnDate.get(Calendar.YEAR));
    }

    public Calendar randomDate(int periodo, int año){

        int cuatrimestre = periodo/7;

        int duracion=0, diasem;

        Calendar date = Calendar.getInstance();

        diasem = periodo%7;

        switch (cuatrimestre)
        {
            case 0:
                duracion = 90;
                if (añoBisiesto(año))
                    duracion++;
                date.set(año,Calendar.JANUARY,1);
                break;
            case 1:
                duracion = 91;
                date.set(año,Calendar.APRIL,1);
                break;
            case 2:
                duracion = 92;
                date.set(año,Calendar.JULY,1);
                break;
            case 3:
                duracion = 92;
                date.set(año,Calendar.OCTOBER,1);
                break;
        }

        int addDebug = (int)(diasem*duracion/7.0);
        date.add(Calendar.DAY_OF_YEAR, (int)(diasem*duracion/7.0));

        long seed = parseSeed(date);
        Random rnd = new Random(seed);

        Calendar dateAfter = (Calendar) date.clone();
        dateAfter.add(Calendar.DAY_OF_YEAR, (int)(duracion/7.0));

        Calendar iDate = (Calendar) date.clone();
        int i=0;

        //If month date equals actual month date, return current date of that month
        while(iDate.get(Calendar.DAY_OF_MONTH) != dateAfter.get(Calendar.DAY_OF_MONTH) && i < 10000)
        {
            if(iDate.get(Calendar.DAY_OF_MONTH) == Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
                return iDate;

            iDate.add(Calendar.DAY_OF_YEAR,1);
            i++;
        }

        date.add(Calendar.DAY_OF_YEAR,(int)(rnd.nextDouble()*duracion/7.0));

        return date;
    }

    public void enableDatePicker(View v)
    {
        ini = false;
        spinnerDia.setEnabled(true);
        spinnerMes.setEnabled(true);
        spinnerAño.setEnabled(true);
    }

    public class Localizacion implements LocationListener {

        MainActivity mainActivity;

        public MainActivity getMainActivity() {
            return mainActivity;
        }

        public void setMainActivity(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        public void onLocationChanged(Location loc) {
            // Este metodo se ejecuta cada vez que el GPS recibe nuevas coordenadas
            // debido a la deteccion de un cambio de ubicacion

            latitud = loc.getLatitude();
            longitud = loc.getLongitude();
            altitud = loc.getAltitude();
        }

        @Override
        public void onProviderDisabled(String provider) {
            // Este metodo se ejecuta cuando el GPS es desactivado
            Toast.makeText(mainActivity, "GPS Desactivado", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderEnabled(String provider) {
            // Este metodo se ejecuta cuando el GPS es activado
            Toast.makeText(mainActivity, "GPS Activado", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

            switch (status) {
                case LocationProvider.AVAILABLE:
                    Log.d("debug", "LocationProvider.AVAILABLE");
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    Log.d("debug", "LocationProvider.OUT_OF_SERVICE");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.d("debug", "LocationProvider.TEMPORARILY_UNAVAILABLE");
                    break;
            }
        }
    }
}
