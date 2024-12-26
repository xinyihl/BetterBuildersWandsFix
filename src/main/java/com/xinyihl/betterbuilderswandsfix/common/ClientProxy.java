package com.xinyihl.betterbuilderswandsfix.common;

import com.xinyihl.betterbuilderswandsfix.common.client.KeyEvents;

public class ClientProxy extends CommonProxy {
    KeyEvents keyevents;
    @Override
    public void init() {
        this.keyevents = new KeyEvents();
    }
}
