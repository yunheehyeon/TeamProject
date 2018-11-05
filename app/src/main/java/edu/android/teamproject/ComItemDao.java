package edu.android.teamproject;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.facebook.FacebookSdk.getApplicationContext;

public class ComItemDao implements ChildEventListener{

    interface ComItemCallback{
        void dateCallback();
    }
    interface EndCallback{
        void endUpload();
    }

    private ComItemCallback callback;
    private EndCallback endCallback;

    private FirebaseDatabase database;
    private DatabaseReference reference;

    private FirebaseAuth mAuth;
    private String providerId,uid,name,email;
    private Uri photoUrl;
    private UserInfo profile;

    private List<ComItem> comItems = new ArrayList<>();

    private static ComItemDao comItemDaoInstance;

    public static ComItemDao getComItemInstance(Object object){
        if(comItemDaoInstance == null){
            comItemDaoInstance = new ComItemDao();
        }
        if(object instanceof ComItemCallback){
            comItemDaoInstance.callback = (ComItemCallback) object;
        }
        if(object instanceof EndCallback){
            comItemDaoInstance.endCallback = (EndCallback) object;
        }

        return comItemDaoInstance;
    }

    private ComItemDao(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            for (UserInfo profile : user.getProviderData()) {
                // Id of the provider (ex: google.com)
                providerId = profile.getProviderId();

                // UID specific to the provider
                uid = profile.getUid();

                // Name, email address, and profile photo Url
                name = profile.getDisplayName();
                email =  profile.getEmail();
                photoUrl = profile.getPhotoUrl();
            }

        }
        database = FirebaseDatabase.getInstance();
        reference = database.getReference().child("ComItem");
        reference.addChildEventListener(this);
    }

    public void insert(ComItem comItem) {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date now = new Date();
        String date = formatter.format(now);
        comItem.setDate(date);
        comItem.setUserEmail(email.split("@")[0]);
        comItem.setUserId(uid);
        comItem.setViewCount(0);

        reference.push().setValue(comItem);
    }

    public List<ComItem> update(){
        return comItems;
    }

    @Override
    public void onChildAdded( DataSnapshot dataSnapshot,  String s) {
        ComItem comItem = dataSnapshot.getValue(ComItem.class);
        comItem.setItemId(dataSnapshot.getKey());
        comItems.add(comItem);

        callback.dateCallback();
    }

    @Override
    public void onChildChanged( DataSnapshot dataSnapshot,  String s) {

    }

    @Override
    public void onChildRemoved( DataSnapshot dataSnapshot) {

    }

    @Override
    public void onChildMoved( DataSnapshot dataSnapshot,  String s) {

    }

    @Override
    public void onCancelled( DatabaseError databaseError) {

    }

    public static final String community = "Community/";
    private int minTerm = 0;
    public List<String> photoUpload(Context context, final List<Uri> uris) {

        final ProgressDialog progressDialog = new ProgressDialog(
                context);

        progressDialog.setMessage("저장중입니다.");
        progressDialog.show();

        List<String> filenames = new ArrayList<>();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl("gs://timproject-14aaa.appspot.com");
        for(Uri uri : uris) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_mmss");
            Date now = new Date();
            String filename = formatter.format(now)+ minTerm + ".png";

            filenames.add(filename);
            storageRef.child(community + filename).putFile(uri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            if(uris.size() == minTerm) {
                                progressDialog.dismiss();
                                endCallback.endUpload();
                            }
                        }
                    })
                    //실패시
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(), "업로드 실패!", Toast.LENGTH_SHORT).show();
                        }
                    });

            minTerm++;
        }

        return filenames;
    }

}
