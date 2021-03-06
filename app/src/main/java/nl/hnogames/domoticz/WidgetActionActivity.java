package nl.hnogames.domoticz;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

import nl.hnogames.domoticz.Containers.DevicesInfo;
import nl.hnogames.domoticz.Domoticz.Domoticz;
import nl.hnogames.domoticz.Interfaces.DevicesReceiver;
import nl.hnogames.domoticz.Service.WidgetProviderLarge;
import nl.hnogames.domoticz.Utils.SharedPrefUtil;
import nl.hnogames.domoticz.Welcome.WelcomeViewActivity;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

public class WidgetActionActivity extends AppCompatActivity {
    private final int iWelcomeResultCode = 885;
    int mAppWidgetId;
    private SharedPrefUtil mSharedPrefs;
    private Domoticz domoticz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widget_configuration);
        setResult(RESULT_CANCELED);

        mSharedPrefs = new SharedPrefUtil(this);
        domoticz = new Domoticz(this);

        this.setTitle(getString(R.string.choose_switch));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(false);
        }

        //1) Is domoticz connected?
        if (mSharedPrefs.isFirstStart()) {
            mSharedPrefs.setNavigationDefaults();
            Intent welcomeWizard = new Intent(this, WelcomeViewActivity.class);
            startActivityForResult(welcomeWizard, iWelcomeResultCode);
            mSharedPrefs.setFirstStart(false);
        } else {
            //2) Show list of switches to choose from
            initListViews();
        }
    }

    /* Called when the second activity's finished */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null && resultCode == RESULT_OK) {
            switch (requestCode) {
                case iWelcomeResultCode:
                    Bundle res = data.getExtras();
                    if (!res.getBoolean("RESULT", false))
                        this.finish();
                    else {
                        initListViews();
                    }
                    break;
            }
        }
    }

    public void initListViews() {
        if (mSharedPrefs.isWelcomeWizardSuccess()) {
            Log.i(this.getClass().getSimpleName(), "Showing switches for widget");
            domoticz.getDevices(new DevicesReceiver() {
                @Override
                public void onReceiveDevices(final ArrayList<DevicesInfo> mDevicesInfo) {
                    final ArrayList<DevicesInfo> mDevices = new ArrayList<>();
                    for (DevicesInfo s : mDevicesInfo) {
                        mDevices.add(s);
                    }
                    Collections.sort(mDevices);

                    String[] listData = processSwitches(mDevices);
                    ListView listView = (ListView) findViewById(R.id.list);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(WidgetActionActivity.this,
                            android.R.layout.simple_list_item_1, android.R.id.text1, listData);

                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            DevicesInfo selected = mDevices.get(position);
                            showAppWidget(selected);
                        }
                    });

                    listView.setAdapter(adapter);
                }

                @Override
                public void onReceiveDevice(DevicesInfo mDevicesInfo) {
                }

                @Override
                public void onError(Exception error) {
                    Toast.makeText(WidgetActionActivity.this, R.string.failed_to_get_switches, Toast.LENGTH_SHORT).show();
                    WidgetActionActivity.this.finish();
                }
            }, 0, null);
        } else {
            Intent welcomeWizard = new Intent(this, WelcomeViewActivity.class);
            startActivityForResult(welcomeWizard, iWelcomeResultCode);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    public String[] processSwitches(ArrayList<DevicesInfo> switches) {
        String[] listData = new String[switches.size()];
        int counter = 0;
        for (DevicesInfo s : switches) {
            String log = s.getIdx() + " | " + s.getName();
            listData[counter] = log;
            counter++;
        }
        return listData;
    }

    private void showAppWidget(DevicesInfo mSelectedSwitch) {
        mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        int idx = mSelectedSwitch.getIdx();
        if (extras != null) {
            mAppWidgetId = extras.getInt(EXTRA_APPWIDGET_ID,
                    INVALID_APPWIDGET_ID);

            if (mSelectedSwitch.getType().equals(Domoticz.Scene.Type.GROUP) || mSelectedSwitch.getType().equals(Domoticz.Scene.Type.SCENE))
                mSharedPrefs.setWidgetIDX(mAppWidgetId, idx, true);
            else
                mSharedPrefs.setWidgetIDX(mAppWidgetId, idx, false);

            mSharedPrefs.setWidgetIDforIDX(mAppWidgetId, idx);
            Intent startService = new Intent(WidgetActionActivity.this,
                    WidgetProviderLarge.UpdateWidgetService.class);
            startService.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
            startService.setAction("FROM CONFIGURATION ACTIVITY");
            startService(startService);
            setResult(RESULT_OK, startService);

            finish();
        }
        if (mAppWidgetId == INVALID_APPWIDGET_ID) {
            Log.i("I am invalid", "I am invalid");
            finish();
        }
    }
}