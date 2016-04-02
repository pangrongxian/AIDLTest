package com.example.prx.aidltest;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Created by Administrator on 2016/4/1.
 *
 * 服务端的实现
 */
public class BookManagerService extends Service {

    private static final String TAG = "BookManagerService";

    /**
     * CopyOnWriteArrayList支持并发读写，并且能够进行自动的线程同步。
     */
    private CopyOnWriteArrayList<Book> mBookList = new CopyOnWriteArrayList<Book>();

    private CopyOnWriteArrayList<IOnNewBookArrivedListener> mListenersList = new CopyOnWriteArrayList<IOnNewBookArrivedListener>();

    private AtomicBoolean mIsServiceDestroyed = new AtomicBoolean(false);

    /**
     * 2.创建一个Binder对象,这个对象继承自IBookManager.Stub，并实现它内部的AIDL的方法
     */


    private Binder mBinder = new IBookManager.Stub() {

        //AIDL方法是在Binder线程池中执行的

        @Override
        public List<Book> getBookList() throws RemoteException {
            return mBookList;
        }

        @Override
        public void addBook(Book book) throws RemoteException {
            mBookList.add(book);
        }

        /**
         * 新添加的两个方法
         * @param listener
         * @throws RemoteException
         */
        @Override
        public void registerListener(IOnNewBookArrivedListener listener) throws RemoteException {
            if (!mListenersList.contains(listener)){
                mListenersList.add(listener);
            }else {
                Log.d(TAG, "already exists.");
            }
            Log.d(TAG, "mListenersList.size():" + mListenersList.size());
        }

        @Override
        public void unregisterListener(IOnNewBookArrivedListener listener) throws RemoteException {
            if (mListenersList.contains(listener)){
                mListenersList.remove(listener);
                Log.d(TAG, "unregister listener succeed");
            }else {
                Log.d(TAG, "not found , can not unregister");
            }
            Log.d(TAG, "unregisterListener,current size():" + mListenersList.size());
        }
    };

    /**
     * 1.初始化添加两本图书的信息
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mBookList.add(new Book(1,"Android"));
        mBookList.add(new Book(2,"IOS"));

        //开启一个线程，每隔5秒就向图书库中添加一本新书，兵通知所有感兴趣的用户
        new Thread(new ServiceWork()).start();
    }

    /**
     * 3.在onBind（）方法中返回Binder对象
     * @param intent
     * @return
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public void onDestroy() {
        mIsServiceDestroyed.set(true);
        super.onDestroy();
    }

    private void onNewBookArrived(Book book) throws RemoteException{
        mBookList.add(book);
        Log.d(TAG, "onNewBookArrived, notify listener:" + mBookList.size());
        for (int i = 0; i < mListenersList.size(); i++) {
            IOnNewBookArrivedListener listener = mListenersList.get(i);
            Log.d(TAG, "onNewBookArrived, notify listener:" + listener);
            listener.onNewBookArrived(book);
        }
    }

    /**
     * 线程
     */
    private class ServiceWork implements Runnable {
        @Override
        public void run() {
            while (!mIsServiceDestroyed.get()){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int bookId = mBookList.size() + 1;
                Book newBook = new Book(bookId, "new book#" + bookId);
                try {
                    onNewBookArrived(newBook);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
