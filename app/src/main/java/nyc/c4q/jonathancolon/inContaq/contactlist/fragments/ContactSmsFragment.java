package nyc.c4q.jonathancolon.inContaq.contactlist.fragments;


import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.klinker.android.send_message.Message;
import com.klinker.android.send_message.Transaction;
import com.tuenti.smsradar.SmsListener;
import com.tuenti.smsradar.SmsRadar;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;

import nyc.c4q.jonathancolon.inContaq.R;
import nyc.c4q.jonathancolon.inContaq.contactlist.activities.ContactListActivity;
import nyc.c4q.jonathancolon.inContaq.contactlist.adapters.SmsAdapter;
import nyc.c4q.jonathancolon.inContaq.contactlist.model.Contact;
import nyc.c4q.jonathancolon.inContaq.sms.SmsHelper;
import nyc.c4q.jonathancolon.inContaq.sms.model.Sms;
import nyc.c4q.jonathancolon.inContaq.utlities.Animations;
import nyc.c4q.jonathancolon.inContaq.utlities.PicassoHelper;
import nyc.c4q.jonathancolon.inContaq.utlities.Settings;

import static nyc.c4q.jonathancolon.inContaq.utlities.sqlite.SqlHelper.saveToDatabase;

public class ContactSmsFragment extends Fragment implements SmsAdapter.Listener {

    private static final int RESULT_LOAD_BACKGROUND_IMG = 2;
    private static final int RESULT_LOAD_CONTACT_IMG = 1;
    private static ContactSmsFragment inst;
    private TextView contactName;
    private ImageView contactImageIV, backgroundImageIV;
    private Contact contact;
    private SmsAdapter adapter;
    private RecyclerView recyclerView;
    private ArrayList<Sms> smsList;
    private EditText smsEditText;
    private Handler handler;

    private Settings settings;

    public ContactSmsFragment() {
    }

    public static ContactSmsFragment instance() {
        return inst;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            smsList = Parcels.unwrap(bundle.getParcelable("smslist"));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        inst = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.fragment_contact_sms, container, false);
        contact = Parcels.unwrap(getActivity().getIntent().getParcelableExtra(ContactListActivity.PARCELLED_CONTACT));
        handler = new Handler();
        SmsHelper.getLastContactedDate(getContext(), contact);

        initializeSmsRadarService();
        initViews(view);
        initSettings();
        setupRecyclerView(contact);
        scrollListToBottom();
        showRecyclerView();

