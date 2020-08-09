package com.jsrd.talk;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.jsrd.talk.interfaces.ProgressSuccessCallBack;
import com.jsrd.talk.interfaces.ReceiverCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ContactFragmentDialog extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = "ContactFragmentDialog";
    ListView contactListView;
    EditText searchText;
    ContactAdapter contactAdapter;
    List<Contact> contacts;
    String filterText = "";
    private FirebaseUtils firebaseUtils;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_contact_dialog, container, false);
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initLoader(0);
        contactListView = getView().findViewById(R.id.listView);
        ImageView backbutton = getView().findViewById(R.id.backButton);
        backbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goBack();
            }
        });
        searchText = getView().findViewById(R.id.searchText);
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterText = s.toString();
                initLoader(0);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        contactListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Contact contact = (Contact) adapterView.getItemAtPosition(i);
                String number = contact.getNumber().trim();
                final String num;
                if (number.length() == 10) {
                    num = "+91" + number;
                } else {
                    num = number;
                }
                firebaseUtils = new FirebaseUtils(getContext());
                firebaseUtils.getReceiversUID(num, new ReceiverCallback() {
                    @Override
                    public void onComplete(String UID) {
                        if (UID != null) {
                            ((MainActivity) getActivity()).openChatWithSelectedUser(UID, num);
                            goBack();
                        } else {
                            Toast.makeText(getContext(), "This Number is not registered", Toast.LENGTH_SHORT).show();
                        }
                    }
                });


            }
        });

    }

    public void goBack() {
        getDialog().dismiss();
    }

    private void initLoader(int id) {
        getLoaderManager().initLoader(id,
                null,
                this);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {

        if (id == 0) {
            return contactsLoader();
        } else if (id == 1) {
            return contactsLoader();
        }
        return null;
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {

        if (loader.getId() == 0) {
            contacts = contactsFromCursor(cursor, filterText);
            contactAdapter = new ContactAdapter(getActivity().getApplicationContext(), contacts);
            contactListView.setAdapter(contactAdapter);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

    private Loader<Cursor> contactsLoader() {
        Uri contactsUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI; // The content URI of the phone contacts

        String[] projection = {
                ContactsContract.Contacts.DISPLAY_NAME,                            // The columns to return for each row
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };

        String sortOrder = ContactsContract.Contacts.DISPLAY_NAME + " ASC";  //The sort order for the returned rows

        return new CursorLoader(
                getActivity().getApplicationContext(),
                contactsUri,
                projection,
                null,
                null,
                sortOrder
        );
    }

    private List<Contact> contactsFromCursor(Cursor cursor, String filterText) {
        List<Contact> contacts = new ArrayList<Contact>();

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                String number = null;
                number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)).trim();
                number = removeCountryCode(number);
                if (name.toLowerCase().contains(filterText.toLowerCase())) {
                    Boolean dup = checkDuplicateNumber(number, contacts);
                    if (dup == false) {
                        Contact contact = new Contact(name, number);
                        contacts.add(contact);
                    }
                }
            }
        }
        Collections.sort(contacts, Contact.ContactNameComparator);

        return contacts;
    }

    private Boolean checkDuplicateNumber(String number, List<Contact> contacts) {

        Boolean dup = false;
        for (Contact contact : contacts) {
            if (number.equalsIgnoreCase(contact.getNumber())) {
                dup = true;
            }
        }
        return dup;
    }


    private String removeCountryCode(String num) {
        String prefix = "+91";

        if (num.startsWith(prefix)) {
            num = num.substring(prefix.length());
        }
        if (num.startsWith("0")) {
            num = num.substring(1);
        }

        if (num.contains("-") || num.contains(" ")) {
            num = num.replaceAll("-", "");
            num = num.replaceAll(" ", "");
        }
        return num;
    }

}