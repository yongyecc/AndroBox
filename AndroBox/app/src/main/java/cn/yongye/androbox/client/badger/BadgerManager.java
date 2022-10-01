package cn.yongye.androbox.client.badger;

import android.content.Intent;

import java.util.HashMap;
import java.util.Map;

import cn.yongye.androbox.client.ipc.VActivityManager;
import cn.yongye.androbox.remote.BadgerInfo;

/**
 * @author Lody
 */
public class BadgerManager {

    private static final Map<String, IBadger> BADGERS = new HashMap<>(10);

    static {
        addBadger(new BroadcastBadger1.AdwHomeBadger());
        addBadger(new BroadcastBadger1.AospHomeBadger());
        addBadger(new BroadcastBadger1.LGHomeBadger());
        addBadger(new BroadcastBadger1.NewHtcHomeBadger2());
        addBadger(new BroadcastBadger1.OPPOHomeBader());
        addBadger(new BroadcastBadger2.NewHtcHomeBadger1());

    }

    private static void addBadger(IBadger badger) {
        BADGERS.put(badger.getAction(), badger);
    }

    public static boolean handleBadger(Intent intent) {
        IBadger badger = BADGERS.get(intent.getAction());
        if (badger != null) {
            BadgerInfo info = badger.handleBadger(intent);
            VActivityManager.get().notifyBadgerChange(info);
            return true;
        }
        return false;
    }

}

