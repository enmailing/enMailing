
package com.enmailing.k9.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.text.method.TextKeyListener.Capitalize;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.enmailing.EnMailing;
import com.enmailing.k9.*;
import com.enmailing.k9.activity.Accounts;
import com.enmailing.k9.activity.K9Activity;
import com.enmailing.k9.helper.Utility;
import com.enmailing.k9.R;

public class AccountSetupEnMailing extends K9Activity implements OnClickListener {
    private TextView mAuthStatus;
	
	private EditText mUsername;

    private EditText mPassword;
    
    private TextView mDeviceDescDesc;
    
    private EditText mDeviceDesc;
    
    private TextView mDeviceFullAccessDesc;
    
    private CheckBox mDeviceFullAccess;
    
    private Button mAuthorizeButton;

    private Button mDoneButton;

    public static void actionSetEnMailing(Context context) {
        Intent i = new Intent(context, AccountSetupEnMailing.class);
        //i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_enmailing);
        mAuthStatus = (TextView)findViewById(R.id.setup_enmailing_authstatus);
        mUsername = (EditText)findViewById(R.id.setup_enmailing_username);
        mPassword = (EditText)findViewById(R.id.setup_enmailing_password);
        mDeviceDescDesc = (TextView)findViewById(R.id.setup_enmailing_device_desc_desc);
        mDeviceDesc = (EditText)findViewById(R.id.setup_enmailing_device_desc);
        mDeviceFullAccessDesc = (TextView)findViewById(R.id.setup_enmailing_full_access_desc);
        mDeviceFullAccess = (CheckBox)findViewById(R.id.setup_enmailing_device_full_access);
        mAuthorizeButton = (Button)findViewById(R.id.setup_enmailing_authbtn);
        //mAuthorizeButton.setOnClickListener(this);
        mAuthorizeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doAuth();
            }
        });
        mDoneButton = (Button)findViewById(R.id.done);
        //mDoneButton.setOnClickListener(this);
        mDoneButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onNext();
            }
        });
    }

    protected void onNext() {
    	Accounts.listAccounts(this);
        finish();
    }

    public void onClick(View v) {
        /*switch (v.getId()) {
        case R.id.done:
            onNext();
            break;
        case R.id.enmailing_authbtn:
        	doAuth();
        	break;
        }*/
    }
    
    public void doAuth() {
    	final String user = mUsername.getText().toString();
    	final String pass = mPassword.getText().toString();
    	final String ddesc = mDeviceDesc.getText().toString();
    	final boolean fullaccess = mDeviceFullAccess.isChecked();
    	EnMailing.EnMailingCallback cb = new EnMailing.EnMailingCallback() {
    		public void execute(Object data) {
                if (data instanceof Boolean) {
                	boolean success = (Boolean) data;
                	if (success) {
                		runOnUiThread(new Runnable() {
    		                @Override
    		                public void run() {
    		                	onNext();
    		                }
    		            });
                	} else {
                		
                	}
                }
            }  
    	};
    	EnMailing enMailing = new EnMailing(this);
		enMailing.doAuth(user, pass, ddesc, fullaccess, cb);
    }
}
