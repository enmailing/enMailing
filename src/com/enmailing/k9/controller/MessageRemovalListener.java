package com.enmailing.k9.controller;

import com.enmailing.k9.mail.Message;

public interface MessageRemovalListener {
    public void messageRemoved(Message message);
}
