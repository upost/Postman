package de.spas.postman;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.spas.tools.BaseGameActivity;

public class MainActivity extends BaseGameActivity implements View.OnClickListener, ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {


    public static final String TYPEFACE_NAME = "AlfaSlabOne-Regular";
    private static final double MIN_DISTANCE_STATIONS = 100;
    private static final int POSTSTATION_COST = 500;
    private static final float LETTER_CASH_PER_METER = 0.4f;
    private static final int DEFAULT_CASH = 1200;
    private static final double POSTSTATION_VISIT_MIN_DISTANCE = 20;
    public static final String LOG_TAG = "postman";
    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private ItemizedIconOverlay<OverlayItem> postStationsOverlay;
    private GameStorage gameStorage;
    private List<OverlayItem> items = new ArrayList<OverlayItem>();
    private Random rnd = new Random();
    private LetterAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(LOG_TAG, "creating activity");

        setContentView(R.layout.activity_main);

        addTypeface(TYPEFACE_NAME);
        setTypeface((TextView) findViewById(R.id.title), TYPEFACE_NAME);
        setTypeface((TextView) findViewById(R.id.player_cash), TYPEFACE_NAME);

        // open game engine
        gameStorage = new GameStorage(this, DEFAULT_CASH);

        // create mapView
        mapView = new MapView(this);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(16);

        // create stations overlay
        postStationsOverlay = new ItemizedIconOverlay<OverlayItem>(items,
                getResources().getDrawable(R.drawable.poststation), this, new DefaultResourceProxyImpl(this));
        updateStationItems();
        mapView.getOverlays().add(postStationsOverlay);

        // create my location overlay
        myLocationOverlay = new MyLocationNewOverlay(getApplicationContext(), mapView);
        mapView.getOverlays().add(myLocationOverlay);

        ViewGroup container = (ViewGroup) findViewById(R.id.container);
        container.removeAllViews();
        container.addView(mapView);

        showMap();

        adapter = new LetterAdapter(this,0,0, gameStorage.findLetters());

        ListView listView = (ListView) findViewById(R.id.letters);
        listView.setAdapter(adapter);

