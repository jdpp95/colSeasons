package com.example.android.colseasons;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.apache.commons.math3.distribution.NormalDistribution;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;

/*
* This spaghetti code has to be fixed
* */

public class MainActivity extends AppCompatActivity {

    public Spinner spinnerDia, spinnerMes, spinnerAño;
    public int dia, mes, año;
    public Calendar time, today;
    boolean monthFirst = true;
    boolean ini = true;
    boolean report = false;
    double[] weights;
    double centerT;

    public double latitud, longitud, altitud, porcentaje, avgTemp, hora, osD, osN, temperatura;
    public int seconds = 0;
    public ArrayList<Estacion> estaciones;
    public Estacion[] nearestS;
    public ArrayList<TempStamp> randomDayArray;

    TextView textAltitud, textDiff, textTemperatura;
    CheckBox centerEnabledCheckBox;

    //private final int[] añoNormal = {31,28,31,30,31,30,31,31,30,31,30,31};
    //private final int[] añoBisiesto = {31,29,31,30,31,30,31,31,30,31,30,31};
    private final int añoActual = 1983;
    private final double THERMOMETER_DIFF = 1.5;
    private final int SHORT = 0, LONG = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.v("Debug","Marco");
        setContentView(R.layout.activity_main);
        Bundle bundle = getIntent().getExtras();
        if(bundle != null)
            centerT = bundle.getDouble("CENTER_TEMPERATURE");
        else
            centerT = 0;
        //Log.v("Debug","Polo");
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
        textTemperatura = (TextView) findViewById(R.id.temperature);
        spinnerDia = (Spinner) findViewById(R.id.dia);
        spinnerMes = (Spinner) findViewById(R.id.mes);
        spinnerAño = (Spinner) findViewById(R.id.año);
        centerEnabledCheckBox = (CheckBox) findViewById(R.id.centerEnabled);
        weights = new double[3];
        today = getAlternativeDate(Calendar.getInstance());
        año = today.get(Calendar.YEAR); spinnerAño.setSelection(añoActual - año); spinnerAño.setEnabled(false);
        mes = today.get(Calendar.MONTH); spinnerMes.setSelection(mes); spinnerMes.setEnabled(false);
        dia = time.get(Calendar.DATE);
        randomDayArray = new ArrayList<>();

        spinnerDia.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                dia = Integer.parseInt((String) spinnerDia.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                time = Calendar.getInstance();
                //dia = time.get(Calendar.DATE);
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
                if(ini)
                    setRandomDayArray(SHORT);
                else
                    setRandomDayArray(LONG);
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

                        if (seconds%2 == 0) updateTime();
                        if (seconds == 5) {
                            //alterLocation();
                            nearestS = nearestStations();
                        }

                        if(nearestS == null)
                            Log.e("Error","Nearest stations error");
                        else {

                            seconds = (seconds + 1) % 10;
                            //tsnm[0,1,2] = values, tsnm[3] = weighted average
                            double weightsDist[] = new double[3];
                            double weightsAlt[] = new double[3];
                            double altDiff[] = new double[3];

                            //alterLocation();

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
                                for(int j=0;j<3;j++) {
                                    temp[3][j] += temp[i][j] * weights[i];
                                }
                            }
                        }

                        avgTemp = temp[3][0] - altitud/180; //Computes average temp for a place
                        osD = temp[3][1];
                        osN = temp[3][2];

                        updateTemperature();
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
        if (Double.isNaN(number)) return "NaN";
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
        final int[] CHILLY = {0, 224, 128};
        final int[] COLD = {0, 128, 0};
        final int[] COOL = {152, 255, 0};
        final int[] COMFORTABLE = {255, 255, 0};
        final int[] WARM = {255, 192, 0};
        final int[] HOT = {255, 128, 0};
        final int[] SWELTERING = {255, 0, 0};

