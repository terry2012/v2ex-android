package com.czbix.v2ex.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.czbix.v2ex.AppCtx;
import com.czbix.v2ex.event.BaseEvent;
import com.google.common.eventbus.Subscribe;

public abstract class BaseActivity extends AppCompatActivity {
    private static final int REQ_CODE_LOADING = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Wrapper wrapper = new Wrapper();
        AppCtx.getEventBus().register(wrapper);

        if (AppCtx.getInstance().isInited()) {
            AppCtx.getEventBus().unregister(wrapper);
            return;
        }

        startActivityForResult(new Intent(this, LoadingActivity.class), REQ_CODE_LOADING);
    }

    private class Wrapper {
        @Subscribe
        public void onContextInitFinishEvent(BaseEvent.ContextInitFinishEvent e) {
            AppCtx.getEventBus().unregister(this);

            finishActivity(REQ_CODE_LOADING);
        }
    }
}
