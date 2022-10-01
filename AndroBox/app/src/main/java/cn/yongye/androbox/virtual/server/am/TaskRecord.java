package cn.yongye.androbox.virtual.server.am;

import android.content.ComponentName;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.yongye.androbox.remote.AppTaskInfo;

class TaskRecord {
    public final List<ActivityRecord> activities = Collections.synchronizedList(new ArrayList<ActivityRecord>());
    public int taskId;
    public int userId;
    public String affinity;
    public Intent taskRoot;

    TaskRecord(int taskId, int userId, String affinity, Intent intent) {
        this.taskId = taskId;
        this.userId = userId;
        this.affinity = affinity;
        this.taskRoot = intent;
    }

    AppTaskInfo getAppTaskInfo() {
        int len = activities.size();
        if (len <= 0) {
            return null;
        }
        ComponentName top = activities.get(len - 1).component;
        return new AppTaskInfo(taskId, taskRoot, taskRoot.getComponent(), top);
    }

    public boolean isFinishing() {
        boolean allFinish = true;
        for (ActivityRecord r : activities) {
            if (!r.marked) allFinish = false;
        }
        return allFinish;
    }
}
