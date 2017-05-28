package com.jrmeza.equipmentcontrol;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class MasterView extends AppCompatActivity {
    private Toolbar mToolbar;
    private FirebaseDatabase mFirebase;
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private EditText searchBar;
    TextWatcher searchTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            updateRecyclerView();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_view);

        searchBar = (EditText) findViewById(R.id.search_bar);
        mToolbar = (Toolbar) findViewById(R.id.app_toolbar);
        mRecyclerView = (RecyclerView) findViewById(R.id.master_list);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(new EquipmentAdapter(new Equipment[]{}));
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mFirebase = FirebaseDatabase.getInstance();
        DatabaseReference equipmentDatabase =
                mFirebase.getReference(MainActivity.EQUIPMENT_DB).child(MainActivity.STORE_ID);
        equipmentDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> parent = dataSnapshot.getChildren();
                int size = 0;
                for (DataSnapshot child : parent) {
                    size++;
                }
                Equipment[] equipmentList = new Equipment[size];
                int counter = 0;
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Equipment equipment = child.getValue(Equipment.class);
                    equipmentList[counter] = equipment;
                    counter++;
                }

                EquipmentAdapter equipmentAdapter = new EquipmentAdapter(equipmentList);
                mRecyclerView.setAdapter(equipmentAdapter);

                updateRecyclerView();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        searchBar.addTextChangedListener(searchTextWatcher);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateRecyclerView() {
        DatabaseReference equipmentDatabase =
                mFirebase.getReference(MainActivity.EQUIPMENT_DB).child(MainActivity.STORE_ID);

        equipmentDatabase.orderByKey().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> parent = dataSnapshot.getChildren();
                int size = 0;
                for (DataSnapshot child : parent) {
                    size++;
                }
                Equipment[] equipmentList = new Equipment[size];
                int counter = 0;
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Equipment equipment = child.getValue(Equipment.class);
                    equipmentList[counter] = equipment;
                    counter++;
                }
                ArrayList<Equipment> equipmentArrayList = new ArrayList<Equipment>();
                for (Equipment equipment : equipmentList) {
                    if (equipment.alias.toLowerCase().replace("#", "")
                            .contains(searchBar.getText().toString().toLowerCase().replace("#", ""))) {
                        equipmentArrayList.add(equipment);
                    }
                }

                EquipmentAdapter equipmentAdapter =
                        new EquipmentAdapter(equipmentArrayList.toArray(new Equipment[equipmentArrayList.size()]));
                mRecyclerView.setAdapter(equipmentAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.ViewHolder> {

        Equipment[] equipmentList;

        public EquipmentAdapter(Equipment[] equipmentList) {
            this.equipmentList = equipmentList;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.equipment_item_view, parent, false);
            return new EquipmentAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Equipment equipment = equipmentList[position];
            holder.equipmentName.setText(equipment.alias);
            holder.lastKnowTime.setText("" + equipment.lastTransaction.timestamp);
            holder.lastKnownTM.setText(equipment.lastTransaction.teamMember);
            SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a MM/dd/yyyy");
            if (equipment.lastTransaction.timestamp != 0)
                holder.lastKnowTime.setText(dateFormat.format(equipment.lastTransaction.timestamp));
            else
                holder.lastKnowTime.setText("No Records");
            switch (equipment.status) {
                case 0:
                    holder.status.setText("Available");
                    break;
                case 1:
                    holder.status.setText("Unavailable");
                    break;

                case 2:
                    holder.status.setText("Out For Repair");
                    break;
            }

        }

        @Override
        public int getItemCount() {
            return equipmentList.length;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            TextView equipmentName, status, lastKnownTM, lastKnowTime;

            public ViewHolder(View itemView) {
                super(itemView);
                equipmentName = (TextView) itemView.findViewById(R.id.equipment_name);
                status = (TextView) itemView.findViewById(R.id.equipment_status);
                lastKnownTM = (TextView) itemView.findViewById(R.id.last_known_tm);
                lastKnowTime = (TextView) itemView.findViewById(R.id.last_interaction_time);
            }
        }
    }
}