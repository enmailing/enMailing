package com.enmailing;

import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Hashtable;
import java.util.Set;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import android.text.SpannableStringBuilder;
//import java.io.*;
import android.util.Base64;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.enmailing.k9.K9;
import com.enmailing.k9.Preferences;
import com.enmailing.k9.activity.MessageCompose;
import com.enmailing.k9.helper.HtmlConverter;
import com.enmailing.k9.view.MessageWebView;
import com.enmailing.k9.R;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;
import android.preference.PreferenceManager;
import android.net.Uri;

public class EnMailing extends Activity {
	public static final String LOG_TAG = "enmailing";
	private Context context;
	//private String messageEnc;
	
	public EnMailing(Context c) {
		context = c;
		//messageEnc = null;
	}
	
	public EnMailing(View v) {
		this(v.getContext());
	}
	
	public static boolean hasEncryptions(String text) {
		//Pattern p = Pattern.compile("-----enMAILING-----(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?-----enMAILING-----(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?-----END-----");
		Pattern p = Pattern.compile("-----enMAILING-----(?:[A-Za-z0-9+/=\r\n])*-----enMAILING-----(?:[A-Za-z0-9+/=\r\n])*-----END-----");
		Matcher m = p.matcher(text);
		boolean found = false;
		if (m.find()) {
			found = true;
		}
		return found;
	}
	
	public static ArrayList<String> getEncryptions(String text) {
		ArrayList<String> encryptions = new ArrayList<String>();
		//Pattern p = Pattern.compile("-----enMAILING-----(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?-----enMAILING-----(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?-----END-----");
		Pattern p = Pattern.compile("-----enMAILING-----(?:[A-Za-z0-9+/=\r\n])*-----enMAILING-----(?:[A-Za-z0-9+/=\r\n])*-----END-----");
		Matcher m = p.matcher(text);
		while (m.find()) {
			encryptions.add(m.group());
		}
		
		return encryptions;
	}
	
	private static Cipher getCipher(String strKey, String strIv, int cipherMode) {
		byte[] key = EnMailing.hexToByteArray(strKey);
		byte[] iv = EnMailing.hexToByteArray(strIv);
		Cipher cipher = null;
		try {
			SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(cipherMode, skeySpec, new IvParameterSpec(iv));
		} catch (Exception e) {
			Log.e(EnMailing.LOG_TAG, "Error getting cipher: "+e.getMessage());
			cipher = null;
		}
		return cipher;
	}
	
	private String decrypt(String text, String strKey, String strIv) {
		try {
			Cipher cipher = getCipher(strKey, strIv, Cipher.DECRYPT_MODE);
		    byte[] original = cipher.doFinal(Base64.decode(text, Base64.DEFAULT));
		    return new String(original);
		} catch (Exception e) {
			Log.e(EnMailing.LOG_TAG, "Error decrypting: "+e.getMessage());
		}
		return text;
	}
	
	private String encrypt(String text, String strKey, String strIv) {
		try {
			Cipher cipher = getCipher(strKey, strIv, Cipher.ENCRYPT_MODE);
		    byte[] crypted = cipher.doFinal(text.getBytes());
		    String strEnc = Base64.encodeToString(crypted, Base64.NO_WRAP);
		    return strEnc;
		} catch (Exception e) {
			Log.e(EnMailing.LOG_TAG, "Error encrypting: "+e.getMessage());
		}
		return text;
	}
	
