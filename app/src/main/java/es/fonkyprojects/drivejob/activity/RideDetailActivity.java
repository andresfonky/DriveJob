package es.fonkyprojects.drivejob.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.ButterKnife;
import es.fonkyprojects.drivejob.SQLQuery.SQLConnect;
import es.fonkyprojects.drivejob.model.Car;
import es.fonkyprojects.drivejob.model.Messaging;
import es.fonkyprojects.drivejob.model.Ride;
import es.fonkyprojects.drivejob.model.User;
import es.fonkyprojects.drivejob.model.UserDays;
import es.fonkyprojects.drivejob.model.local.UsernameDays;
import es.fonkyprojects.drivejob.restMethods.DeleteTask;
import es.fonkyprojects.drivejob.restMethods.GetTask;
import es.fonkyprojects.drivejob.restMethods.Messaging.MessagingPostTask;
import es.fonkyprojects.drivejob.restMethods.Rides.RideAvSeatsPutTask;
import es.fonkyprojects.drivejob.restMethods.Rides.RideJoinPutTask;
import es.fonkyprojects.drivejob.restMethods.Rides.RideRequestPutTask;
import es.fonkyprojects.drivejob.utils.Constants;
import es.fonkyprojects.drivejob.utils.FirebaseUser;
import es.fonkyprojects.drivejob.viewholder.UserJoinViewAdapter;
import es.fonkyprojects.drivejob.viewholder.UserRequestViewAdapter;

public class RideDetailActivity extends AppCompatActivity {

    private static final String TAG = "RideDetailActivity";
    public static final String EXTRA_RIDE_KEY = "ride_key";

    private String mRideKey;
    private String authorID;
    private Ride ride;

    private List<UsernameDays> listUsersRequest;
    private List<UsernameDays> listUsersJoin;
    private List<Integer> avSeats;
    @BindArray(R.array.shortdaysofweek)String[] shortListDays;

    @BindView(R.id.userrequest_list)
    RecyclerView requestRecyclerView;
    public RecyclerView.Adapter requestAdapter;
    public RecyclerView.LayoutManager requestLayoutManager;

    @BindView(R.id.userjoin_list) RecyclerView joinRecyclerView;
    public RecyclerView.Adapter joinAdapter;
    public RecyclerView.LayoutManager joinLayoutManager;

    //private ImageView authorImage;
    @BindView(R.id.ride_author_photo) ImageView imgView;
    @BindView(R.id.ride_author) TextView authorView;
    @BindView(R.id.ride_timeGoing) TextView timeGoingView;
    @BindView(R.id.ride_timeReturn) TextView timeReturnView;
    @BindView(R.id.ride_placeGoing) TextView placeGoingView;
    @BindView(R.id.ride_placeReturn) TextView placeReturnView;
    @BindView(R.id.ride_days) TextView daysView;
    @BindView(R.id.ride_price) TextView priceView;
    @BindView(R.id.ride_avSeatsDay) TextView avSeatsDayView;
    @BindView(R.id.ride_car) TextView carView;
    @BindView(R.id.btn_join) Button btnJoin;
    @BindView(R.id.btn_exit) Button btnExit;
    @BindView(R.id.detailToolbar) Toolbar toolbar;