        return view;
    }

    private void initViews(View view) {
        contactName = (TextView) view.findViewById(R.id.name);
        contactImageIV = (ImageView) view.findViewById(R.id.contact_image);
        backgroundImageIV = (ImageView) view.findViewById(R.id.background_image);
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        ImageView smsSendButton = (ImageView) view.findViewById(R.id.send_button);
        smsEditText = (EditText) view.findViewById(R.id.sms_edit_text);

        smsSendButton.setOnClickListener(v -> {
            sendMessage();
        });

        setClickListenersWhenInPortraitMode();
    }

    private void setupRecyclerView(Contact contact) {
        adapter = new SmsAdapter(this, contact);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        Collections.sort(smsList);
        adapter.setData(smsList);
        recyclerView.setAdapter(adapter);
    }

    public synchronized void refreshRecyclerView() {
        smsList = SmsHelper.getAllSms(getActivity(), contact);
        adapter = (SmsAdapter) recyclerView.getAdapter();
        Collections.sort(smsList);
        adapter.setData(smsList);
        adapter.notifyDataSetChanged();
        scrollListToBottom();
    }

    synchronized private void scrollListToBottom() {
        recyclerView.post(() -> recyclerView.scrollToPosition(smsList.size() - 1));
    }

    synchronized private void showRecyclerView() {
        Animations anim = new Animations(getActivity());
        recyclerView.setVisibility(View.VISIBLE);
        anim.fadeIn(recyclerView);
    }

    private void initSettings() {
        settings = Settings.get(getContext());
    }

    private void initializeSmsRadarService() {
        SmsRadar.initializeSmsRadarService(getContext(), new SmsListener() {
            @Override
            public void onSmsSent(com.tuenti.smsradar.Sms sms) {
                updateSms();
            }

            @Override
            public void onSmsReceived(com.tuenti.smsradar.Sms sms) {
                updateSms();
            }
        });
    }

    synchronized public void sendMessage() {
        new Thread(() -> {
            String messageNumber = contact.getCellPhoneNumber();
            String messageText = smsEditText.getText().toString();
            com.klinker.android.send_message.Settings sendSettings = new com.klinker.android.send_message.Settings();
            sendSettings.setMmsc(settings.getMmsc());
            sendSettings.setProxy(settings.getMmsProxy());
            sendSettings.setPort(settings.getMmsPort());
            sendSettings.setUseSystemSending(true);

            Transaction transaction = new Transaction(getContext(), sendSettings);

            Message message = new Message(messageText, messageNumber);

            transaction.sendNewMessage(message, Transaction.NO_THREAD_ID);
        }).start();

//        updateSms();
    }

    public void updateSms() {
        handler.postDelayed(() -> {
            refreshRecyclerView();
            scrollListToBottom();
        }, 2000);
    }

    private void setClickListenersWhenInPortraitMode() {
        int value = getActivity().getResources().getConfiguration().orientation;
        if (value == Configuration.ORIENTATION_PORTRAIT) {
            Typeface jaapokkiRegular = Typeface.createFromAsset(contactName.getContext().
                    getApplicationContext().getAssets(), "fonts/jaapokkiregular.ttf");
            contactName.setTypeface(jaapokkiRegular);
            displayContactInfo(contact);
            contactImageIV.setOnClickListener(v -> {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                ContactSmsFragment.this.startActivityForResult(galleryIntent,
                        RESULT_LOAD_CONTACT_IMG);
            });
            if (backgroundImageIV != null) {
                backgroundImageIV.setOnClickListener(v -> {
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    ContactSmsFragment.this.startActivityForResult(galleryIntent,
                            RESULT_LOAD_BACKGROUND_IMG);
                });
            }
        }
    }

    synchronized private void displayContactInfo(Contact contact) {
        String nameValue = contact.getFirstName() + " " + contact.getLastName();
        PicassoHelper ph = new PicassoHelper(getActivity());

        if (contact.getBackgroundImage() != null) {
            ph.loadImageFromString(contact.getBackgroundImage(), backgroundImageIV);
        }
        if (contact.getContactImage() != null) {
            ph.loadImageFromString(contact.getContactImage(), contactImageIV);
        }
        contactName.setText(nameValue);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (resultCode == Activity.RESULT_OK) {
                PicassoHelper ph = new PicassoHelper(getActivity());

                switch (requestCode) {
                    case 1:
                        setContactImage(data, ph);
                        break;
                    case 2:
                        setBackgroundImage(data, ph);
                        break;
                }
            } else {
                Toast.makeText(getActivity(), R.string.error_message_photo_not_selected,
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(getActivity(), R.string.error_message_general, Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void setContactImage(Intent data, PicassoHelper ph) {
        Uri contactUri = data.getData();
        ph.loadImageFromUri(contactUri, contactImageIV);
        contact.setContactImage(contactUri.toString());
        saveToDatabase(contact, getActivity());
    }

    private void setBackgroundImage(Intent data, PicassoHelper ph) {
        Uri backgroundUri = data.getData();
        ph.loadImageFromUri(backgroundUri, backgroundImageIV);
        contact.setBackgroundImage(backgroundUri.toString());
        saveToDatabase(contact, getActivity());
    }

    @Override
    public void onContactClicked(Sms sms) {
        //TODO add functionality
    }

    @Override
    public void onContactLongClicked(Sms sms) {
        //TODO add functionality
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSmsRadarService();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopSmsRadarService();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSms();
    }

    private void stopSmsRadarService() {
        SmsRadar.stopSmsRadarService(getContext());
    }

    private void showSmsToast(com.tuenti.smsradar.Sms sms) {
        Toast.makeText(getContext(), sms.toString(), Toast.LENGTH_LONG).show();

    }
}
