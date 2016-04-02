package com.example.prx.clientadiltest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.example.prx.aidltest.Book;
import com.example.prx.aidltest.IBookManager;
import com.example.prx.aidltest.IOnNewBookArrivedListener;

import java.util.List;

public class BookManagerActivity extends Activity {

    private static final String TAG = "BookManagerActivity";

    private static final int MESSAGE_NEW_BOOK_ARRIVED = 1;

    private IBookManager mRemoteBookManager;

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
                mRemoteBookManager = iBookManager;
                //!!!注意，服务端的方法有可能需要很久才能执行完毕，这个时候回出现ANR。
                //耗时操作不能在主线程中执行
                List<Book> bookList = iBookManager.getBookList();
                Log.d(TAG, "然后就可以通过这个接口调用服务端的远程方法了");
                //获取到远程服务返回的数据
                Log.d(TAG, "query book list:"+bookList.toString());

                //添加一本新书
                Book newBook = new Book(3,"Android开发艺术探索");
                iBookManager.addBook(newBook);

                //再次取出书单列表
                List<Book> newBookList = iBookManager.getBookList();
                Log.d(TAG, "newBookList:" + newBookList);

                //客户端注册 IOnNewBookArrivedListener 到远程服务
                iBookManager.registerListener(mOnNewBookArrivedListener);


            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //释放资源
            mRemoteBookManager = null;
            Log.d(TAG, "binder died");
        }
    };

    /**
     * 二、当有新书时，服务端会回调客户端的 IOnNewBookArrivedListener 对象中的onNewBookArrived（）方法
     * 但是这个方法是在客户端的Binder线程池中执行,因此，为了便于UI操作，
     * 创建一个Handler将其切换到客户端的主线程中执行。
     */
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MESSAGE_NEW_BOOK_ARRIVED:
                    Log.d(TAG, "msg.obj:" + msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };


    /**
     * 一、首先要注册 IOnNewBookArrivedListener 到远程服务端
     * 这样，当有新书时服务端才能通知客户端,同时，我们要在Activity退出时解除这个歌注册
     */
    private IOnNewBookArrivedListener mOnNewBookArrivedListener = new IOnNewBookArrivedListener.Stub() {

        @Override
        public void onNewBookArrived(Book newBook) throws RemoteException {
            mHandler.obtainMessage(MESSAGE_NEW_BOOK_ARRIVED,newBook).sendToTarget();
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
        if (mRemoteBookManager != null && mRemoteBookManager.asBinder().isBinderAlive()){
            Log.d(TAG, " unregisterListener listener:" + mOnNewBookArrivedListener);
            try {
                //解除注册
                mRemoteBookManager.unregisterListener(mOnNewBookArrivedListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        unbindService(conn);
        super.onDestroy();
    }
}