	private String decryptBlock(String block) {
		String dec = block.replaceAll("[\r\n]", "");
		String headerBumper = EnMailing.headerBumper();
		String hdr = block.substring(0, block.lastIndexOf(headerBumper)+headerBumper.length());
		//Log.e(EnMailing.LOG_TAG, "Header: "+hdr);
		String body = block.replace(hdr, "");
		body = body.replace(EnMailing.messageBumper(), "");
		hdr = hdr.replaceAll(EnMailing.headerBumper(), "");
		hdr = hdr.replaceAll("[\r\n]", "");
		
		Hashtable<String,String> params = new Hashtable<String,String>();
		params.put("request", "decode");
		//params.put("user", K9.getEnMailingUsername());
		//params.put("authkey", K9.getEnMailingAuthKey());
		params.put("user", getUsername());
		params.put("authkey", getAuthKey());
		params.put("head", hdr);
		
		try {
			JSONObject result = EnMailing.webRequest(params);
			if (result.getBoolean("success")) {
				String key = result.getString("key");
				String iv = result.getString("iv");
				dec = this.decrypt(body, key, iv);
				if (dec != body && EnMailing.hasEncryptions(dec)) {
					dec = this.decryptString(dec);
				}
			} else {
				String message = result.getString("message");
				if (message.matches("access denied")) {
					Log.e(EnMailing.LOG_TAG, "Access denied. "+result.getString("longmessage"));
					//Toast.makeText(context, "enMailing: Access denied. Please authenticate this device.", Toast.LENGTH_LONG).show();
					this.showToast("Access denied. "+result.getString("longmessage"));
				} else {
					Log.e(EnMailing.LOG_TAG, "Decrypt not successful: "+result.getString("longmessage"));
					//Toast.makeText(context, "Decrypt not successful: "+result.getString("message"), Toast.LENGTH_LONG).show();
					this.showToast("Decrypt not successful: "+result.getString("longmessage"));
				}
			}
		} catch (Exception e) {
			Log.e(EnMailing.LOG_TAG, "Error decrypting block: "+e.getMessage());
			return block;
		}
		return dec;
	}
	
	public String decryptString(String text) {
		try {
			ArrayList<String> encryptions = EnMailing.getEncryptions(text);
			for (int i=0; i < encryptions.size(); i++) {
				String enc = encryptions.get(i);
				String dec = this.decryptBlock(enc);
				text = text.replace(enc, dec);
			}
			return text;
		} catch (Exception e) {
			Log.e(EnMailing.LOG_TAG, "Error decrypting string: "+e.getMessage());
		}
		return text;
	}
	
	//For decrypting received messages
	//public void decryptMessage(final android.webkit.WebView view, final String text) {
	public void decryptMessage(final com.enmailing.k9.view.MessageWebView view, final String text) {
		//String username = K9.getEnMailingUsername();
		//String authkey = K9.getEnMailingAuthKey();
		String username = getUsername();
		String authkey = getAuthKey();
		
		if (username.length() < 1 || authkey.length() < 1) {
			Log.e(EnMailing.LOG_TAG, "Device not authenticated");
			this.showToast("You must authenticate this device to decrypt this message.");
			return;
		}
		new Thread() {
			public void run() {
				final String decrypted = EnMailing.this.decryptString(text);				

		        try {
					runOnUiThread(new Runnable() {
		                @Override
		                public void run() {
		                	view.setText(decrypted);
		                }
		            });
				} catch (Exception e) {
					Log.e(EnMailing.LOG_TAG, "Error updating message view: "+e.getMessage());
				}
			}
		}.start();
	}
	
	//For decrypting encryptions made in the compose window
	public void decryptMessage(final MessageCompose msg) {
		//String username = K9.getEnMailingUsername();
		//String authkey = K9.getEnMailingAuthKey();
		String username = getUsername();
		String authkey = getAuthKey();
		
		if (username.length() < 1 || authkey.length() < 1) {
			Log.e(EnMailing.LOG_TAG, "Device not authenticated");
			this.showToast("You must authenticate this device to decrypt this message.");
			return;
		}
		final String text = msg.getMessageContent().getText().toString();
		new Thread() {
			public void run() {
				final String dec = EnMailing.this.decryptString(text);
				runOnUiThread(new Runnable() {
	                @Override
	                public void run() {
	                	msg.getMessageContent().setText(dec);
	                }
	            });
			}
		}.start();
	}
	
