package com.erm.erm_debug;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Juan José on 10-01-2017.
 */

public class SamplesManager extends ActionBarActivity {

    List<Map<String, String>> samplesList = new ArrayList<>();
    List<Map<String, String>> matchedSamples = new ArrayList<>();


    public void defaultList() {
        Map<String, String> map = new HashMap<>();
        map.put("value", "25.3");
        samplesList.add(map);
    }


    // Retorna el tamaño de la lista de muestras.
    Integer getSizeOfSamples(){
        return samplesList.size();
    }

    // Retorna el tamaño de la lista de muestras.
    Integer getSizeOfMatchedSamples() {
        return matchedSamples.size();
    }

    List getMatchedSamples(){
        return matchedSamples;
    }

    public void makeMatch(List coordenadas) throws ParseException {
        //matchedSamples = coordenadas;
        System.out.println("makematch" + String.valueOf(samplesList.size()));

        for (int i = 1; i < samplesList.size(); i++)
        {
            Map<String, String> sample = samplesList.get(i);
            //System.out.println("SAMPLE: " + String.valueOf(sample.get("datetime")));

            // Transformando datetime a objeto
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            c.setTime(df.parse(String.valueOf(sample.get("datetime"))));
            String formattedDate = df.format(c.getTime());
            System.out.println("FORMATED: " + formattedDate);
            //System.out.println(String.valueOf(c.getTimeInMillis()));



            for (int j = 0; j < coordenadas.size(); j++)
            {
                Map<String, String> coordenada = (Map<String, String>) coordenadas.get(j);
                //System.out.println("coordenada:" + String.valueOf(coordenada.get("datetime")));

                Calendar cc = Calendar.getInstance();
                SimpleDateFormat dfc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                cc.setTime(dfc.parse(String.valueOf(coordenada.get("datetime"))));
                String formattedDatec = dfc.format(cc.getTime());
                System.out.println("FORMATED CHORD: " + formattedDatec);

                //System.out.println(String.valueOf(cc.getTimeInMillis()));

                long diff = Math.abs(c.getTimeInMillis() - cc.getTimeInMillis())/1000;
                System.out.println(String.valueOf(diff));
                //System.out.println(menorDiferencia);

                if(diff <= 15) {
                    Map<String, String> matched = new HashMap<>();
                    matched.put("value", sample.get("value"));
                    matched.put("datetime", sample.get("datetime"));
                    matched.put("lat", coordenada.get("lat"));
                    matched.put("lng", coordenada.get("lng"));
                    matchedSamples.add(matched);
                    System.out.println("MATCHED: " + String.valueOf(matched));

                }



            }


        }






    }

    public void readSamplesFile(AssetManager assets) throws IOException {

        InputStream is = assets.open("samples.csv");
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
                map.put("value", String.valueOf(RowData[0]));
                map.put("timestampt", String.valueOf(RowData[1]));
                map.put("datetime", String.valueOf(RowData[2]));

                samplesList.add(map);
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                is.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }










}