        if (temperatura < 0) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = 255;
        } else if (temperatura < 14) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(255, FRIGID[i], 0, 14, temperatura) + 0.5);
        } else if (temperatura < 32) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(FRIGID[i], FREEZING[i], 14, 32, temperatura) + 0.5);
        } else if (temperatura < 41) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(FREEZING[i], CHILLY[i], 32, 41, temperatura) + 0.5);
        } else if (temperatura < 50) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(CHILLY[i], COLD[i], 41, 50, temperatura) + 0.5);
        } else if (temperatura < 55.4) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(COLD[i], COOL[i], 50, 55.4, temperatura) + 0.5);
        } else if (temperatura < 68) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(COOL[i], COMFORTABLE[i], 55.4, 68, temperatura) + 0.5);
        } else if (temperatura < 80) {
            for (int i = 0; i < rgb.length; i++)
                rgb[i] = (int) (transicion(COMFORTABLE[i], WARM[i], 68, 80, temperatura) + 0.5);
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


        if(altitud != 0) {
            if(diffEnabled())
                main.setBackgroundColor(Color.rgb(rgb[0], rgb[1], rgb[2]));
            else {
                textTemperatura.setBackgroundColor(Color.rgb(rgb[0], rgb[1], rgb[2]));
                main.setBackgroundColor(Color.BLACK);

                if (temperatura > 25 && temperatura < 35 || temperatura > 55 && temperatura < 90)
                    textTemperatura.setTextColor(Color.BLACK);
                else
                    textTemperatura.setTextColor(Color.WHITE);
            }
        } else {
            main.setBackgroundColor(Color.BLACK);
            textTemperatura.setBackgroundColor(Color.WHITE);
            textTemperatura.setTextColor(Color.BLACK);
        }

        for(int i=0; i<texts.length; i++)
            if (temperatura > 25 && temperatura < 35 || temperatura > 60 && temperatura < 90)
                texts[i].setTextColor(Color.BLACK);
            else
                texts[i].setTextColor(Color.WHITE);
    }

    public double transicion(double start1, double end1, double start2, double end2, double value2) {
        //128, 192, 20, 30, 23

        double proporcion = (value2 - start2) / (end2 - start2);

        return start1 + (end1 - start1) * proporcion;
    }

    public double computeTemperature(Calendar today, boolean real, double avgT)
    {
        //Calendar today = Calendar.getInstance();
        Calendar previous = Calendar.getInstance();
        Calendar next = Calendar.getInstance();

        long seed = parseSeed(today);
        //Log.v("Seed Value", String.valueOf(seed));.


        //Random rnd = new Random(seed);
        //double oscilacionAnual = (avgT * -0.6485 + 28.712) * (osD + osN) / 10;
        //double tempMediaAnual = (avgT * 5000 - 29571) / 4117;
        double rawWinter = 8 - (8.1 + 10.2)/2;

        final double WINTER_T = rawWinter;
        final double SUMMER_T = 24.5;

        double oscilacionAnual = (SUMMER_T - WINTER_T);
        double tempMediaAnual = (WINTER_T + SUMMER_T)/2;

        double prevTemp, nextTemp, prevRandom = 0, nextRandom = 0;
        long previousDateMillis = 0, nextDateMillis = 0;
        int index = 0;

        for(TempStamp ts : randomDayArray)
        {
            nextDateMillis = ts.getTimestamp();
            nextRandom = ts.getRandom();
            if(previousDateMillis == 0)
            {
                previousDateMillis = nextDateMillis; //First iteration
                prevRandom = nextRandom;
            }

            if(nextDateMillis > today.getTimeInMillis())
                if(index > 0)
                    break;

            previousDateMillis = nextDateMillis;
            prevRandom = nextRandom;
            index++;
        }
        previous.setTimeInMillis(previousDateMillis);
        next.setTimeInMillis(nextDateMillis);

        if(!real){
            previous = getAlternativeDate(previous);
            next = getAlternativeDate(next);
        }

        /*double dateDebug = today.get(Calendar.DAY_OF_YEAR);
        todayTemp = tempMediaAnual - Math.cos(2*Math.PI*((today.get(Calendar.DAY_OF_YEAR) / 365.25) - 1.0/16)) * oscilacionAnual/2
                + random*3;*/

        NormalDistribution normal = new NormalDistribution(0, 3);
        Double nextNormalRnd;

        nextNormalRnd = normal.inverseCumulativeProbability(prevRandom);
        prevTemp = tempMediaAnual - Math.cos(2 * Math.PI * ((previous.get(Calendar.DAY_OF_YEAR) / 365.25) - 1.0 / 16)) * oscilacionAnual / 2
                + nextNormalRnd;

        //Log.v("Random", nextNormalRnd+" Base: "+prevRandom);

        nextNormalRnd = normal.inverseCumulativeProbability(nextRandom);

        nextTemp = tempMediaAnual - Math.cos(2 * Math.PI * ((next.get(Calendar.DAY_OF_YEAR) / 365.25) - 1.0 / 16)) * oscilacionAnual / 2
                + nextNormalRnd;

        //Log.v("Inverse2: ",normal.inverseCumulativeProbability(nextRandom)+"");

        //Log.v("Forecast","Today "+todayTemp+"\nTomorrow "+tomorrowTemp);

        return transicion(prevTemp, nextTemp, previousDateMillis, nextDateMillis, today.getTimeInMillis());
    }
    //Updates seasonal temperature (Should be updated at least every minute)
    public void updateTemperature() {
        Calendar today = Calendar.getInstance();
        //double avgTemp2 = 14.1;

        if (!ini) today.set(año, mes - 1, dia);

        temperatura = computeTemperature(today, !ini, avgTemp);
        if(report)
        {
            report = false;
            showHourlyReport(today);
        }

        if (ini) today.set(año, mes - 1, dia);

        double actualT;
        EditText thermometerDisplay = (EditText) findViewById(R.id.thermometer);
        EditText actualTDisplay = (EditText) findViewById(R.id.actualT);
        String strThermometer = thermometerDisplay.getText().toString();
        String strActualT = actualTDisplay.getText().toString();
        try {
            actualT = Double.parseDouble(strActualT);
        } catch (NullPointerException | NumberFormatException e) {
            actualT = -1000;
        }

        //If there isn't any value in "Temperatura real" use "Lectura termometro" value.
        if(actualT == -1000) {
            findViewById(R.id.thermoBox).setVisibility(View.VISIBLE);
            try {
                double thermometer = Double.parseDouble(strThermometer);

                //If value given is greater than 40°, °F is assumedff
                if(thermometer > 40)
                    thermometer = (thermometer - 32) * 5 / 9;
                actualT = thermometer - THERMOMETER_DIFF;
            } catch (NullPointerException | NumberFormatException e) {
                actualT = -999;
            }
        } else {
            findViewById(R.id.thermoBox).setVisibility(View.GONE);
        }

        double diff = temperatura - avgTemp;
        double finalTemp = actualT + diff;

        if(centerEnabledCheckBox.isChecked())
            finalTemp = transicion(finalTemp, centerT, 0, 1, closenessToUniversity(finalTemp - centerT));

        if(altitud != 0 && (actualT != -999)) {
            //double alteredT = avgTemp; // Replace by 14.1
            //double actualT = Double.parseDouble(actualTDisplay.getText().toString());

            textDiff.setText(formatNumber(diff, 1) + " °C");

            if(((RadioGroup)findViewById(R.id.radioScale)).getCheckedRadioButtonId() == R.id.celsius)
                textTemperatura.setText(formatNumber(finalTemp,0) + " °C");
            else if(((RadioGroup)findViewById(R.id.radioScale)).getCheckedRadioButtonId() == R.id.raw)
                textTemperatura.setText(formatNumber(finalTemp + THERMOMETER_DIFF,1));
            else
                textTemperatura.setText(formatNumber(finalTemp*1.8 + 32,0) + " °F");
        }
        else {
            textDiff.setText("-- °C");
            if(((RadioGroup)findViewById(R.id.radioScale)).getCheckedRadioButtonId() == R.id.celsius)
                textTemperatura.setText("-- °C");
            else if(((RadioGroup)findViewById(R.id.radioScale)).getCheckedRadioButtonId() == R.id.raw)
                textTemperatura.setText("Lo");
            else
                textTemperatura.setText("-- °F");
        }

        if(diffEnabled())
            colorear(temperatura * 1.8 + 32);
        else
            colorear(finalTemp * 1.8 + 32);
    }

    public static long parseSeed(Calendar time)
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

        String binary = Long.toBinaryString(seed);

        //System.out.println(binary);
        seed = CBC(binary);
        //seed = Long.parseLong(binary,2);

        return seed;
    }

    public static long CBC(String s)
    {
        ArrayList<Short> M = new ArrayList<>();
        ArrayList<Short> C = new ArrayList<>();

        String m = "";
        for (int i=s.length() - 1; i >= 0; i--)
        {
            m += s.charAt(i);
            if ((s.length() - i)%4 == 0 || i == 0)
            {
                M.add(Short.parseShort(m,2));
                m = "";
            }
        }

        C.add((short)0b1100);
        for (int i=0; i < M.size(); i++){
            C.add(Epi((short)(C.get(i)^M.get(i))));
        }

        String binary = "";
        for (Short c : C)
        {
            binary += Integer.toBinaryString(c);
        }

        return Long.parseLong(binary,2);
    }

    public static short Epi(short nibble)
    {
        boolean odd = nibble > 7;
        nibble = (short)(nibble << 1);
        return odd? ++nibble : nibble;
    }

    public boolean añoBisiesto(int año){
        return año%4 == 0 && (año%100 != 0 || año%400 == 0);
    }

    public Calendar getAlternativeDate(Calendar date){
        final long millisInDay = 86400000;

        //date = resetToMidnight(date);
        //date.add(Calendar.MINUTE, 1);
        //Calendar now = Calendar.getInstance();

        Calendar y1935 = Calendar.getInstance();
        y1935.set(1935,0,0);

        Calendar y2012 = Calendar.getInstance();
        y2012.set(2012,6,29);

        //long millis1935 = y1935.getTimeInMillis();
        long millis2012 = y2012.getTimeInMillis();

        //System.out.println(millis1935/millisInDay+", "+millis2012/millisInDay);

        long millis = date.getTimeInMillis() - millis2012;
        int days = (int)((365.25/28)*millis/millisInDay);
        //Log.v("Millis: ",date.getTimeInMillis()+"");

        Calendar returnDate = (Calendar) y1935.clone();
        /*returnDate.add(Calendar.YEAR, years);
        millis = ((returnDate.getTimeInMillis() + millisInDay/2)/millisInDay)*millisInDay;
        returnDate.setTimeInMillis(millis);
        */
        //Log.v("Datebase",printDate(returnDate));
        returnDate.add(Calendar.DAY_OF_YEAR, days);
        if(date.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY && (
                returnDate.get(Calendar.MONTH) == Calendar.APRIL ||
                returnDate.get(Calendar.MONTH) == Calendar.DECEMBER))
            returnDate.add(Calendar.DAY_OF_YEAR, 10);
        //Log.v("New date",printDate(returnDate));
        while(date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && returnDate.get(Calendar.MONTH)%3 == 0)
            returnDate.add(Calendar.DAY_OF_YEAR, -1);
        int periodo = 0;
        periodo += 7*(returnDate.get(Calendar.MONTH)/3);
        int diasem = (date.get(Calendar.DAY_OF_WEEK) + 5)%7;
        periodo += diasem;

        return randomizeDate(periodo, returnDate.get(Calendar.YEAR), date.get(Calendar.DAY_OF_MONTH));
    }

    public Calendar randomizeDate(int periodo, int año, int dayOfMonth){

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

        //int addDebug = (int)(diasem*duracion/7.0);
        date.add(Calendar.DAY_OF_YEAR, (int)(diasem*duracion/7.0));

        //Calculates seed
        long seed = parseSeed(date);
        Random rnd = new Random(seed);

        //Calculates this date limit. (E.g Limit for a Sunday is the last day of Mar, Jun, Sep and Dec)
        Calendar dateAfter = (Calendar) date.clone();
        dateAfter.add(Calendar.DAY_OF_YEAR, (int)(duracion/7.0));

        Calendar d = (Calendar) date.clone();
        int i=0;

        //If month date equals actual month date, return current date of that month
        while(d.get(Calendar.DAY_OF_MONTH) != dateAfter.get(Calendar.DAY_OF_MONTH) && i < 10000)
        {
            //Calendar tomorrow = Calendar.getInstance();
            // (isTomorrow) tomorrow.add(Calendar.DAY_OF_YEAR,1);
            if(d.get(Calendar.DAY_OF_MONTH) == dayOfMonth)
                return d;

            d.add(Calendar.DAY_OF_YEAR,1);
            i++;
        }

        date.add(Calendar.DAY_OF_YEAR,(int)(rnd.nextDouble()*duracion/7.0));
        date = resetToMidnight(date);

        return date;
    }

    //Returns in which period is a date of the 28 days period.
    public int getPeriod(Calendar fictionalDate, Calendar realDate)
    {
        int periodo = 0;
        periodo += 7*(fictionalDate.get(Calendar.MONTH)/3);
        int diasem = (realDate.get(Calendar.DAY_OF_WEEK) + 5)%7;
        periodo += diasem;
        return periodo;
    }

    public void enableDatePicker(View v)
    {
        ini = false;
        spinnerDia.setEnabled(true);
        spinnerMes.setEnabled(true);
        spinnerAño.setEnabled(true);
        setRandomDayArray(LONG);
    }

    public void showHourlyReport(View v)
    {
        report = true;
    }

    public void selectorEscala(View v) {
        RadioGroup escala = (RadioGroup)findViewById(R.id.radioScale);
        switch (escala.getCheckedRadioButtonId()) {
            case R.id.celsius:
                textTemperatura.setText(formatNumber(temperatura, 0) + "°C");
                break;
            case R.id.raw:
                textTemperatura.setText(formatNumber(temperatura + THERMOMETER_DIFF, 1));
                break;
            case R.id.fahrenheit:
                textTemperatura.setText(formatNumber(temperatura * 1.8 + 32, 0) + "°F");
                break;
        }
    }

    public void checkDiff(View v)
    {
        LinearLayout diff = (LinearLayout)findViewById(R.id.diffDisplay);
        LinearLayout tDisplay = (LinearLayout)findViewById(R.id.temperatureDisplay);
        if(((CheckBox)v).isChecked())
        {
            diff.setVisibility(View.VISIBLE);
            tDisplay.setVisibility(View.GONE);
        } else {
            diff.setVisibility(View.GONE);
            tDisplay.setVisibility(View.VISIBLE);
        }
    }

    public void showHourlyReport(Calendar date)
    {
        Log.v("Showing","...");
        int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        nearestS = nearestStations();
        //hora = 23;
        StringBuilder text = new StringBuilder();

        date = resetToMidnight(date);

        //double avgTemp2 = 14.1;

        for(int i=0; i<hora+1; i++)
        {
            double t = computeTemperature(date, !ini, avgTemp);
            //Log.v("Temperature computed for: "+i+"h",t+"");
            date.add(Calendar.HOUR_OF_DAY, 1);
            text.append(i + ":00 "+ formatNumber(t - avgTemp,1) +" °C\n");
        }
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }

    public boolean diffEnabled()
    {
        return ((CheckBox) findViewById(R.id.diffCheck)).isChecked();
    }

    public void setRandomDayArray(int mode)
    {
        //HashMap randomDayArray = null; // Null because it must be initialized
        randomDayArray.clear();
        int period;
        Calendar c = Calendar.getInstance();
        Calendar ad = Calendar.getInstance();
        if(mode == LONG)
            c.set(año, 0, 1, 0, 0, 0); //Set Jan 1st/year
        if(mode == SHORT) {
            ad = getAlternativeDate(c); //Alternative Date
            period = getPeriod(ad, c);
            Log.v("Period",period+"");
            c.add(Calendar.DAY_OF_YEAR, -period);
            c = resetToMidnight(c);
            //c.add(Calendar.MINUTE, 1);
        }
        Calendar oneYearLater = null;
        oneYearLater = (Calendar) c.clone();
        if(mode == LONG) {
            Log.v("Now: ",printDate(oneYearLater));
            oneYearLater.add(Calendar.YEAR, 1); //Set variable for one year timespan
            Log.v("One year later: ",printDate(oneYearLater));
        } else if (mode == SHORT) {
            oneYearLater.add(Calendar.DAY_OF_YEAR, 28);
        }
        oneYearLater.add(Calendar.DAY_OF_YEAR, -1); //Avoids big jumps of random variable in December
        Random rnd = new Random(parseSeed(c));
        double random = rnd.nextDouble(); //First use of seeded Random instance.
        //if(mode == SHORT) randomDayArray.add(new TempStamp(ad.getTimeInMillis(), random));
        //else if (mode == LONG)
        randomDayArray.add(new TempStamp(c.getTimeInMillis(), random));

        Log.v("Date","\t"+printDate(c)+"\t"+Double.toString(random).replace(".",","));

        do {
            int millis = (int)((rnd.nextDouble()*1.9+0.1)*24*3600*1000); //Random timespan selection between 0.1 - 2 days.
            //Log.v("Millis: ",millis+"");
            c.add(Calendar.MILLISECOND, millis);
            if(c.getTimeInMillis() >= oneYearLater.getTimeInMillis())
                break;

            double fuzziness = 0.3*millis/(1000*3600*24);
            double nextRandom = 0;

            do {
                nextRandom = random + (rnd.nextDouble() * fuzziness * 2 - fuzziness);
                //Log.v("Fuzziness", fuzziness+"");
            } while (nextRandom < 0 || nextRandom > 1);
            random = nextRandom;

            randomDayArray.add(new TempStamp(c.getTimeInMillis(), random));

            Log.v("Date","\t"+printDate(c)+"\t("+printDate(this.getAlternativeDate(c))+")\t"+Double.toString(random).replace(".",","));
        } while(true);

        oneYearLater.add(Calendar.DAY_OF_YEAR, 1);
        rnd.setSeed(parseSeed(oneYearLater));
        random = rnd.nextDouble();
        randomDayArray.add(new TempStamp(oneYearLater.getTimeInMillis(), random));
        Log.v("Date","\t"+printDate(c)+"\t"+Double.toString(random).replace(".",","));
    }

    public String printDate(Calendar c)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm"); //Debug purposes
        return sdf.format(c.getTime());
    }

    public Calendar resetToMidnight(Calendar c)
    {
        int y, m, d;
        y = c.get(Calendar.YEAR);
        m = c.get(Calendar.MONTH);
        d = c.get(Calendar.DAY_OF_MONTH);
        c.set(y,m,d,0,0,0);
        return c;
    }

    public void viewStations(View v)
    {
        String text = formatNumber((28.234 - temperatura)*180,1) + '\n';

        for(int i=0;i<3;i++)
        {
            try {
                text += nearestS[i].toString();
                text += ": ";
                text += formatNumber(weights[i] * 100, 0) + "%";
                text += '\n';
            } catch (NullPointerException e)
            {
                text = "Cargando...";
            }
        }

        Toast toast = Toast.makeText(this,text,Toast.LENGTH_SHORT);
        toast.show();
    }

    public void editCenterData(View v)
    {
        Intent intent = new Intent(this, CenterActivity.class);
        startActivity(intent);
    }

    public double closenessToUniversity(double difference)
    {
        //final double LAT = 4.6349, LONG = -74.0803; //Entrace by 45th street
        final double LAT = 4.7017, LONG = -74.0518; //ITAC
        float results[] = new float[2];
        Location.distanceBetween(latitud, longitud, LAT, LONG, results);
        double limit = 17000;

        if (Math.abs(difference*1000) < limit) limit = Math.abs(difference*1000);

        double distance = 0;

        if(results[0] > limit) distance = 0;
        else if(Math.abs(difference*1000) < limit/2) {
            distance = (limit*2 - results[0])/(limit*2);
        }
        else distance = (limit - results[0])/limit;

        Log.v("Closeness to UN", formatNumber(distance*100, 0) + "%");
        return distance;
    }

    /*
    public void alterLocation(){
        //hora = 6 + 0.0/60;
        //5.8676238,-72.9903201
        latitud = 4.6129;
        longitud = -74.201;
        altitud = 2541;
        avgTemp = 14.1;
        nearestS = nearestStations();
    }
    */

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

            double a = loc.getAltitude();
            double lat = loc.getLatitude();
            double lon = loc.getLongitude();

            if (a != 0) altitud = loc.getAltitude();
            if (lat != 0) latitud = loc.getLatitude();
            if (lon !=0) longitud = loc.getLongitude();
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
