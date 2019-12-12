package ed.doron.pedometer;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.firebase.ui.auth.AuthUI;

import ed.doron.pedometer.ui.main.PedometerViewModel;

public class MainActivity extends AppCompatActivity {

    private String TAG = "tag";
    private Menu menu;
    private Toolbar toolbar;
    private PedometerViewModel viewModel;
    StepCounterService stepCounterService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.viewModel = ViewModelProviders.of(this).get(PedometerViewModel.class);
        initializeViewModel();
        setCustomTheme(Preferences.getDayMode(MainActivity.this));
        setContentView(R.layout.main_activity);
        this.toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SectionPagerAdapter sectionsPagerAdapter = new SectionPagerAdapter(getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.container);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.addOnPageChangeListener(onPageChangeListener);
    }

    private void initializeViewModel() {
        viewModel.setStepLimit(Preferences.getStepLimit(MainActivity.this));
        viewModel.setStepLength(Preferences.getStepLength(MainActivity.this));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent serviceIntent = new Intent(this, StepCounterService.class);
        if (!isServiceRunning()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, serviceIntent);
            } else startService(serviceIntent);
        }
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    private ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            changePageTitle(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    private void changePageTitle(int position) {
        switch (position) {
            case 1:
                toolbar.setTitle(R.string.progress);
                break;
            case 2:
                toolbar.setTitle(R.string.statistics);
                break;
            default:
                toolbar.setTitle(R.string.diagram);
                break;
        }
    }

    private void setCustomTheme(boolean dayMode) {
        if (dayMode)
            setTheme(R.style.AppTheme_Day);
        else setTheme(R.style.AppTheme_Night);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present
        getMenuInflater().inflate(R.menu.menu_pedometer, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        this.menu = menu;
        final MenuItem dayNightItem = menu.findItem(R.id.action_day_night);
        final MenuItem stepLengthItem = menu.findItem(R.id.action_step_length);
        final MenuItem stepLimitItem = menu.findItem(R.id.action_step_limit);

        setDayNightSwitch(dayNightItem, (Switch) dayNightItem.getActionView());
        setStepLengthEditText((EditText) stepLengthItem.getActionView());
        setStepLimitEditText((EditText) stepLimitItem.getActionView());

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sign_out)
            signOut();
        else
            collapseOtherMenuItems(item.getItemId());
        return super.onOptionsItemSelected(item);
    }

    private void collapseOtherMenuItems(int itemId) {
        int size = menu.size();
        for (int i = 0; i < size; i++) {
            if (menu.getItem(i).getItemId() != itemId)
                for (int j = 0; j < size; j++) {
                    if (i != j && menu.getItem(j).isActionViewExpanded())
                        menu.getItem(j).collapseActionView();
                }
        }
    }

    private void setDayNightSwitch(MenuItem dayNightItem, Switch dayNightSwitch) {
        dayNightSwitch.setChecked(!Preferences.getDayMode(MainActivity.this));
        dayNightSwitch.setText(getString(R.string.night));
        dayNightSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            dayNightItem.collapseActionView();
            Preferences.setDayMode(MainActivity.this, !isChecked);
            //we cannot change theme on runtime
            reloadActivity();
        });
    }


    private void setStepLengthEditText(EditText stepLengthEditText) {
        stepLengthEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        stepLengthEditText.setHint(String.format(getString(R.string.current_step_length), Preferences.getStepCount(MainActivity.this)));
        stepLengthEditText.setOnFocusChangeListener((view, focused) -> {
            if (focused)
                stepLengthEditText.setText(String.valueOf(Preferences.getStepLength(MainActivity.this)));
            else {
                stepLengthEditText.setText("");
                stepLengthEditText.setHint(String.format(getString(R.string.current_step_length), Preferences.getStepLength(MainActivity.this)));
            }
        });
        stepLengthEditText.setOnEditorActionListener((textView, actionId, event) -> {
            //works both for DONE and RETURN
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                if (!stepLengthEditText.getText().toString().isEmpty()) {
                    Log.d(TAG, "changing step length");
                    try {
                        viewModel.setStepLength(Integer.parseInt(stepLengthEditText.getText().toString()));
                        Preferences.setStepLength(MainActivity.this, Integer.parseInt(stepLengthEditText.getText().toString()));
                    } catch (Exception e) {
                        Log.d(TAG, "incorrect step length integer value");
                    }
                }
            }
            return false;
        });

    }

    private void setStepLimitEditText(EditText stepLimitEditText) {
        stepLimitEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        stepLimitEditText.setHint(String.format(getString(R.string.current_step_limit), Preferences.getStepLimit(MainActivity.this)));
        stepLimitEditText.setOnFocusChangeListener((view, focused) -> {
            if (focused)
                stepLimitEditText.setText(String.valueOf(Preferences.getStepLimit(MainActivity.this)));
            else {
                stepLimitEditText.setText("");
                stepLimitEditText.setHint(String.format(getString(R.string.current_step_limit), Preferences.getStepLimit(MainActivity.this)));
            }
        });
        stepLimitEditText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if ((keyEvent != null && (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                if (!stepLimitEditText.getText().toString().isEmpty()) {
                    Log.d(TAG, "changing step limit");
                    try {
                        viewModel.setStepLimit(Integer.parseInt(stepLimitEditText.getText().toString()));
                        Preferences.setStepLimit(MainActivity.this, Integer.parseInt(stepLimitEditText.getText().toString()));
                    } catch (Exception e) {
                        Log.d(TAG, "incorrect step limit integer value");
                    }
                }
            }
            return false;
        });
    }

    private void signOut() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(task -> {
                            //start LoginActivity
                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                );
    }

    private void reloadActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                Log.d(TAG, service.getClass().getName());
                if (StepCounterService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to StepCounterService, cast the IBinder and get LocalService instance
            StepCounterService.LocalBinder binder = (StepCounterService.LocalBinder) service;
            stepCounterService = binder.getService();

            stepCounterService.stepCount.observe(MainActivity.this, integer -> viewModel.setStepCount(integer));
            viewModel.getStepCount().observe(MainActivity.this, integer -> Log.d("myLogs", "steps from mainActivity " + integer));

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };


}
