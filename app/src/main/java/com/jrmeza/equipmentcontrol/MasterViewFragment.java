package com.jrmeza.equipmentcontrol;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class MasterViewFragment extends Fragment {
    private FirebaseDatabase mFirebase;
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private EditText searchBar;
    private Spinner searchBarFilter;
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


    public MasterViewFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_master_view, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
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
                    boolean containsSearchText = true;
                    boolean isPartOfSearchFilter = true;


                    long filterId = searchBarFilter.getSelectedItemId();
                    switch ((int) filterId){
                        // Equipment Name
                        case 0:
                            containsSearchText = equipment.alias.toLowerCase().replace("#", "")
                                    .contains(searchBar.getText().toString().toLowerCase().replace("#", ""));
                            break;

                        // Team Member Name
                        case 1:
                            containsSearchText = equipment.activeTM.toLowerCase()
                                    .contains(searchBar.getText().toString().toLowerCase());
                            break;

                        // Equipment is Available
                        case 2:
                            isPartOfSearchFilter = equipment.status == 0;
                            containsSearchText = equipment.alias.toLowerCase().replace("#", "")
                                    .contains(searchBar.getText().toString().toLowerCase().replace("#", ""));
                            break;

                        // Equipment Is Unavailable
                        case 3:
                            isPartOfSearchFilter = equipment.status == 1;
                            containsSearchText = equipment.alias.toLowerCase().replace("#", "")
                                    .contains(searchBar.getText().toString().toLowerCase().replace("#", ""));
                            break;

                        // Equipment Is Out For Repair
                        case 4:
                            isPartOfSearchFilter = equipment.status == 2;
                            containsSearchText = equipment.alias.toLowerCase().replace("#", "")
                                    .contains(searchBar.getText().toString().toLowerCase().replace("#", ""));
                            break;

                        default:

                            break;

                    }

                    if (containsSearchText && isPartOfSearchFilter) {
                        equipmentArrayList.add(equipment);
                    }
                }

                MasterViewFragment.EquipmentAdapter equipmentAdapter =
                        new MasterViewFragment.EquipmentAdapter(equipmentArrayList.toArray(new Equipment[equipmentArrayList.size()]));
                mRecyclerView.setAdapter(equipmentAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public class EquipmentAdapter extends RecyclerView.Adapter<MasterViewFragment.EquipmentAdapter.ViewHolder> {

        Equipment[] equipmentList;

        public EquipmentAdapter(Equipment[] equipmentList) {
            this.equipmentList = equipmentList;
        }

        @Override
        public MasterViewFragment.EquipmentAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.equipment_item_view, parent, false);
            return new MasterViewFragment.EquipmentAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MasterViewFragment.EquipmentAdapter.ViewHolder holder, int position) {
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

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        searchBar = (EditText) getActivity().findViewById(R.id.search_bar);
        searchBarFilter = (Spinner) getActivity().findViewById(R.id.search_bar_filter);
        mRecyclerView = (RecyclerView) getActivity().findViewById(R.id.master_list);
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(new MasterViewFragment.EquipmentAdapter(new Equipment[]{}));
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(getContext()));
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

                MasterViewFragment.EquipmentAdapter equipmentAdapter = new MasterViewFragment.EquipmentAdapter(equipmentList);
                mRecyclerView.setAdapter(equipmentAdapter);

                updateRecyclerView();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        searchBar.addTextChangedListener(searchTextWatcher);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.search_bar_filters, android.R.layout.simple_spinner_dropdown_item);
        searchBarFilter.setAdapter(adapter);
        searchBarFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateRecyclerView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }
}
