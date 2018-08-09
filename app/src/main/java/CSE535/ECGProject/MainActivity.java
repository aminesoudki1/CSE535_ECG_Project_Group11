package CSE535.ECGProject;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import mobilecomputing.ecgproject.R;

import static java.lang.StrictMath.max;
import static java.lang.StrictMath.round;

public class MainActivity extends AppCompatActivity {
    String[] values;
    int frequency = 250;
    int sample;
    int[] value;
    int[] heartRate;
    int minute_samples;
    double[] index;
    double[] peaks;
    boolean rgflag;
    GraphView graph;
    private RadioButton p1radio;
    private RadioButton p2radio;
    private RadioButton p3radio;
    private RadioButton p4radio;
    private Button detectButton;
    private TextView tv_status;
    private RadioGroup rg;
    private String file_name = "";
    private int lines = 0;
    private long startTime = 0;
    private TextView et;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        p1radio = (RadioButton) findViewById(R.id.pat1_radio);
        p2radio = (RadioButton) findViewById(R.id.pat2_radio);
        p3radio = (RadioButton) findViewById(R.id.pat3_radio);
        p4radio = (RadioButton) findViewById(R.id.pat4_radio);
        detectButton = (Button) findViewById(R.id.detectbtn);
        graph = (GraphView) findViewById(R.id.graph);
        tv_status = (TextView) findViewById(R.id.tv_status);
        rg = (RadioGroup) findViewById(R.id.rg);
        et = (TextView) findViewById(R.id.et1);

        sample = (int) (250 * 1.2);

        detectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detectButton.setClickable(false);
                startTime = System.nanoTime();
                rgflag = Radiobutton_selection();
                if (rgflag == true) {
                    if (p1radio.isChecked()) {
                        file_name = "p1.csv";
                    } else if (p2radio.isChecked()) {
                        file_name = "p2.csv";
                    } else if (p3radio.isChecked()) {
                        file_name = "p3.csv";
                    } else if (p4radio.isChecked()) {
                        file_name = "p4.csv";
                    }
                    try{
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(getAssets().open(file_name)));
                        lines = 0;
                        while (reader.readLine() != null) lines++;
                        reader.close();

                        BufferedReader br = new BufferedReader(
                                new InputStreamReader(getAssets().open(file_name)));
                        String line;
                        int i=0;
                        value = new int[lines];
                        peaks = new double[lines];
                        index = new double[lines];
                        while((line = br.readLine())!= null){
                            //value[i] = Integer.parseInt(line);
                            String[] split = line.split("\\s+");
                            index[i] = Double.parseDouble(split[0]);
                            peaks[i] = Double.parseDouble(split[1]);
                            i++;
                        }

                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                   // Bradycardia();
                    calculateHeartRate();
                }
                else {
                    Toast.makeText(getApplicationContext(),
                            "Please select a patient...", Toast.LENGTH_LONG).show();
                }
                detectButton.setClickable(true);
            }
        });
    }

    public boolean Radiobutton_selection() {
        RadioButton rb = ((RadioButton) findViewById(rg.getCheckedRadioButtonId()));
        if (rb == null) {
            return false;
        } else {
            return true;
        }
    }
    double[] bpm;
    public void calculateHeartRate() {
        /*
        %%calculating heart rate from RR method
for i=2:length(peaks)
    interval = pindex(i)-pindex(i-1);
    r = interval*0.004;
    rr = 1/r;
    bpm(i-1) = rr*60;
end
         */
        long startTime = System.currentTimeMillis();
        bpm = new double[lines];
        for(int i=1;i<peaks.length;i++) {
            double interval = index[i] - index[i-1];
            double r = interval*0.004;
            double rr = 0;
            if(r!=0) {
                rr = 1/r;
                bpm[i] = rr*60;
            } else {
                bpm[i] = 500;
            }

            Log.d("msg",bpm[i]+" ");
        }

        //clean heart rate

        Map<Double,Double> heartRate = new TreeMap<>();
        for(int i=0;i<bpm.length;i++){
            if(bpm[i]>=0 &&bpm[i]<=300) {
                heartRate.put(index[i],bpm[i]);
            }
        }
        ArrayList<Double> heartR = new ArrayList<>(heartRate.values());

        ArrayList<Double> time = new ArrayList<>(heartRate.keySet());

        double average  = 0;
        for(int i=0;i<heartR.size();i++) {
            average = average  + heartR.get(i);
        }
        average = average / heartR.size();
        LineGraphSeries k = new LineGraphSeries();


        k.setColor(Color.RED);
        Iterator it = heartRate.entrySet().iterator();
        LineGraphSeries<DataPoint> c = new LineGraphSeries<>();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            c.appendData(new DataPoint(Double.parseDouble(pair.getKey().toString())/(frequency*60),
                    Double.parseDouble(pair.getValue().toString())),true,999999);
            k.appendData(new DataPoint(Double.parseDouble(pair.getKey().toString())/(frequency*60),
                    average),true,999999);
        }

        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);
        graph.addSeries(c);
        graph.addSeries(k);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(400);
        graph.getViewport().setMaxX(Double.parseDouble(heartRate.keySet().toArray()[heartRate.size()-1].toString())/(frequency*60));
        boolean seq = true;
       ArrayList<Integer> indexes = new ArrayList<>();
        for(int i=0;i<heartR.size();i++) {
            for (int j=i+1;j<heartR.size();j++) {
                if(heartR.get(j) < 60 && seq) {

                } else {
                    seq = false;
                }
                if(!seq) {
                    indexes.add(i);
                    indexes.add(j-1);
                    i=j+1;
                    seq = true;
                    break;
                }
            }
        }


        double timeUnder60 = 0;
        for(int i=0;i<indexes.size()-1;i++) {
            timeUnder60 = timeUnder60  + (index[indexes.get(i+1)]-index[indexes.get(i)])*0.004/60;

        }
        double totalTime = (index[index.length-1] -0 )*0.004/60;
        double percentageUnder60 = timeUnder60/totalTime* 100;
        Log.e("msg",percentageUnder60+" ");





        long difference = System.currentTimeMillis() - startTime;
        tv_status.setText("Total Time : " + round(totalTime)+"\nTime With Heart Rate Under 60 bpm : " + round(timeUnder60) +  "\n" +
                "Percentage : " + round(percentageUnder60) +"%" +"\nAverage Heart Rate : " + round(average) + "\nExecution Time : " + difference + " msec");

    }


    /*public void Bradycardia(){
        int brc_count =0;
        boolean Flag = false;
        int max_bc =0;
        int cur_bc =0;
        int j=0;
        int arr_ind=0;
        int multiplier = 60* frequency;
        heartRate = new int[lines-1];

        for(int i = 0; i< value.length-1; i++){
                heartRate[i] = (int) (multiplier / (value[i + 1] - value[i]));
        }
        for(int i = 0; i< heartRate.length; i++){
            if(heartRate[i]< 60){
                brc_count = brc_count+1;
                if(Flag == false){
                    cur_bc = 1;
                    Flag = true;
                }
                else{
                    cur_bc = cur_bc + 1;
                }
                max_bc = max(max_bc, cur_bc);
            }
            else
                Flag = false;
        }
        int count = value.length;
        minute_samples = 2*60* frequency;
        int[] avg_hr = new int[lines-1];
        int hr=0;
        for(int i=0; i<count; i++) {
            j = i + 1;
            while (j < count && (value[j] - value[i]) < minute_samples) {
                j = j + 1;
            }
            if (j < count) {
                    for (int k = i; k <= j - 1; k++) {
                        hr = hr + heartRate[k];
                    }
                    hr = hr / (j - 1 - i);
                    avg_hr[arr_ind] = hr;
                    arr_ind++;
                }
            }
        int fp = 0;
        int fn = 0;
        int tp = 0;
        int tn = 0;
        for(int x=0;x<avg_hr.length;x++){
            if(heartRate[x] < 60 && avg_hr[x] < 60)
                tp++;
            else if(heartRate[x] >= 60 && avg_hr[x] >= 60)
                tn++;
            else if(heartRate[x] < 60 && avg_hr[x] >= 60)
                fn++;
            else if(heartRate[x] >= 60 && avg_hr[x] < 60)
                fp++;
        }
        int total = avg_hr.length;
        Arrays.sort(avg_hr);
        j = 0;
        int i = avg_hr[j];
        while(i==0){
            i = avg_hr[++j];
        }
        long endTime = System.nanoTime();
        long duration = (endTime - startTime)/1000000;
        String txt = "";
        if(i < 60) {
            txt = "Patient is affected by Bradycardia!\nExecution Time(in ms) is: "
                    +String.valueOf(duration)+"\nMin Avg 2-min heartRate:"+String.valueOf(i);
            txt += "\nTotal heartRate count: "+total+"\nTrue Positives: "+tp+
                    "\nTrue Negatives: "+tn+"\nFalse Posivites: "+fp+"\nFalse Negatives: "+fn;
            Toast.makeText(getApplicationContext(), txt, Toast.LENGTH_LONG).show();
        }
        else{
            txt = "Patient is NOT affected by Bradycardia!\nExecution Time(in ms) is: "+
                    String.valueOf(duration)+"\nMin Avg 2-min heartRate:"+String.valueOf(i);
            txt += "\nTotal heartRate count: "+total+"\nTrue Positives: "+tp+
                    "\nTrue Negatives: "+tn+"\nFalse Posivites: "+fp+"\nFalse Negatives: "+fn;
            Toast.makeText(getApplicationContext(), txt, Toast.LENGTH_LONG).show();
        }
        et.setText(txt);
    }*/

}
