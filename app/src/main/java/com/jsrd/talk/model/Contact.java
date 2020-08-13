package com.jsrd.talk.model;

import java.util.Comparator;

public class Contact {

    private String ContactName;
    private String Number;

    public Contact(String contactName, String number) {
        ContactName = contactName;
        Number = number;
    }

    public String getContactName() {
        return ContactName;
    }

    public String getNumber() {
        return Number;
    }


    public static Comparator<Contact> ContactNameComparator = new Comparator<Contact>() {

        public int compare(Contact c1, Contact c2) {
            String ContactName1 = c1.getContactName().toUpperCase();
            String ContactName2 = c2.getContactName().toUpperCase();

            //ascending order
            return ContactName1.compareTo(ContactName2);

            //descending order
            //return StudentName2.compareTo(StudentName1);
        }
    };


}
