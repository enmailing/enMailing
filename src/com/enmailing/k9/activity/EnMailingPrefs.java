
package com.enmailing.k9.activity;

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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.enmailing.EnMailing;
import com.enmailing.k9.*;
import com.enmailing.k9.activity.Accounts;
import com.enmailing.k9.activity.K9Activity;
import com.enmailing.k9.helper.Utility;
import com.enmailing.k9.R;

public class EnMailingPrefs extends K9Activity {
    private TextView mAuthStatus;
	private EditText mUsername;
    private EditText mPassword;
    private TextView mDeviceDescDesc;
    private EditText mDeviceDesc;
    private TextView mDeviceFullAccessDesc;
    private CheckBox mDeviceFullAccess;
    private Button mAuthorizeButton;
    private Button mDeAuthorizeButton;
    private CheckBox mNotifyOnly;
    private Button mOkButton;

    public static void actionEnMailingPrefs(Context context) {
        Intent i = new Intent(context, EnMailingPrefs.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enmailing_preferences);
        mAuthStatus = (TextView)findViewById(R.id.enmailing_authstatus);
        mUsername = (EditText)findViewById(R.id.enmailing_username);
        mPassword = (EditText)findViewById(R.id.enmailing_password);
        mDeviceDescDesc = (TextView)findViewById(R.id.enmailing_device_desc_desc);
        mDeviceDesc = (EditText)findViewById(R.id.enmailing_device_desc);
        mDeviceFullAccessDesc = (TextView)findViewById(R.id.enmailing_device_full_access_desc);
        mDeviceFullAccess = (CheckBox)findViewById(R.id.enmailing_device_full_access);
        mAuthorizeButton = (Button)findViewById(R.id.enmailing_authbtn);
        mAuthorizeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doAuth();
            }
        });
        mDeAuthorizeButton = (Button)findViewById(R.id.enmailing_deauthbtn);
        mDeAuthorizeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doDeAuth();
            }
        });
        final EnMailing enMailing = new EnMailing(this);
        mNotifyOnly = (CheckBox)findViewById(R.id.enmailing_notify_only);
        mNotifyOnly.setChecked(enMailing.getNotifyOnly());
        mNotifyOnly.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        	@Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enMailing.setNotifyOnly(buttonView.isChecked());
            }
        });
        mOkButton = (Button)findViewById(R.id.enmailing_prefs_ok);
        mOkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	finish();
            }
        });
        
        String user = enMailing.getUsername();
        String authkey = enMailing.getAuthKey();
        if (user.length() > 1 && authkey.length() > 1) {
        	toggleAuthControls(false);
        	checkAuth();
        } else {
        	toggleAuthControls(true);
        }
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
    		                	toggleAuthControls(false);
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
    
    private void toggleAuthControls(boolean show) {
    	if (show) {
    		mAuthStatus.setText(R.string.account_setup_enmailing_authstatus_default);
        	mUsername.setVisibility(View.VISIBLE);
        	mPassword.setVisibility(View.VISIBLE);
        	mDeviceDescDesc.setVisibility(View.VISIBLE);
        	mDeviceDesc.setVisibility(View.VISIBLE);
        	mDeviceFullAccessDesc.setVisibility(View.VISIBLE);
            mDeviceFullAccess.setVisibility(View.VISIBLE);
        	mAuthorizeButton.setVisibility(View.VISIBLE);
        	mDeAuthorizeButton.setVisibility(View.GONE);
    	} else {
    		//EnMailing enMailing = new EnMailing(this);
    		mAuthStatus.setText("Authorized User: "+EnMailing.getUsername()+"\r\nDevice: "+EnMailing.getDeviceName());
        	mUsername.setVisibility(View.GONE);
        	mPassword.setVisibility(View.GONE);
        	mDeviceDescDesc.setVisibility(View.GONE);
        	mDeviceDesc.setVisibility(View.GONE);
        	mDeviceFullAccessDesc.setVisibility(View.GONE);
            mDeviceFullAccess.setVisibility(View.GONE);
        	mAuthorizeButton.setVisibility(View.GONE);
        	mDeAuthorizeButton.setVisibility(View.VISIBLE);
    	}
    }
    
    private void checkAuth() {
    	EnMailing.EnMailingCallback cb = new EnMailing.EnMailingCallback() {
    		public void execute(Object data) {
                if (data instanceof Boolean) {
                	boolean success = (Boolean) data;
                	if (success) {
                		runOnUiThread(new Runnable() {
    		                @Override
    		                public void run() {
    		                	toggleAuthControls(false);
    		                }
    		            });
                	} else {
                		runOnUiThread(new Runnable() {
    		                @Override
    		                public void run() {
    		                	toggleAuthControls(true);
    		                }
    		            });
                	}
                }
            }  
    	};
    	EnMailing enMailing = new EnMailing(this);
		enMailing.checkAuth(cb);
    }
    
    public void doDeAuth() {
    	EnMailing.EnMailingCallback cb = new EnMailing.EnMailingCallback() {
    		public void execute(Object data) {
                if (data instanceof Boolean) {
                	boolean success = (Boolean) data;
                	if (success) {
                		runOnUiThread(new Runnable() {
    		                @Override
    		                public void run() {
    		                	toggleAuthControls(true);
    		                }
    		            });
                	} else {
                		runOnUiThread(new Runnable() {
    		                @Override
    		                public void run() {
    		                	//toggleAuthControls(true);
    		                }
    		            });
                	}
                }
            }  
    	};
    	EnMailing enMailing = new EnMailing(this);
		enMailing.deAuthorize(cb);
    }
}
