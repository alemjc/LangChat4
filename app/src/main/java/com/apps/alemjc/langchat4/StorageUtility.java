package com.apps.alemjc.langchat4;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ImageView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by alemjc on 10/16/16.
 */
public class StorageUtility {

    private StorageUtility(){}

    /**
     * @param context Activity of caller
     * @param imageView Image view that will be receiving the file bitmap
     * @param firebaseStorage Firebase storage that contains the uploaded files
     * @param uid id of user that owns the image
     * @param fileName name of the file in the firebase storage
     * @param imagePath path of file in firebase storage
     */
    public static void setImage(Activity context, final ImageView imageView, FirebaseStorage firebaseStorage,
                                String uid, String fileName, String imagePath, Handler uiHandler, Handler bgHandler){

        if(context == null){
            Log.d("StorageUtility", "context is null");
            return;
        }


        SharedPreferences sharedPreferences = context.getPreferences(Context.MODE_PRIVATE);
        Log.d("StorageUtility", "contains it? "+sharedPreferences.contains(uid));


        if(sharedPreferences.contains(uid)){
            String unmarshalled = sharedPreferences.getString(uid, null);
            Log.d("StorageUtility", "unmarshalled: "+unmarshalled);

            // User changed picture so would need to download new image.
            if(unmarshalled == null){
                if(firebaseStorage != null){
                    downloadImage(context, imageView, firebaseStorage, uid, fileName, imagePath);
                }
                else{
                    downloadImage(context, imageView, uid, fileName, imagePath, uiHandler, bgHandler);
                }

            }
            // We have the user image so we can retrieve from local storage
            else{
                String imageInfoSet[] = unmarshalled.split("&");
                Log.d("StorageUtility", "imageInfoSet[0] = "+imageInfoSet[0]);
                Log.d("StorageUtility", String.format("imageInfoSet[0] = %s and fileName = %s", imageInfoSet[0], fileName));

                if(!imageInfoSet[0].equals(fileName)){
                    if(firebaseStorage != null)
                        downloadImage(context, imageView, firebaseStorage, uid, fileName, imagePath);
                    else
                        downloadImage(context, imageView, uid, fileName, imagePath, uiHandler, bgHandler);
                }
                else{
                    Log.d("StorageUtility", "retrieving from local storage");
                    Bitmap bitmap = retrieveFromLocalStorage(context, imageInfoSet[1]);
                    if(bitmap != null){
                        imageView.setBackground(null);
                        imageView.setImageBitmap(bitmap);
                    }

                }
            }
        }
        // First time we encounter this user so will need to download picture
        else{
            if(firebaseStorage != null)
                downloadImage(context, imageView, firebaseStorage, uid, fileName, imagePath);
            else
                downloadImage(context, imageView, uid, fileName, imagePath, uiHandler, bgHandler);
        }

    }

    /**
     *
     * @param context Activity of caller
     * @param imageView Image view that will be receiving the file bitmap
     * @param uid id of user that owns the image
     * @param fileName name of the file
     * @param imagePath path of file
     */

    private static void downloadImage(final Activity context, final ImageView imageView, final String uid,
                                      final String fileName,
                                      final String imagePath, final Handler uiHandler, Handler bgHandler){

        bgHandler.post(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection httpURLConnection = null;
                try{
                    URL url = new URL(imagePath);
                    httpURLConnection = (HttpURLConnection) url.openConnection();

                    httpURLConnection.connect();

                    if(httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK){
                        InputStream inputStream = url.openStream();

                        final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        String path = Environment.getExternalStorageDirectory().getPath();
                        OutputStream outputStream = null;
                        final File file = new File(path, uid+fileName);
                        outputStream = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
                        outputStream.flush();
                        outputStream.close();

                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setBackground(null);
                                imageView.setImageBitmap(bitmap);
                                setUserFileDetails(context, uid, fileName, Uri.parse(file.getAbsolutePath()));
                            }
                        });
                    }

                }
                catch(MalformedURLException e){
                    Log.d("StorageUtility", e.getMessage());
                }
                catch(IOException e){
                    Log.d("StorageUtility", e.getMessage());
                }
                finally {
                    if(httpURLConnection != null){
                        httpURLConnection.disconnect();
                    }
                }
            }
        });


    }


    /**
     *
     * @param context Activity of caller
     * @param imageView Image view that will be receiving the file bitmap
     * @param firebaseStorage Filebase storage that contains the uploaded files
     * @param uid id of user that owns the image
     * @param fileName name of the file in the firebase storage
     * @param fsStorageImagePath path of file in firebase storage
     */

    private static void downloadImage(final Activity context, final ImageView imageView, FirebaseStorage firebaseStorage,
                                      final String uid, final String fileName, String fsStorageImagePath){

        StorageReference fsRef = firebaseStorage.getReference();
        Log.d("fFragments", fsStorageImagePath);
        StorageReference storageReference = fsRef.child(fsStorageImagePath);


        final File file = new File(context.getFilesDir(), fileName);

        storageReference.getFile(file).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                InputStream inputStream = null;
                Log.d("StorageUtility", "filePath: "+file.getPath());
                try{
                    inputStream = new FileInputStream(file);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    imageView.setBackground(null);
                    imageView.setImageBitmap(bitmap);
                    setUserFileDetails(context, uid, fileName, Uri.fromFile(file));

                }catch (FileNotFoundException e){
                    Log.e("StorageUtility", e.getMessage());
                }
                finally {
                    if(inputStream != null){
                        try{
                            inputStream.close();
                        }catch (IOException e){
                            Log.e("StorageUtility",e.getMessage());
                        }
                    }
                }
            }
        }).addOnFailureListener(context, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Failed to download file. For now will let image sit empty. //TODO: check if image should stay empty
                Log.e("StorageUtility", e.getMessage());
            }
        });

    }

    /**
     *
     * @param context: Activity of caller
     * @param uid: Id user that owns the image
     * @param fileName: firebase external storage filename
     * @param fileUri: The uri of file located in local storage
     */
    private static void setUserFileDetails(Activity context, String uid, String fileName,Uri fileUri){
        SharedPreferences sharedPreferences = context.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // Set a mapping on SharedPreferences in which the uid is the id of the user which will be mapped to the file path.
        editor.putString(uid, fileName+"&"+fileUri.getPath());
        editor.apply();
    }

    /**
     *
     * @param context
     * @param filePath
     * @return Bitmap of local file
     */

    @Nullable  private static Bitmap retrieveFromLocalStorage(Activity context, String filePath){
        InputStream inputStream = null;
        Uri filePathUri = Uri.parse("file://"+filePath);

        try{
            inputStream = context.getContentResolver().openInputStream(filePathUri);
            return BitmapFactory.decodeStream(inputStream);
        }
        catch (FileNotFoundException e){
            Log.e("StorageUtility", e.getMessage());
        }
        finally {
            if(inputStream != null){
                try{
                    inputStream.close();
                }
                catch (IOException e){
                    Log.e("StorageUtility", e.getMessage());
                }
            }
        }
        return null;
    }
}
