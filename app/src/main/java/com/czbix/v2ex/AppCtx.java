package com.czbix.v2ex;

import android.app.Application;
import android.os.RemoteException;

import com.czbix.v2ex.common.UserState;
import com.czbix.v2ex.common.exception.ConnectionException;
import com.czbix.v2ex.dao.ConfigDao;
import com.czbix.v2ex.dao.DraftDao;
import com.czbix.v2ex.dao.NodeDao;
import com.czbix.v2ex.eventbus.BusEvent;
import com.czbix.v2ex.eventbus.BusEvent.GetNodesFinishEvent;
import com.czbix.v2ex.model.Node;
import com.czbix.v2ex.network.Etag;
import com.czbix.v2ex.network.RequestHelper;
import com.czbix.v2ex.util.ExecutorUtils;
import com.czbix.v2ex.eventbus.executor.HandlerExecutor;
import com.czbix.v2ex.util.LogUtils;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.List;

public class AppCtx extends Application {
    private static final String TAG = AppCtx.class.getSimpleName();

    private static AppCtx mInstance;
    private EventBus mEventBus;

    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;
        init();
    }

    private void init() {
        // event bus is the first
        mEventBus = new AsyncEventBus(new HandlerExecutor());
        mEventBus.register(this);

        UserState.getInstance().init();

        ExecutorUtils.execute(new AsyncInitTask());
    }

    @Subscribe
    public void onDeadEvent(DeadEvent e) {
        final BusEvent event = (BusEvent) e.getEvent();
        LogUtils.i(TAG, "dead event: %s", event.toString());
    }

    public static EventBus getEventBus() {
        return mInstance.mEventBus;
    }

    public static AppCtx getInstance() {
        return mInstance;
    }

    private class AsyncInitTask implements Runnable {
        @Override
        public void run() {
            DraftDao.cleanExpired();
            loadAllNodes();
            checkDailyAward();
        }

        private void loadAllNodes() {
            final String etagStr = ConfigDao.get(ConfigDao.KEY_NODE_ETAG, null);

            Etag etag = new Etag(etagStr);
            List<Node> result;
            try {
                result = RequestHelper.getAllNodes(etag);
            } catch (ConnectionException | RemoteException e) {
                // TODO
                e.printStackTrace();
                return;
            }

            if (etag.isModified()) {
                NodeDao.putAll(result);
                ConfigDao.put(ConfigDao.KEY_NODE_ETAG, etag.getNewEtag());
            } else {
                LogUtils.d(TAG, "nodes not modified");
            }
            LogUtils.d(TAG, "load nodes finish!");
            mEventBus.post(new GetNodesFinishEvent());
        }

        private void checkDailyAward() {
            try {
                final boolean hasAward = RequestHelper.hasDailyAward();
                if (hasAward) {
                    mEventBus.post(new BusEvent.DailyAwardEvent(true));
                }
            } catch (ConnectionException | RemoteException e) {
                // TODO
                e.printStackTrace();
                return;
            }
        }
    }
}
