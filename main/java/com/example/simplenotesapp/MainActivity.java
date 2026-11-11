package com.example.simplenotesapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    // Explicit Firebase URL to ensure connection since it might be missing from google-services.json
    private static final String DATABASE_URL = "https://simplenotesapp-22f06-default-rtdb.firebaseio.com/";

    private EditText etNoteInput;
    private Button btnAdd;
    private ListView lvNotes;
    private CalendarView calendarView;
    
    // Initialize lists as final and early to prevent NullPointerExceptions
    private final List<Note> allNotesList = new ArrayList<>();
    private final List<Note> displayNotesList = new ArrayList<>();
    private ArrayAdapter<Note> adapter;
    private DatabaseReference databaseNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // 1. Initialize UI elements
        etNoteInput = findViewById(R.id.etNoteInput);
        btnAdd = findViewById(R.id.btnAdd);
        lvNotes = findViewById(R.id.lvNotes);
        calendarView = findViewById(R.id.calendarView);

        // 2. Setup the List Adapter
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayNotesList);
        lvNotes.setAdapter(adapter);

        // 3. Initialize Firebase with the specific project URL
        try {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            databaseNotes = FirebaseDatabase.getInstance(DATABASE_URL).getReference("notes").child(userId).child(userId);

            databaseNotes.keepSynced(true);
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed: " + e.getMessage());
            Toast.makeText(this, "Firebase Error. Check your connection.", Toast.LENGTH_LONG).show();
            return;
        }

        // 4. Real-time data sync from Firebase
        databaseNotes.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allNotesList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Note note = snapshot.getValue(Note.class);
                    if (note != null) {
                        note.setId(snapshot.getKey());
                        allNotesList.add(note);
                    }
                }
                // Sort: Newest notes first
                allNotesList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                
                // Show all notes by default
                updateDisplay(allNotesList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
                Toast.makeText(MainActivity.this, "Sync failed.", Toast.LENGTH_SHORT).show();
            }
        });

        // 5. Save Note Logic
        btnAdd.setOnClickListener(v -> {
            String text = etNoteInput.getText().toString().trim();
            if (!text.isEmpty()) {
                Note newNote = new Note(System.currentTimeMillis(), text);
                databaseNotes.push().setValue(newNote)
                    .addOnSuccessListener(aVoid -> {
                        etNoteInput.setText("");
                        Toast.makeText(MainActivity.this, "Note saved!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Save failed: " + e.getMessage());
                        Toast.makeText(MainActivity.this, "Could not save note.", Toast.LENGTH_SHORT).show();
                    });
            } else {
                Toast.makeText(MainActivity.this, "Please write something first", Toast.LENGTH_SHORT).show();
            }
        });

        // 6. Delete Note (Long press)
        lvNotes.setOnItemLongClickListener((parent, view, position, id) -> {
            if (position < displayNotesList.size()) {
                Note noteToDelete = displayNotesList.get(position);
                if (noteToDelete != null && noteToDelete.getId() != null) {
                    databaseNotes.child(noteToDelete.getId()).removeValue();
                    Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        });

        // 7. Filter by Date Logic
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long startOfDay = cal.getTimeInMillis();
            long endOfDay = startOfDay + (24L * 60 * 60 * 1000);

            ArrayList<Note> filtered = new ArrayList<>();
            for (Note n : allNotesList) {
                if (n.getTimestamp() >= startOfDay && n.getTimestamp() < endOfDay) {
                    filtered.add(n);
                }
            }
            
            updateDisplay(filtered);
            
            if (filtered.isEmpty()) {
                Toast.makeText(this, "No notes found for this date", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Safely update the list shown on screen
    private void updateDisplay(List<Note> newList) {
        displayNotesList.clear();
        displayNotesList.addAll(newList);
        adapter.notifyDataSetChanged();
    }
}