        setAnimatedClickListener(R.id.poststation,R.anim.buttonpress,this);
        setAnimatedClickListener(R.id.poststation_new,R.anim.buttonpress,this);
        setAnimatedClickListener(R.id.letter,R.anim.buttonpress,this);

    }



    private void showMap() {
        hideView(R.id.letters);
        showView(R.id.container);
    }



    private void showLetters() {
        hideView(R.id.container);
        adapter.clear();
        for(GameStorage.Letter l : gameStorage.findLetters()) {
            adapter.add(l);
        }
        adapter.notifyDataSetChanged();
        showView(R.id.letters);
    }

    public class LetterAdapter extends ArrayAdapter<GameStorage.Letter> {
        public LetterAdapter(Context context, int resource, int textViewResourceId, List<GameStorage.Letter> objects) {
            super(context, resource, textViewResourceId, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView==null) {
                View v = getLayoutInflater().inflate(R.layout.letter, null);
                setTypeface((TextView) v.findViewById(R.id.target),TYPEFACE_NAME);
                setTypeface((TextView) v.findViewById(R.id.value),TYPEFACE_NAME);
                fillView(v,position);
                return v;
            } else {
                fillView(convertView,position);
                return convertView;
            }
        }

        private void fillView(View view, int position) {
            TextView t = (TextView) view.findViewById(R.id.target);
            t.setText(getItem(position).target);
            TextView c = (TextView) view.findViewById(R.id.value);
            c.setText(Integer.toString(getItem(position).value));
            if(targetStationReached(getItem(position))!=null) {
                t.setTextColor(getResources().getColor(R.color.green));
                c.setTextColor(getResources().getColor(R.color.green));
            } else {
                t.setTextColor(getResources().getColor(R.color.yellow));
                c.setTextColor(getResources().getColor(R.color.yellow));
            }
            view.setTag(getItem(position));
            view.setOnClickListener(MainActivity.this);
        }

    }

    private void updateStationItems() {
        postStationsOverlay.removeAllItems();
        for(GameStorage.PostStation station : gameStorage.findPostStations()) {
            postStationsOverlay.addItem(new OverlayItem(station.name,"",new GeoPoint(station.latitude, station.longitude)));
        }
        mapView.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        update();
    }

    @Override
    protected void onPause() {
        super.onPause();
        myLocationOverlay.disableMyLocation();
        myLocationOverlay.runOnFirstFix(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,R.string.has_location,Toast.LENGTH_SHORT).show();
                        Log.d("postman", "location=" + myLocationOverlay.getMyLocation().toString());
                    }
                });
            }
        });
    }

    private void update() {
        GameStorage.Player p = gameStorage.findPlayer();
        Log.d("postman","cash="+p.cash);
        //((TextView)findViewById(R.id.player_cash)).setText("test");
        setText(R.id.player_cash, String.format("%05d", p.cash));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.poststation_new) {
            Location location = myLocationOverlay.getLastFix();
            buildPostStation(location);
        }
        if(view.getId()==R.id.poststation) {
            showMap();
        }
        if(view.getId()==R.id.letter) {
            showLetters();
        }
        if(view.getId()==R.id.delivery) {
            GameStorage.Letter letter = (GameStorage.Letter) view.getTag();
            deliverLetter(letter);
        }
    }

    private void deliverLetter(GameStorage.Letter letter) {
        GameStorage.PostStation station = targetStationReached(letter);
        if(station!=null) {
            Toast.makeText(this,getString(R.string.msg_delivered) + letter.value,Toast.LENGTH_SHORT).show();
            gameStorage.addCash(letter.value);
            gameStorage.deleteLetter(letter.id);
            showLetters();
            update();
        }
    }

    private GameStorage.PostStation targetStationReached(GameStorage.Letter letter) {
        Location location = myLocationOverlay.getLastFix();
        GameStorage.PostStation station = gameStorage.findPostStationNear(location.getLongitude(), location.getLatitude(), POSTSTATION_VISIT_MIN_DISTANCE);
        if(station!=null && letter.target.equals(station.name)) return station;
        return null;
    }

    private void buildPostStation(final Location location) {
        if(gameStorage.findPlayer().cash < POSTSTATION_COST) {
            Toast.makeText(this,getString(R.string.msg_not_enough_cash) + POSTSTATION_COST,Toast.LENGTH_SHORT).show();
            return;
        }
        GameStorage.PostStation station = gameStorage.findPostStationNear(location.getLongitude(), location.getLatitude(), MIN_DISTANCE_STATIONS);
        if(station!=null) {
            Toast.makeText(this,getString(R.string.msg_station_exists) + station.name,Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText et = new EditText(this);
        builder.setView(et).setTitle(R.string.enter_station_name).setCancelable(true)
                .setPositiveButton(android.R.string.ok,new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        gameStorage.addPostStation(et.getText().toString(), location.getLongitude(), location.getLatitude());
                        gameStorage.addCash(-POSTSTATION_COST);
                        gameStorage.setLastStation(et.getText().toString());
                        updateStationItems();
                        update();
                    }
                });
        builder.create().show();


    }

    @Override
    public boolean onItemSingleTapUp(int i, OverlayItem overlayItem) {
        if(overlayItem.getTitle().equals(gameStorage.findPlayer().lastStation)) {
            Log.d("postman","tapped last station again");
            Toast.makeText(this, R.string.no_new_letter, Toast.LENGTH_SHORT).show();
            return false;
        }
        GameStorage.PostStation station = gameStorage.findPostStationByName(overlayItem.getTitle());
        Location location = myLocationOverlay.getLastFix();
        Log.d("postman","tapped item " + station.name);
        float res[] =new float[1];
        Location.distanceBetween(location.getLatitude(),location.getLongitude(),station.latitude,station.longitude,res);
        if(res[0]< POSTSTATION_VISIT_MIN_DISTANCE) {
            Log.d("postman","visit station");
            generateLetter(station);
            gameStorage.setLastStation(station.name);
            return true;

        }
        return false;
    }

    private void generateLetter(final GameStorage.PostStation station) {
        // find possible target station
        List<GameStorage.PostStation> possibleTargets = new ArrayList<GameStorage.PostStation>();
        for(GameStorage.PostStation s : gameStorage.findPostStations()) {
            if(!s.equals(station)) {
                possibleTargets.add(s);
            }
        }
        if(possibleTargets.isEmpty()) {
            Log.d("postman","no letter targets");
            return;
        }
        final GameStorage.PostStation target = possibleTargets.get(rnd.nextInt(possibleTargets.size()));
        float[] distance=new float[1];
        Location.distanceBetween(station.latitude,station.longitude,target.latitude,target.longitude,distance);
        final int value = calcLetterValue(distance[0]);
        Log.d("postman","generated letter to " + target.name + " for " + value);
        String msg = getString(R.string.msg_new_letter, target.name, value);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(station.name).setMessage(msg).setCancelable(true)
                .setPositiveButton(android.R.string.yes,new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        gameStorage.addLetter(target.name, value);
                    }
                });
        builder.create().show();
    }

    private int calcLetterValue(float distance) {
        return (int) Math.round( Math.log10(distance)*25);
    }

    @Override
    public boolean onItemLongPress(int i, OverlayItem overlayItem) {
        return false;
    }


}