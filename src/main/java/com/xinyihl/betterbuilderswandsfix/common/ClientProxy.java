package com.xinyihl.betterbuilderswandsfix.common;

import com.xinyihl.betterbuilderswandsfix.common.client.KeyEvents;

public class ClientProxy extends CommonProxy {
    KeyEvents keyevents;
    @Override
    public void RegisterEvents() {
        super.RegisterEvents();
        this.keyevents = new KeyEvents();
    }
}
