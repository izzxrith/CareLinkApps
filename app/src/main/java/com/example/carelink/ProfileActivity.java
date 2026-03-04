package com.example.carelink;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUsername, tvHeartRate, tvCalories, tvWeight, tvBirthday, tvPhone, tvAddress;
    private LinearLayout rowInformation, rowFaq, rowLogout;
    private LinearLayout navHome, navMessages, navSchedule, navProfile;
    private Toolbar toolbar;
    private ImageView profileImage;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private static final int CALL_PERMISSION_REQUEST_CODE = 100;
    private static final int IMAGE_REQUEST_CODE = 101;

    private String selectedFamilyMemberId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        initViews();
        setupToolbar();
        setupBottomNavigation();
        setupClickListeners();
        loadUserData();
        setupRealTimeStats();
        loadSelectedFamilyMember();
    }

    private void initViews() {

        toolbar = findViewById(R.id.toolbar);

        profileImage = findViewById(R.id.profileImage);

        tvUsername = findViewById(R.id.tvUsername);
        tvHeartRate = findViewById(R.id.tvHeartRate);
        tvCalories = findViewById(R.id.tvCalories);
        tvWeight = findViewById(R.id.tvWeight);

        tvBirthday = findViewById(R.id.tvBirthday);
        tvPhone = findViewById(R.id.tvPhone);
        tvAddress = findViewById(R.id.tvAddress);

        rowInformation = findViewById(R.id.rowInformation);
        rowFaq = findViewById(R.id.rowFaq);
        rowLogout = findViewById(R.id.rowLogout);

        navHome = findViewById(R.id.navHome);
        navMessages = findViewById(R.id.navMessages);
        navSchedule = findViewById(R.id.navSchedule);
        navProfile = findViewById(R.id.navProfile);

        profileImage.setOnClickListener(v -> openImagePicker());
    }

    private void setupToolbar() {

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("Profile");
    }

    private void setupBottomNavigation() {

        navHome.setOnClickListener(v ->
                startActivity(new Intent(this, DashboardActivity.class)));

        navMessages.setOnClickListener(v ->
                startActivity(new Intent(this, MessageActivity.class)));

        navSchedule.setOnClickListener(v ->
                startActivity(new Intent(this, ScheduleActivity.class)));

        navProfile.setOnClickListener(v ->
                Toast.makeText(this, "Already here", Toast.LENGTH_SHORT).show());
    }

    private void setupClickListeners() {

        rowInformation.setOnClickListener(v -> showEditDialog());

        rowFaq.setOnClickListener(v -> showFaqDialog());

        rowLogout.setOnClickListener(v -> logout());
    }

    private void loadUserData() {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .addSnapshotListener((doc, error) -> {

                    if (doc == null || !doc.exists()) return;

                    tvUsername.setText(getSafe(doc.getString("name")));
                    tvBirthday.setText(getSafe(doc.getString("birthday")));
                    tvPhone.setText(getSafe(doc.getString("phone")));
                    tvAddress.setText(getSafe(doc.getString("address")));

                    String imageUrl = doc.getString("profileImage");

                    if (imageUrl != null && !imageUrl.isEmpty()) {

                        Glide.with(ProfileActivity.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(profileImage);

                    } else {

                        profileImage.setImageResource(R.drawable.default_profile);

                    }

                });
    }

    private String getSafe(String value) {

        return value == null || value.isEmpty() ? "Not set" : value;
    }

    private void showEditDialog() {

        Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.dialog_edit_profile);

        EditText etName = dialog.findViewById(R.id.etName);
        EditText etBirthday = dialog.findViewById(R.id.etBirthday);
        EditText etPhone = dialog.findViewById(R.id.etPhone);
        EditText etAddress = dialog.findViewById(R.id.etAddress);

        Button btnSave = dialog.findViewById(R.id.btnSave);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        etName.setText(tvUsername.getText());
        etBirthday.setText(tvBirthday.getText());
        etPhone.setText(tvPhone.getText());
        etAddress.setText(tvAddress.getText());

        etBirthday.setFocusable(false);

        etBirthday.setOnClickListener(v -> openDatePicker(etBirthday));

        btnSave.setOnClickListener(v -> {

            String name = etName.getText().toString().trim();
            String birthday = etBirthday.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String address = etAddress.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Required");
                return;
            }

            if (!phone.isEmpty() && !phone.matches("^01[0-9]{8,9}$")) {
                etPhone.setError("Invalid phone");
                return;
            }

            saveProfile(name, birthday, phone, address);

            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void openDatePicker(EditText etBirthday) {

        Calendar cal = Calendar.getInstance();

        new DatePickerDialog(this,
                (view, year, month, day) ->
                        etBirthday.setText(day + "/" + (month+1) + "/" + year),
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void saveProfile(String name, String birthday, String phone, String address) {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) return;

        Map<String, Object> map = new HashMap<>();

        map.put("name", name);
        map.put("birthday", birthday);
        map.put("phone", phone);
        map.put("address", address);
        map.put("updatedAt", System.currentTimeMillis());

        db.collection("users")
                .document(user.getUid())
                .update(map)
                .addOnSuccessListener(unused -> {

                    tvUsername.setText(getSafe(name));
                    tvBirthday.setText(getSafe(birthday));
                    tvPhone.setText(getSafe(phone));
                    tvAddress.setText(getSafe(address));

                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void openImagePicker() {

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");

        startActivityForResult(intent, IMAGE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_REQUEST_CODE
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            Uri uri = data.getData();

            uploadImage(uri);
        }
    }

    private void uploadImage(Uri uri) {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null || uri == null) return;

        profileImage.setImageURI(uri);

        StorageReference ref = storageRef.child("profile_images/" + user.getUid() + ".jpg");

        ref.putFile(uri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(downloadUrl -> {

                            db.collection("users")
                                    .document(user.getUid())
                                    .update("profileImage", downloadUrl.toString())
                                    .addOnSuccessListener(unused -> {

                                        Glide.with(ProfileActivity.this)
                                                .load(downloadUrl)
                                                .into(profileImage);

                                        Toast.makeText(ProfileActivity.this,
                                                "Profile image updated successfully",
                                                Toast.LENGTH_SHORT).show();
                                    });

                        })
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Upload failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void setupRealTimeStats(){

        FirebaseUser user = mAuth.getCurrentUser();

        if(user == null) return;

        db.collection("users")
                .document(user.getUid())
                .collection("health_stats")
                .document("current")
                .addSnapshotListener((doc,error)->{

                    if(doc == null || !doc.exists()) return;

                    Long bpm = doc.getLong("bpm");
                    Long cal = doc.getLong("calories");
                    Long weight = doc.getLong("weight");

                    tvHeartRate.setText((bpm != null ? bpm : 0) + " bpm");
                    tvCalories.setText((cal != null ? cal : 0) + " cal");
                    tvWeight.setText((weight != null ? weight : 0) + " kg");

                });
    }

    private void loadSelectedFamilyMember(){

        SharedPreferences prefs=getSharedPreferences("CareLinkPrefs",MODE_PRIVATE);

        selectedFamilyMemberId=
                prefs.getString("selected_member",null);

        if(selectedFamilyMemberId!=null)
            loadFamilyMember(selectedFamilyMemberId);
    }

    private void loadFamilyMember(String id){

        FirebaseUser user=mAuth.getCurrentUser();

        db.collection("users")
                .document(user.getUid())
                .collection("family_members")
                .document(id)
                .addSnapshotListener((doc,error)->{

                    if(doc==null)return;

                    tvUsername.setText(doc.getString("name"));
                });
    }

    private void showFaqDialog(){

        AlertDialog.Builder builder=
                new AlertDialog.Builder(this);

        builder.setTitle("FAQ");

        builder.setMessage("Contact support");

        builder.setPositiveButton("Call",(d,w)->{

            Intent call=
                    new Intent(Intent.ACTION_CALL,
                            Uri.parse("tel:+60165570027"));

            if(ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CALL_PHONE)
                    != PackageManager.PERMISSION_GRANTED){

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CALL_PHONE},
                        CALL_PERMISSION_REQUEST_CODE);

            }else startActivity(call);

        });

        builder.setNegativeButton("Close",null);

        builder.show();
    }

    private void logout(){

        mAuth.signOut();

        startActivity(new Intent(this,IntroActivity.class));

        finish();
    }
}