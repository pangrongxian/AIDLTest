// IBookManager.aidl
package com.example.prx.aidltest;

//尽管Book类已经和IBookManager位于相同的包中，但是IBookManager中仍然要导入Book类，这就是AIDL的特别之处
import com.example.prx.aidltest.Book;
// Declare any non-default types here with import statements

interface IBookManager {//AIDL的方法
    List<Book> getBookList();
    void addBook(in Book book);
}