	//Encrypts a whole message
	public void encryptMessage(final MessageCompose msg, final String group) {
		this.encryptMessage(msg, group, false);
	}
	
	//Encrypts a whole message and then sends that message
	public void encryptMessage(final MessageCompose msg, final String group, final boolean doSend) {
		final String text = msg.getMessageContent().getText().toString();
		new Thread() {
			public void run() {
				final String enc = EnMailing.this.encryptString(text, group);
				runOnUiThread(new Runnable() {
	                @Override
	                public void run() {
	                	msg.getMessageContent().setText(enc);
	                	if (doSend) {
	                		msg.sendMessage();
	                	}
	                }
	            });
			}
		}.start();
	}
	
	/*public void encryptSendMessage(final MessageCompose msg, final String group) {
		new Thread() {
			public void run() {
				messageEnc = EnMailing.this.encryptString(msg.buildText(false).getText(), group);
				runOnUiThread(new Runnable() {
	                @Override
	                public void run() {
                		msg.sendMessage();
	                }
	            });
			}
		}.start();
	}*/
	
	public String encryptStringSync(String text, String group) {
		return this.encryptString(text, group);
	}
	
	//For decrypting edit text boxes
	public void decryptEditText(final android.widget.EditText edit) {
		//String username = K9.getEnMailingUsername();
		//String authkey = K9.getEnMailingAuthKey();
		String username = getUsername();
		String authkey = getAuthKey();
		
		if (username.length() < 1 || authkey.length() < 1) {
			Log.e(EnMailing.LOG_TAG, "Device not authenticated");
			this.showToast("You must authenitcate this device to decrypt this message.");
			return;
		}
		new Thread() {
			public void run() {
				final String decrypted = EnMailing.this.decryptString(edit.getText().toString());
		        try {
					runOnUiThread(new Runnable() {
		                @Override
		                public void run() {
		                	edit.setText(decrypted);
		                }
		            });
				} catch (Exception e) {
					Log.e(EnMailing.LOG_TAG, "Error updating message view: "+e.getMessage());
				}
			}
		}.start();
	}
	
	public void encryptSelection(final EditText editor, final String group) {
		final int start = editor.getSelectionStart();
        final int end = editor.getSelectionEnd();
        //String allText = editor.getText().toString();
        final SpannableStringBuilder ssb = new SpannableStringBuilder(editor.getText());
        final String selectedText = editor.getText().toString().substring(start, end);
        new Thread() {
			public void run() {
				final String enc = EnMailing.this.encryptString(selectedText, group);
				runOnUiThread(new Runnable() {
	                @Override
	                public void run() {
	                	ssb.replace(start, end, enc);
	                	editor.setText(ssb);
	                }
	            });
			}
		}.start();
	}
	
	private String encryptString(String text, String group) {
		//final String username = K9.getEnMailingUsername();
		//final String authkey = K9.getEnMailingAuthKey();
		final String username = getUsername();
		final String authkey = getAuthKey();
		
		if (username.length() < 1 || authkey.length() < 1) {
			Log.e(EnMailing.LOG_TAG, "Device not authenticated");
			this.showToast("You must authenticate this device to decrypt this message.");
			return text;
		}
		
		Hashtable<String,String> params = new Hashtable<String,String>();
		params.put("request", "encode");
		params.put("user", username);
		params.put("authkey", authkey);
		params.put("group", group);
		
		try {
			JSONObject result = EnMailing.webRequest(params);
			if (result.getBoolean("success")) {
				String key = result.getString("key");
				String iv = result.getString("iv");
				String enc = result.getString("header")+EnMailing.this.encrypt(text, key, iv)+EnMailing.messageBumper();
				text = enc;
			} else {
				String message = result.getString("message");
				if (message.matches("access denied")) {
					Log.e(EnMailing.LOG_TAG, "Access denied. "+result.getString("longmessage"));
					EnMailing.this.showToast("Access denied. "+result.getString("longmessage"));
				} else {
					Log.e(EnMailing.LOG_TAG, "Encryption failed: "+result.getString("longmessage"));
					EnMailing.this.showToast("Encryption failed: "+result.getString("longmessage"));
				}
			}
		} catch (Exception e) {
			Log.e(EnMailing.LOG_TAG, "Error encrypting: "+e.getMessage());
		}
		return text;
	}
	
