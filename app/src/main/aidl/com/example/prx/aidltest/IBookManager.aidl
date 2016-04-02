// IBookManager.aidl
package com.example.prx.aidltest;

//尽管Book类已经和IBookManager位于相同的包中，但是IBookManager中仍然要导入Book类，这就是AIDL的特别之处
import com.example.prx.aidltest.Book;

//添加新的aidl接口也需要到到相应接口的包进来
import com.example.prx.aidltest.IOnNewBookArrivedListener;

// Declare any non-default types here with import statements

interface IBookManager {//AIDL的方法
    List<Book> getBookList();
    void addBook(in Book book);

    //在原来的接口中添加两个新的方法
    void registerListener(IOnNewBookArrivedListener listener);
    void unregisterListener(IOnNewBookArrivedListener listener);
}
