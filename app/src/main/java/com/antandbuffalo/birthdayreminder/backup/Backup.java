package com.antandbuffalo.birthdayreminder.backup;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.antandbuffalo.birthdayreminder.R;
import com.antandbuffalo.birthdayreminder.models.DateOfBirth;
import com.antandbuffalo.birthdayreminder.utilities.Constants;
import com.antandbuffalo.birthdayreminder.utilities.DataHolder;
import com.antandbuffalo.birthdayreminder.utilities.Storage;
import com.antandbuffalo.birthdayreminder.utilities.Util;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Backup extends AppCompatActivity {
    FirebaseUser firebaseUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.backup);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button backupNow = findViewById(R.id.backupNow);
        backupNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getStoragePermission(Constants.MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE)) {
                    updateLocalBackup();
                    // backupToFirebase(FirebaseFirestore.getInstance(), firebaseUser);
                    // File file = Util.getLocalFile("dob.txt");
                }
                else {
                    Toast.makeText(DataHolder.getInstance().getAppContext(), "Please provide permission to access local storage", Toast.LENGTH_SHORT).show();
                }
                syncNow();
            }
        });

        Button selectAccount = findViewById(R.id.selectAccount);
        selectAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        firebaseUser = checkFirebaseUser();
        if(firebaseUser != null) {
            Log.d("FirebaseUser", firebaseUser.getEmail());
            TextView textView = findViewById(R.id.accountName);
            textView.setText("Account: " + firebaseUser.getEmail());
        }
        else {
            Log.d("FirebaseError", "User Not found. Please login first");
            startFirebaseAuth();
        }
        getServerBackupTime();
        getLocalBackupTime();
        //restoreFromFirebase(FirebaseFirestore.getInstance(), firebaseUser);
    }

    public void updateLocalBackup() {
        Util.writeToFile(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("BRJB", item.getItemId() + "");
        int id = item.getItemId();
        if(id == android.R.id.home) {
            Log.d("BRJB", item.getItemId() + " : back");
        }
        finish();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public Boolean getStoragePermission(int permissionType) {
        switch (permissionType) {
            case Constants.MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE: {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            Constants.MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);

                    return false;
                } else {
                    // Permission has already been granted
                    return true;
                }
            }
            case Constants.MY_PERMISSIONS_READ_EXTERNAL_STORAGE: {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            Constants.MY_PERMISSIONS_READ_EXTERNAL_STORAGE);

                    return false;
                } else {
                    // Permission has already been granted
                    return true;
                }
            }

        }
        return false;
    }

    public void readPermission(Boolean isGranted) {
        if(isGranted) {
            File file = Util.getLocalFile("dob.txt");
        }
    }

    public void writePermission(Boolean isGranted) {
        if(isGranted) {
            updateLocalBackup();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constants.MY_PERMISSIONS_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    readPermission(true);

                } else {
                    // permission denied, boo! Disable the
                    readPermission(false);
                }
                return;
            }
            case Constants.MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    writePermission(true);

                } else {
                    // permission denied, boo! Disable the
                    writePermission(false);
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    public void startFirebaseAuth() {
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());
// Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                Constants.firebaseSignInCode);
    }

    public FirebaseUser checkFirebaseUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // User is signed in
            System.out.println("User available");
        } else {
            // No user is signed in
            System.out.println("User not available");
        }
        return user;
    }

    public void backupToFirebase(FirebaseFirestore db, FirebaseUser firebaseUser) {
        // Create a new user with a first and last name
        Map<String, DateOfBirth> dateOfBirthMap = Util.getDateOfBirthMap();
        DocumentReference documentReference = db.collection(firebaseUser.getUid()).document("friends");
        documentReference.set(dateOfBirthMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("Success", "data updated successfully");
                updateLastUpdatedTime(db, firebaseUser);
                Toast.makeText(DataHolder.getInstance().getAppContext(), "Successfully updated to server", Toast.LENGTH_SHORT).show();
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("F", e.getLocalizedMessage());
                Toast.makeText(DataHolder.getInstance().getAppContext(), "Error while updating to server", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void restoreFromFirebase(FirebaseFirestore firebaseFirestore, FirebaseUser firebaseUser) {
        DocumentReference documentReference = firebaseFirestore.collection(firebaseUser.getUid()).document("friends");
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d("FirebaseGetData", "DocumentSnapshot data: " + document.getData());
                        Util.inserDateOfBirthFromServer(document.getData());
                        Storage.setDbBackupTime(Util.getDateFromString(Storage.getServerBackupTime(), Constants.backupDateFormatToStore));
                        getLocalBackupTime();
                        DataHolder.getInstance().refresh = true;
                    } else {
                        Log.d("FirebaseGetData", "No such document");
                    }
                } else {
                    Log.d("FirebaseGetData", "get failed with ", task.getException());
                }
            }
        });
    }

    public void updateLastUpdatedTime(FirebaseFirestore firebaseFirestore, FirebaseUser firebaseUser) {
        Map<String, Date> updateTime = new HashMap<>();
        updateTime.put(Constants.serverBackupTime, Util.getDateFromString(Storage.getDbBackupTime(), Constants.backupDateFormatToStore));
        DocumentReference documentReference = firebaseFirestore.collection(firebaseUser.getUid()).document("settings");
        documentReference.set(updateTime).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("Success", "Last updated time updated");
                Storage.setServerBackupTime(Util.getDateFromString(Storage.getDbBackupTime(), Constants.backupDateFormatToStore));
                TextView cloudBackup = findViewById(R.id.cloudBackup);
                cloudBackup.setText("Server: " + Storage.getServerBackupTime());
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("F", e.getLocalizedMessage());
            }
        });
    }

    public void getFirebaseLastUpdatedTime(FirebaseFirestore firebaseFirestore, FirebaseUser firebaseUser, String caller) {
        DocumentReference documentReference = firebaseFirestore.collection(firebaseUser.getUid()).document("settings");
        documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Timestamp serverTimestamp = (Timestamp) documentSnapshot.get(Constants.serverBackupTime);
                String formattedDate = Util.getStringFromDate(serverTimestamp.toDate(), Constants.backupDateFormatToStore);
                Storage.putString(Util.getSharedPreference(), Constants.serverBackupTime,formattedDate);
                switch (caller) {
                    case "updateUI": {
                        TextView cloudBackup = findViewById(R.id.cloudBackup);
                        cloudBackup.setText("Server: " + formattedDate);
                        break;
                    }
                    case "syncNow": {
                        compareBackupTimes();
                        break;
                    }
                    case "saveLocally": {

                        break;
                    }
                }
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("ERROR", "Not able to get last updated time");
            }
        });
    }


    public void getFirebaseLastUpdatedTime(FirebaseFirestore firebaseFirestore, FirebaseUser firebaseUser) {
        getFirebaseLastUpdatedTime(firebaseFirestore, firebaseUser, "");
    }

    public void syncNow() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getAllNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected()) {
            getFirebaseLastUpdatedTime(FirebaseFirestore.getInstance(), firebaseUser, "syncNow");
        }
        else {
            Toast.makeText(DataHolder.getInstance().getAppContext(), "Please connect to Internet", Toast.LENGTH_LONG).show();
        }
    }

    public void compareBackupTimes() {
        String dbBackupString = Storage.getDbBackupTime();
        String serverBackupString = Storage.getServerBackupTime();
        if(!dbBackupString.equalsIgnoreCase("") && serverBackupString != null) {
            Date dbBackupDate = Util.getDateFromString(Storage.getDbBackupTime(), Constants.backupDateFormatToStore);
            Date serverBackupDate = Util.getDateFromString(Storage.getServerBackupTime(), Constants.backupDateFormatToStore);

            if(serverBackupDate == null) {
                backupToFirebase(FirebaseFirestore.getInstance(), firebaseUser);
                return;
            }
            if(dbBackupDate == null) {
                restoreFromFirebase(FirebaseFirestore.getInstance(), firebaseUser);
                return;
            }
            if(dbBackupDate.getTime() == serverBackupDate.getTime()) {
                return;
            }
            if(dbBackupDate.getTime() > serverBackupDate.getTime()) {
                // Local data is latest. Upload to server
                backupToFirebase(FirebaseFirestore.getInstance(), firebaseUser);
            }
            else {
                // Server data is latest. Download
                restoreFromFirebase(FirebaseFirestore.getInstance(), firebaseUser);
            }
        }
    }

    public void getServerBackupTime() {
        TextView cloudBackup = findViewById(R.id.cloudBackup);
        cloudBackup.setText("Server: " + Storage.getServerBackupTime());
        getFirebaseLastUpdatedTime(FirebaseFirestore.getInstance(), firebaseUser, "updateUI");
    }

    public void getLocalBackupTime() {
        TextView localBackup = findViewById(R.id.localBackup);
        localBackup.setText("Local: " + Storage.getDbBackupTime());
    }

    // download prgress link
    // https://developers.google.com/drive/android/files

    // use firebase instead of app data
    // https://ammar.lanui.online/integrate-google-drive-rest-api-on-android-app-bc4ddbd90820

}