package com.example.Student_Library_Management_System.Services;


import com.example.Student_Library_Management_System.DTOs.IssueBookRequestDto;
import com.example.Student_Library_Management_System.Enums.CardStatus;
import com.example.Student_Library_Management_System.Enums.TransactionStatus;
import com.example.Student_Library_Management_System.Models.Book;
import com.example.Student_Library_Management_System.Models.Card;
import com.example.Student_Library_Management_System.Models.Transactions;
import com.example.Student_Library_Management_System.Repositories.BookRepository;
import com.example.Student_Library_Management_System.Repositories.CardRepository;
import com.example.Student_Library_Management_System.Repositories.TransactionRepository;
import jakarta.transaction.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TransactionService {

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    BookRepository bookRepository;

    @Autowired
    CardRepository cardRepository;

    public  String issueBook(IssueBookRequestDto issueBookRequestDto)throws Exception{


        int bookId = issueBookRequestDto.getBookId();
        int cardId = issueBookRequestDto.getCardId();

        //Get the Book Entity and Card Entity ??? Why do we need this
        //We are this bcz we want to set the Transaction attributes...

        Book book = bookRepository.findById(bookId).get();

        Card card = cardRepository.findById(cardId).get();


        //Final goal is to make a transaction Entity, set its attribute
        //and save it.
        Transactions transaction = new Transactions();

        //Setting the attributes
        transaction.setBook(book);
        transaction.setCard(card);
        transaction.setTransactionId(UUID.randomUUID().toString());
        transaction.setIssueOperation(true);
        transaction.setTransactionStatus(TransactionStatus.PENDING);


        //attribute left is success/Failure
        //Check for validations
        if(book==null || book.isIssued()==true){
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new Exception("Book is not available");

        }

        if(card==null || (card.getCardStatus()!=CardStatus.ACTIVATED)){

            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw  new Exception("Card is not valid");
        }



        //We have reached a success case now.

        transaction.setTransactionStatus(TransactionStatus.SUCCESS);


        //set attributes of book
        book.setIssued(true);
        //Btw the book and transaction : bidirectional
        List<Transactions> listOfTransactionForBook = book.getListOfTransactions();
        listOfTransactionForBook.add(transaction);
        book.setListOfTransactions(listOfTransactionForBook);


        //I need to make changes in the card
        //Book and the card
        List<Book> issuedBooksForCard = card.getBooksIssued();
        issuedBooksForCard.add(book);
        card.setBooksIssued(issuedBooksForCard);


        //Card and the Transaction : bidirectional (parent class)
        List<Transactions> transactionsListForCard = card.getTransactionsList();
        transactionsListForCard.add(transaction);
        card.setTransactionsList(transactionsListForCard);


        //save the parent.
        cardRepository.save(card);
        //automatically by cascading : book and transaction will be saved.
        //Saving the parent

        return "Book issued successfully";
    }

//    public String getTransactions(int bookId,int cardId){
//
//        List<Transactions> transactionsList = transactionRepository.getTransactionsForBookAndCard(bookId,cardId);
//
//        String transactionId = transactionsList.get(0).getTransactionId();
//
//        return transactionId;
//    }
public int  returnBook(int cardId, int bookId) throws Exception{

    List<Transactions> transactions = transactionRepository.getTransactionsForBookAndCard(cardId, bookId);
    Transactions transaction = transactions.get(transactions.size() - 1);

    Date issueDate = transaction.getTransactionDate();

    long timeIssuetime = Math.abs(System.currentTimeMillis() - issueDate.getTime());

    long no_of_days_passed = TimeUnit.DAYS.convert(timeIssuetime, TimeUnit.MILLISECONDS);

    int fine = 0;
    int fine_per_day=5;
    int max_allowed_days=15;
    if(no_of_days_passed > max_allowed_days)
    {
        fine = (int)((no_of_days_passed - max_allowed_days) * fine_per_day);
    }


    //update the book and its status
    Book book = transaction.getBook();
    book.setAvailable(true);
    book.setCard(null);
    bookRepository.updateBook(book);

    //Remove that book from that card list

    Transactions tr = new Transactions();
    tr.setBook(transaction.getBook());
    tr.setCard(transaction.getCard());
    tr.setIssueOperation(false);
    tr.setFine(fine);
    tr.setTransactionStatus(TransactionStatus.SUCCESS);

    transactionRepository.save(tr);

 return fine;
}


}
