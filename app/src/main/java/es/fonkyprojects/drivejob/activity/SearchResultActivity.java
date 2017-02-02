package es.fonkyprojects.drivejob.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.List;

import butterknife.ButterKnife;
import es.fonkyprojects.drivejob.SQLQuery.SQLConnect;
import es.fonkyprojects.drivejob.model.Ride;
import es.fonkyprojects.drivejob.model.RideSearch;
import es.fonkyprojects.drivejob.viewholder.RideViewAdapter;

public class SearchResultActivity extends Activity {

    private static final String TAG = "SearchResultActivity";

    public RecyclerView recyclerView;
    public RecyclerView.Adapter adapter;
    public RecyclerView.LayoutManager layoutManager;

    private List<Ride> listRides;

    public static final String EXTRA_RIDE_SEARCH = "ride_search";

    private String authorId;
    private String timeGoing;
    private String timeReturn;
    private double latGoing;
    private double latReturning;
    private double lngGoign;
    private double lngReturning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);
        ButterKnife.bind(this);

        getValues();

        recyclerView = (RecyclerView) findViewById(R.id.ride_list);
    }

    @Override
    protected void onStart() {
        super.onStart();

        listRides = (new SQLConnect()).searchRide(authorId, latGoing, latReturning, lngGoign, lngReturning, timeGoing, timeReturn);
        adapter = new RideViewAdapter(listRides, new RideViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Ride item) {
                Intent intent = new Intent(SearchResultActivity.this, RideDetailActivity.class);
                intent.putExtra(RideDetailActivity.EXTRA_RIDE_KEY, item.getID());
                startActivity(intent);
                finish();
            }
        });
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    public void getValues(){
        RideSearch rs = (RideSearch) getIntent().getSerializableExtra(EXTRA_RIDE_SEARCH);
        authorId = rs.getAuthorID();
        timeGoing = rs.getTimeGoing();
        timeReturn = rs.getTimeReturn();
        latGoing = rs.getLatGoing();
        latReturning = rs.getLatReturn();
        lngGoign = rs.getLngGoing();
        lngReturning = rs.getLngReturn();
    }
}
