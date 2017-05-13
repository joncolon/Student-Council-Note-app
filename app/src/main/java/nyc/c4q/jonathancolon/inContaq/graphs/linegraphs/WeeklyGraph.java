package nyc.c4q.jonathancolon.inContaq.graphs.linegraphs;

import android.content.Context;
import android.view.animation.BounceInterpolator;

import com.db.chart.Tools;
import com.db.chart.animation.Animation;
import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import nyc.c4q.jonathancolon.inContaq.R;
import nyc.c4q.jonathancolon.inContaq.data.asynctasks.WeeklyReceivedWorkerTask;
import nyc.c4q.jonathancolon.inContaq.data.asynctasks.WeeklySentWorkerTask;
import nyc.c4q.jonathancolon.inContaq.data.asynctasks.params.WeeklyTaskParams;

import static android.graphics.Color.parseColor;
import static com.db.chart.renderer.AxisRenderer.LabelPosition.NONE;
import static com.db.chart.renderer.AxisRenderer.LabelPosition.OUTSIDE;

public class WeeklyGraph {

    private static final String SENT_COLOR = "#EF7674";
    private static final String LABEL_COLOR = "#FDFFFC";
    private static final String RECEIVED_COLOR = "#FDFFFC";

    private static final int SUN = 1;
    private static final int MON = 2;
    private static final int TUE = 3;
    private static final int WED = 4;
    private static final int THUR = 5;
    private static final int FRI = 6;
    private static final int SAT = 7;
    private static final int DEFAULT_VALUE = 0;
    private Context context;
    private int highestValue;
    private LineChartView lineGraph;
    private float[] receivedValues;
    private float[] sentValues;
    private String phoneNumber;

    public WeeklyGraph(Context context, LineChartView lineGraph, String phoneNumber) {
        this.context = context;
        this.lineGraph = lineGraph;
        this.phoneNumber = phoneNumber;
    }

    public void showWeeklyGraph() {
        getLineGraphValues(phoneNumber);
        getHigehstValueForYaxis();
        loadGraph();
    }

    private void getLineGraphValues(String phoneNumber) {
        receivedValues = setValues(getWeeklyReceived(phoneNumber));
        sentValues = setValues(getWeeklySent(phoneNumber));
    }

    private float[] setValues(TreeMap<Integer, Integer> numberOfTexts) {

        ArrayList<Float> list = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : numberOfTexts.entrySet()) {

            Float value = entry.getValue().floatValue();
            list.add(value);
        }
        return convertFloats(list);
    }

    private TreeMap<Integer, Integer> getWeeklyReceived(String phoneNumber) {

        TreeMap<Integer, Integer> weeklyReceived = setUpWeeklyTextMap();
        WeeklyTaskParams receivedWeeklyParams = new WeeklyTaskParams(phoneNumber, weeklyReceived);
        WeeklyReceivedWorkerTask weeklyReceivedWorkerTaskTask = new WeeklyReceivedWorkerTask();

        try {
            weeklyReceived = weeklyReceivedWorkerTaskTask.execute(receivedWeeklyParams).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return weeklyReceived;
    }

    private TreeMap<Integer, Integer> getWeeklySent(String phoneNumber) {

        TreeMap<Integer, Integer> weeklySent = setUpWeeklyTextMap();
        WeeklyTaskParams sentWeeklyParams = new WeeklyTaskParams(phoneNumber, weeklySent);
        WeeklySentWorkerTask weeklySentWorkerTaskWorkerTask = new WeeklySentWorkerTask();

        try {
            weeklySent = weeklySentWorkerTaskWorkerTask.execute(sentWeeklyParams).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return weeklySent;
    }

    // This converts the generic "Float" to primitive float because the graphs only uses primitives
    private float[] convertFloats(List<Float> floats) {

        float[] ret = new float[floats.size()];
        Iterator<Float> iterator = floats.iterator();
        for (int i = 0; i < ret.length; i++) {
            ret[i] = iterator.next().intValue();
        }
        return ret;
    }

    private TreeMap <Integer, Integer> setUpWeeklyTextMap() {

        TreeMap<Integer, Integer> weeklyMap = new TreeMap<>();

        weeklyMap.put(SUN, DEFAULT_VALUE);
        weeklyMap.put(MON, DEFAULT_VALUE);
        weeklyMap.put(TUE, DEFAULT_VALUE);
        weeklyMap.put(WED, DEFAULT_VALUE);
        weeklyMap.put(THUR, DEFAULT_VALUE);
        weeklyMap.put(FRI, DEFAULT_VALUE);
        weeklyMap.put(SAT, DEFAULT_VALUE);

        return weeklyMap;
    }

    private void getHigehstValueForYaxis() {
        highestValue = getYValue(sentValues, receivedValues);
    }

    synchronized private int getYValue(float[] sentValues, float[] receivedValues) {
        int maxSent = findMaximumValue(sentValues);
        int maxReceived = findMaximumValue(receivedValues);
        if (maxSent > maxReceived) {
            return highestValue = (int) getRound(maxSent);
        }
        return highestValue = (int) getRound(maxReceived);
    }

    private static int findMaximumValue(float[] inputArray) {

        float maxValue = inputArray[0];

        for (int i = 1; i < inputArray.length; i++) {

            if (inputArray[i] > maxValue) {
                maxValue = Math.round(inputArray[i]);
            }
        }
        return (int) maxValue;
    }

    private long getRound(int input) {
        return Math.round(input * 1.25);
    }

    synchronized private void loadGraph() {
        setGraphData();
        setGraphAttributes();
        animateGraph();
    }

    private void setGraphData() {

        final String[] xAxisLabels = {
                context.getString(R.string.sun),
                context.getString(R.string.mon),
                context.getString(R.string.tue),
                context.getString(R.string.wed),
                context.getString(R.string.thu),
                context.getString(R.string.fri),
                context.getString(R.string.sat)
        };

        LineSet receivedValueDataSet = new LineSet(xAxisLabels, receivedValues);
        receivedValueDataSet.setColor(parseColor(RECEIVED_COLOR))
                .setDotsColor(parseColor(RECEIVED_COLOR))
                .setThickness(4)
                .beginAt(0);
        lineGraph.addData(receivedValueDataSet);

        LineSet sentValueDataSet = new LineSet(xAxisLabels, sentValues);
        sentValueDataSet.setColor(parseColor(SENT_COLOR))
                .setDotsColor(parseColor(SENT_COLOR))
                .setDashed(new float[]{1f, 1f})
                .setThickness(4)
                .beginAt(0);
        lineGraph.addData(sentValueDataSet);
    }

    private void setGraphAttributes() {

        setHighestValueTo100();

        lineGraph.setBorderSpacing(Tools.fromDpToPx(2))
                .setAxisBorderValues(0, highestValue)
                .setYLabels(NONE)
                .setXLabels(OUTSIDE)
                .setFontSize(24)
                .setAxisLabelsSpacing(15f)
                .setLabelsColor(parseColor(LABEL_COLOR))
                .setXAxis(false)
                .setYAxis(false);
    }

    private void animateGraph() {

        Animation anim = new Animation().setEasing(new BounceInterpolator());
        lineGraph.show(anim);
    }

    private void setHighestValueTo100() {

        if (highestValue == 0) {
            highestValue = 100;
        }
    }
}
