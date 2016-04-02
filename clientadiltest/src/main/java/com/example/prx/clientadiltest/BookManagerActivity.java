package com.example.prx.clientadiltest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.example.prx.aidltest.Book;
import com.example.prx.aidltest.IBookManager;

import java.util.List;

public class BookManagerActivity extends Activity {

    private static final String TAG = "BookManagerActivity";

    /**
     * 2.连接远程服务端，连接成功后将服务端返回的Binder对象转换成AIDL接口
     */
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "连接远程服务端，连接成功后将服务端返回的Binder对象转换成AIDL接口");
            /**
             * 3.然后就可以通过这个接口调用服务端的远程方法了
             */
            IBookManager iBookManager = IBookManager.Stub.asInterface(service);
            try {
                //!!!注意，服务端的方法有可能需要很久才能执行完毕，这个时候回出现ANR。
                //耗时操作不能在主线程中执行
                List<Book> bookList = iBookManager.getBookList();
                Log.d(TAG, "然后就可以通过这个接口调用服务端的远程方法了");

                Log.d(TAG, "query book list,list type:"+ bookList.getClass().getCanonicalName().toString());

                //获取到远程服务返回的数据
                Log.d(TAG, "query book list:"+bookList.toString());

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //释放资源
            conn = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //1.绑定服务
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.example.prx.aidltest","com.example.prx.aidltest.BookManagerService"));
        bindService(intent,conn,BIND_AUTO_CREATE);
        Log.d(TAG, "1.绑定服务");
    }

    @Override
    protected void onDestroy() {
        unbindService(conn);
        super.onDestroy();
    }
}