	public void refreshGroups(final android.widget.Spinner groupSpinner) {
		//String username = K9.getEnMailingUsername();
		//String authkey = K9.getEnMailingAuthKey();
		String username = getUsername();
		String authkey = getAuthKey();
		//if (context == null) context = groupSpinner.getContext();
		if (username.length() < 1 || authkey.length() < 1) {
			Log.e(EnMailing.LOG_TAG, "Device not authenticated");
			this.showToast("You must authenticate this device.");
			return;
		}
		new Thread() {
			public void run() {
				//ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.enmailing_group_default, android.R.layout.simple_spinner_item);
				final List<String> list=new ArrayList<String>();
		        //list.add("Everyone");
		        //String strGroups = "Everyone";
		        
		        //ArrayList<String> retrievedGroups = EnMailing.this.retrieveGroups();
		        EnMailing.this.retrieveGroups();
		        ArrayList<String> retrievedGroups = EnMailing.getSavedGroups();
		        
		        String group = EnMailing.getGroup();
		        int grpPos = 0;
		        for (int i=0; i < retrievedGroups.size(); i++) {
		        	String grp = retrievedGroups.get(i);
		        	//if (!grp.matches("Everyone")) list.add(grp);
		        	list.add(grp);
		        	//strGroups += "|"+grp;
		        	if (grp.matches(group)) grpPos = i;
		        }
		        //EnMailing.setSavedGroups(strGroups);
		        final int gpos = grpPos;
		        try {
					runOnUiThread(new Runnable() {
		                @Override
		                public void run() {
		                	ArrayAdapter<String> adapter=new ArrayAdapter<String>(context,
		    		                android.R.layout.simple_list_item_1,list);
		    		        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		    		        groupSpinner.setOnItemSelectedListener(null);
		                	groupSpinner.setAdapter(adapter);
		                	groupSpinner.setSelection(gpos);
		                	groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
		                    	@Override
		                    	public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
		                            EnMailing enMailing = new EnMailing(groupSpinner);
		                            enMailing.setGroup(groupSpinner.getItemAtPosition(position).toString());
		                        }
		                    	
		                    	@Override
		                        public void onNothingSelected(AdapterView<?> parentView) {
		                            // nothing to do
		                        }
		                    });
		                }
		            });
				} catch (Exception e) {
					Log.e(EnMailing.LOG_TAG, "Error updating groups spinner: "+e.getMessage());
				}
			}
		}.start();
	}
	
	//private ArrayList<String> retrieveGroups() {
	private void retrieveGroups() {
		//ArrayList<String> groups = new ArrayList<String>();
		
		Hashtable<String,String> params = new Hashtable<String,String>();
		params.put("request", "getgroups");
		params.put("user", getUsername());
		params.put("authkey", getAuthKey());
		
		try {
			JSONObject result = EnMailing.webRequest(params);
			if (result.getBoolean("success")) {
				//String strGroups = result.getString("groups");
				JSONArray arrGroups = result.getJSONArray("groups");
				/*for (int i=0; i<arrGroups.length(); i++) {
					String group = arrGroups.getString(i);
					if (!group.matches("Everyone")) {
						groups.add(group);
					}
				}*/
				EnMailing.setSavedGroups(arrGroups);

			} else {
				String message = result.getString("message");
				if (message.matches("access denied")) {
					Log.e(EnMailing.LOG_TAG, "Access denied. "+result.getString("longmessage"));
					this.showToast("Access denied. "+result.getString("longmessage"));
				} else {
					Log.e(EnMailing.LOG_TAG, "Could not retrieve groups: "+result.getString("longmessage"));
					this.showToast("Could not retrieve groups: "+result.getString("longmessage"));
				}
			}
		} catch (Exception e) {
			Log.e(EnMailing.LOG_TAG, "Error decrypting block: "+e.getMessage());
		}
		
		//return groups;
	}
	
	private static JSONObject webRequest(Hashtable<String,String> p) {
	    HttpClient client = new DefaultHttpClient();
	    HttpPost httpPost = new HttpPost(EnMailing.getApiPath());
	    
	    List<NameValuePair> params = new ArrayList<NameValuePair>(p.size());
	    Set<String> keys = p.keySet();
	    for (String key: keys) {
	    	params.add(new BasicNameValuePair(key, p.get(key)));
	    }
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
			HttpResponse response = client.execute(httpPost);
		    StatusLine statusLine = response.getStatusLine();
		    int statusCode = statusLine.getStatusCode();
		    if (statusCode == 200) {
		    	HttpEntity entity = response.getEntity();
		    	InputStream content = entity.getContent();
		    	BufferedReader reader = new BufferedReader(new InputStreamReader(content));
		    	String line;
		    	StringBuilder builder = new StringBuilder();
		    	while ((line = reader.readLine()) != null) {
		    		line = line.replaceAll("\\\\u0000", "");
		    		builder.append(line);
		    	}
		    	return new JSONObject(builder.toString());
		    } else {
		    	Log.e(EnMailing.LOG_TAG, "Failed to connect to server");
		    }
		} catch (Exception e) {
			Log.e(EnMailing.LOG_TAG, "Error making web request: "+e);
	    }
		JSONObject object = new JSONObject();
		try {
			object.put("success", Boolean.valueOf(false));
		} catch (Exception e) {
			Log.e(EnMailing.LOG_TAG, "Error creating failure JSON: "+e.getMessage());
		}
		return object;
	}
	
	private static String headerBumper() {
		return "-----enMAILING-----";
	}
	
	private static String messageBumper() {
		return "-----END-----";
	}
	
	private static byte[] hexToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
            						+ Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
	
	private void showToast(final String message) {
		try {
			runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	if (context != null) {
                		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                	} else {
                		Log.e(EnMailing.LOG_TAG, "Could not show toast messge: "+message);
                	}
                }
            });
		} catch (Exception e) {
			Log.e(EnMailing.LOG_TAG, "Error showing Toast: "+e.getMessage());
		}
	}
	
	/*public String getMessageEnc() {
		return messageEnc;
	}*/
	
	public interface EnMailingCallback {
        public void execute(Object data);
    }
	
	public void doAuth(final String username, final String password, final String devicename, final boolean fullaccess, final EnMailingCallback callback) {
		if (username.length() < 1 || password.length() < 1 || devicename.length() < 1) {
			Log.e(EnMailing.LOG_TAG, "Username, Password, and Device Description cannot be blank to authenticate");
			this.showToast("You must enter your username, password, and a device description to authenticate.");
			if (callback != null) {
				if (callback != null) callback.execute(Boolean.valueOf(false));
			}
			return;
		}
		new Thread() {
			public void run() {
				try {
					Hashtable<String,String> params = new Hashtable<String,String>();
					params.put("request", "authorizedevice");
					params.put("user", username);
					params.put("pass", password);
					params.put("devicename", devicename);
					params.put("fullaccess", String.valueOf(fullaccess));
					params.put("devicetype", EnMailing.getDeviceType());
					JSONObject result = EnMailing.webRequest(params);
					if (result.getBoolean("success")) {
						EnMailing.setAuthKey(result.getString("authkey"));
						EnMailing.setUsername(username);
						EnMailing.setDeviceName(devicename);
						EnMailing.setSavedGroups(result.getJSONArray("groups"));
						if (callback != null) callback.execute(Boolean.valueOf(true));
					} else {
						String message = result.getString("message");
						if (message.matches("access denied")) {
							Log.e(EnMailing.LOG_TAG, "Access denied. "+result.getString("longmessage"));
							EnMailing.this.showToast("Access denied. "+result.getString("longmessage"));
						} else {
							Log.e(EnMailing.LOG_TAG, "Could not authorize device: "+result.getString("longmessage"));
							EnMailing.this.showToast("Could not authorize device: "+result.getString("longmessage"));
						}
						if (callback != null) callback.execute(Boolean.valueOf(false));
					}
				} catch (Exception e) {
					Log.e(EnMailing.LOG_TAG, "Error authorizing device: "+e.getMessage());
					if (callback != null) callback.execute(Boolean.valueOf(false));
				}
			}
		}.start();
	}
	
	public void checkAuth(final EnMailingCallback callback) {
		final String username = getUsername();
		final String authkey = getAuthKey();
		if (username.length() < 1 || authkey.length() < 1) {
			Log.e(EnMailing.LOG_TAG, "Username and authorization key cannot be blank to authenticate");
			this.showToast("You must authenticate this device to use enMailing.");
			if (callback != null) {
				if (callback != null) callback.execute(Boolean.valueOf(false));
			}
			return;
		}
		new Thread() {
			public void run() {
				try {
					Hashtable<String,String> params = new Hashtable<String,String>();
					params.put("request", "authcheck");
					params.put("user", username);
					params.put("authkey", authkey);
					JSONObject result = EnMailing.webRequest(params);
					if (result.getBoolean("success")) {
						EnMailing.setDeviceName(result.getString("devicename"));
						EnMailing.setSavedGroups(result.getJSONArray("groups"));
						if (callback != null) callback.execute(Boolean.valueOf(true));
					} else {
						String message = result.getString("message");
						if (message.matches("access denied")) {
							Log.e(EnMailing.LOG_TAG, "Access denied. "+result.getString("longmessage"));
							EnMailing.this.showToast("Access denied. "+result.getString("longmessage"));
						} else {
							Log.e(EnMailing.LOG_TAG, "Could not authenticate device: "+result.getString("longmessage"));
							EnMailing.this.showToast("Could not authenticate device: "+result.getString("longmessage"));
						}
						if (callback != null) callback.execute(Boolean.valueOf(false));
					}
				} catch (Exception e) {
					Log.e(EnMailing.LOG_TAG, "Error authenticating device: "+e.getMessage());
					if (callback != null) callback.execute(Boolean.valueOf(false));
				}
			}
		}.start();
	}
	
	public void deAuthorize(final EnMailingCallback callback) {
		final String username = getUsername();
		final String authkey = getAuthKey();
		if (username.length() < 1 || authkey.length() < 1) {
			Log.e(EnMailing.LOG_TAG, "Username and authorization key cannot be blank to de-authenticate");
			//this.showToast("You must authenticate this device to use enMailing.");
			if (callback != null) {
				if (callback != null) callback.execute(Boolean.valueOf(false));
			}
			return;
		}
		new Thread() {
			public void run() {
				try {
					Hashtable<String,String> params = new Hashtable<String,String>();
					params.put("request", "deauthorizedevice");
					params.put("user", username);
					params.put("authkey", authkey);
					JSONObject result = EnMailing.webRequest(params);
					if (result.getBoolean("success")) {
						EnMailing.setDeviceName("");
						EnMailing.setUsername("");
						EnMailing.setAuthKey("");
						if (callback != null) callback.execute(Boolean.valueOf(true));
					} else {
						String message = result.getString("message");
						if (message.matches("access denied")) {
							Log.e(EnMailing.LOG_TAG, "Access denied. "+result.getString("longmessage"));
							EnMailing.this.showToast("Access denied. "+result.getString("longmessage"));
						} else {
							Log.e(EnMailing.LOG_TAG, "Could not authenticate device: "+result.getString("longmessage"));
							EnMailing.this.showToast("Could not authenticate device: "+result.getString("longmessage"));
						}
						if (callback != null) callback.execute(Boolean.valueOf(false));
					}
				} catch (Exception e) {
					Log.e(EnMailing.LOG_TAG, "Error authenticating device: "+e.getMessage());
					if (callback != null) callback.execute(Boolean.valueOf(false));
				}
			}
		}.start();
	}
	
	public static SharedPreferences getPreferences() {
		//Preferences prefs = Preferences.getPreferences(this.context);
		Preferences prefs = Preferences.getPreferences(K9.app.getApplicationContext());
		return prefs.getPreferences();
	}
	
	public static Editor getEditor() {
		return getPreferences().edit();
	}
	
	public static String getDeviceType() {
		return "android";
	}
	
	public static String getUsername() {
		return getPreferences().getString("enMailingUsername", "");
	}
	
	public static void setUsername(String user) {
		Editor edit = getEditor();
		edit.putString("enMailingUsername", user);
		edit.commit();
	}
	
	public static String getAuthKey() {
		return getPreferences().getString("enMailingAuthKey", "");
	}
	
	public static void setAuthKey(String authkey) {
		Editor edit = getEditor();
		edit.putString("enMailingAuthKey", authkey);
		edit.commit();
	}

	public static ArrayList<String> getSavedGroups() {
		//SharedPreferences preferences = this.getPrefs();
		String strGroups = getPreferences().getString("enMailingGroups", "Everyone");
		if (strGroups == null || strGroups == "") {
			ArrayList<String> groups = new ArrayList<String>(1);
			groups.add("Everyone");
			return groups;
		}
		String[] groupsArr = strGroups.split("\\|");
		ArrayList<String> groups = new ArrayList<String>(Arrays.asList(groupsArr));
		return groups;
	}
	
	public static void setSavedGroups(String strGroups) {
		String groups = "Everyone";
		String[] grpsArr = strGroups.split("\\|");
		for (int i=0; i < grpsArr.length; i++) {
			String grp = grpsArr[i];
			if (!grp.matches("Everyone")) groups += "|"+grp;
		}
		Editor editor = getEditor();
		editor.putString("enMailingGroups", groups);
		editor.commit();
	}
	
	public static void setSavedGroups(JSONArray arrGroups) {
		String groups = "Everyone";
		
		for (int i=0; i<arrGroups.length(); i++) {
			try {
				String group = arrGroups.getString(i);
				if (!group.matches("Everyone")) groups += "|"+group;
			} catch (JSONException ex) {
				Log.e(EnMailing.LOG_TAG, "Error setting groups: "+ex.getMessage());
			}
		}
		Editor editor = getEditor();
		editor.putString("enMailingGroups", groups);
		editor.commit();
	}
	
	public static String getGroup() {
		//SharedPreferences preferences = this.getPrefs();
		return getPreferences().getString("enMailingGroup", "Everyone");
	}
	
	public static void setGroup(String strGroup) {
		//SharedPreferences preferences = this.getPrefs();
		Editor editor = getEditor();//preferences.edit();
		editor.putString("enMailingGroup", strGroup);
		editor.commit();
	}
	
	public static String getDeviceName() {
		return getPreferences().getString("enMailingDeviceName", "");
	}
	
	public static void setDeviceName(String dname) {
		Editor edit = getEditor();
		edit.putString("enMailingDeviceName", dname);
		edit.commit();
	}
	
	public static boolean getNotifyOnly() {
		return getPreferences().getBoolean("enMailingNotifyOnly", false);
	}
	
	public static void setNotifyOnly(boolean notifyonly) {
		Editor edit = getEditor();
		edit.putBoolean("enMailingNotifyOnly", notifyonly);
		edit.commit();
	}
	
	public static String getApiPath() {
		return "https://api.enmailing.com/api.php";
	}
}
