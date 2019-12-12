package ed.doron.pedometer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.MutableLiveData;

import ed.doron.pedometer.Sensor.StepDetector;

public class StepCounterService extends Service implements SensorEventListener, StepListener {

    // Notifications
    private static final int NOTIFY_ID = 42;
    private static String CHANNEL_ID = "Pedometer step channel";

    private NotificationCompat.Builder builder;
    private NotificationManagerCompat notificationManager;

    private StepDetector stepDetector;
    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    public MutableLiveData<Integer> stepCount;

    private IBinder binder;

    // private PeriodicWorkRequest updater;


    @Override
    public void onCreate() {
        Log.d("myLogs", "service started");
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
        binder = new LocalBinder();
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometer;
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        }
        this.stepDetector = new StepDetector();
        this.stepDetector.registerListener(this);

        this.stepCount = new MutableLiveData<>(Preferences.getStepCount(StepCounterService.this));


        builder = new NotificationCompat.Builder(StepCounterService.this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.step_count))
                .setContentText(String.format(getString(R.string.current_steps), this.stepCount.getValue()))
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        Notification notification;

        notification = builder.build();
        notificationManager = NotificationManagerCompat.from(StepCounterService.this);
        notificationManager.notify(NOTIFY_ID, notification);

        startForeground(NOTIFY_ID, notification);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            stepDetector.updateAccelerometer(
                    sensorEvent.timestamp, sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void updateNotification() {
        builder.setContentText(String.format(getString(R.string.current_steps), this.stepCount.getValue()));
        notificationManager.notify(NOTIFY_ID, builder.build());
    }

    @Override
    public void step(long count) {
        stepCount.setValue(stepCount.getValue() + 1);
        Log.d("tag", TEXT_NUM_STEPS + this.stepCount);
        updateNotification();

    }

    @Override
    public void onDestroy() {
        Log.d("tag", "onDestroy");
        Preferences.setStepCount(StepCounterService.this, this.stepCount.getValue());
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d("tag", "onTaskRemoved");
        Preferences.setStepCount(StepCounterService.this, this.stepCount.getValue());
        super.onTaskRemoved(rootIntent);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null)
                notificationManager.createNotificationChannel(channel);
        }
    }

    class LocalBinder extends Binder {
        StepCounterService getService() {
            // Return this instance of StepCounter so clients can call public methods
            return StepCounterService.this;
        }
    }
}


/*        Data data = new Data.Builder().putInt("count", this.stepCount.getValue()).build();
        stepCount.observeForever(new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                data = new Data.Builder().putInt("count",integer).build();
            }
        });*//*


        //This will take care of the first execution
        Calendar currentDate = Calendar.getInstance();
        Calendar dueDate = Calendar.getInstance();
        // Set Execution around 05:00:00 AM
        dueDate.set(Calendar.HOUR_OF_DAY, 13);
        dueDate.set(Calendar.MINUTE, 0);
        dueDate.set(Calendar.SECOND, 0);

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24);
        }

        long timeDiff = dueDate.getTimeInMillis() - currentDate.getTimeInMillis();


        if (stepCount.getValue() != null) {
            OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(DatabaseInfoUpdater.class)
                    .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                    .setInputData(new Data.Builder().putInt(getString(R.string.steps), stepCount.getValue()).build()).build();
            WorkManager.getInstance().enqueue(oneTimeWorkRequest);
        } else Log.d("myLogs", "value = null");*/