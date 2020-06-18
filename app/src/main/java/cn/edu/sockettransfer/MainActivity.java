package cn.edu.sockettransfer;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import com.nononsenseapps.filepicker.FilePickerActivity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.edu.sockettransfer.Transfer.Client;
import cn.edu.sockettransfer.Transfer.PermissionUtils;
import cn.edu.sockettransfer.Transfer.ScanDeviceTool;
import cn.edu.sockettransfer.Transfer.Server;

import static cn.edu.sockettransfer.Transfer.Client.checkConnect;

public class MainActivity extends AppCompatActivity {
    private static final int FILE_CODE = 0;
    private TextView tvMsg;
    private EditText txtIP, txtPort, txtEt;
    private Button btnSend,btnReceive,btnScan;
    private Handler handler;
    private final int EXTERNAL_STORAGE_PERMISSIONS = 12;
    private ScanDeviceTool scanDeviceTool;
    private String ip;
    private int port;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

//        FloatingActionButton fab = findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
        tvMsg = (TextView)findViewById(R.id.tvMsg);
        txtIP = (EditText)findViewById(R.id.txtIP);
        txtPort = (EditText)findViewById(R.id.txtPort);
        txtEt = (EditText)findViewById(R.id.et);
        btnSend = (Button)findViewById(R.id.btnSend);
        btnReceive = (Button)findViewById(R.id.btnReceive);
        btnScan = (Button)findViewById(R.id.btnScan);
        if (PermissionUtils.isStoragePermissionsGranted(this)) {
        } else
            PermissionUtils.requestPermissions(this, EXTERNAL_STORAGE_PERMISSIONS, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*final String ipAddress = txtIP.getText().toString();
                final int port = Integer.parseInt(txtPort.getText().toString());
                Client client = new Client(port,ipAddress);
                ArrayList<String> paths = new ArrayList<>();
                paths.add("/storage/emulated/0/18级1班课表.png");
                client.startSender(paths);*/
                port = Integer.parseInt(txtPort.getText().toString());
                Intent i = new Intent(MainActivity.this, FilePickerActivity.class);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
                i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
                startActivityForResult(i, FILE_CODE);
            }
        });
        btnReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                port = Integer.parseInt(txtPort.getText().toString());
                Message.obtain(handler, 1, port).sendToTarget();
                Server.startServer(port, Environment.getExternalStorageDirectory().getPath()+"/SavedPic",handler);
            }
        });
        btnScan.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                ScanDevice();
            }
        });
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what){
                    case 0:
                        SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");
                        txtEt.append("\n[" + format.format(new Date()) + "]" + msg.obj.toString());
                        break;
                    case 1:
                        tvMsg.setText("本机IP：" + GetIpAddress() + " 口令:" + msg.obj.toString());
                        Toast.makeText(getApplicationContext(), "将口令"+ msg.obj.toString()+"告诉对方吧！", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
                        break;
                    case 3:
                        setIp(msg.obj.toString()); ;
                        txtIP.setText(getIp());
                }
            }
        };
        tvMsg.setText("本机IP：" + GetIpAddress());
        ScanDevice();
    }

    private void ScanDevice() {
        new Thread(){
            @Override
            public void run() {
                scanDeviceTool = new ScanDeviceTool();
                List<String> pList = scanDeviceTool.scan();
                if(pList != null && pList.size() >0) {
                    Message.obtain(handler, 0, "扫描成功，发现"+pList.size()+"个设备：").sendToTarget();
                    for (final String ip : pList) {/*TODO:如果找到连接就应该终止尝试的*/
                        Message.obtain(handler, 0, "尝试连接:"+ip+"……").sendToTarget();
                        /*TODO:连接逻辑*/
                        /*if(getIp()!=""){
                            break;
                        }*/
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Log.d("connect", "-------->conncting: "+ip);
                                    checkConnect(ip,port);
                                    Log.d("connect", "-------->conncted: "+ip);
                                    Message.obtain(handler, 0, "连接成功，IP为"+ip+"的设备").sendToTarget();
                                    Message.obtain(handler, 3, ip).sendToTarget();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();
                    }
                    Message.obtain(handler, 0, "连接失败,请和对方在同一局域网环境下再次尝试!或手动输入对方的ip地址。").sendToTarget();
                }
            }
        }.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            final String ipAddress = txtIP.getText().toString();
            final int port = Integer.parseInt(txtPort.getText().toString());
            /*申请权限*/
            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true)) {
                // For JellyBean and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ClipData clip = data.getClipData();
                    final ArrayList<String> fileNames = new ArrayList<>();
                    final ArrayList<String> paths = new ArrayList<>();
                    if (clip != null) {
                        for (int i = 0; i < clip.getItemCount(); i++) {
                            Uri uri = clip.getItemAt(i).getUri();
                            paths.add(GetFilePath(uri.getPath()));
                            fileNames.add(uri.getLastPathSegment());
                            Log.v("URIs",GetFilePath(uri.getPath().toString()));
                            Log.v("fileNames",uri.getLastPathSegment().toString());
                        }
                        Client client = new Client(ipAddress,port,handler);
                        client.startSender(paths);
                    }
                } else {
                    final ArrayList<String> paths = data.getStringArrayListExtra
                            (FilePickerActivity.EXTRA_PATHS);
                    final ArrayList<String> fileNames = new ArrayList<>();
                    if (paths != null) {
                        for (String path : paths) {
                            Uri uri = Uri.parse(path);
                            paths.add(GetFilePath(uri.getPath()));
                            fileNames.add(uri.getLastPathSegment());
                            Log.v("Paths",GetFilePath(uri.getPath().toString()));
                            Log.v("fileNames",uri.getLastPathSegment().toString());
                        }
                        Client client = new Client(ipAddress,port,handler);
                        client.startSender(paths);
                    }
                }

            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public String GetIpAddress() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int i = wifiInfo.getIpAddress();
        return (i & 0xFF) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ((i >> 16 ) & 0xFF)+ "." +
                ((i >> 24 ) & 0xFF );
    }

    public String GetFilePath(String path) {
        return path.substring(5);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
    }
}
