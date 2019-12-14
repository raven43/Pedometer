package ed.doron.pedometer.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.core.cartesian.series.Line;
import com.anychart.data.Mapping;
import com.anychart.data.Set;
import com.anychart.enums.Anchor;
import com.anychart.enums.MarkerType;
import com.anychart.enums.TooltipPositionMode;
import com.anychart.graphics.vector.Stroke;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ed.doron.pedometer.R;
import ed.doron.pedometer.data.PedometerViewModel;
import ed.doron.pedometer.models.DayResult;

public class DiagramFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private View view;
    private PedometerViewModel pedometerViewModel;
    private SwipeRefreshLayout swipeRefreshLayout;

    public static DiagramFragment newInstance() {
        return new DiagramFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.diagram_fragment, container, false);
        pedometerViewModel = ViewModelProviders.of(this).get(PedometerViewModel.class);
        setupSwipeRefreshListener();
        setupLineChart();
        return view;
    }

    private void setupSwipeRefreshListener() {
        swipeRefreshLayout = view.findViewById(R.id.diagram_swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(this);
    }

    private void setupLineChart() {
        AnyChartView anyChartView = view.findViewById(R.id.diagram_any_chart_view);
        anyChartView.setProgressBar(view.findViewById(R.id.diagram_progress_bar));

        Cartesian cartesian = AnyChart.line();

        cartesian.animation(true);

       // cartesian.padding();

/*        cartesian.crosshair().enabled(true);
        cartesian.crosshair()
                .yLabel(true)
                .yStroke((Stroke) null, 1, null, (String) null, (String) null);*/
        cartesian.tooltip().positionMode(TooltipPositionMode.POINT);

        cartesian.title(this.getString(R.string.last_30_days_statistics));
        cartesian.xAxis(0).labels().padding(5d, 5d, 5d, 5d);

        List<DataEntry> seriesData = getDataEntry();

        Set set = Set.instantiate();
        set.data(seriesData);
        Mapping series1Mapping = set.mapAs("{ x: 'x', value: 'value' }");

        Line series1 = cartesian.line(series1Mapping);
        series1.name("Steps");
        series1.hovered().markers().enabled(true);
        series1.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(4d);
        series1.tooltip()
                .position("right")
                .anchor(Anchor.LEFT_CENTER)
                .offsetX(5d)
                .offsetY(5d);


        cartesian.legend().enabled(true);
        cartesian.legend().fontSize(14d);
        cartesian.legend().padding(0d, 0d, 10d, 0d);

        anyChartView.setChart(cartesian);
    }

    @Override
    public void onRefresh() {
        setupLineChart();
        swipeRefreshLayout.setRefreshing(false);
    }

    private ArrayList<DataEntry> getDataEntry() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("mm.ss", Locale.ENGLISH);
        ArrayList<DataEntry> seriesData = new ArrayList<>();
        ArrayList<DayResult> results = (ArrayList<DayResult>) pedometerViewModel.getAllResults();
        int size = results.size();
        int i = (size < 30) ? size : 30;
        for (; i > 0; i--) {
            Log.d("myLogs", dateFormat.format(new Date(results.get(size - i).getTime())) + results.get(size - i).getStepCount());
            seriesData.add(new ValueDataEntry(dateFormat.format(new Date(results.get(size - i).getTime())), results.get(size - i).getStepCount()));
        }
/*        seriesData.add(new ValueDataEntry("1987", 100));
        seriesData.add(new ValueDataEntry("1988", 200));
        seriesData.add(new ValueDataEntry("1989", 300));
        seriesData.add(new ValueDataEntry("1990", 450));
        seriesData.add(new ValueDataEntry("1991", 10));*/

        return seriesData;
    }

    private class CustomDataEntry extends ValueDataEntry {

        CustomDataEntry(String x, Number value, Number value2, Number value3) {
            super(x, value);
            setValue("value2", value2);
            setValue("value3", value3);
        }

    }
}
