package com.example.tronku.eventmanager;


import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.IllegalCharsetNameException;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class DashboardFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView eventsRecyclerView;
    private ArrayList<Event> eventList = new ArrayList<>();
    private View view;
    private CardView filter;
    private EventsAdapter adapter;
    private boolean hasExtra = false;
    private TextView noEvent, infoText;

    public DashboardFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        adapter = new EventsAdapter(getContext(), eventList);
        swipeRefreshLayout = view.findViewById(R.id.swiperefresh);
        eventsRecyclerView = view.findViewById(R.id.eventsListView);
        infoText = view.findViewById(R.id.infoText);
        noEvent = view.findViewById(R.id.noevents);
        filter = view.findViewById(R.id.filter);
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventsRecyclerView.setAdapter(adapter);

        if(hasExtra)
            infoText.setText(getArguments().getString("name") + " events list - ");

        updateEvents(hasExtra);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateEvents(hasExtra);
            }
        });

        filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Dialog dialog = new Dialog(getContext());
                dialog.setContentView(R.layout.filter_dialog);
                Button society = dialog.findViewById(R.id.societiesFilter);
                CardView remove = dialog.findViewById(R.id.removeFilter);

                society.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                        Intent society = new Intent(dialog.getContext(), SocietyFilterActivity.class);
                        society.putExtra("upcoming", "true");
                        startActivity(society);
                    }
                });

                remove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                        Intent remove = new Intent(dialog.getContext(), MainActivity.class);
                        remove.putExtra("remove","true");
                        remove.putExtra("upcoming", "true");
                        startActivity(remove);
                    }
                });

                dialog.show();
            }
        });

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments()!=null){
            hasExtra = true;
        }
    }

    public void updateEvents(boolean hasExtra) {
        eventList.clear();
        String url;
        if(hasExtra){
            url = "http://13.126.64.67/api/society/" + getArguments().getString("society") + "/events/upcoming";
        }
        else
            url = API.EVENTS_API;

        final String token = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("token", "token_no");
        Log.d("token", token);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                for(int i=0; i<response.length(); i++) {
                    try {
                        JSONObject event = response.getJSONObject(i);
                        String name = event.getString("name");
                        String society = event.getString("society_name");
                        String desc = event.getString("notes");
                        String image = event.getString("image");
                        String startFullDate = event.getString("start_day");
                        String endFullDate = event.getString("end_day");
                        String contact_person = event.getString("contact_person");
                        String contact_no = event.getString("contact_number");
                        String venue = event.getString("venue");
                        String logo = event.getString("society_logo");
                        String regLink = event.getString("registration_link");


                        eventList.add(new Event(society, name, desc, startFullDate, endFullDate, image, contact_person, contact_no, venue, logo, regLink));

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("error", error.getCause().toString());
            }
        })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Token " + token);
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(getContext());
        queue.add(request);
        queue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<JSONObject>() {
            @Override
            public void onRequestFinished(Request<JSONObject> request) {
                if(eventList.size()!=0) {
                    adapter.updateEvents(eventList);
                    eventsRecyclerView.setVisibility(View.VISIBLE);
                    swipeRefreshLayout.setRefreshing(false);
                    noEvent.setVisibility(View.INVISIBLE);
                }
                else {
                    noEvent.setVisibility(View.VISIBLE);
                }
            }
        });
    }


}