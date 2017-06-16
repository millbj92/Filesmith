package com.purgatory.filesmith;


import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.text.DateFormat;
import java.util.Locale;


import android.content.Intent;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;


import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends ListActivity
        implements NavigationView.OnNavigationItemSelectedListener, ProgressRequestBody.UploadCallbacks {

    private File currentDir;
    private String currentServerDir;
    private FileArrayAdapter adapter;
    private ProgressBar progressBar;
    private RequestQueue requestQueue;
    private boolean isServer = false;
    private String ip;
    private String port;
    private String connectionString;

    public static String videoAddress;

    SharedPreferences prefs;
    SharedPreferences.Editor prefsEditor;

    AlertDialog ipdialog;
    Button ipSaveButton;
    EditText ipText;
    EditText portText;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        requestQueue = Volley.newRequestQueue(this);
        prefs = this.getPreferences(Context.MODE_PRIVATE);
        prefsEditor = prefs.edit();

        //Setup the Settings dialog.
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");

        builder.setView(View.inflate(this, R.layout.ip_dialog, null));
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        ipdialog = builder.create();

        //If there is no IP or Port in the prefs, open the settings dialog.
        //If there is, then get the strings. If all else fails, resort to "localhost:8080".
        if(!prefs.contains("ip"))
            ShowIPDialog();
        else
            ip = prefs.getString("ip", "localhost");

        if(!prefs.contains("port"))
            ShowIPDialog();
        else
            port = prefs.getString("port", "8080");

        if(ip != null && port != null)
            connectionString = (ip.contains("http://")) ? ip + ":" + port : "http://" + ip + ":" + port;

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);



        currentDir = new File("/storage/");
        fill(currentDir);
    }


    public void ShowIPDialog()
    {
        ipdialog.show();
        ipText = (EditText)ipdialog.findViewById(R.id.iptext);
        portText = (EditText)ipdialog.findViewById(R.id.porttext);
        ipSaveButton = (Button)ipdialog.findViewById(R.id.saveBtn);

        //Check for nulls, if none, save the new values into preferences,
        //assign "ip" and "port" vars to new prefs and dismiss the dialog.
        ipSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ipText.getText().toString().isEmpty() || ipText.getText() == null){
                    Toast.makeText(MainActivity.this, "You must enter a valid hostname", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(portText.getText().toString().isEmpty() || portText.getText() == null){
                    Toast.makeText(MainActivity.this, "You must enter a valid port number", Toast.LENGTH_SHORT).show();
                    return;
                }

                prefsEditor.putString("ip", ipText.getText().toString());
                prefsEditor.putString("port", portText.getText().toString());
                prefsEditor.commit();

                ip = prefs.getString("ip", "localhost");
                port = prefs.getString("port", "8080");

                connectionString =  (ip.contains("http://")) ? ip + ":" + port : "http://" + ip + ":" + port;
                ipdialog.dismiss();
            }
        });
    }

    //Once we get the files from the server, fill the ListView
    private final com.android.volley.Response.Listener<String> onFilesLoaded = new com.android.volley.Response.Listener<String>(){
        @Override
        public void onResponse(String response) {
            fillFromServer(response);
        }
    };

    //Parse the Json into an ItemResponse and fill the list.
    private void fillFromServer(String response)
    {
        Gson gson = new Gson();
        ItemResponse ir = gson.fromJson(response, ItemResponse.class);
        Item[] items = ir.getItems();
        Log.e("Items", response);


        for(Item i : items)
        {
            if(i.getImage().equalsIgnoreCase("directory_icon"))
                continue;

            String extenstion = FilenameUtils.getExtension(i.getPath());

            if(MainActivity.this.getResources().getIdentifier(extenstion, "drawable", MainActivity.this.getPackageName()) == 0)
                i.setImage("_blank");

        }

        List<Item> itms = new ArrayList<>(Arrays.asList(items));

        if(!currentServerDir.equalsIgnoreCase("server")) {
            itms.add(0, new Item("", "Parent Directory", "", ir.getParent(), "directory_up"));
        }

        adapter = new FileArrayAdapter(MainActivity.this,R.layout.file_view,itms);
        MainActivity.this.setListAdapter(adapter);
    }


    private final com.android.volley.Response.ErrorListener onFilesError = new com.android.volley.Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("PostActivity", error.toString());
        }
    };

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull  MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_files) {
            isServer = false;
            currentDir = new File("/storage/");
            fill(currentDir);
        } else if (id == R.id.nav_sfiles) {

            if(!prefs.contains("ip") || !prefs.contains("port")) {
                ShowIPDialog();
                return false;
            }

            try {
                currentServerDir = "server";
                StringRequest request = new StringRequest(Request.Method.GET, connectionString +"/getfiles?folder=", onFilesLoaded, onFilesError);
                requestQueue.add(request);
                isServer = true;
            }catch (Exception e)
            {
                Log.w("Exc", e.getMessage());
            }

        } else if (id == R.id.nav_settings) {
            ShowIPDialog();
        } else if(id == R.id.nav_new){
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("New File");

            builder.setView(View.inflate(this, R.layout.create_dialog, null));


            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });

            final AlertDialog dialog = builder.create();
            dialog.show();

            final EditText textInput = (EditText)dialog.findViewById(R.id.folderText);
            final Button createBtn = (Button)dialog.findViewById(R.id.createBtn);
            assert  createBtn != null;

            createBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String folderName = (textInput != null && !TextUtils.isEmpty(textInput.getText())) ? textInput.getText().toString() : "New Folder";
                    if(!isServer)
                    {
                        File f = new File(currentDir + "/" + folderName);
                        Boolean created = f.mkdir();
                        dialog.dismiss();
                        fill(currentDir);

                        if(!created)
                            Toast.makeText(MainActivity.this, "Folder could not be created!", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        try {
                            File f = new File(currentServerDir + "/" + folderName);
                            String url = URLEncoder.encode(f.getPath(), StandardCharsets.UTF_8.displayName());
                            StringRequest request = new StringRequest(Request.Method.GET, connectionString + "/makefolder?folder=" + url, onFolderCreated, onFolderError);
                            dialog.dismiss();
                            requestQueue.add(request);
                        }
                        catch(Exception e){
                            Log.e("Error", e.getMessage());
                        }
                    }

                }
            });
        }



        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //Once a folder has been created on the server, sleep for half a second so we don't request before its been created, and then fill the list again.
    private final com.android.volley.Response.Listener<String> onFolderCreated = new com.android.volley.Response.Listener<String>(){
        @Override
        public void onResponse(String response) {
            try {
                Thread.sleep(500);
                String name = URLEncoder.encode(currentServerDir, StandardCharsets.UTF_8.displayName());
                StringRequest request = new StringRequest(Request.Method.GET, connectionString + "/getfiles?folder=" + name, onFilesLoaded, onFilesError);
                requestQueue.add(request);
            }catch(Exception e)
            {
                Log.e("Exception", e.getMessage());
            }
        }
    };

    private final com.android.volley.Response.ErrorListener onFolderError = new com.android.volley.Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("Folder Error", error.toString());
        }
    };

    private void fill(File f)
    {
        File[]dirs = f.listFiles();
        this.setTitle("Current Dir: "+f.getName());
        List<Item>dir = new ArrayList<>();
        List<Item>fls = new ArrayList<>();

        if(currentDir.getName().equalsIgnoreCase("storage")){
            DateFormat formatter = DateFormat.getDateTimeInstance();
            Date lastModDate;
            //File[] storage = getExternalMediaDirs();
            File internal = new File(Environment.getExternalStorageDirectory().getAbsolutePath());

            Log.e("Internal", internal.getAbsolutePath());


            File[] inbuf = internal.listFiles();
            int buf = (inbuf != null) ? inbuf.length : 0;
            String in_items = String.valueOf(buf);
            lastModDate = new Date(internal.lastModified());
            String in_modify = formatter.format(lastModDate);
            in_items = (buf == 1) ? in_items + " item" : in_items + " items";

            dir.add(new Item("Internal Storage",in_items,in_modify,internal.getAbsolutePath(),"hdd"));

            if(System.getenv("SECONDARY_STORAGE") != null)
            {
                File external = new File(System.getenv("SECONDARY_STORAGE"));
                Log.e("External", external.getAbsolutePath());
                File[] exbuf = external.listFiles();
                buf = (exbuf != null) ? exbuf.length : 0;
                String ex_items = String.valueOf(buf);
                ex_items = (buf == 1) ? ex_items + " item" : ex_items + " items";
                lastModDate = new Date(external.lastModified());
                String ex_modify = formatter.format(lastModDate);
                dir.add(new Item("SD Card",ex_items,ex_modify,external.getAbsolutePath(),"hdd"));
            }


            adapter = new FileArrayAdapter(MainActivity.this,R.layout.file_view,dir);
            this.setListAdapter(adapter);
            return;
        }
        try{
            for(File ff: dirs)
            {
                Date lastModDate = new Date(ff.lastModified());
                DateFormat formater = DateFormat.getDateTimeInstance();
                String date_modify = formater.format(lastModDate);
                if(ff.isDirectory()){


                    File[] fbuf = ff.listFiles();

                    int buf = (fbuf != null) ? fbuf.length : 0;

                    String num_item = String.valueOf(buf);
                    num_item = (buf == 1) ? num_item + " item" : num_item + " items";

                    if(ff.getName().matches("^(?i)sdcard\\d$") || ff.getName().matches("sdcard"))
                        Log.e("Storage", "true");



                    dir.add(new Item(ff.getName(),num_item,date_modify,ff.getAbsolutePath(),"directory_icon"));

                }
                else
                {
                    String extenstion = FilenameUtils.getExtension(ff.getName());

                    String lengthValue = "B";
                    float length = ff.length();
                    if(ff.length() > 1024 && ff.length() < 1048576) {
                        lengthValue = "KB";
                        length = (float)ff.length() / 1024;
                    }
                    else if(ff.length() > 1048576 && ff.length() < 1073741824){
                        lengthValue = "MB";
                        length = (float)ff.length() / 1048576;
                    }
                    else if(ff.length() > 1073741824){
                        lengthValue = "GB";
                        length = (float)ff.length() / 1073741824;
                    }

                    if(this.getResources().getIdentifier(extenstion, "drawable", this.getPackageName()) == 0)
                        fls.add(new Item(ff.getName(), String.format(Locale.getDefault(), "%.2f", length) + lengthValue, date_modify, ff.getAbsolutePath(), "_blank"));
                    else
                        fls.add(new Item(ff.getName(), String.format(Locale.getDefault(), "%.2f", length) + lengthValue, date_modify, ff.getAbsolutePath(), extenstion));


                }
            }
        }catch(Exception e)
        {
            Log.e("Error", e.getMessage());
        }
        Collections.sort(dir);
        Collections.sort(fls);
        dir.addAll(fls);
        if(!f.getName().equalsIgnoreCase("storage"))
        {
            if(f.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath()))
                dir.add(0,new Item("..","Parent Directory","","/storage/","directory_up"));
            else
                dir.add(0,new Item("..","Parent Directory","",f.getParent(),"directory_up"));
        }


        adapter = new FileArrayAdapter(MainActivity.this,R.layout.file_view,dir);
        this.setListAdapter(adapter);
    }
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Item o = adapter.getItem(position);

        if(o != null)
        if(o.getImage().equalsIgnoreCase("directory_icon")||o.getImage().equalsIgnoreCase("directory_up")||o.getImage().equalsIgnoreCase("hdd")){
            if(!isServer)
            {

                currentDir = new File(o.getPath());
                fill(currentDir);
            }
            else
            {
                try {
                    currentServerDir = o.getPath();

                    String name = URLEncoder.encode(o.getPath(), StandardCharsets.UTF_8.displayName());
                    Log.e("Charset", name);
                    StringRequest request = new StringRequest(Request.Method.GET, connectionString + "/getfiles?folder=" + name, onFilesLoaded, onFilesError);
                    requestQueue.add(request);
                }catch(Exception e){
                    Log.e("Error", e.getMessage());
                }
            }

        }
        else
        {
            onFileClick(o);
        }
    }

    @Override
    public void onProgressUpdate(int percentage) {
        // set current progress
        progressBar.setProgress(percentage);
    }

    @Override
    public void onError() {
        Log.e("Error", "There was an error.");
    }

    @Override
    public void onFinish() {
        progressBar.setProgress(100);
        progressBar.setVisibility(View.GONE);
    }

    private void uploadFile(Item item) {

        progressBar.setVisibility(View.VISIBLE);
        File file = new File(item.getPath());


        // create RequestBody instance from file
        ProgressRequestBody requestFile = new ProgressRequestBody(file, MainActivity.this);

        // MultipartBody.Part is used to send also the actual file name
        MultipartBody.Part body =
                MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(connectionString)
                .addConverterFactory(GsonConverterFactory.create());

        Retrofit retrofit = builder.build();

        FileUploadService service = retrofit.create(FileUploadService.class);



        // finally, execute the request
        Call<ResponseBody> call = service.upload(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse( @NonNull Call<ResponseBody> call,
                                  @NonNull Response<ResponseBody> response) {
                if(response.code() != 200)
                {
                    Toast.makeText(MainActivity.this, "File was not uploaded, error code: " + Integer.toString(response.code()), Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(MainActivity.this, "File uploaded successfully!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e("Upload error:", t.getMessage());
            }
        });
    }
    private void onFileClick(Item o)
    {
        if(!isServer)
        {
            final Item item = o;
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("File Options");
            builder.setView(View.inflate(this, R.layout.file_dialog, null));
            final AlertDialog dialog = builder.create();
            dialog.show();

            final Button UploadBtn = (Button)dialog.findViewById(R.id.UploadBtn);
            assert UploadBtn != null;
            final Button OpenBtn = (Button)dialog.findViewById(R.id.OpenBtn);
            assert OpenBtn != null;
            final Button DeleteBtn = (Button)dialog.findViewById(R.id.DeleteBtn);
            assert DeleteBtn != null;
            final Button CloseBtn = (Button)dialog.findViewById(R.id.CloseBtn);
            assert  CloseBtn != null;
            final Toast t = Toast.makeText(this, "Could not delete file.", Toast.LENGTH_SHORT);
            progressBar = (ProgressBar)dialog.findViewById(R.id.progressBar);


            UploadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(!prefs.contains("ip") || !prefs.contains("port")) {
                        dialog.dismiss();
                        ShowIPDialog();
                        return;
                    }
                    uploadFile(item);

                }

            });

            OpenBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        File f = new File(item.getPath());

                        Intent intent = new Intent();
                        intent.setAction(android.content.Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(f), "application/mp4");
                        startActivity(intent);

                    }catch(Exception e){
                        Log.e("File Not Found", e.getMessage());
                    }
                }
            });

            DeleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    File f = new File(item.getPath());
                    try {
                        boolean deleted = FileUtils.deleteQuietly(f);

                        if(!deleted)
                            t.show();

                        fill(currentDir);
                        dialog.dismiss();
                    }catch(Exception e) {
                        Log.e("Exc", e.getMessage());
                    }


                }
            });

            CloseBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }
        else
        {

            String ext = FilenameUtils.getExtension(o.getPath());
            if(ext.equalsIgnoreCase("mkv") || ext.equalsIgnoreCase("avi") || ext.equalsIgnoreCase("mp4"))
            {
                File f = new File(o.getPath());

                String filename = f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf("\\") + 1);
                String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                String[] split = mime.split("/");
                Log.e("split", split[1]);
                if(split[1].equalsIgnoreCase("x-matroska"))
                    mime = "application/x-matroska";
                Log.e("MIME", mime);
                videoAddress = connectionString + "/videos/" + filename;
                Intent intent = new Intent(Intent.ACTION_VIEW);
                String pkg;
                if(isPackageExisted("org.videolan.vlc"))
                    pkg = "org.videolan.vlc";
                else {
                    Toast.makeText(this, "You need to install MX Video Player or VLC Video Player", Toast.LENGTH_SHORT).show();
                    Log.e("Not installed", "MX Player not installed.");
                    return;
                }
                Log.e("video", videoAddress);
                intent.setPackage(pkg);
                intent.setDataAndType(Uri.parse(videoAddress), mime);
                startActivityForResult(intent, 42);
                return;
            }


            final Item item = o;
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("File Options");
            builder.setView(View.inflate(this, R.layout.file_dialog_server, null));
            final AlertDialog dialog = builder.create();
            dialog.show();

            final Button DownloadButton = (Button)dialog.findViewById(R.id.DownloadBtn);
            assert DownloadButton != null;
            final Button DeleteBtn = (Button)dialog.findViewById(R.id.DeleteBtn1);
            assert DeleteBtn != null;
            final Button CloseBtn = (Button)dialog.findViewById(R.id.CloseBtn1);
            assert CloseBtn != null;

            DownloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    File f = new File(item.getPath());
                    String filename = f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf("\\") + 1);

                    try {
                        Uri uri = Uri.parse(connectionString + "/files/" + filename);
                        DownloadManager.Request r = new DownloadManager.Request(uri);

                        // This put the download in the same Download dir the browser uses
                        r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

                        // When downloading music and videos they will be listed in the player
                        // (Seems to be available since Honeycomb only)
                        r.allowScanningByMediaScanner();

                        // Notify user when download is completed
                        // (Seems to be available since Honeycomb only)
                        r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                        // Start download
                        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                        dm.enqueue(r);
                    }catch(Exception e)
                    {
                        Log.e("Download Error", e.getMessage());
                    }
                }
            });

            DeleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    try {
                        String path = URLEncoder.encode(item.getPath(), StandardCharsets.UTF_8.displayName());
                        StringRequest request = new StringRequest(Request.Method.GET, connectionString + "/deletefile?path=" + path, onFilesDeleted, onDeleteError);
                        requestQueue.add(request);

                    }catch(Exception e) {
                        Log.e("Could not delete file", e.getMessage());
                    }

                    dialog.dismiss();
                }
            });

            CloseBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("Result", Integer.toString(resultCode));
    }

    public boolean isPackageExisted(String targetPackage){
        List<ApplicationInfo> packages;
        PackageManager pm;

        pm = getPackageManager();
        packages = pm.getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            if(packageInfo.packageName.equals(targetPackage))
                return true;
        }
        return false;
    }
    
    private final com.android.volley.Response.Listener<String> onFilesDeleted = new com.android.volley.Response.Listener<String>(){
        @Override
        public void onResponse(String response) {
            try {
                Thread.sleep(500);
                String name = URLEncoder.encode(currentServerDir, StandardCharsets.UTF_8.displayName());
                StringRequest request = new StringRequest(Request.Method.GET, connectionString + "/getfiles?folder=" + name, onFilesLoaded, onFilesError);
                requestQueue.add(request);
            }catch(Exception e)
            {
                Log.e("Exception", e.getMessage());
            }
        }
    };

    private final com.android.volley.Response.ErrorListener onDeleteError = new com.android.volley.Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("Folder Error", error.toString());
        }
    };


}


