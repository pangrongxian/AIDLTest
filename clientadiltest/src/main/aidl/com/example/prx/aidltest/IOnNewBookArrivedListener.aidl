// IOnNewBookArrivedListener.aidl
package com.example.prx.aidltest;

// Declare any non-default types here with import statements

import com.example.prx.aidltest.Book;

interface IOnNewBookArrivedListener {
    //新添加接口
    void onNewBookArrived(in Book newBook);
}
