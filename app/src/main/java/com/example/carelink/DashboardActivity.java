package com.example.carelink;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import com.example.carelink.adapters.TopDoctorAdapter;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvSubtitle, tvSeeAllLocation;
    private EditText etSearch;
    private LinearLayout btnDoctor, btnMonitor, btnEmotion, btnAmbulance, btnLink;
    private CardView cardBanner;
    private RecyclerView rvTopDoctors;
    private ImageView ivMapPreview;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TopDoctorAdapter doctorAdapter;
    private List<DoctorItem> doctorList;
    private EmergencyMonitor emergencyMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        loadUserData();
        setupClickListeners();
        loadTopDoctors();
        startEmergencyMonitoring();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        etSearch = findViewById(R.id.etSearch);
        btnDoctor = findViewById(R.id.btnDoctor);
        btnMonitor = findViewById(R.id.btnMonitor);
        btnEmotion = findViewById(R.id.btnEmotion);
        btnAmbulance = findViewById(R.id.btnAmbulance);
        cardBanner = findViewById(R.id.cardBanner);
        rvTopDoctors = findViewById(R.id.rvTopDoctors);
        ivMapPreview = findViewById(R.id.ivMapPreview);
        btnLink = findViewById(R.id.btnLink);
        tvSeeAllLocation = findViewById(R.id.tvSeeAllLocation);

        setupCustomBottomNavigation();

        rvTopDoctors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        doctorList = new ArrayList<>();
        doctorAdapter = new TopDoctorAdapter(doctorList, this::onDoctorClick);
        rvTopDoctors.setAdapter(doctorAdapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupCustomBottomNavigation() {
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navMessages = findViewById(R.id.navMessages);
        LinearLayout navSchedule = findViewById(R.id.navSchedule);
        LinearLayout navProfile = findViewById(R.id.navProfile);

        if (navHome == null || navMessages == null || navSchedule == null || navProfile == null) {
            Toast.makeText(this, "Navigation not found", Toast.LENGTH_SHORT).show();
            return;
        }

        navHome.setOnClickListener(v -> {
            Toast.makeText(this, "Already on Home", Toast.LENGTH_SHORT).show();
        });

        navMessages.setOnClickListener(v -> {
            Intent intent = new Intent(this, MessageActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        navSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            if (name != null && !name.isEmpty()) {
                tvWelcome.setText("Welcome, " + name);
            } else {
                db.collection("users").document(user.getUid())
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String userName = doc.getString("name");
                                if (userName != null) {
                                    tvWelcome.setText("Welcome, " + userName);
                                }
                            }
                        });
            }
        }
    }

    private void setupClickListeners() {
        etSearch.setOnClickListener(v -> {
            startActivity(new Intent(this, BookingActivity.class));
        });

        btnDoctor.setOnClickListener(v -> {
            startActivity(new Intent(this, BookingActivity.class));
        });

        btnMonitor.setOnClickListener(v -> {
            startActivity(new Intent(this, MonitorActivity.class));
        });

        btnEmotion.setOnClickListener(v -> {
            Intent intent = new Intent(this, EmotionActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        btnAmbulance.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(android.net.Uri.parse("tel:999"));
            startActivity(intent);
        });

        cardBanner.setOnClickListener(v -> {
            Toast.makeText(this, "Learn more about family health", Toast.LENGTH_SHORT).show();
        });

        ivMapPreview.setOnClickListener(v -> {
            startActivity(new Intent(this, LocationMapActivity.class));
        });

        btnLink.setOnClickListener(v -> {
            startActivity(new Intent(this, QRCodeActivity.class));
        });

        tvSeeAllLocation.setOnClickListener(v -> {
            startActivity(new Intent(this, LocationMapActivity.class));
        });
    }

    private void loadTopDoctors() {
        doctorList.add(new DoctorItem("Dr. Sarah", "Nephrology (Kidney diseases & Care)", "4.7", "2km away", R.drawable.ic_doctor_male));
        doctorList.add(new DoctorItem("Dr. Rajesh Kumar", "Radiology (X-ray & CT Scan)", "4.9", "1.5km away", R.drawable.ic_doctor_female));
        doctorList.add(new DoctorItem("Dr. Lim Mei Hua", "General Surgery", "4.8", "3km away", R.drawable.ic_doctor_male));
        doctorList.add(new DoctorItem("Dr. Ahmad Abdullah", "Dermatology (Skin & Hair)", "4.6", "2.5km away", R.drawable.ic_doctor_female));

        doctorAdapter.notifyDataSetChanged();
    }

    private void onDoctorClick(DoctorItem doctor) {
        Intent intent = new Intent(this, BookingActivity.class);
        intent.putExtra("DOCTOR_NAME", doctor.name);
        intent.putExtra("DOCTOR_SPECIALTY", doctor.specialty);
        startActivity(intent);
    }

    public static class DoctorItem {
        public String name, specialty, rating, distance;
        public int imageRes;

        public DoctorItem(String name, String specialty, String rating, String distance, int imageRes) {
            this.name = name;
            this.specialty = specialty;
            this.rating = rating;
            this.distance = distance;
            this.imageRes = imageRes;
        }
    }

    private void startEmergencyMonitoring() {
        Intent fallService = new Intent(this, FallDetectionService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(fallService);
        } else {
            startService(fallService);
        }

        emergencyMonitor = new EmergencyMonitor(this);
        emergencyMonitor.startMonitoring();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (emergencyMonitor != null) {
            emergencyMonitor.stopMonitoring();
        }
    }
}