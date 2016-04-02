package com.example.prx.aidltest;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Pack200;


/**
 * Created by Administrator on 2016/4/1.
 *
 * ！！！由于服务端方法本身就运行在服务端的BInder线程池中，所以服务端本身就可以执行大量的耗时操作，
 *      这个时候切记不要在服务端中开线程去进行一步任务。
 *
 * 服务端的实现
 */
public class BookManagerService extends Service {

    private static final String TAG = "BookManagerService";

    /**
     * CopyOnWriteArrayList支持并发读写，并且能够进行自动的线程同步。
     */
    private CopyOnWriteArrayList<Book> mBookList = new CopyOnWriteArrayList<Book>();

    //private CopyOnWriteArrayList<IOnNewBookArrivedListener> mListenersList = new CopyOnWriteArrayList<IOnNewBookArrivedListener>();

    /**
     *  RemoteCallbackList 是系统专门提供用于删除进程listener的接口。
     *
     *  Callback中封装了真正的远程listener。
     */
    private RemoteCallbackList<IOnNewBookArrivedListener> mListenersList = new RemoteCallbackList<IOnNewBookArrivedListener>();


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


        //在服务端的onTransact方法中进行权限验证
       /* @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int check = checkCallingOrSelfPermission("com.example.prx.aidltest.ACCESS_BOOK_SERVICE");
            if (check == PackageManager.PERMISSION_DENIED){
                return false;
            }

            String packageName = null;
            String[] packagesForUid = getPackageManager().getPackagesForUid(getCallingUid());
            if (packagesForUid != null && packagesForUid.length > 0){
                packageName  = packagesForUid[0];
            }
            if (!packageName.startsWith("com.prx")){
                return false;
            }
            return super.onTransact(code,data,reply,flags);
        }
*/
        /**
         * 新添加的两个方法
         * @param listener
         * @throws RemoteException
         */
        @Override
        public void registerListener(IOnNewBookArrivedListener listener) throws RemoteException {
//            if (!mListenersList.contains(listener)){
//                mListenersList.add(listener);
//            }else {
//                Log.d(TAG, "already exists.");
//            }
//            Log.d(TAG, "mListenersList.size():" + mListenersList.size());

            mListenersList.register(listener);
            final int N = mListenersList.beginBroadcast();
            Log.d(TAG, "registerListener, current size:" + N);
            mListenersList.finishBroadcast();
        }

        @Override
        public void unregisterListener(IOnNewBookArrivedListener listener) throws RemoteException {
//            if (mListenersList.contains(listener)){
//                mListenersList.remove(listener);
//                Log.d(TAG, "unregister listener succeed");
//            }else {
//                Log.d(TAG, "not found , can not unregister");
//            }
//            Log.d(TAG, "unregisterListener,current size():" + mListenersList.size());


            /**
             * RemoteCallbackList 不是一个List，
             * 遍历 RemoteCallbackList 必须按照下面的方式进行
             * 其中，beginBroadcast 和 finishBroadcast 必须要配对使用
             *
             */
            boolean success = mListenersList.unregister(listener);
            if (success) {
                Log.d(TAG, "unregister success.");
            } else {
                Log.d(TAG, "not found, can not unregister.");
            }
            final int N = mListenersList.beginBroadcast();
            mListenersList.finishBroadcast();
            Log.d(TAG, "unregisterListener, current size:" + N);


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
        /**
         * 1.首先在AndroidManifest.xml文件中声明连接服务的权限
         */
//        int check = checkCallingOrSelfPermission("com.example.prx.aidltest.ACCESS_BOOK_SERVICE");
//        if (check == PackageManager.PERMISSION_DENIED){
//            return null;
//        }
        return mBinder;
    }


    @Override
    public void onDestroy() {
        mIsServiceDestroyed.set(true);
        super.onDestroy();
    }

    /**
     * 当远程服务端需要调用客户端的listener中的方法时，被调用的方法也运行在线程池中，
     * 只不过是客户端的线程池。 所以，我们同样不可以在服务中调用客户端的耗时方法
     *
     * @param book
     * @throws RemoteException
     *
     *  BookManagerService 的 onNewBookArrived（）方法，
     *  它内部调用了客户端的 IOnNewBookArrivedListener 中的 onNewBookArrived方法，
     *  如果客户端的这个 onNewBookArrived 方法比较耗时的话，
     *  那么请确保 BookManagerService 中的 onNewBookArrived（）方法运行在非UI线程中！
     *  否则会导致服务器无法响应.
     *
     */
    private void onNewBookArrived(Book book) throws RemoteException{
//        mBookList.add(book);
//        Log.d(TAG, "onNewBookArrived, notify listener:" + mBookList.size());
//        for (int i = 0; i < mListenersList.size(); i++) {
//            IOnNewBookArrivedListener listener = mListenersList.get(i);
//            Log.d(TAG, "onNewBookArrived, notify listener:" + listener);
//            listener.onNewBookArrived(book);
//        }

        mBookList.add(book);
        final int N = mListenersList.beginBroadcast();
        for (int i = 0; i < N; i++) {
            IOnNewBookArrivedListener listener = mListenersList.getBroadcastItem(i);
            if (listener != null){
                try {
                    listener.onNewBookArrived(book);
                }catch (RemoteException e){
                    e.printStackTrace();
                }
            }
        }
        mListenersList.finishBroadcast();
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
