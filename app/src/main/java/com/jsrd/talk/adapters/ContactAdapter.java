package com.jsrd.talk.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.jsrd.talk.R;
import com.jsrd.talk.model.Contact;

import java.util.List;

public class ContactAdapter extends ArrayAdapter<Contact> {

    Context mContext;


    public ContactAdapter(Context context, List<Contact> contacts) {
        super(context, 0,contacts);
        mContext = context;

    }


    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {


            View listItemView = convertView;
            if (listItemView == null) {
                listItemView = LayoutInflater.from(getContext()).inflate(R.layout.contact_list_item, parent, false);
            }

                Contact contact = getItem(position);

                TextView nameIcon = listItemView.findViewById(R.id.contactImage);

                nameIcon.setText(getNameIcon(contact.getContactName())       );

                TextView name = listItemView.findViewById(R.id.contactName);
                name.setText(contact.getContactName());


                TextView number = listItemView.findViewById(R.id.contactNumber);
                number.setText(contact.getNumber());

                return listItemView;

    }

    private String getNameIcon(String contactName) {

        contactName = String.valueOf(contactName.charAt(0)).toUpperCase();
        if (contactName.matches("[+|1|2|3|4|9]") ){
            contactName = "#";
        }
        return contactName;
        }

}