    private boolean showUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_detail);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        listUsersRequest = new ArrayList<>();
        listUsersJoin = new ArrayList<>();

        // Get post key from intent
        mRideKey = getIntent().getStringExtra(EXTRA_RIDE_KEY);
        if (mRideKey == null) {
            throw new IllegalArgumentException("Must pass EXTRA_POST_KEY");
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mRideKey != null) {
            //GET Ride from key
            try {
                String result = new GetTask(this).execute(Constants.BASE_URL + "ride/" + mRideKey).get();

                //Show ride
                ride = new Gson().fromJson(result, Ride.class);
                authorID = ride.getAuthorID();
                authorView.setText(ride.getAuthor());
                timeGoingView.setText(getString(R.string.goingDetail, ride.getTimeGoing()));
                timeReturnView.setText(getString(R.string.returningDetail, ride.getTimeReturn()));
                placeGoingView.setText(getString(R.string.fromDetail, ride.getPlaceGoing()));
                placeReturnView.setText(getString(R.string.toDetail, ride.getPlaceReturn()));
                priceView.setText(getString(R.string.priceDetail, ride.getPrice()));

                //Get days -> Convert from array days to string
                List<Boolean> listDays = ride.getDays();
                String sDays = "";
                for (int i = 0; i < listDays.size(); i++) {
                    if (listDays.get(i)) {
                        sDays = sDays + shortListDays[i] + ",";
                    }
                }
                sDays = sDays.substring(0, sDays.length() - 1);
                daysView.setText(getString(R.string.daysDetail, sDays));

                //AvSeatsDays to String
                avSeats = ride.getAvSeats();
                String sAvSeatsDay = "";
                for(int i=0; i<avSeats.size(); i++){
                    if(avSeats.get(i)>0){
                        sAvSeatsDay += shortListDays[i] + "=" + avSeats.get(i) + ",";
                    }
                }
                sAvSeatsDay = sAvSeatsDay.substring(0, sAvSeatsDay.length() -1);
                avSeatsDayView.setText(getString(R.string.avSeatsDayDetail, sAvSeatsDay));

                //Get car from CarId
                Car c = getCar(ride.getCarID());
                carView.setText(getString(R.string.carDetail, c.toString()));

                boolean request = getUserRequest();
                boolean join = getUserJoin();
                showUser = (FirebaseUser.getUid().equals(authorID) || join);

                createRequestAdapter();
                createJoinAdapter();

                //Check if avSeats AND if author, request or join
                boolean avSeatsFree = false;
                for (int i = 0; i < avSeats.size(); i++) {
                    int space = avSeats.get(i);
                    if (space > 0)
                        avSeatsFree = true;
                }
                if (FirebaseUser.getUid().equals(authorID) || !avSeatsFree)
                    btnJoin.setVisibility(View.GONE);
                if(request || join) {
                    btnJoin.setVisibility(View.INVISIBLE);
                    btnExit.setVisibility(View.VISIBLE);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean getUserRequest() {
        //Get UserRequest
        boolean request = false;
        listUsersRequest.clear();
        List<UserDays> listRequests = ride.getRequest();
        for (int i = 0; i < listRequests.size(); i++) { //Get Users
            User u = getUser(listRequests.get(i).getUserId());
            UsernameDays ud = new UsernameDays(u.getUserId(), u.getUsername() + " " + u.getSurname(), listRequests.get(i).getDays());
            if (listRequests.get(i).getUserId().equals(FirebaseUser.getUid())) {
                request = true;
            }
            listUsersRequest.add(ud);
        }
        return request;
    }

    private void createRequestAdapter() {
        requestAdapter = new UserRequestViewAdapter(listUsersRequest, new UserRequestViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(UsernameDays item) {
                Intent intent = new Intent(RideDetailActivity.this, MyProfileActivity.class);
                intent.putExtra(MyProfileActivity.EXTRA_USER_ID, item.getUserId());
                startActivity(intent);
                finish();
            }
        }, new UserRequestViewAdapter.OnAcceptClickListener() {
            @Override
            public void onAcceptClick(UsernameDays item) {
                if (FirebaseUser.getUid().equals(authorID)){
                    acceptJoin(item);
                    listUsersRequest.remove(item);
                    listUsersJoin.add(item);
                    requestAdapter.notifyDataSetChanged();
                    joinAdapter.notifyDataSetChanged();
                }
            }
        }, new UserRequestViewAdapter.OnRefuseClickListener() {
            @Override
            public void onRefuseClick(UsernameDays item) {
                refuseJoin(item, true);
                listUsersRequest.remove(item);
                requestAdapter.notifyDataSetChanged();
            }
        }, FirebaseUser.getUid().equals(authorID), showUser);

        requestLayoutManager = new LinearLayoutManager(this);
        requestRecyclerView.setLayoutManager(requestLayoutManager);
        requestRecyclerView.setAdapter(requestAdapter);
    }

    private boolean getUserJoin() {
        listUsersJoin.clear();
        boolean join = false;
        //Get Users join
        List<UserDays> listJoin = ride.getJoin();
        for (int i = 0; i < listJoin.size(); i++) { //Get Users
            User u = getUser(listJoin.get(i).getUserId());
            UsernameDays ud = new UsernameDays(u.getUserId(), u.getUsername() + " " + u.getSurname(), listJoin.get(i).getDays());
            if (listJoin.get(i).getUserId().equals(FirebaseUser.getUid())) {
                join = true;
            }
            listUsersJoin.add(ud);
        }
        return join;
    }

    private void createJoinAdapter(){
        joinAdapter = new UserJoinViewAdapter(listUsersJoin, new UserJoinViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(UsernameDays item) {
                Intent intent = new Intent(RideDetailActivity.this, MyProfileActivity.class);
                intent.putExtra(MyProfileActivity.EXTRA_USER_ID, item.getUserId());
                startActivity(intent);
                finish();
            }
        }, new UserJoinViewAdapter.OnRefuseClickListener() {
            @Override
            public void onRefuseClick(UsernameDays item) {
                kickJoin(item, true);
                listUsersJoin.remove(item);
                joinAdapter.notifyDataSetChanged();
            }
        }, FirebaseUser.getUid().equals(authorID), showUser);
        joinLayoutManager = new LinearLayoutManager(this);
        joinRecyclerView.setLayoutManager(joinLayoutManager);
        joinRecyclerView.setAdapter(joinAdapter);
    }

    private User getUser(String userId) {
        User user = new User();
        try {
            GetTask gt = new GetTask(this);
            String result = gt.execute(Constants.BASE_URL + "user/?userId=" + userId).get();
            Type type = new TypeToken<List<User>>() {
            }.getType();
            List<User> inpList = new Gson().fromJson(result, type);
            user = inpList.get(0);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return user;
    }

    private Car getCar(String carId) {
        Car car = new Car();
        try {
            GetTask gt = new GetTask(this);
            String result = gt.execute(Constants.BASE_URL + "car/" + carId).get();
            car = new Gson().fromJson(result, Car.class);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return car;
    }

    public void goProfile(View view) {
        Intent intent = new Intent(RideDetailActivity.this, MyProfileActivity.class);
        intent.putExtra(MyProfileActivity.EXTRA_USER_ID, authorID);
        startActivity(intent);
    }

    //Window to select days if avaliable
    public void selectDaysJoin(View view) {
        ArrayList<String> temp = new ArrayList<>();
        String[] listDays = getResources().getStringArray(R.array.daysofweek); //días de la semana completo
        String[] myListDays; //listado con los dias de la Ride
        final boolean[] myCheckedDays; //días seleccionados
        final ArrayList<Integer> myMUserDays = new ArrayList<>(); //Selected days
        final ArrayList<Integer> mUserDays = new ArrayList<>(); //List of general position

        //Add day of week to temp/mListDays
        //Add pos to mUserDays
        List<Boolean> items = ride.getDays();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) && avSeats.get(i) > 0) {
                temp.add(listDays[i]);
                mUserDays.add(i);
            }
        }
        myListDays = new String[temp.size()];
        myListDays = temp.toArray(myListDays);
        myCheckedDays = new boolean[myListDays.length];

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(RideDetailActivity.this);
        mBuilder.setTitle(R.string.days);
        mBuilder.setMultiChoiceItems(myListDays, myCheckedDays, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int position, boolean isChecked) {
                if (isChecked) {
                    myMUserDays.add(mUserDays.get(position));
                } else {
                    myMUserDays.remove((Integer.valueOf(position)));
                }
            }
        });

        mBuilder.setCancelable(false);
        mBuilder.setPositiveButton(R.string.ok_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                Collections.sort(myMUserDays);
                if (myMUserDays.isEmpty())
                    dialogInterface.dismiss();
                else
                    request(myMUserDays);
            }
        });

        mBuilder.setNegativeButton(R.string.dismiss_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog mDialog = mBuilder.create();
        mDialog.show();
    }

    public void request(List<Integer> selectedDays) {
        btnJoin.setVisibility(View.INVISIBLE);
        btnExit.setVisibility(View.VISIBLE);

        //Create user request
        String uid = FirebaseUser.getUid();
        List<UserDays> listUd = ride.getRequest();
        listUd.add(new UserDays(uid, selectedDays));
        List<String> listUser = ride.getRequestUser();
        listUser.add(uid);
        try {
            RideRequestPutTask rrpt = new RideRequestPutTask(getApplicationContext());
            rrpt.setRideRequestPutTask(listUd);
            rrpt.setRideRequestUserPutTask(listUser);
            String result = rrpt.execute(Constants.BASE_URL + "ride/" + mRideKey).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        //Update seats -1
        updateAvSeats(selectedDays, -1);

        // Send message
        User u = getUser(uid);
        sendMessage(u.getUsername(), ride.getAuthorID(), mRideKey, 0);

        //Add to list
        UsernameDays und = new UsernameDays(u.getUserId(), u.getUsername() + " " + u.getSurname(), selectedDays);
        listUsersRequest.add(und);
        requestAdapter.notifyDataSetChanged();
    }

    private void acceptJoin(UsernameDays und) {
        String uid = und.getUserId();
        List<UserDays> listUd = ride.getJoin();
        listUd.add(new UserDays(uid, und.getDays()));
        List<String> listId = ride.getJoinUser();
        listId.add(uid);

        try {
            RideJoinPutTask rupt = new RideJoinPutTask(getApplicationContext());
            rupt.setRideJoinPutTask(listUd);
            rupt.setRideJoinUserPutTask(listId);
            String result = rupt.execute(Constants.BASE_URL + "ride/" + mRideKey).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        deleteRequest(und);
        sendMessage(ride.getAuthor(), und.getUserId(), mRideKey, 10);
    }

    private void refuseJoin(UsernameDays und, boolean mes) {
        deleteRequest(und);
        updateAvSeats(und.getDays(), 1);
        if(mes)
            sendMessage(ride.getAuthor(), und.getUserId(), mRideKey, 20);
    }

    private void kickJoin(UsernameDays und, boolean mes) {
        UserDays ud = new UserDays(und.getUserId(), und.getDays());
        List<UserDays> listUd = ride.getJoin();
        listUd.remove(ud);
        try {
            RideJoinPutTask rjpt = new RideJoinPutTask(getApplicationContext());
            rjpt.setRideJoinPutTask(listUd);
            String result = rjpt.execute(Constants.BASE_URL + "ride/" + mRideKey).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        //Update seats +1
        updateAvSeats(und.getDays(), 1);
        if(mes)
            sendMessage(ride.getAuthor(), und.getUserId(), mRideKey, 30);
    }

    public void exitRide(View view){
        boolean found = false;
        UsernameDays und = null;
        for(int i = 0; i<listUsersRequest.size(); i++){
            if(listUsersRequest.get(i).getUserId().equals(FirebaseUser.getUid())){
                und = listUsersRequest.get(i);
                refuseJoin(listUsersRequest.get(i), false);
                listUsersRequest.remove(i);
                requestAdapter.notifyDataSetChanged();
                found = true;
            }
        }
        if (!found) {
            for(int i = 0; i<listUsersJoin.size(); i++) {
                if (listUsersJoin.get(i).getUserId().equals(FirebaseUser.getUid())) {
                    und = listUsersJoin.get(i);
                    kickJoin(listUsersJoin.get(i), false);
                    listUsersJoin.remove(i);
                    joinAdapter.notifyDataSetChanged();
                    found = true;
                }
            }
        }

        if(found){
            btnExit.setVisibility(View.INVISIBLE);
            btnJoin.setVisibility(View.VISIBLE);
            sendMessage(und.getUsername(), ride.getAuthorID(), mRideKey, 40);
        }
    }

    private String updateAvSeats(List<Integer> days, int value) {
        //GET int days
        for (int i = 0; i < days.size(); i++) {
            int day = days.get(i);
            avSeats.set(day, avSeats.get(day) + value);
        }

        //REST Update avSeats
        String result = "";
        try {
            RideAvSeatsPutTask raspt = new RideAvSeatsPutTask(getApplicationContext());
            raspt.setRideAvSeatsPutTask(avSeats);
            result = raspt.execute(Constants.BASE_URL + "ride/" + mRideKey).get();
            (new SQLConnect()).updateAvSeatsDay(avSeats, mRideKey);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        //AvSeats to String
        //AvSeatsDays to String
        String sAvSeatsDay = "";
        for(int i=0; i<avSeats.size(); i++){
            if(avSeats.get(i)>0){
                sAvSeatsDay += shortListDays[i] + "=" + avSeats.get(i) + ",";
            }
        }
        sAvSeatsDay = sAvSeatsDay.substring(0, sAvSeatsDay.length() -1);
        avSeatsDayView.setText(getString(R.string.avSeatsDayDetail, sAvSeatsDay));

        return result;
    }

    private void deleteRequest(UsernameDays und) {
        UserDays ud = new UserDays(und.getUserId(), und.getDays());
        List<UserDays> listUd = ride.getRequest();
        boolean b = listUd.remove(ud);
        List<String> listId = ride.getRequestUser();
        b = listId.remove(und.getUserId());
        try {
            RideRequestPutTask rrpt = new RideRequestPutTask(getApplicationContext());
            rrpt.setRideRequestPutTask(listUd);
            rrpt.setRideRequestUserPutTask(listId);
            String result = rrpt.execute(Constants.BASE_URL + "ride/" + mRideKey).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private String deleteRide() {
        String result = "";
        sendMessageCreateList();
        DeleteTask dt = new DeleteTask(this);
        try {
            result = dt.execute(Constants.BASE_URL + "ride/" + mRideKey).get();
            if (result.equals("Ok")) {
                (new SQLConnect()).deleteRide(mRideKey);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void sendMessage(String fromUsername, String toUserID, String rideKey, int code) {
        MessagingPostTask mpt = new MessagingPostTask(this);
        Messaging m = new Messaging(fromUsername, toUserID, rideKey, code);
        mpt.setMessaging(m);

        try {
            String result = mpt.execute(Constants.MESSAGING_URL).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (FirebaseUser.getUid().equals(authorID)) {
            getMenuInflater().inflate(R.menu.mnu_ride_detail, menu);
        }else {
            getMenuInflater().inflate(R.menu.mnu_blank, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.mnu_edit:
                intent = new Intent(RideDetailActivity.this, RideEditActivity.class);
                intent.putExtra(RideEditActivity.EXTRA_RIDE, ride);
                startActivity(intent);
                break;
            case R.id.mnu_delete:
                String result = deleteRide();
                if (result.equals("Ok")) {
                    intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Error deleting", Toast.LENGTH_LONG).show();
                }
                break;
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendMessageCreateList() {
        List<UserDays> ud = new ArrayList<>(ride.getRequest());
        ud.addAll(ride.getJoin());
        for(int i=0; i<ud.size(); i++){
            sendMessage(ride.getAuthor(), ud.get(i).getUserId(), ride.getID(), 60);
        }
    }
}
