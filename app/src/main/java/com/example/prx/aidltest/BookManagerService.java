package com.example.prx.aidltest;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

    /**
     * 2.创建一个Binder对象,这个对象继承自IBookManager.Stub，并实现它内部的AIDL的方法
     */
    private Binder mBinder = new IBookManager.Stub(){

        //AIDL方法是在Binder线程池中执行的

        @Override
        public List<Book> getBookList() throws RemoteException {
            return mBookList;
        }

        @Override
        public void addBook(Book book) throws RemoteException {
            mBookList.add(book);
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
}
