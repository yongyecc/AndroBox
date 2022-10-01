package cn.yongye.androbox.client.badger;

import android.content.Intent;

import cn.yongye.androbox.remote.BadgerInfo;


/**
 * @author Lody
 */
public interface IBadger {

    String getAction();

    BadgerInfo handleBadger(Intent intent);

}
